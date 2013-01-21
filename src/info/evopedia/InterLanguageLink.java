package info.evopedia;

public class InterLanguageLink {
    private String languageID;
    private String languageName;
    private String articleName;

    public InterLanguageLink(String languageID, String languageName, String articleName) {
        this.languageID = languageID;
        this.languageName = languageName;
        this.articleName = articleName;
    }

    public String getLanguageID() {
        return languageID;
    }

    public String getLanguageName() {
        return languageName;
    }

    public String getArticleName() {
        return articleName;
    }

    public String toString() {
        return languageName;
    }
}
