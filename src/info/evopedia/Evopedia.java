package info.evopedia;

import java.util.Locale;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;

public class Evopedia extends Application {
    private ArchiveManager archiveManager;
    private EvopediaWebServer webServer;

    public void onCreate() {
        Intent intent = new Intent(this, EvopediaService.class);
        startService(intent);

        archiveManager = ArchiveManager.getInstance(this);
        webServer = new EvopediaWebServer(archiveManager, getAssets());
    }

    public ArchiveManager getArchiveManager() {
        return archiveManager;
    }

    public EvopediaWebServer getWebServer() {
        return webServer;
    }

    public Uri getServerUri() {
        /* TODO is this really they way to construct URIs? */
        return Uri.parse(String.format(Locale.US,
                            "http://127.0.0.1:%d", webServer.getPort()));
    }

    public Uri getArticleUri(Title title) {
        /* TODO is this really they way to construct URIs? */
        String uri = String.format(Locale.US, "http://127.0.0.1:%d/wiki/%s/%s",
                webServer.getPort(),
                Uri.encode(title.getArchive().getLanguage()),
                Uri.encode(title.getName()));
        return Uri.parse(uri);
    }
}
