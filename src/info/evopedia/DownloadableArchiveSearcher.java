package info.evopedia;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.AsyncTask;

public class DownloadableArchiveSearcher extends AsyncTask<URL, Long, Map<ArchiveID, DownloadableArchive>> {
    private ArchiveManager manager;

    public DownloadableArchiveSearcher() {
    }

    @Override
    protected Map<ArchiveID, DownloadableArchive> doInBackground(URL... urls) {
        HashMap<ArchiveID, DownloadableArchive> archivesFound = new HashMap<ArchiveID, DownloadableArchive>();

        Pattern p = Pattern.compile("<!-- METAINFO ([^>]*/wikipedia_([a-z_-]*)_([0-9-]*)\\.torrent) ([0-9]*) -->");
        for (URL url : urls) {
            Matcher m;
            try {
                m = p.matcher(Utils.readInputStream(url.openStream()));
            } catch (IOException exc) {
                /* TODO we should report some error */
                continue;
            }
            while (m.find()) {
                URL archiveURL;
                try {
                    archiveURL = new URL(m.group(1));
                } catch (MalformedURLException exc) {
                    continue;
                }

                DownloadableArchive a = new DownloadableArchive(
                                                    m.group(2), m.group(3),
                                                    archiveURL, m.group(4));
                archivesFound.put(a.getID(), a);
            }
        }

        return archivesFound;
    }

    @Override
    protected void onPostExecute(Map<ArchiveID, DownloadableArchive> archives) {
        manager.setDownloadableArchives(archives);
    }
}
