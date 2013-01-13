package info.evopedia;

public abstract class Archive implements Comparable<Archive>{
    protected String language;
    protected String date;
    
    public Archive() {
    }
    public String getLanguage() {
        return language;
    }
    public String getDate() {
        return date;
    }
    public ArchiveID getID() {
        return new ArchiveID(language, date);
    }
    public abstract boolean isMoreLocal(Archive other);
    public abstract String toJSON();
    @Override
    public int compareTo(Archive other) {
        return getID().compareTo(other.getID());
    }
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!this.getClass().isInstance(other))
            return false;
        Archive othera = (Archive) other;
        return getID().equals(othera.getID());
    }
    @Override
    public int hashCode() {
        return 41 + 31 * getID().hashCode() + getClass().hashCode();
    }
}
