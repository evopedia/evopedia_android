package info.evopedia;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Locale;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;

import android.util.Log;

/* TODO make this thread-safe */

public class LocalArchive extends Archive {
	private StringNormalizer normalizer;

	private String directory;
	private Boolean readable;
	private File titleFile;

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

		normalizedTitles = true;

		if (normalizedTitles) {
			this.normalizer = normalizer;
		} else {
			this.normalizer = new NeutralNormalizer();
		}

		readable = true;
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
		byte[] block = new byte[(int) (title.getBlockOffset() + title.getArticleLength())];
		try {
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(directory, filename)));
			in.skip(title.getBlockStart() + 2); /* skip two header bytes */
			CBZip2InputStream bzip = new CBZip2InputStream(in);

			int read = 0;
			while (read < block.length) {
				int r = bzip.read(block, read, block.length - read);
				if (r == -1)
					break;
				read += r;
			}
		} catch (FileNotFoundException exc1) {
			return null;
		} catch (IOException exc2) {
			Log.e("LocalArchive", "Error reading article", exc2);
			return null;
		}

		byte[] articleData = new byte[(int) title.getArticleLength()];
		System.arraycopy(block, (int) title.getBlockOffset(), articleData, 0, articleData.length);
		return articleData;
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
