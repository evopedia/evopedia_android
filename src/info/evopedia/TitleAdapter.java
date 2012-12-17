package info.evopedia;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TitleAdapter extends BaseAdapter implements ArchiveManager.OnArchiveChangeListener {
	private Context context;
	private ArchiveManager archiveManager;
	private Iterator<Title> titleIterator;
	private String currentPrefix = "";
	private ArrayList<Title> currentTitles;
	private static final int increments = 20;

	public TitleAdapter(Context context) {
		archiveManager = ArchiveManager.getInstance(context.getApplicationContext());
		this.context = context;

		archiveManager.addOnArchiveChangeListener(this);

		setPrefix("");
	}

	public void setPrefix(String prefix) {
		currentPrefix = prefix;
		currentTitles = new ArrayList<Title>(increments);

		ArrayList<TitleIterator> iterators = new ArrayList<TitleIterator>();
		for (LocalArchive archive : archiveManager.getDefaultLocalArchives().values()) {
			iterators.add(archive.getTitlesWithPrefix(prefix));
		}
		titleIterator = new MergingTitleIterator(iterators);
		loadMore();
	}

	public void loadMore() {
		for (int i = 0; titleIterator.hasNext() && i < increments; i ++) {
			currentTitles.add(titleIterator.next());
		}
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return currentTitles.size();
	}

	@Override
	public Title getItem(int position) {
		return currentTitles.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        TextView v;
        if (convertView != null) {
        	v = (TextView) convertView;
        } else {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = vi.inflate(R.layout.titlelistitem, null);
            v = (TextView) view;
        }
        Title t = currentTitles.get(position);
		v.setText(t.getReadableName() + " (" + t.getArchive().getLanguage() + ")");
		return v;
	}

	@Override
	public void onArchiveChange(boolean localArchivesChanged,
			ArchiveManager manager) {
		if (localArchivesChanged)
			setPrefix(currentPrefix);
	}
}