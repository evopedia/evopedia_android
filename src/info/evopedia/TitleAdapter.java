package info.evopedia;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TitleAdapter extends BaseAdapter implements ArchiveManager.OnArchiveChangeListener, Runnable {
    private ArrayList<Title> currentTitles;
    private ArrayList<String> currentFirstLines;
    private ArrayList<String> currentSecondLines;
    private static final int increments = 20;

    /* accessed by new thread */
    private final Activity activity;
    private ArchiveManager archiveManager;

    /* thread input */
    private int prefixSeq = 0;
    private String prefix = "";
    private boolean loadMore = false;

    private Thread thread;
    private boolean terminate = false;

    public TitleAdapter(Activity activity) {
        this.activity = activity;

        archiveManager = ArchiveManager.getInstance(activity.getApplicationContext());
        currentTitles = new ArrayList<Title>();
        currentFirstLines = new ArrayList<String>();
        currentSecondLines = new ArrayList<String>();
        archiveManager.addOnArchiveChangeListener(this);

        startOrRestartThread();
    }

    private synchronized void startOrRestartThread() {
        if (thread == null || thread.getState() == State.TERMINATED) {
            thread = new Thread(this);
            thread.start();
        }
        notify();
    }

    public synchronized void setPrefix(String prefix) {
        this.prefixSeq ++;
        this.prefix = prefix;

        startOrRestartThread();
    } 

    public synchronized void loadMore() {
        loadMore = true;

        startOrRestartThread();
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
            LayoutInflater vi = (LayoutInflater) activity.getSystemService(
                                    Context.LAYOUT_INFLATER_SERVICE);
            View view = vi.inflate(R.layout.titlelistitem, null);
            v = (LinearLayout) view;
        }

        ((TextView) v.findViewById(R.id.titleListItemFirstLine)).setText(
                currentFirstLines.get(position));
        ((TextView) v.findViewById(R.id.titleListItemSecondLine)).setText(
                currentSecondLines.get(position));
        return v;
    }

    @Override
    public void onArchiveChange(boolean localArchivesChanged, ArchiveManager manager) {
        if (localArchivesChanged)
            setPrefix(prefix);
    }

    /* below this line, everything runs in the new thread */

    @Override
    public void run() {
        int prefixSeq = -1;
        String prefix = "";
        MergingTitleIterator titleIterator = null;
        while (!terminate) {
            boolean reload = false;
            synchronized (this) {
                while (!this.loadMore && this.prefixSeq == prefixSeq) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                if (this.prefixSeq > prefixSeq) {
                    prefix = this.prefix;
                    prefixSeq = this.prefixSeq;
                    titleIterator = null;
                    reload = true;
                }
                this.loadMore = false;
            }
            if (reload)
                titleIterator = createTitleIterator(prefix);
            sendResult(prefixSeq, reload, loadTitles(titleIterator));
        }
    }

    private String getFirstLine(Title t) {
        return t.getReadableName();
    }

    private String getSecondLine(Title t) { 
        long articleLength = 0;
        if (t.isRedirect()) {
            Title orig = t.resolveRedirect();
            if (orig != null)
                articleLength = orig.getArticleLength();
        } else {
            articleLength = t.getArticleLength();
        }
        return "Wikipedia " + t.getArchive().getLanguage() +
                activity.getString(R.string._1f_kb,
                                   (double) articleLength / 1000.0);
    }

    private void sendResult(final int prefixSeq, final boolean reload, final List<Title> titles) {
        final ArrayList<String> firstLines = new ArrayList<String>(titles.size());
        final ArrayList<String> secondLines = new ArrayList<String>(titles.size());
        for (Title t : titles) {
            firstLines.add(getFirstLine(t));
            secondLines.add(getSecondLine(t));
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (TitleAdapter.this.prefixSeq != prefixSeq)
                    return;
                if (reload) {
                    currentTitles = new ArrayList<Title>(titles);
                    currentFirstLines = firstLines;
                    currentSecondLines = secondLines;
                    notifyDataSetChanged();
                } else if (titles.size() > 0) {
                    currentTitles.addAll(titles);
                    currentFirstLines.addAll(firstLines);
                    currentSecondLines.addAll(secondLines);
                    notifyDataSetChanged();
                }
            }
        });
    }

    private MergingTitleIterator createTitleIterator(String query) {
        ArrayList<TitleIterator> iterators = new ArrayList<TitleIterator>();

        if (PreferenceManager.getDefaultSharedPreferences(activity)
                        .getBoolean("pref_titles_infix_search", false)) {
            for (LocalArchive archive : archiveManager.getDefaultLocalArchives().values()) {
                iterators.add(archive.getTitlesWithPrefix(query));
                iterators.add(archive.getTitlesWithInfix(query));
            }
        } else {
            for (LocalArchive archive : archiveManager.getDefaultLocalArchives().values()) {
                iterators.add(archive.getTitlesWithPrefix(query));
            }
        }

        return new MergingTitleIterator(iterators);
    }

    private List<Title> loadTitles(MergingTitleIterator titleIterator) {
        ArrayList<Title> newTitles = new ArrayList<Title>();
        if (!titleIterator.hasNext())
            return newTitles;

        for (int i = 0; titleIterator.hasNext() && i < increments; i ++) {
            newTitles.add(titleIterator.next());
        }
        return newTitles;
    }
}
