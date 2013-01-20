package info.evopedia;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AvailableArchivesDatabase {
    private static class DBOpenHelper extends SQLiteOpenHelper {
        public DBOpenHelper(Context context) {
            super(context, "archives", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE archives (" +
                       "lang TEXT, date TEXT, " +
                       "type INTEGER, data TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE archives");
            onCreate(db);
        }
    }

    private DBOpenHelper dbOpenHelper;

    public AvailableArchivesDatabase(Context context) {
        dbOpenHelper = new DBOpenHelper(context);
    }

    public Map<ArchiveID, Archive> getArchives(ArchiveManager archiveManager) {
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        Cursor c = db.query("archives", null, null, null, null, null, null);

        HashMap<ArchiveID, Archive> archives = new HashMap<ArchiveID, Archive>();
        while (c.moveToNext()) {
            String lang = c.getString(0);
            String date = c.getString(1);
            int type = c.getInt(2);
            String data = c.getString(3);

            Archive a = null;
            switch (type) {
                case 0:
                    LocalArchive la = (LocalArchive) LocalArchive.fromDatabase(lang, date, data,
                                                             archiveManager.getDefaultNormalizer());
                    if (la.isReadable())
                        a = la;
                    break;
                case 1:
                    a = PartialArchive.fromDatabase(lang, date, data, archiveManager.getDefaultNormalizer());
                    break;
                default:
                    a = DownloadableArchive.fromDatabase(lang, date, data, archiveManager.getDefaultNormalizer());
                    break;
            }
            if (a != null)
                archives.put(a.getID(), a);
        }
        return archives;
    }

    public void putArchive(Archive archive) {
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("archives", "lang = ? AND date = ?", new String[]{archive.getLanguage(), archive.getDate()});

            ContentValues values = new ContentValues();
            values.put("lang", archive.getLanguage());
            values.put("date", archive.getDate());
            values.put("type", (archive instanceof PartialArchive) ? 1 : (archive instanceof LocalArchive ? 0 : 2));
            values.put("data", archive.toJSON());
            db.insert("archives", null, values);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void removeArchive(Archive archive) {
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();

        db.delete("archives", "lang = ? AND date = ?", new String[]{archive.getLanguage(), archive.getDate()});
    }
}
