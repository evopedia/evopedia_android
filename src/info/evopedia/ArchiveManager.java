package info.evopedia;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/* TODO make this thread-safe */

public class ArchiveManager {
	private Context context;

	private DefaultNormalizer defaultNormalizer;

	private HashMap<ArchiveID, Archive> archives;
	private HashMap<String, LocalArchive> defaultLocalArchives;
	private boolean archiveBulkChangeUnderway;

	public interface OnArchiveChangeListener {
		public void onArchiveChange(boolean localArchivesChanged, ArchiveManager manager);
	}
	private List<OnArchiveChangeListener> onArchiveChangeListeners = new CopyOnWriteArrayList<OnArchiveChangeListener>();

	private static ArchiveManager instance;

	private ArchiveManager(Context context) {
		this.context = context;

		initializeNormalizer();
		archiveBulkChangeUnderway = true;
		try {
			initializeArchives();
		} finally {
			archiveBulkChangeUnderway = false;
			updateDefaultLocalArchives();
			fireOnArchiveChange(true);
		}
	}

	private void initializeNormalizer() {
    	InputStream translationTable = context.getResources().openRawResource(R.raw.transtbl);
    	defaultNormalizer = new DefaultNormalizer(translationTable);
	}

	public StringNormalizer getDefaultNormalizer() {
		return defaultNormalizer;
	}

	private void initializeArchives() {
		archives = new HashMap<ArchiveID, Archive>();
		defaultLocalArchives = new HashMap<String, LocalArchive>();

		SharedPreferences prefs = context.getSharedPreferences("Evopedia", 0);
		String[] archives = prefs.getString("archives", "").split(",");
		if (archives == null || archives.length == 0)
			return;

		for (String archiveName : archives) {
			if (prefs.getBoolean("archives/" + archiveName + "/complete", false)) {
				String dataDirectory = prefs.getString("archives/" + archiveName + "/data_directory", "");
				LocalArchive archive = new LocalArchive(dataDirectory, defaultNormalizer);
				if (archive.isReadable()) {
					addArchiveInternal(archive);
				}
			}
		}
	}

	private boolean addArchiveInternal(Archive archive) {
		ArchiveID id = archive.getID();

		if (archives.containsKey(id)) {
			Archive current = archives.get(id);
			/* TODO some magic to determine which archive is "more local".
			 * currently, there is only an implementation of LocalArchive, so do this later
			 */
			return false;
		}
		archives.put(id, archive);

		if (!archiveBulkChangeUnderway)
			updateDefaultLocalArchives();

		return true;
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

	private boolean addArchiveAndStoreInPrefs(Archive archive) {
		if (!addArchiveInternal(archive))
			return false;
		if (!(archive instanceof LocalArchive))
			return true;

		LocalArchive larchive = (LocalArchive) archive;

		SharedPreferences prefs = context.getSharedPreferences("Evopedia", 0);
		Editor prefsEditor = prefs.edit();

		HashSet<String> archives = new HashSet<String>(Arrays.asList(prefs.getString("archives", "").split(",")));
		archives.remove("");

		String archiveName = larchive.getID().toString();
		archives.add(archiveName);

		StringBuilder archiveList = new StringBuilder("");
		for (String an : archives) {
			if (archiveList.length() > 0)
				archiveList.append(",");
			archiveList.append(an);
		}
		prefsEditor.putString("archives", archiveList.toString());

		prefsEditor.putBoolean("archives/" + archiveName + "/complete", true);
		prefsEditor.putString("archives/" + archiveName + "/data_directory", larchive.getDirectory());
		prefsEditor.apply();

		return true;
	}

	public boolean addArchive(Archive archive) {
		if (addArchiveAndStoreInPrefs(archive)) {
			fireOnArchiveChange(archive instanceof LocalArchive);
			return true;
		} else {
			return false;
		}
	}

	public boolean removeArchive(Archive archive) {
		/* TODO */
		return false;
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
				addArchiveAndStoreInPrefs(archive);
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
		if (instance == null) {
			instance = new ArchiveManager(context.getApplicationContext());
		}
		return instance;
	}
}
