package info.evopedia;

import java.util.Locale;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;

/*
 * TODO Browser: reload button
 * TODO browser: zoom - perhaps simply as setting (initial zoom in html header)
 * TODO browser: back navigation
 * TODO translation
 * TODO menu: "fullscreen" (hide action bar)?
 * TODO only show the search button if there are archives
 */

public class Evopedia extends Application {
    private ArchiveManager archiveManager;
    private EvopediaWebServer webServer;

    public void onCreate() {
        Intent intent = new Intent(this, EvopediaService.class);
        startService(intent);

        registerStart();

        archiveManager = ArchiveManager.getInstance(this);
        webServer = new EvopediaWebServer(archiveManager, getAssets());
    }

    private void registerStart() {
        SharedPreferences p = getSharedPreferences("evopedia", 0);
        Editor e = p.edit();
        int numRuns = p.getInt("num_runs", 0);
        e.putInt("num_runs", numRuns + 1);
        e.commit();
    }

    public int getNumApplicationRuns() {
        SharedPreferences p = getSharedPreferences("evopedia", 0);
        return p.getInt("num_runs", 0);
    }

    public void setFeedbackReminderShown() {
        SharedPreferences p = getSharedPreferences("evopedia", 0);
        Editor e = p.edit();
        e.putBoolean("feedback_reminder_shown", true);
        e.commit();
    }

    public boolean wasFeedbackReminderShown() {
        SharedPreferences p = getSharedPreferences("evopedia", 0);
        return p.getBoolean("feedback_reminder_shown", false);
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
