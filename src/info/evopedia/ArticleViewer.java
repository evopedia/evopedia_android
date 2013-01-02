package info.evopedia;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

public class ArticleViewer extends SherlockFragmentActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);

        setContentView(R.layout.activity_article_viewer);
        setupWebView();

        Intent intent = getIntent();
        if (intent.getAction().equals(Intent.ACTION_MAIN)) {
            ArchiveManager manager = ArchiveManager.getInstance(this);
            if (manager.getDefaultLocalArchives().isEmpty()) {
                showInitialPage();
            } else {
                onSearchRequested();
            }
        } else if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
            /* TODO */
        } else if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            Title t = Title.fromUri(getIntent().getData());
            loadArticle(t);
        }
    }
    private void setupWebView() {
        webView = (WebView) findViewById(R.id.webView);

        //webView.getSettings().setJavaScriptEnabled(true);
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
                return !url.startsWith("http://127.0.0.1");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_article_viewer, menu);
        return true;
    }

    @Override
    public boolean onSearchRequested() {
        EvopediaSearch search = new EvopediaSearch();
        search.show(getSupportFragmentManager(), "EvopediaSearch");
        return true;
    }

    private void loadArticle(Title t) {
        if (t == null) {
            /* TODO show some not-found page */ 
        } else {
            Evopedia evopedia = (Evopedia) getApplication();
            webView.loadUrl(evopedia.getArticleUri(t).toString());
        }
    }

    private void showInitialPage() {
        Evopedia evopedia = (Evopedia) getApplication();
        webView.loadUrl(evopedia.getServerUri().toString());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                onSearchRequested();
                return true;
            case R.id.scan_for_archives:
                String storageState = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(storageState) ||
                        Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState)) {
                    LocalArchiveSearcher searcher = new LocalArchiveSearcher(this);
                    searcher.execute(Environment.getExternalStorageDirectory());
                } else {
                    Toast.makeText(this, "External storage not mounted.", Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.menu_settings:
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
