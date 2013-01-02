package info.evopedia;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
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
	private ListView titleListView;

	@Override
	public void onAttach(Activity activity) {
	    super.onAttach(activity);

        titleAdapter = new TitleAdapter(activity);
	}

	@Override
	public void onCreate(Bundle savedInstance) {
	    super.onCreate(savedInstance);
	    /* TODO setStyle(DialogFragment.STYLE_NO_TITLE, )*/
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                  Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_evopedia_search,
                                     container, false);

        titleListView = (ListView) view.findViewById(R.id.titleListView);
        titleListView.setAdapter(titleAdapter);
        titleListView.setOnScrollListener(this);
        titleListView.setOnItemClickListener(this);

        EditText titleSearch = (EditText) view.findViewById(R.id.titleSearch);
        titleSearch.addTextChangedListener(this);
        getDialog().getWindow().setSoftInputMode(
                LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        titleSearch.requestFocus();
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
	    dismiss();
	    /* TODO this activity is perhaps already running */
	}
}