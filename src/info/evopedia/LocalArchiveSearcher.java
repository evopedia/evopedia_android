package info.evopedia;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.AsyncTask;

public class LocalArchiveSearcher extends AsyncTask<File, Long, Map<ArchiveID, LocalArchive>> {
	private ArchiveManager manager;

	public LocalArchiveSearcher(Context context) {
		this.manager = ArchiveManager.getInstance(context);
	}

	@Override
	protected Map<ArchiveID, LocalArchive> doInBackground(File... dirs) {
		HashMap<ArchiveID, LocalArchive> archivesFound = new HashMap<ArchiveID, LocalArchive>();

		for (File dir: dirs)
			searchRecursively(dir, archivesFound);

		return archivesFound;
	}

	private void searchRecursively(File dir, HashMap<ArchiveID, LocalArchive> archivesFound) {
		if (isCancelled())
			return;

		/* TODO will break if there are symlink-loops */

		if ((new File(dir, "titles.idx")).exists() &&
				(new File(dir, "metadata.txt")).exists()) {
			LocalArchive archive = new LocalArchive(dir.getAbsolutePath(), manager.getDefaultNormalizer());
			if (archive.isReadable()) {
				archivesFound.put(archive.getID(), archive);
			}
		}

		File[] subdirectories = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return dir.isDirectory();
			}
		});

		if (subdirectories == null)
			return;

		for (File subdir : subdirectories) {
			searchRecursively(subdir, archivesFound);
		}
	}

	@Override
	protected void onPostExecute(Map<ArchiveID, LocalArchive> archives) {
		manager.setLocalArchives(archives);
	}
}
