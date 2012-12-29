package info.evopedia;

import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

public class DownloadableArchive extends Archive {
    private URL url;
    private String size;

    public DownloadableArchive(String language, String date, URL url,
                                String size) {
        this.language = language;
        this.date = date;
        this.url = url;
        this.size = size;
    }

    public String getSize() {
        return size;
    }

    public static Archive fromDatabase(String language, String date, String data) {
        URL url;
        String size;
        try {
            JSONObject o = new JSONObject(data);
            url = new URL(o.getString("url"));
            size = o.getString("size");
        } catch (Exception e) {
            return null;
        }
        return new DownloadableArchive(language, date, url, size);
    }

    public String toJSON() {
        JSONObject o = new JSONObject();
        try {
            o.put("url", url.toString());
            o.put("size", size);
        } catch (JSONException e) {
            return null;
        }
        return o.toString();
    }

    @Override
    public boolean isMoreLocal(Archive other) {
        return false;
    }
}
