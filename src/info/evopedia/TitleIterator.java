package info.evopedia;

import java.io.RandomAccessFile;
import java.util.Iterator;

/* TODO add intermediate iterator that discards subsequent titles that
 * normalized to the same name and only retains the non-redirect title */

public abstract class TitleIterator implements Iterator<Title> {
    protected final RandomAccessFile file;
    protected final String query;
    protected final StringNormalizer normalizer;
    protected final LocalArchive archive;

    private boolean fullyInitialized = false;
    private Title nextTitle;

    public TitleIterator(RandomAccessFile file, String query, LocalArchive archive) {
        this.file = file;
        this.archive = archive;
        this.normalizer = archive.getStringNormalizer();
        this.query = normalizer.normalize(query);
    }

    @Override
    public boolean hasNext() {
        if (!fullyInitialized)
            checkHasNext();
        return nextTitle != null;
    }

    private void checkHasNext() {
        fullyInitialized = true;
        if (file == null) {
            nextTitle = null;
        } else {
            nextTitle = retrieveNext();
        }
    }

    protected abstract Title retrieveNext();

    public Title peekNext() {
        if (!fullyInitialized)
            checkHasNext();
        return nextTitle;
    }

    @Override
    public Title next() {
        if (!fullyInitialized)
            checkHasNext();
        Title t = nextTitle;
        checkHasNext();
        return t;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
