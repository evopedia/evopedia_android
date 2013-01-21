package info.evopedia;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.net.Uri;
import android.text.Html;
import android.util.Log;

/* TODO general idea:
 * is it possible to register a new url handler so that
 * we could retrieve article data via an intent call? 
 */

public class EvopediaWebServer implements Runnable {
    private final ArchiveManager manager;
    private final AssetManager assets;
    private ServerSocket socket;
    private final Thread thread;
    private final InterLanguageLinkCache interLanguageLinks;

    private interface RequestHandler {
        public void handleRequest(Socket client, Uri uri,
                List<String> pathSegments) throws IOException;
    }

    private Map<String, RequestHandler> pathMappings;

    public EvopediaWebServer(ArchiveManager archiveManager, AssetManager assets) {
        this.manager = archiveManager;
        this.assets = assets;

        interLanguageLinks = new InterLanguageLinkCache();

        initializePathMappings();
        bindSocket();

        thread = new Thread(this);
        thread.start();
    }

    public List<InterLanguageLink> getInterLanguageLinks(String articleUrl) {
        return interLanguageLinks.getLinks(articleUrl);
    }

    public Title getTitleFromURL(String url) {
        List<String> parts = Uri.parse(url).getPathSegments();
        if (parts.size() != 3 || !parts.get(0).equals("wiki"))
            return null;

        String language = parts.get(1);
        String articleName = parts.get(2);

        LocalArchive archive = manager.getDefaultLocalArchive(language);

        if (archive == null)
            return null;

        return archive.getTitle(articleName);
    }

