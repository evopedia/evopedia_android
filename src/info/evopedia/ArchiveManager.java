package info.evopedia;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;

/* TODO make this thread-safe */

public class ArchiveManager {
	private Context context;

	private DefaultNormalizer defaultNormalizer;

	private AvailableArchivesDatabase availableArchivesDatabase;

	private Map<ArchiveID, Archive> archives;
	private Map<String, LocalArchive> defaultLocalArchives;
	private boolean archiveBulkChangeUnderway;

	public interface OnArchiveChangeListener {
		public void onArchiveChange(boolean localArchivesChanged, ArchiveManager manager);
	}
	private List<OnArchiveChangeListener> onArchiveChangeListeners = new CopyOnWriteArrayList<OnArchiveChangeListener>();

	private static ArchiveManager instance = null;

	private ArchiveManager(Context context) {
		this.context = context;

		initializeNormalizer();

		availableArchivesDatabase = new AvailableArchivesDatabase(context);
		archives = new HashMap<ArchiveID, Archive>();
        defaultLocalArchives = new HashMap<String, LocalArchive>();
        archives = availableArchivesDatabase.getArchives(this);

        updateDefaultLocalArchives();
		fireOnArchiveChange(true);
	}

	private void initializeNormalizer() {
    	InputStream translationTable = context.getResources().openRawResource(R.raw.transtbl);
    	defaultNormalizer = new DefaultNormalizer(translationTable);
	}

	public StringNormalizer getDefaultNormalizer() {
		return defaultNormalizer;
	}

	private void updateDefaultLocalArchives() {
		HashMap<String, LocalArchive> newDefaultLocalArchives = new HashMap<String, LocalArchive>();

		for (Archive ar : archives.values()) {
			if (!(ar instanceof LocalArchive))
				continue;
			LocalArchive lar = (LocalArchive) ar;
			String lang = lar.getLanguage();
			if (newDefaultLocalArchives.containsKey(lang) &&
					lar.compareTo(newDefaultLocalArchives.get(lang)) < 0)
				continue;
			newDefaultLocalArchives.put(lang, lar);
		}

		defaultLocalArchives = newDefaultLocalArchives;
	}

	private boolean addArchive(Archive archive) {
        ArchiveID id = archive.getID();

        if (archives.containsKey(id)) {
            /* only add archive, if it is more "local" than the current one */
            Archive current = archives.get(id);
            if (!archive.isMoreLocal(current) && !(current instanceof DownloadableArchive))
                return false;
        }
        archives.put(id, archive);

        availableArchivesDatabase.putArchive(archive);

        if (archiveBulkChangeUnderway)
            return true;

        updateDefaultLocalArchives();
        fireOnArchiveChange(archive instanceof LocalArchive);

		return true;
	}

	public void removeArchive(Archive archive) {
	    archives.remove(archive.getID());
	    availableArchivesDatabase.removeArchive(archive);

        if (!archiveBulkChangeUnderway) {
            updateDefaultLocalArchives();
            fireOnArchiveChange(archive instanceof LocalArchive);
        }
	}

    public void setDownloadableArchives(Map<ArchiveID, DownloadableArchive> newArchives) {
        archiveBulkChangeUnderway = true;
        try {
            for (Archive archive : archives.values()) {
                if (archive instanceof DownloadableArchive)
                    removeArchive(archive);
            }
            for (Archive archive : newArchives.values()) {
                addArchive(archive);
            }
        } finally {
            archiveBulkChangeUnderway = false;
            updateDefaultLocalArchives();
            fireOnArchiveChange(true);
        }
    }

	public void setLocalArchives(Map<ArchiveID, LocalArchive> newArchives) {
		HashMap<ArchiveID, LocalArchive> currentLocalArchives = new HashMap<ArchiveID, LocalArchive>();
		for (Archive archive : archives.values()) {
			if (archive instanceof LocalArchive) {
				currentLocalArchives.put(archive.getID(), (LocalArchive) archive);
			}
		}

		if (currentLocalArchives.equals(newArchives))
			return;

		archiveBulkChangeUnderway = true;
		try {
			for (Archive archive : currentLocalArchives.values()) {
				removeArchive(archive);
			}
			for (Archive archive : newArchives.values()) {
				addArchive(archive);
			}
		} finally {
			archiveBulkChangeUnderway = false;
			updateDefaultLocalArchives();
			fireOnArchiveChange(true);
		}
	}

	public Map<String, LocalArchive> getDefaultLocalArchives() {
		return Collections.unmodifiableMap(defaultLocalArchives);
	}

	public LocalArchive getDefaultLocalArchive(String lang) {
		return defaultLocalArchives.get(lang);
	}

	public Archive getArchive(ArchiveID archiveID) {
		return archives.get(archiveID);
	}

	public void addOnArchiveChangeListener(OnArchiveChangeListener l) {
		onArchiveChangeListeners.add(l);
	}

	public void removeOnArchiveChangeListener(OnArchiveChangeListener l) {
		onArchiveChangeListeners.remove(l);
	}

	private void fireOnArchiveChange(boolean localArchivesChanged) {
		for (OnArchiveChangeListener l : onArchiveChangeListeners) {
			l.onArchiveChange(localArchivesChanged, this);
		}
	}

	public static ArchiveManager getInstance(Context context) {
		if (instance == null && context != null) {
			instance = new ArchiveManager(context.getApplicationContext());
		}
		return instance;
	}
}
