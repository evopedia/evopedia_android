package info.evopedia;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class EvopediaSearch extends Activity implements OnScrollListener, OnItemClickListener, TextWatcher {
	private TitleAdapter titleAdapter;
	private ListView titleListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        titleAdapter = new TitleAdapter(this);

        setContentView(R.layout.activity_evopedia_search);

        titleListView = (ListView) findViewById(R.id.titleListView);
        titleListView.setAdapter(titleAdapter);
        titleListView.setOnScrollListener(this);
        titleListView.setOnItemClickListener(this);

        EditText titleSearch = (EditText) findViewById(R.id.titleSearch);
        titleSearch.addTextChangedListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_evopedia_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.scan_for_archives:
    		    String storageState = Environment.getExternalStorageState();
    		    if (Environment.MEDIA_MOUNTED.equals(storageState) ||
    		    		Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState)) {
    		    	LocalArchiveSearcher searcher = new LocalArchiveSearcher(this);
    		    	searcher.execute(Environment.getExternalStorageDirectory());
    		    } else {
    		        /* TODO test that */
    		        Toast.makeText(this, "External storage not mounted.", Toast.LENGTH_SHORT).show();
    		    }
    		case R.id.menu_settings:
    		default:
    			return super.onOptionsItemSelected(item);
    	}
    }

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if (firstVisibleItem + visibleItemCount >= totalItemCount - 10) {
			titleAdapter.loadMore();
		}
		
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	@Override
	public void afterTextChanged(Editable e) {
		titleAdapter.setPrefix(e.toString());
		/* TODO scrolling to origin does not work yet */
		titleListView.scrollTo(0, 0);
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {
	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	    Intent intent = new Intent(Intent.ACTION_VIEW, titleAdapter.getItem(position).toUri());
	    startActivity(intent);
	}
}