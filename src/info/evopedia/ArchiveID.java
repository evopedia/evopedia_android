package info.evopedia;

public class ArchiveID implements Comparable<ArchiveID> {
	private final String language;
	private final String date;
	
	public ArchiveID(String language, String date) {
		if (language == null || date == null)
			throw new NullPointerException("date and language cannot be null");
		this.language = language;
		this.date = date;
	}
	public String getLanguage() {
		return language;
	}
	public String getDate() {
		return date;
	}
   public int hashCode() {
    	int languageHash = language != null ? language.hashCode() : 0;
    	int dateHash = date != null ? date.hashCode() : 0;

    	return (languageHash + dateHash) * dateHash + languageHash;
    }
    @Override
    public boolean equals(Object other) {
    	if (!(other instanceof ArchiveID))
    		return false;

    	ArchiveID otherAID = (ArchiveID) other;
		return (this.language.equals(otherAID.language) &&
				 this.date.equals(otherAID.date));
    }

	@Override
	public int compareTo(ArchiveID other) {
		if (other == null)
			return 1;

		int c = this.language.compareTo(other.language);
		if (c != 0) {
		    return c;
		} else {
		    return -this.date.compareTo(other.date);
		}
	}

	public String toString() {
		return language + "_" + date;
	}

	public static ArchiveID fromString(String s) {
		String[] parts = s.split("_");
		if (parts.length != 2)
			return null;
		return new ArchiveID(parts[0], parts[1]);
	}
}