    public void bindSocket() {
        try {
            socket = new ServerSocket(0, 0,
                    InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));
// TODO make this a setting
//            socket = new ServerSocket(0, 0,
//                    InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 }));
        } catch (UnknownHostException e) {
            socket = null;
        } catch (IOException e) {
            socket = null;
        }
    }

    @Override
    public void run() {
        while (true) {
            Socket client;
            try {
                client = socket.accept();
            } catch (IOException e) {
                client = null;
            }
            if (client == null)
                continue;
            handleRequest(client);
        }
    }

    private void initializePathMappings() {
        pathMappings = new HashMap<String, EvopediaWebServer.RequestHandler>();

        pathMappings.put("", new RequestHandler() {
            @Override
            public void handleRequest(Socket client, Uri uri,
                    List<String> pathSegments) throws IOException {
                outputResponse(client, getAssetFile("static/index.html")    );
            }
        });

        pathMappings.put("static", new RequestHandler() {
            @Override
            public void handleRequest(Socket client, Uri uri,
                    List<String> pathSegments) throws IOException {
                /* TODO use browser cache! */
                if (pathSegments.size() < 2) {
                    outputHttpHeader(client, "404");
                    return;
                }

                String contentType = "image/png";
                if (pathSegments.get(1).equals("main.css")) {
                    contentType = "text/css";
                } else if (pathSegments.get(1).equals("evopedia.js")) {
                    contentType = "text/javascript";
                }
                outputResponse(client, getAssetFile("static/" + pathSegments.get(1)), contentType);
            }
        });

        RequestHandler articleRequestHandler = new RequestHandler() {
            private void redirectToCorrectURL(Socket client,
                    String language, String articleName,
                    List<String> pathSegments) throws IOException {
                if (language != null && language.length() > 1
                        /* TODO and network connection allowed */) {
                        /* TODO redirect to online Wikipedia */
                        /*
                        String language = pathSegments.get(1);
                        outputRedirect(client, Uri.fromParts("http",
                                "//" + language + ".wikipedia.org/wiki/" +
                                pathSegments.subList(1, pathSegments.size()).join("/"), null));
                        */
                    return;
                }
                /* language not specified, try to find title in any local archive */
                for (LocalArchive a : manager.getDefaultLocalArchives().values()) {
                    if (a.getTitle(articleName) != null) {
                        outputRedirect(client, Uri.parse("/wiki/" + a.getLanguage() + "/" + articleName));
                        return;
                    }
                }
                outputHttpHeader(client, "404");
            }

            @Override
            public void handleRequest(Socket client, Uri uri,
                    List<String> pathSegments) throws IOException {
                /* TODO add misplaced math image workaround (bug still there?) */
                if (pathSegments.size() < 2) {
                    outputHttpHeader(client, "404");
                    return;
                }
                if (pathSegments.get(1).equals("math")) {
                    /* this is actually a math image */
                    /* TODO redirect */
                    return;
                }

                /* TODO we should use getTitleFromURL().resolveRedirect(), but then we would not be
                 * able to retrieve articleName and language if there is an error
                 */
                String articleName;
                String language;

                if (uri.getEncodedPath().endsWith("/")) {
                    articleName = "";
                    language = pathSegments.size() >= 2 ? pathSegments.get(1) : null;
                } else {
                    articleName = pathSegments.get(pathSegments.size() - 1);
                    language = pathSegments.size() >= 3 ? pathSegments.get(1) : null;
                }

                /* TODO this does not work if language is null! */
                LocalArchive archive = manager.getDefaultLocalArchive(language);

                if (archive == null) {
                    redirectToCorrectURL(client, language, articleName, pathSegments);
                    return;
                }

                Title t = archive.getTitle(articleName).resolveRedirect();
                byte[] article;
                try {
                    article = archive.getArticle(t);
                } catch (OutOfMemoryError e) {
                    /* TODO nicer error page, ask browser to release memory? */
                    outputHttpHeader(client, "500");
                    return;
                }
                if (article == null) {
                    /* TODO could be the url of an image or data file, redirect to online
                     * wikipedia in this case */
                    outputHttpHeader(client, "404");
                    return;
                }
                article = extractInterLanguageLinks(article, uri);
                /* TODO header stuff */
                /* TODO we have too much copying going on here,
                 * in extractInterLanguageLinks and in outputResponse.
                 * try to stream the data out (possibly even from the gzip routine). */
                ByteArrayOutputStream data = new ByteArrayOutputStream(article.length);
                data.write(getHTMLHeader(t.getReadableName()));
                /* TODO some links */
                data.write("</div>".getBytes());
                /* TODO rtl */
                data.write(article);
                data.write(getAssetFile("footer.html"));
                outputResponse(client, data.toByteArray(), "text/html; charset=\"utf-8\"");
            }
        };
        pathMappings.put("wiki", articleRequestHandler);
        pathMappings.put("articles", articleRequestHandler);

        pathMappings.put("math", new RequestHandler() {
            @Override
            public void handleRequest(Socket client, Uri uri,
                    List<String> pathSegments) throws IOException {
                String hexStr = pathSegments.get(pathSegments.size() - 1).substring(0, 32);

                byte[] hash;
                try {
                    hash = Utils.decodeHexString(hexStr);
                } catch (IllegalArgumentException exc) {
                    outputHttpHeader(client, "404");
                    return;
                }
                for (LocalArchive a : manager.getDefaultLocalArchives().values()) {
                    byte[] data = a.getMathImage(hash);
                    if (data != null) {
                        outputResponse(client, data, "image/png");
                        return;
                    }
                }
                outputHttpHeader(client, "404");
            }
        });


        /*
         * TODO random searchsuggest search settings
         * select_archive_location add_archive exit map opensearch
         */
    }

    private void handleRequest(Socket client) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    client.getInputStream()));

            String[] tokens = reader.readLine().split(" ");
            if (!tokens[0].equals("GET") || tokens.length < 2) {
                outputHttpHeader(client, "404");
                return;
            }
            Uri uri = Uri.parse(tokens[1]);
            Log.i("EvopediaWebServer", "Got request: " + uri.toString());

            List<String> pathSegments = uri.getPathSegments();

            RequestHandler handler = pathMappings.get(pathSegments.size() > 0 ? pathSegments.get(0) : "");
            if (handler == null) {
                outputHttpHeader(client, "404");
            } else {
                handler.handleRequest(client, uri, pathSegments);
            }
        } catch (IOException exc) {
            Log.e("EvopediaWebServer", "IOException", exc);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
            }
        }
    }

    private byte[] extractInterLanguageLinks(byte[] articleData, Uri uri) {
        final String article = new String(articleData);

        final int langStart = article.lastIndexOf("<h5>");
        if (langStart < 0 || article.indexOf("<div class=\"pBody\">", langStart) < 0)
            return articleData;

        final int langEnd = article.indexOf("<div class=\"visualClear\"></div>", langStart);
        if (langEnd < 0)
            return articleData;

        ArrayList<InterLanguageLink> links = new ArrayList<InterLanguageLink>();

        final Pattern p = Pattern.compile("<a href=\"(\\./)?\\.\\./([^/]*)/([^\"]*)\">([^<]*)</a>");
        final Matcher m = p.matcher(article.substring(langStart, langEnd));
        while (m.find()) {
            String languageID = m.group(2);
            String languageName = m.group(4);
            String articleName = m.group(3);
            links.add(new InterLanguageLink(languageID, languageName, articleName));
        }
        interLanguageLinks.setLinks(uri.toString(), links);

        byte[] cleanedArticleData = new byte[articleData.length - (langEnd - langStart)];
        System.arraycopy(articleData, 0, cleanedArticleData, 0,
                         langStart);
        System.arraycopy(articleData, langEnd, cleanedArticleData, langStart,
                         articleData.length - langEnd);

        return cleanedArticleData;
    }

    private void outputHttpHeader(Socket client, String code, String contentType)
            throws IOException {
        String str = "HTTP/1.0 " + code + " Ok\r\n" + "Content-type: "
                + contentType + "\r\n\r\n";
        client.getOutputStream().write(str.getBytes());
    }

    private void outputHttpHeader(Socket client, String code)
            throws IOException {
        outputHttpHeader(client, code, "text/html; charset=\"utf-8\"");
    }

    private void outputHttpHeader(Socket client) throws IOException {
        outputHttpHeader(client, "200", "text/html; charset=\"utf-8\"");
    }

    private void outputRedirect(Socket client, Uri target) throws IOException {
        client.getOutputStream().write(
                ("HTTP/1.0 302 Ok\r\nLocation: " +
                        target.toString()).getBytes());
    }


    private void outputResponse(Socket client, byte[] response, String contentType) throws IOException {
        /* TODO cache issues */
        String header = "HTTP/1.1 200 Ok\r\n" +
                        "Content-Type: " + contentType + "\r\n" +
                        "Content-Length: " + response.length + "\r\n" +
                        "\r\n";
        client.getOutputStream().write(header.getBytes());
        client.getOutputStream().write(response);
    }

    private void outputResponse(Socket client, byte[] response) throws IOException {
        outputResponse(client, response, "text/html; charset=\"utf-8\"");
    }

    private byte[] getHTMLHeader(String title) throws IOException {
        String header = new String(getAssetFile("header.html"));
        header = header.replace("TITLE", title); /* TODO escape: Html.escapeHtml(title)); */
        return header.getBytes();
    }

    private byte[] getAssetFile(String name) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] buf = new byte[512];
        InputStream str = assets.open(name);
        while (true) {
            int n = str.read(buf);
            if (n < 0) {
                return data.toByteArray();
            } else {
                data.write(buf, 0, n);
            }
        }
    }

    public int getPort() {
        return socket.getLocalPort();
    }
}
