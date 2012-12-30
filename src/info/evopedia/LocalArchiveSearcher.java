package info.evopedia;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.Toast;

public class LocalArchiveSearcher extends AsyncTask<File, Integer, Map<ArchiveID, LocalArchive>> implements OnCancelListener {
    private Context context;
	private ArchiveManager manager;
	private ProgressDialog dialog;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public LocalArchiveSearcher(Context context) {
	    this.context = context;
		manager = ArchiveManager.getInstance(context);
		dialog = new ProgressDialog(context);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setCancelable(true);
		dialog.setMax(100);
		if (android.os.Build.VERSION.SDK_INT >= 11) {
		    dialog.setProgressNumberFormat(null);
		}
		dialog.setOnCancelListener(this);
		dialog.setTitle("Searching for archives...");
	}

	@Override
    protected void onPreExecute() {
        dialog.show();
    }

	@Override
	protected Map<ArchiveID, LocalArchive> doInBackground(File... dirs) {
		HashMap<ArchiveID, LocalArchive> archivesFound = new HashMap<ArchiveID, LocalArchive>();

		ArrayList<File> firstLevel = new ArrayList<File>();
		for (File dir : dirs) {
		    firstLevel.addAll(getSubdirectories(dir));
		}
		int progress = 0;
		int total = firstLevel.size();
		for (File dir : firstLevel) {
			searchRecursively(dir, archivesFound);
			this.publishProgress(progress * 100 / total);
			progress ++;
		}

		return archivesFound;
	}

	private List<File> getSubdirectories(File directory) {
        File[] subdirectories = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return dir.isDirectory();
            }
        });

        if (subdirectories == null) {
            return new ArrayList<File>(0);
        } else {
            return Arrays.asList(subdirectories);
        }
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

		for (File subdir : getSubdirectories(dir)) {
			searchRecursively(subdir, archivesFound);
		}
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
	    dialog.setProgress(progress[0]);
	}

	@Override
	protected void onPostExecute(Map<ArchiveID, LocalArchive> archives) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        Toast.makeText(context, "Found " + archives.size() + " archives.", Toast.LENGTH_SHORT).show();
        manager.setLocalArchives(archives);
	}

    @Override
    public void onCancel(DialogInterface dialog) {
        this.cancel(false);
    }
}
