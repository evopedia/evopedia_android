package info.evopedia;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ListView;

public class ArticleViewer extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);

        setContentView(R.layout.activity_article_viewer);

        webView = (WebView) findViewById(R.id.webView);

        //webView.getSettings().setJavaScriptEnabled(true);
        final Activity activity = this;
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress < 100) {
                    activity.setProgressBarVisibility(true);
                }
                activity.setProgress(progress * 100);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !url.startsWith("http://127.0.0.1");
            }
        });
        Title t = Title.fromUri(getIntent().getData());
        if (t == null) {
            /* TODO show some not-found page */ 
        } else {
            Evopedia evopedia = (Evopedia) getApplication();
            webView.loadUrl(evopedia.getArticleUri(t).toString());
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_article_viewer, menu);
        return true;
    }

}
