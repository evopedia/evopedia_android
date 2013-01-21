package info.evopedia;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class InterLanguageLinkCache {
    private class CacheEntry {
        public final String articleUrl;
        public final ArrayList<InterLanguageLink> links;

        public CacheEntry(String articleUrl, ArrayList<InterLanguageLink> links) {
            this.articleUrl = articleUrl;
            this.links = links;
        }
    }
    private LinkedList<CacheEntry> interLanguageLinks;
    private final int cacheSize;

    public InterLanguageLinkCache() {
        this(10);
    }

    public InterLanguageLinkCache(int cacheSize) {
        this.cacheSize = cacheSize;
        interLanguageLinks = new LinkedList<CacheEntry>();
    }

    public synchronized ArrayList<InterLanguageLink> getLinks(String articleUrl) {
        for (CacheEntry entry : interLanguageLinks) {
            if (entry.articleUrl.equals(articleUrl)) {
                return entry.links;
            }
        }
        return null;
    }

    public synchronized void setLinks(String articleUrl, ArrayList<InterLanguageLink> links) {
        while (interLanguageLinks.size() >= cacheSize) {
            interLanguageLinks.removeLast();
        }
        interLanguageLinks.addFirst(new CacheEntry(articleUrl, links));
    }
}
