package info.evopedia;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
    private WebView webView;
    private MenuItem searchMenuItem;
    private boolean expandSearchMenuItem = false;
    private EvopediaSearch evopediaSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);

        evopediaSearch = new EvopediaSearch(this, this);

        setContentView(R.layout.main_activity);
        setupWebView();
        if (savedInstanceState != null)
            webView.restoreState(savedInstanceState);

        evopediaSearch.setTitleListView((ListView) findViewById(R.id.titleListView));

        onNewIntent(getIntent());

        checkFeedbackReminder();
    }

    private void setupWebView() {
        webView = (WebView) findViewById(R.id.webView);

        // webView.getSettings().setJavaScriptEnabled(true);
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
                activity.getWindow().setTitle(title + " - evopedia");
            }
        });
        webView.setWebViewClient(new WebViewClient() {
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
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.main_activity, menu);
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
        /* TODO why is this not done automatically? */
        webView.saveState(outState);
        Log.i("mainactivity", outState.toString());
    }

    private void loadArticle(Title t) {
        if (t == null) {
            /* TODO show some not-found page */
        } else {
            Evopedia evopedia = (Evopedia) getApplication();
            webView.loadUrl(evopedia.getArticleUri(t).toString());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_MAIN)) {
            ArchiveManager manager = ArchiveManager.getInstance(this);
            if (manager.getDefaultLocalArchives().isEmpty()) {
                showInitialPage();
            } else {
                // we rather display the last page (restored from the bundle)
                //onSearchRequested();
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
        webView.loadUrl(evopedia.getServerUri().toString());
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
                        + "I tested evopedia and have the following remarks:\n"
                        + "\n"
                        + "I was able to download an archive:\n"
                        + "(if not, what were the problems?)\n"
                        + "\n"
                        + "I found the following bugs:\n"
                        + "(please provide detailed information)\n"
                        + "\n"
                        + "The following improvements would be great:\n"
                        + "\n"
                        + "I found the \"global search\" feature and have the following remarks:\n"
                        + "\n" + "General ideas/remarks:\n" + "\n"
                        + "I used the following devices for testing:\n" + "\n"
                        + "I learned about evopedia as follows:\n" + "\n"
                        + "General rating (1-5):\n" + "\n");
        startActivity(i);
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
                    Toast.makeText(this, "External storage not mounted.",
                            Toast.LENGTH_SHORT).show();
                }
                return true;
                // case R.id.menu_settings:
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
