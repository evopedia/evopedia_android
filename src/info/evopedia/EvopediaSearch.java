package info.evopedia;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class EvopediaSearch extends SherlockDialogFragment
                implements OnScrollListener, OnItemClickListener, TextWatcher {
	private TitleAdapter titleAdapter;
    private EditText titleSearch;
	private ListView titleListView;

	private String nextPrefix;

	@Override
	public void onAttach(Activity activity) {
	    super.onAttach(activity);

        titleAdapter = new TitleAdapter(activity);
	}

	public void setSearchPrefix(String prefix) {
	    if (titleSearch == null) {
	        nextPrefix = prefix;
	    } else {
	        titleSearch.setText(prefix);
	    }
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                  Bundle savedInstanceState) {
        /* TODO use savedInstanceState */
        View view = inflater.inflate(R.layout.activity_evopedia_search,
                                     container, false);

        titleSearch = (EditText) view.findViewById(R.id.titleSearch);
        titleSearch.addTextChangedListener(this);
        titleSearch.requestFocus();

        titleListView = (ListView) view.findViewById(R.id.titleListView);
        titleListView.setAdapter(titleAdapter);
        titleListView.setOnScrollListener(this);
        titleListView.setOnItemClickListener(this);

        if (nextPrefix != null) {
            titleSearch.setText(nextPrefix);
            titleAdapter.setPrefix(nextPrefix);
            nextPrefix = null;
        }

        getDialog().requestWindowFeature(STYLE_NO_TITLE);

        return view;
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
		/* scroll to top */
		titleListView.setSelectionAfterHeaderView();
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
	    dismiss();
	    /* TODO this activity is perhaps already running */
	}
}