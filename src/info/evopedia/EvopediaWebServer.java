package info.evopedia;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.util.ByteArrayBuffer;

import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

/* TODO general idea:
 * is it possible to register a new url handler so that
 * we could retrieve article data via an intent call? 
 */

public class EvopediaWebServer implements Runnable {
    private ArchiveManager manager;
    private AssetManager assets;
    private ServerSocket socket;
    private Thread thread;

    private interface RequestHandler {
        public void handleRequest(Socket client, Uri uri,
                List<String> pathSegments) throws IOException;
    }

    private Map<String, RequestHandler> pathMappings;

    public EvopediaWebServer(ArchiveManager archiveManager, AssetManager assets) {
        this.manager = archiveManager;
        this.assets = assets;

        initializePathMappings();
        bindSocket();

        thread = new Thread(this);
        thread.start();
    }

    public void bindSocket() {
        try {
            socket = new ServerSocket(0, 0,
                    InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));
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
                /* TODO */
            }
        });

        pathMappings.put("static", new RequestHandler() {
            @Override
            public void handleRequest(Socket client, Uri uri,
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
                outputResponse(client, getAssetFile("/static/" + pathSegments.get(1)), contentType);
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

                String articleName = pathSegments.get(pathSegments.size() - 1);
                String language = pathSegments.size() >= 3 ? pathSegments.get(1) : null;

                LocalArchive archive = manager.getDefaultLocalArchive(language);

                if (archive == null) {
                    redirectToCorrectURL(client, language, articleName, pathSegments);
                    return;
                }

                byte[] article = archive.getArticle(archive.getTitle(articleName));
                if (article == null) {
                    /* TODO could be the url of an image or data file, redirect to online
                     * wikipedia in this case */
                    outputHttpHeader(client, "404");
                    return;
                }
                /* TODO header stuff */
                /* TODO we have too much copying going on here and in outputResponse */
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                data.write(getHTMLHeader());
                /* TODO some links */
                data.write("</div>")
                /* TODO rtl */
                data.write(article);
                data.write(getAssetFile("footer.html"));
                outputResponse(client, data.toByteArray(), "text/html; charset=\"utf-8\"");
            }
        };
        pathMappings.put("wiki", articleRequestHandler);
        pathMappings.put("articles", articleRequestHandler);

        /*
         * TODO math random searchsuggest search settings
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

            RequestHandler handler = pathMappings.get(pathSegments.get(0));
            if (handler == null) {
                outputHttpHeader(client, "404");
            } else {
                handler.handleRequest(client, uri, pathSegments);
            }
            client.close();
        } catch (IOException exc) {
        } finally {
            try {
                client.close();
            } catch (IOException e) {
            }
        }
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

    private byte[] getHTMLHeader() throws IOException {
        String header = new String(getAssetFile("header.html"));
        header = header.replace("OPENSEARCHHEADERS", ""); /* TODO */
        return header.enc
        /* TODO replace opensearch stuff */
        return 
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
                data.write(buf);
            }
        }
    }

    public int getPort() {
        return socket.getLocalPort();
    }
}
