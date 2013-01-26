package info.evopedia;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.actionbarsherlock.view.Window;

public class MainActivity extends SherlockFragmentActivity implements
        OnActionExpandListener, EvopediaSearch.OnTitleSelectedListener {
    private Evopedia evopedia;
    private WebView webView;
    private MenuItem searchMenuItem;
    private MenuItem onlineArticleMenuItem;
    private MenuItem otherLanguagesMenuItem;
    private boolean expandSearchMenuItem = false;
    private EvopediaSearch evopediaSearch;
    private List<InterLanguageLink> currentInterLanguageLinks; 
    private Title currentTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);

        evopedia = (Evopedia) getApplication();
        evopediaSearch = new EvopediaSearch(this, this);

        setContentView(R.layout.main_activity);
        setupWebView();
        if (savedInstanceState != null)
            webView.restoreState(savedInstanceState);

        evopediaSearch.setTitleListView((ListView) findViewById(R.id.titleListView));

        onNewIntent(getIntent());

        checkAndSendCrashReports();
        checkFeedbackReminder();
    }

    private void setupWebView() {
        webView = (WebView) findViewById(R.id.webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        /* TODO setting?
        webView.getSettings().setBlockNetworkImage(true); */

        final SherlockFragmentActivity activity = this;
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress < 100) {
                    activity.setSupportProgressBarVisibility(true);
                }
                activity.setSupportProgress(progress * 100);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                getSupportActionBar().setTitle(title + " - Evopedia");
                updateCurrentArticleFromView();
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            /* TODO we could perhaps go completely without a web server by
             * also overriding shouldInterceptRequest (from api 11 on...)
             */
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://127.0.0.1")) {
                    return false;
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
            }
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                currentInterLanguageLinks = null;
                currentTitle = null;
                if (otherLanguagesMenuItem != null)
                    otherLanguagesMenuItem.setVisible(false);
                if (onlineArticleMenuItem != null)
                    onlineArticleMenuItem.setVisible(false);
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                updateCurrentArticleFromView();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.main_activity, menu);
        onlineArticleMenuItem = menu.findItem(R.id.menu_online_article);
        otherLanguagesMenuItem = menu.findItem(R.id.menu_other_languages);
        searchMenuItem = menu.findItem(R.id.menu_search_view);
        searchMenuItem.setOnActionExpandListener(this);
        if (expandSearchMenuItem) {
            searchMenuItem.expandActionView();
            expandSearchMenuItem = false;
        }

        evopediaSearch.setEditText((EditText) searchMenuItem.getActionView()
                                        .findViewById(R.id.searchEditText));

        return true;
    }

    private void checkAndSendCrashReports() {
        final File filesDir = getApplicationContext().getFilesDir().getAbsoluteFile();
        File[] stackTraces = filesDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith("crashreport_");
            }
        });
        if (stackTraces.length == 0)
            return;

        String crashReport = "";
        for (File trace : stackTraces) {
            try {
                crashReport += "\n\n--------------------------------\n" +
                        Utils.readInputStream(new FileInputStream(trace));
            } catch (IOException e) {
            }
            trace.delete();
        }
        if (crashReport.length() == 0)
            return;

        final String finalCrashReport = crashReport;
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Application crashed");
        builder.setMessage("Unfortunately, Evopedia crashed last time. " +
                           "We are very sorry about that but with your " +
                           "permission, information about the crash can be sent " +
                           "to the developers so they can fix this bug.");
        builder.setNegativeButton("Close", null);
        builder.setPositiveButton("Send report", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent(Intent.ACTION_SENDTO);
                i.setData(Uri.parse("mailto:devs@evopedia.info"));
                i.putExtra(Intent.EXTRA_SUBJECT, "Evopedia Crash Report");
                i.putExtra(Intent.EXTRA_TEXT,
                        "Dear developers,\n"
                                + "please help, Evopedia just crashed!\n"
                                + "(you can add additional information if you like)\n"
                                + "\n"
                                + finalCrashReport);
                startActivity(i);
            }
        });
        builder.show();
    }

    @Override
    public boolean onSearchRequested() {
        return onSearchRequested("");
    }

    public boolean onSearchRequested(String query) {
        if (searchMenuItem == null) {
            expandSearchMenuItem = true;
        } else {
            searchMenuItem.expandActionView();
        }
        evopediaSearch.setSearchPrefix(query);
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (Integer.valueOf(VERSION.SDK) < 5 && keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    private void updateCurrentArticleFromView() {
        EvopediaWebServer webServer = evopedia.getWebServer();
        try {
            String path = (new URL(webView.getUrl())).getPath();
            currentTitle = webServer.getTitleFromPath(path, null);
            currentInterLanguageLinks = webServer.getInterLanguageLinks(path);
        } catch (MalformedURLException e) {
            currentTitle = null;
            currentInterLanguageLinks = null;
        }
        if (currentInterLanguageLinks != null && !currentInterLanguageLinks.isEmpty())
            otherLanguagesMenuItem.setVisible(true);
        if (currentTitle != null)
            onlineArticleMenuItem.setVisible(true);
    }

    private void loadArticle(Title t) {
        if (t == null) {
            /* TODO show some not-found page */
        } else {
            Evopedia evopedia = (Evopedia) getApplication();
            webView.loadUrl(evopedia.getArticleURL(t).toString());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_MAIN)) {
            ArchiveManager manager = ArchiveManager.getInstance(this);
            if (manager.getDefaultLocalArchives().isEmpty()) {
                showInitialPage();
            } else if (webView.canGoBack()) {
                // we rather display the last page (restored from the bundle)
            } else {
                onSearchRequested();
            }
        } else if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
            onSearchRequested(intent.getStringExtra(SearchManager.QUERY));
        } else if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            Title t = Title.fromUri(intent.getData());
            loadArticle(t);
        }
    }

    private void showInitialPage() {
        Evopedia evopedia = (Evopedia) getApplication();
        webView.loadUrl(evopedia.getServerURL().toString());
    }

    private void checkFeedbackReminder() {
        Evopedia evopedia = (Evopedia) getApplication();
        if (evopedia.getNumApplicationRuns() < 10
                || evopedia.wasFeedbackReminderShown())
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You have started evopedia at least ten times now.\n"
                + "We hope you enjoy it!\n"
                + "It would be great if you could give us some feedback "
                + "about your experience with the software.\n"
                + "(You can do so at any later time via the menu.)");
        builder.setTitle("Send Feedback");
        builder.setPositiveButton("Send Feedback",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendFeedback();
                    }
                });
        builder.setNegativeButton("Maybe Later", null);
        AlertDialog dialog = builder.create();
        dialog.show();

        evopedia.setFeedbackReminderShown();
    }

    private void sendFeedback() {
        Intent i = new Intent(Intent.ACTION_SENDTO);
        i.setData(Uri.parse("mailto:devs@evopedia.info"));
        i.putExtra(Intent.EXTRA_SUBJECT, "Evopedia Betatesting Feedback");
        i.putExtra(
                Intent.EXTRA_TEXT,
                "Dear developers,\n"
                        + " - I tested evopedia and have the following remarks:\n"
                        + "\n"
                        + " - I was able to download an archive:\n"
                        + "(if not, what were the problems?)\n"
                        + "\n"
                        + " - I found the following bugs:\n"
                        + "(please provide detailed information)\n"
                        + "\n"
                        + " - The following improvements would be great:\n"
                        + "\n"
                        + " - I found the \"global search\" feature and have the following remarks:\n"
                        + "\n" + " - General ideas/remarks:\n" + "\n"
                        + " - I used the following devices for testing:\n" + "\n"
                        + " - I learned about evopedia as follows:\n" + "\n"
                        + " - General rating (1-5):\n" + "\n");
        startActivity(i);
    }

    private void showOtherLanguagePicker() {
        if (currentInterLanguageLinks == null)
            return;

        final Set<String> localLanguages = evopedia.getArchiveManager()
                        .getDefaultLocalArchives().keySet();
        final ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(
                this, android.R.layout.simple_dropdown_item_1line);
        for (InterLanguageLink l : currentInterLanguageLinks) {
            if (localLanguages.contains(l.getLanguageID())) {
                adapter.add(l);
            }
        }
        adapter.add(""); /* TODO non-selectable plus info: "Online Wikipedia" */
        for (InterLanguageLink l : currentInterLanguageLinks) {
            if (!localLanguages.contains(l.getLanguageID())) {
                adapter.add(l);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.choose_language);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Object item = adapter.getItem(which);
                if (item instanceof InterLanguageLink) {
                    InterLanguageLink l = (InterLanguageLink) item;
                    webView.loadUrl(evopedia.getServerURL().toString() +
                            "/wiki/" + l.getLanguageID() + '/' +
                            l.getArticleName());
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan_for_archives:
                String storageState = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(storageState)
                        || Environment.MEDIA_MOUNTED_READ_ONLY
                                .equals(storageState)) {
                    LocalArchiveSearcher searcher = new LocalArchiveSearcher(
                            this);
                    searcher.execute(Environment.getExternalStorageDirectory());
                } else {
                    Toast.makeText(this, R.string.external_storage_not_mounted_,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_other_languages:
                showOtherLanguagePicker();
                return true;
            case R.id.menu_online_article:
                if (currentTitle != null) {
                    startActivity(new Intent(Intent.ACTION_VIEW, currentTitle.getOrigUri()));
                }
                return true;
            case R.id.menu_send_feedback:
                sendFeedback();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        findViewById(R.id.webView).setVisibility(View.GONE);
        findViewById(R.id.titleListView).setVisibility(View.VISIBLE);
        /* TODO focus? */
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        findViewById(R.id.webView).setVisibility(View.VISIBLE);
        findViewById(R.id.titleListView).setVisibility(View.GONE);
        return true;
    }

    @Override
    public void onTitleSelected(Title title) {
        searchMenuItem.collapseActionView();
        Intent intent = new Intent(Intent.ACTION_VIEW, title.toUri());
        startActivity(intent);
    }
}
