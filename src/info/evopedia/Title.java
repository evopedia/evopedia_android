package info.evopedia;

import android.net.Uri;

public class Title implements Comparable<Title> {
	private long titleOffset;
	private String name;
	private short fileNr;
	private long blockStart;
	private long blockOffset;
	private long articleLength;
	private LocalArchive archive;

	private Title(LocalArchive archive, long titleOffset) {
	    this.archive = archive;
	    this.titleOffset = titleOffset;
	}

	public static Title parseTitle(byte[] encodedTitle, LocalArchive archive, long titleOffset) {
	    if (archive == null) {
	        throw new NullPointerException("archive cannot be null");
	    }
	    if (titleOffset < 0) {
	        throw new IllegalArgumentException("titleOffset cannot be negative (was " + titleOffset + ")");
	    }
	    Title t = new Title(archive, titleOffset);

		if (encodedTitle == null || encodedTitle.length < 15)
			return null;

		int escapes = LittleEndianReader.readUInt16(encodedTitle, 0);
		byte[] positionData = new byte[13];
		System.arraycopy(encodedTitle, 2, positionData, 0, 13);

        if ((escapes & (1 << 14)) == 1)
            escapes |= '\n';

		for (int i = 0; i < 13; i ++) {
			if ((escapes & (1 << i)) == 1)
				positionData[i] = '\n';
		}

		t.fileNr = LittleEndianReader.readUInt8(positionData, 0);
		t.blockStart = LittleEndianReader.readUInt32(positionData, 1);
		t.blockOffset = LittleEndianReader.readUInt32(positionData, 5);
		t.articleLength = LittleEndianReader.readUInt32(positionData, 9);

		int titleLenBytes = encodedTitle.length - 15;
		if (titleLenBytes > 0 && encodedTitle[encodedTitle.length - 1] == '\n')
			titleLenBytes --;

		t.name = new String(encodedTitle, 15, titleLenBytes);

		return t;
	}

	@Override
	public int compareTo(Title other) {
	    if (other == null)
	        return -1;
	
	    String thisName = archive.getStringNormalizer().normalize(name);
	    String otherName = other.archive.getStringNormalizer().normalize(other.name);
	    return thisName.compareTo(otherName);
	}

	public String getName() {
		return name;
	}
	
	public String getReadableName() {
		return name.replace("_", " ");
	}

	public LocalArchive getArchive() {
		return archive;
	}
	
	public short getFileNr() {
		return fileNr;
	}

	public long getBlockStart() {
		return blockStart;
	}

	public long getBlockOffset() {
		return blockOffset;
	}

	public long getArticleLength() {
		return articleLength;
	}
	
	public String toString() {
		return getName();
	}

	public boolean isRedirect() {
		return fileNr == 0xff;
	}
	
	public long getRedirectOffset() {
		return blockStart;
	}

	/* TODO I think these should be moved somewhere else */
	public Uri toUri() {
		if (name == null) return null;
		return Uri.parse("evopedia://" + archive.getLanguage() + "_" + archive.getDate() + "/@" + titleOffset);
	}

	public static ArchiveID archiveIDFromUri(Uri uri) {
		if (uri.getScheme() != "evopedia") return null;
		String[] parts = uri.getHost().split("_");
		return new ArchiveID(parts[0], parts[1]);
	}

	public static long titleOffsetFromUri(Uri uri) {
		if (uri.getScheme() != "evopedia") return -1;
		return Long.parseLong(uri.getEncodedPath().substring(2));
	}
}
