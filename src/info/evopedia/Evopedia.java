package info.evopedia;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Random;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;

public class Evopedia extends Application {
    private ArchiveManager archiveManager;
    private EvopediaWebServer webServer;

    public void onCreate() {
        registerExceptionHandler();
        registerStart();

        archiveManager = ArchiveManager.getInstance(this);
        webServer = new EvopediaWebServer(archiveManager, getAssets());
    }

    private void registerExceptionHandler() {
        final UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        final Context context = getApplicationContext();
        String versionName = "UNKNOWN";
        final File filePath = context.getFilesDir().getAbsoluteFile();
        if (!filePath.exists())
            filePath.mkdirs();

        try {
            PackageInfo packageInfo = context.getPackageManager()
                            .getPackageInfo(context.getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (NameNotFoundException e) {
        }
        final String packageInfo = "Version: " + versionName + "\n" +
                       "Device: " + android.os.Build.MODEL + "\n" +
                       "Android version: " + android.os.Build.VERSION.RELEASE +
                       " (SDK " + android.os.Build.VERSION.SDK + ")\n";

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread thread, final Throwable ex) {
                try {
                    final StringWriter stackTrace = new StringWriter();
                    ex.printStackTrace(new PrintWriter(stackTrace));
                    String crashReport = "Crash date: " + (new Date()).toString() + "\n" +
                                         packageInfo +
                                         "\nStack trace:\n" + stackTrace.toString();
                    FileWriter w = new FileWriter(new File(filePath,
                                            "crashreport_" + crashReport.hashCode() + ".txt"));
                    w.write(crashReport);
                    w.close();
                } catch (Exception e) {
                    Log.e("Evopedia", "Exception occurred in UncaughtExceptionHandler: " + e.toString());
                }
                oldHandler.uncaughtException(thread, ex);
            }
        });
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
