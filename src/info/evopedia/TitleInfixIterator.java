package info.evopedia;

import java.io.IOException;
import java.io.RandomAccessFile;

public class TitleInfixIterator extends TitleIterator {
    private final RandomAccessFile indexFile;
    private long indexPos;

    public TitleInfixIterator() {
        super(null, null, null);
        indexFile = null;
        indexPos = -1;
    }

    public TitleInfixIterator(RandomAccessFile file, RandomAccessFile indexFile, String query, LocalArchive archive) {
        super(file, query, archive);
        this.indexFile = indexFile;
        this.indexPos = findInitialIndexPos();
    }
    private long findInitialIndexPos() {
        if (query.length() == 0)
            return 0;

        long lo = 0;
        try {
            long hi = indexFile.length() / 4;
            while (lo < hi) {
                long mid = (lo + hi) / 2;
                long pos = LittleEndianReader.readUInt32(indexFile, mid * 4);
                file.seek(pos);
                byte[] line = LocalArchive.readByteLine(file);
                if (line.length == 0)
                    return -1;
                String nt = normalizer.normalize(new String(line));
                if (nt.compareTo(query) < 0) {
                    lo = mid + 1;
                } else {
                    hi = mid;
                }
            }
            indexPos = lo;
        } catch (IOException e) {
            return -1;
        }
        return lo;
    }

    private Title readTitleAt(long pos) {
        /* looks for the first '\n' before pos */
        byte[] buffer = new byte[32];
        pos -= 13; /* title header length */
        try {
            while (pos > 0) {
                long lower = Math.max(pos - buffer.length, 0);
                pos -= 1;
                file.seek(lower);
                file.readFully(buffer);
                for (; pos >= lower; pos --) {
                    if (buffer[(int) (pos - lower)] == '\n')
                        return archive.getTitleAtOffset(pos + 1);
                }
            }
        } catch (IOException e) {
            return null;
        }
        return archive.getTitleAtOffset(0);
    }

    protected Title retrieveNext() {
        if (indexPos < 0)
            return null;

        try {
            indexFile.seek(indexPos * 4);
            indexPos ++;
            Title t = readTitleAt(LittleEndianReader.readUInt32(indexFile));
            if (t == null || t.getName() == null)
                return null;
            String tn = normalizer.normalize(t.getName());
            if (tn.contains(query))
                return t;
        } catch (IOException ex) {
        }
        return null;
    }
}
