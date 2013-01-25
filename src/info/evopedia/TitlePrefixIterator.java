package info.evopedia;

import java.io.IOException;
import java.io.RandomAccessFile;

public class TitlePrefixIterator extends TitleIterator {
    public TitlePrefixIterator() {
        super(null, null, null);
    }

    public TitlePrefixIterator(RandomAccessFile file, String query, LocalArchive archive) {
        super(file, query, archive);
    }

    protected Title retrieveNext() {
        Title nextTitle = null;
        try {
            long offset = file.getFilePointer();
            byte[] line = LocalArchive.readByteLine(file);
            nextTitle = Title.parseTitle(line, archive, offset);
            if (nextTitle == null)
                return null;
            if (query == null || query.length() == 0)
                return nextTitle; /* just show all titles */
            String tn = normalizer.normalize(nextTitle.getName());
            if (tn.startsWith(query))
                return nextTitle;
        } catch (IOException ex) {
        }
        return null;
    }
}
