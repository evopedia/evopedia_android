package info.evopedia;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;

/* TODO add intermediate iterator that discards subsequent titles that
 * normalized to the same name and only retains the non-redirect title */

public class TitleIterator implements Iterator<Title> {
    private RandomAccessFile file;
    private String prefix;
    private StringNormalizer normalizer;
    private LocalArchive archive;

    private Title nextTitle;
    
    public TitleIterator() {
        checkHasNext();
    }

    public TitleIterator(RandomAccessFile file, String prefix, LocalArchive archive) {
        this.file = file;
        this.archive = archive;
        this.normalizer = archive.getStringNormalizer();
        this.prefix = normalizer.normalize(prefix);

        checkHasNext();
    }

    @Override
    public boolean hasNext() {
        return nextTitle != null;
    }

    private void checkHasNext() {
        nextTitle = null;
        if (file != null) {
            try {
                long offset = file.getFilePointer();
                byte[] line = LocalArchive.readByteLine(file);
                nextTitle = Title.parseTitle(line, archive, offset);
                if (nextTitle == null)
                    return;
                if (prefix == null || prefix == "")
                    return; /* just show all titles */
                String tn = normalizer.normalize(nextTitle.getName());
                if (!tn.startsWith(prefix))
                    nextTitle = null;
            } catch (IOException ex) {
                nextTitle = null;
            }
        }
    }
    
    public Title peekNext() {
        return nextTitle;
    }

    @Override
    public Title next() {
        Title t = nextTitle;
        checkHasNext();
        return t;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
