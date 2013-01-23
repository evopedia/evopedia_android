package info.evopedia;

import java.util.ArrayList;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;


public class SearchSuggestionProvider extends ContentProvider {
    String TAG = "SearchSuggestionProvider";

    public static String AUTHORITY = "info.evopedia.SearchSuggestionProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/dictionary");

    private static final int maxColumns = 40;
    private ArchiveManager archiveManager;

    @Override
    public boolean onCreate() {
        archiveManager = ArchiveManager.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        String prefix = uri.getEncodedPath().endsWith("/") ? "" : uri.getLastPathSegment();

        ArrayList<TitleIterator> iterators = new ArrayList<TitleIterator>();
        for (LocalArchive archive : archiveManager.getDefaultLocalArchives().values()) {
            iterators.add(archive.getTitlesWithPrefix(prefix));
        }
        MergingTitleIterator titleIterator = new MergingTitleIterator(iterators);

        String[] columnNames = new String[] {
                BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_TEXT_2,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA
        };
        MatrixCursor cursor = new MatrixCursor(columnNames);

        for (int i = 0; i < maxColumns && titleIterator.hasNext(); i ++) {
            Object[] row = new Object[columnNames.length];
            Title t = titleIterator.next();
            String remark = "Wikipedia " + t.getArchive().getLanguage();
            long articleLength = 0;
            if (t.isRedirect()) {
                Title orig = t.resolveRedirect();
                if (orig != null)
                    articleLength = orig.getArticleLength();
            } else {
                articleLength = t.getArticleLength();
            }
            remark += getContext().getString(R.string._1f_kb,
                                    (double) articleLength / 1000.0);

            row[0] = i;
            row[1] = t.getReadableName();
            row[2] = remark;
            row[3] = t.toUri();
            cursor.addRow(row);
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return SearchManager.SUGGEST_MIME_TYPE;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

}
