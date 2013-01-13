package info.evopedia;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
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
        if (!titleIterator.hasNext())
            return;

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
        LinearLayout v;
        if (convertView != null) {
            v = (LinearLayout) convertView;
        } else {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(
                                    Context.LAYOUT_INFLATER_SERVICE);
            View view = vi.inflate(R.layout.titlelistitem, null);
            v = (LinearLayout) view;
        }

        /* TODO performance: when the list of titles is extended,
         * these views are built completely anew? */
        Title t = currentTitles.get(position);
        String remark = "Wikipedia " + t.getArchive().getLanguage(); /* TODO date? */
        long articleLength = 0;
        if (t.isRedirect()) {
            Title orig = t.resolveRedirect();
            if (orig != null)
                articleLength = orig.getArticleLength();
        } else {
            articleLength = t.getArticleLength();
        }
        remark += String.format(", %.1f kB", (double) articleLength / 1000.0);
        ((TextView) v.findViewById(R.id.titleListItemFirstLine)).setText(
                t.getReadableName());
        ((TextView) v.findViewById(R.id.titleListItemSecondLine)).setText(remark);
        return v;
    }

    @Override
    public void onArchiveChange(boolean localArchivesChanged,
            ArchiveManager manager) {
        if (localArchivesChanged)
            setPrefix(currentPrefix);
    }
}
