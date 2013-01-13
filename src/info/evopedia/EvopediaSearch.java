package info.evopedia;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

public class EvopediaSearch implements OnScrollListener, OnItemClickListener, TextWatcher {
    public interface OnTitleSelectedListener {
        public void onTitleSelected(Title title);
    }

    private TitleAdapter titleAdapter;
    private EditText editText;
    private ListView titleListView;
    private OnTitleSelectedListener onTitleSelectedListener;

    private String nextPrefix;

    /* TODO move the title search outside of the ui thread */

    public EvopediaSearch(Context context, OnTitleSelectedListener onTitleSelectedListener) {
        this.onTitleSelectedListener = onTitleSelectedListener;

        titleAdapter = new TitleAdapter(context);
    }

    public void setEditText(EditText editText) {
        this.editText = editText;
        editText.addTextChangedListener(this);
        if (nextPrefix != null) {
            editText.setText(nextPrefix);
            titleAdapter.setPrefix(nextPrefix);
            nextPrefix = null;
        }
    }

    public void setTitleListView(ListView titleListView) {
        this.titleListView = titleListView;
        titleListView.setAdapter(titleAdapter);
        titleListView.setOnScrollListener(this);
        titleListView.setOnItemClickListener(this);
    }

    public void setSearchPrefix(String prefix) {
        if (editText == null) {
            nextPrefix = prefix;
        } else {
            editText.setText(prefix);
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
        if (onTitleSelectedListener != null) {
            onTitleSelectedListener.onTitleSelected(titleAdapter.getItem(position));
        }
    }
}
