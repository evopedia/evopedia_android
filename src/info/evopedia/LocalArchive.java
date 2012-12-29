package info.evopedia;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Locale;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/* TODO make this thread-safe */

public class LocalArchive extends Archive {
    private StringNormalizer normalizer;

    private String directory;
    private Boolean readable;
    private File titleFile;
    private File mathIndexFile;
    private File mathDataFile;

    private String dumpOrigURL;
    private String dumpVersion;
    private String dumpNumArticles;
    private Boolean normalizedTitles;

    public LocalArchive(String directory, StringNormalizer normalizer) {
        this.directory = directory;
        titleFile = new File(directory, "titles.idx");

        if (!checkExistenceOfFiles()) {
            readable = false;
            return;
        }

        if (!titleFile.canRead()) {
            readable = false;
            return;
        }

        if (!readMetadata()) {
            readable = false;
            return;
        }

        mathIndexFile = new File(directory, "math.idx");
        mathDataFile = new File(directory, "math.dat");

        normalizedTitles = true;

        if (normalizedTitles) {
            this.normalizer = normalizer;
        } else {
            this.normalizer = new NeutralNormalizer();
        }

        readable = true;
    }

    public static Archive fromDatabase(String language, String date, String data) {
        String dir;
        try {
            JSONObject o = new JSONObject(data);
            dir = o.getString("dir");
        } catch (Exception e) {
            return null;
        }
        /* TODO try to get the normalizer from somewhere else.
         * best would be to store it in the archive */
        return new LocalArchive(dir, ArchiveManager.getInstance(null).getDefaultNormalizer());
    }

    public String toJSON() {
        JSONObject o = new JSONObject();
        try {
            o.put("dir", directory);
        } catch (JSONException e) {
            return null;
        }
        return o.toString();
    }

    @Override
    public boolean isMoreLocal(Archive other) {
        return !(other instanceof LocalArchive);
    }

    private boolean checkExistenceOfFiles() {
        File dir = new File(directory);
        if (!dir.exists()) {
            return false;
        }
        if (!(new File(dir, "metadata.txt")).exists()) {
            return false;
        }
        if (!titleFile.exists()) {
            return false;
        }
        return true;
    }

    private boolean readMetadata() {
        Wini ini;
        try {
            ini = new Wini(new File(directory, "metadata.txt"));
        } catch (InvalidFileFormatException e) {
            return false;
        } catch (IOException e) {
            return false;
        }

        date = ini.get("dump", "date");
        language = ini.get("dump", "language");
        dumpOrigURL = ini.get("dump", "orig_url");
        dumpVersion = ini.get("dump", "version");
        dumpNumArticles = ini.get("dump", "num_articles");

        normalizedTitles = true;
        if ("0".equals(ini.get("dump", "normalized_titles"))) {
            normalizedTitles = false;
        }
        return true;
    }

    public byte[] getMathImage(byte[] hexHash) throws IOException {
        if (hexHash == null || hexHash.length != 16) {
            throw new IllegalArgumentException("Invalid hexHash");
        }

        RandomAccessFile f = new RandomAccessFile(mathIndexFile, "r");

        long pos = -1, length = -1;

        final int entrysize = 16 + 4 + 4;
        long lo = 0;
        long hi = f.length() / entrysize;
        while (lo < hi) {
            long mid = (lo + hi) / 2;
            f.seek(mid * entrysize);
                byte[] entryHash = new byte[16];
            f.readFully(entryHash);
            int c = Utils.compareByteArrays(hexHash, entryHash);
            if (c == 0) {
                pos = LittleEndianReader.readUInt32(f);
                length = LittleEndianReader.readUInt32(f);
                break;
            } else if (c < 0) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        if (pos < 0 || length <= 0)
            return null;

            f = new RandomAccessFile(mathDataFile, "r");
        f.seek(pos);

        byte[] data = new byte[(int) length];
        f.readFully(data);
        return data;
    }

    public Title getTitle(String name) {
        /* TODO */
        TitleIterator iter = getTitlesWithPrefix(name);
        while (iter.hasNext()) {
            Title t = iter.next();
            String tname = t.getName();
            if (tname.equals(name))
                return t;
            if (tname.length() > name.length())
                return null;
        }
        return null;
    }

    public TitleIterator getTitlesWithPrefix(String prefix) {
        RandomAccessFile titles;
        try {
            titles = new RandomAccessFile(titleFile, "r");
        } catch (FileNotFoundException exc) {
            return new TitleIterator();
        }

        try {
            long lo = 0;
            long hi = titles.length();

            String prefix_normalized = normalizer.normalize(prefix);

            while (lo < hi) {
                long mid = (lo + hi) / 2;
                titles.seek(mid);
                byte[] line = readByteLine(titles);
                long aftermid = mid + line.length;
                if (mid > 0) { /* potentially incomplete line */
                    line = readByteLine(titles);
                    aftermid += line.length;
                }
                if (line.length == 0) { /* end of file */
                    hi = mid;
                } else {
                    Title title = Title.parseTitle(line, this, 0);
                    String nt = normalizer.normalize(title.getName());
                    if (nt.compareTo(prefix_normalized) < 0) {
                        lo = aftermid - 1;
                    } else {
                        hi = mid;
                    }
                }
            }
            if (lo > 0) {
                /* let lo point to the start of an entry */
                lo++;
            }
            titles.seek(lo);
        } catch (IOException exc) {
            return new TitleIterator();
        }
        return new TitleIterator(titles, prefix, this);
    }

    public Title getTitleAtOffset(long offset) {
        RandomAccessFile titles;
        try {
            titles = new RandomAccessFile(titleFile, "r");
        } catch (FileNotFoundException exc) {
            return null;
        }

        try {
            titles.seek(offset);
            byte[] line = readByteLine(titles);
            return Title.parseTitle(line, this, offset);
        } catch (IOException exc) {
            return null;
        }
    }

    public String getDirectory() {
        return directory;
    }

    public boolean isReadable() {
        return readable;
    }

    public byte[] getArticle(Title title) {
        if (title == null)
            return null;

        if (title.isRedirect()) {
            long offset = title.getRedirectOffset();
            if (offset == 0xffffffL) {
                /* invalid redirect */
                return null;
            } else {
                title = getTitleAtOffset(offset);
            }
        }

        if (title == null)
            return null;

        String filename = String.format(Locale.US, "wikipedia_%02d.dat", title.getFileNr());
        try {
            return BZReader.readAt(new FileInputStream(new File(directory, filename)),
                                  title.getBlockStart(), title.getBlockOffset(),
                                  title.getArticleLength());
        } catch (FileNotFoundException exc1) {
            return null;
        } catch (IOException exc2) {
            Log.e("LocalArchive", "Error reading article", exc2);
            return null;
        }
    }
    
    public StringNormalizer getStringNormalizer() {
        return normalizer;
    }

    /**
     * Reads an array of bytes from the current position in file until '\n' or
     * the end of the file is reached. The '\n' is included in the returned byte
     * array.
     * 
     * @param file
     *            the file to read from
     * @return the read bytes
     * @throws IOException
     */
    public static byte[] readByteLine(RandomAccessFile file) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(32);
        while (true) {
            int b = file.read();
            if (b >= 0)
                bytes.write(b);

            if (b == '\n' || b < 0) {
                return bytes.toByteArray();
            }
        }
    }
}
