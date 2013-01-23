package info.evopedia;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

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

    public URL getServerURL() {
        try {
            return new URL("http", "127.0.0.1", webServer.getPort(), "");
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public URL getArticleURL(Title title) {
        try {
            return new URL(getServerURL(), "/wiki/" +
                        URLEncoder.encode(title.getArchive().getLanguage(), "utf-8") +
                        "/" + URLEncoder.encode(title.getName(), "utf-8"));
        } catch (MalformedURLException e) {
            return null;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
