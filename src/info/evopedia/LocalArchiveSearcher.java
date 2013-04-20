package info.evopedia;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
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
        dialog.setTitle(context.getString(R.string.searching_for_archives_));
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
    }

    private List<String> findAdditionalStorageLocations(List<String> dirs) {
        ArrayList<String> mountPoints = new ArrayList<String>(dirs);
        Pattern p = Pattern.compile("^\\S*vold\\S*\\s+(\\S+)\\s+" +
                                    "(vfat|ntfs|exfat|fat32|ext3|ext4).*");
        String mounts = "";
        try {
            mounts = Utils.readInputStream(new FileInputStream("/proc/mounts"));
        } catch (final Exception e) {
            Log.d("LocalArchiveSearcher", "Error reading /proc/mounts", e);
            return dirs;
        }

        for (String line : mounts.split("\n")) {
            if (line.toLowerCase(Locale.US).contains("asec"))
                continue;
            Matcher m = p.matcher(line);
            if (!m.matches())
                continue;
            String mp = m.group(1);
            if (!mountPoints.contains(mp))
                mountPoints.add(m.group(1));
        }

        ArrayList<String> out = new ArrayList<String>(mountPoints.size());
        for (int i = 0; i < mountPoints.size(); i ++) {
            boolean isPrefix = false;
            String mp = mountPoints.get(i);
            for (int j = 0; j < mountPoints.size(); j ++) {
                if (i != j && mp.startsWith(mountPoints.get(j))) {
                    isPrefix = true;
                }
            }
            if (!isPrefix) {
                out.add(mp);
            }
        }
        return out;
    }

    @Override
    protected Map<ArchiveID, LocalArchive> doInBackground(File... manualDirectories) {
        HashMap<ArchiveID, LocalArchive> archivesFound = new HashMap<ArchiveID, LocalArchive>();

        ArrayList<String> manualPaths = new ArrayList<String>(manualDirectories.length);
        for (File f : manualDirectories) {
            manualPaths.add(f.getPath());
        }
        List<String> dirs = findAdditionalStorageLocations(manualPaths);

        ArrayList<File> firstLevel = new ArrayList<File>();
        for (String dir : dirs) {
            firstLevel.addAll(getSubdirectories(new File(dir)));
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
                return (new File(dir, filename)).isDirectory();
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

        /* protection against symlink-loops */
        if (dir.getPath().length() >= 800)
            return;

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
        if (archives.size() == 0) {
            Toast.makeText(context, R.string.could_not_find_any_archives_, Toast.LENGTH_SHORT).show();
            /* TODO better to use alert? */
        } else {
            String text = context.getResources().getQuantityString(
                                        R.plurals.found_x_archives__,
                                        archives.size(),
                                        archives.size());
            int i = 0;
            for (LocalArchive a : archives.values()) {
                text += a.getLanguage() + " (" + a.getDate() + ")";
                if (i < archives.size() - 1)
                    text += ", ";
                i ++;
            }
            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
        }
        manager.setLocalArchives(archives);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        this.cancel(false);
    }
}
