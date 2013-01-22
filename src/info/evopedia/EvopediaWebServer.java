package info.evopedia;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.res.AssetManager;
import android.util.Log;

public class EvopediaWebServer implements Runnable {
    private final ArchiveManager manager;
    private final AssetManager assets;
    private ServerSocket socket;
    private final Thread thread;
    private final InterLanguageLinkCache interLanguageLinks;

    private interface RequestHandler {
        public void handleRequest(Socket client, String encodedPath, List<String> pathSegments) throws IOException;
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

    public Title getTitleFromPath(String encodedPath, List<String> pathSegments) {
        if (pathSegments == null)
            pathSegments = decodePath(encodedPath);
        if (pathSegments.size() != 3 || !pathSegments.get(0).equals("wiki"))
            return null;

        String language = pathSegments.get(1);
        String articleName = pathSegments.get(2);

        LocalArchive archive = manager.getDefaultLocalArchive(language);

        if (archive == null)
            return null;

        return archive.getTitle(articleName);
    }

    public void bindSocket() {
        try {
//            socket = new ServerSocket(0, 0,
//                    InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));
// TODO make this a setting
            socket = new ServerSocket(0, 0,
                    InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 }));
            Log.i("EvopediaWebServer", "Listening on port " + socket.getLocalPort());
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
            public void handleRequest(Socket client, String encodedPath,
                            List<String> pathSegments) throws IOException {
                outputResponse(client, getAssetFile("static/index.html")    );
            }
        });

        pathMappings.put("static", new RequestHandler() {
            @Override
            public void handleRequest(Socket client, String encodedPath,
                            List<String> pathSegments) throws IOException {
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
            private void tryRedirectToCorrectURL(Socket client, String encodedPath,
                        List<String> pathSegments) throws IOException {
                String articleName;
                if (pathSegments.size() >= 3) {
                    /* redirect to Wikipedia */
                    outputRedirect(client, encodedPath.replaceFirst("/wiki/([^/]*)/(.*)",
                                                         "http://$1.wikipedia.org/wiki/$2"));
                    return;
                } else {
                    /* language not specified, try to find title in any local archive */
                    articleName = pathSegments.get(1);
                    for (LocalArchive a : manager.getDefaultLocalArchives().values()) {
                        if (a.getTitle(articleName) != null) {
                            outputRedirect(client, URLEncoder.encode("/wiki/" + a.getLanguage() + "/" + articleName,
                                                                     "utf-8"));
                            return;
                        }
                    }
                }
                outputHttpHeader(client, "404");
            }

            @Override
            public void handleRequest(Socket client, String encodedPath,
                    List<String> pathSegments) throws IOException {
                if (pathSegments.size() < 2) {
                    outputHttpHeader(client, "404");
                    return;
                }

                Title t = getTitleFromPath(encodedPath, pathSegments);
                if (t == null) {
                    tryRedirectToCorrectURL(client, encodedPath, pathSegments);
                    return;
                }

                byte[] article;
                try {
                    article = t.getArchive().getArticle(t);
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
                ByteArrayOutputStream data = new ByteArrayOutputStream(article.length);
                data.write(getHTMLHeader(t.getReadableName()));
                data.write("</div>".getBytes());

                extractInterLanguageLinksAndWrite(article, encodedPath, data);

                getAssetFile("footer.html").writeTo(data);
                outputResponse(client, data, "text/html; charset=\"utf-8\"");
            }
        };
        pathMappings.put("wiki", articleRequestHandler);
        pathMappings.put("articles", articleRequestHandler);

        pathMappings.put("math", new RequestHandler() {
            @Override
            public void handleRequest(Socket client, String encodedPath,
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

    private List<String> decodePath(String path) {
        if (path.startsWith("/"))
            path = path.substring(1);

        List<String> parts = new ArrayList<String>();
        for (String s : path.split("/")) {
            try {
                parts.add(URLDecoder.decode(s, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                parts.add(s);
            }
        }
        return parts;
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
            String path = tokens[1];
            Log.i("EvopediaWebServer", "Got request: " + path);

            List<String> pathSegments = decodePath(path);

            RequestHandler handler = pathMappings.get(pathSegments.size() > 0 ? pathSegments.get(0) : "");
            if (handler == null) {
                outputHttpHeader(client, "404");
            } else {
                handler.handleRequest(client, path, pathSegments);
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

    private void extractInterLanguageLinksAndWrite(byte[] articleData, String path, ByteArrayOutputStream out)
                    throws IOException {
        String article = new String(articleData);

        final int langStart = article.lastIndexOf("<h5>");
        if (langStart < 0 || article.indexOf("<div class=\"pBody\">", langStart) < 0) {
            out.write(articleData);
            return;
        }

        final int langEnd = article.indexOf("<div class=\"visualClear\"></div>", langStart);
        if (langEnd < 0) {
            out.write(articleData);
            return;
        }

        out.write(article.substring(0, langStart).getBytes());
        out.write(article.substring(langEnd).getBytes());

        article = article.substring(langStart, langEnd);
        ArrayList<InterLanguageLink> links = new ArrayList<InterLanguageLink>();

        final Pattern p = Pattern.compile("<a href=\"(\\./)?\\.\\./([^/]*)/([^\"]*)\">([^<]*)</a>");
        final Matcher m = p.matcher(article);
        while (m.find()) {
            String languageID = m.group(2);
            String languageName = m.group(4);
            String articleName = m.group(3);
            links.add(new InterLanguageLink(languageID, languageName, articleName));
        }
        interLanguageLinks.setLinks(path, links);
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

    private void outputRedirect(Socket client, String target) throws IOException {
        client.getOutputStream().write(("HTTP/1.0 302 Ok\r\nLocation: " +
                                            target).getBytes());
    }


    private void outputResponse(Socket client, ByteArrayOutputStream response, String contentType) throws IOException {
        String header = "HTTP/1.1 200 Ok\r\n" +
                        "Content-Type: " + contentType + "\r\n" +
                        "Content-Length: " + response.size() + "\r\n" +
                        "\r\n";
        client.getOutputStream().write(header.getBytes());
        response.writeTo(client.getOutputStream());
    }

    private void outputResponse(Socket client, ByteArrayOutputStream response) throws IOException {
        outputResponse(client, response, "text/html; charset=\"utf-8\"");
    }

    private void outputResponse(Socket client, byte[] response, String contentType) throws IOException {
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
        String header = getAssetFile("header.html").toString();
        header = header.replace("TITLE", title); /* TODO escape: Html.escapeHtml(title)); */
        return header.getBytes();
    }

    private ByteArrayOutputStream getAssetFile(String name) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] buf = new byte[512];
        InputStream str = assets.open(name);
        while (true) {
            int n = str.read(buf);
            if (n < 0) {
                return data;
            } else {
                data.write(buf, 0, n);
            }
        }
    }

    public int getPort() {
        return socket.getLocalPort();
    }
}
