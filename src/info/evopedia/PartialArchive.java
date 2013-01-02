package info.evopedia;

import org.json.JSONObject;

public class PartialArchive extends Archive {
    @Override
    public boolean isMoreLocal(Archive other) {
        return (other instanceof DownloadableArchive);
    }
    public static Archive fromDatabase(String language, String date, String data, StringNormalizer normalizer) {
        return null; /* TODO not implemented */
    }
    @Override
    public String toJSON() {
        JSONObject o = new JSONObject();
        return o.toString();
    }
}
