package info.evopedia;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class TitleInfixIterator extends TitleIterator {
    private final ByteLineReader lineReader;
    private final byte[] byteQuery;

    /* Boyer Moore */
    private final int[] bmBc; /* lastOccurrence */
    private final int[] bmGs;

    public TitleInfixIterator() {
        super(null, null, null);
        lineReader = null;
        byteQuery = null;
        bmBc = null;
        bmGs = null;
    }

    public TitleInfixIterator(RandomAccessFile file, String query, LocalArchive archive) {
        super(file, query, archive);
        ByteLineReader lReader = null;
        try {
            lReader = new ByteLineReader(file);
        } catch (IOException e) {
            file = null;
        }
        lineReader = lReader;
        byteQuery = new byte[query.length()];
        /* TODO this only works if the normalizer is in use. if it is not in use,
         * we can compare everything bytewise
         */
        for (int i = 0; i < byteQuery.length; i ++) {
            byteQuery[i] = (byte) query.charAt(i);
        }

        bmBc = new int[256];
        bmGs = new int[byteQuery.length];

        Arrays.fill(bmBc, byteQuery.length);
        for (int i = 0; i < byteQuery.length - 1; i++)
            bmBc[(byteQuery[i] & 0xff)] = byteQuery.length - i - 1;

        if (byteQuery.length > 0)
            preBmGs();
    }

    private final int[] suffixes() {
        int f = 0, g = 0, i = 0;
        int queryLen = byteQuery.length;
        int[] suffixes = new int[queryLen];

        suffixes[queryLen - 1] = queryLen;
        g = queryLen - 1;
        for (i = queryLen - 2; i >= 0; --i) {
            if (i > g && suffixes[i + queryLen - 1 - f] < i - g) {
                suffixes[i] = suffixes[i + queryLen - 1 - f];
            } else {
                if (i < g)
                    g = i;
                f = i;
                while (g >= 0 && byteQuery[g] == byteQuery[g + queryLen - 1 - f])
                    --g;
                suffixes[i] = f - g;
            }
        }
        return suffixes;
    }

    private final void preBmGs() {
        int i = 0, j = 0;
        int queryLen = byteQuery.length;
        int[] suffixes = suffixes();

        Arrays.fill(bmGs, queryLen);

        j = 0;
        for (i = queryLen - 1; i >= 0; --i) {
            if (suffixes[i] != i + 1)
                continue;
            for (; j < queryLen - 1 - i; ++j) {
                if (bmGs[j] == queryLen)
                    bmGs[j] = queryLen - 1 - i;
            }
        }
        for (i = 0; i <= queryLen - 2; ++i)
            bmGs[queryLen - 1 - suffixes[i]] = queryLen - 1 - i;
    }

    protected Title retrieveNext() {
        /* TODO remember search results and last offset
         * then filter the results if the old query is a substring of the new query
         * (problem: a new instance of the iterator is created)
         */
        Title nextTitle = null;
        try {
            while (true) {
                long offset = lineReader.getPosition();
                byte[] line = lineReader.nextLine();

                boolean isAscii = true; /* TODO determine that */
                int queryLen = byteQuery.length;
                if (isAscii) {
                    boolean found = false;
                    for (int j = 15; j <= line.length - queryLen; ) {
                        int i = queryLen - 1;
                        while (i >= 0 && byteQuery[i] == line[i + j])
                            i --;
                        if (i < 0) {
                            found = true;
                            break;
                        } else {
                            j += Math.max(bmGs[i], bmBc[(line[j + i] & 0xff)] - queryLen + 1 + i);
                        }
                    }
                    if (found) {
                        nextTitle = Title.parseTitle(line, archive, offset);
                        if (nextTitle != null)
                            return nextTitle;
                    }
                } else {
                    String name = Title.parseNameOnly(line);
                    if (name == null)
                        return null;
                    if (query == null || normalizer.normalize(name).contains(query)) {
                        nextTitle = Title.parseTitle(line, archive, offset);
                        if (nextTitle != null)
                            return nextTitle;
                    }
                }
            }
        } catch (IOException ex) {
        }
        return null;
    }
}
