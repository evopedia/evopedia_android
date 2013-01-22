package info.evopedia;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import android.database.CharArrayBuffer;

public class Utils {
    public static final int compareByteArrays(byte[] a, byte[] b) {
        if (a == null || b == null)
            throw new NullPointerException();

        int l = Math.min(a.length, b.length);
        for (int i = 0; i < l; i ++) {
            int c = (a[i] & 0xff) - (b[i] & 0xff);
            if (c != 0) return c;
        }
        return a.length - b.length;
    }

    public static final byte[] decodeHexString(String str) {
        if (str == null)
            throw new NullPointerException();
        if (str.length() % 2 != 0)
            throw new IllegalArgumentException("Hex string does not have even length.");

        ByteArrayOutputStream out = new ByteArrayOutputStream(32);

        try {
            int length = str.length();
            for (int i = 0; i + 1 < length; i += 2) {
                out.write(Integer.decode("0x" + str.substring(i, i + 2)));
            }
        } catch (NumberFormatException exc) {
            throw new IllegalArgumentException("Invalid hex string.", exc);
        }
        return out.toByteArray();
    }

    public static String readInputStream(InputStream s) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        for (int i = 0; (i = s.read(buf)) != -1; ) {
            out.write(buf,  0, i);
        }
        return out.toString();
    }

    public static String joinString(Collection<String> stringList, String junction) {
        if (stringList == null || stringList.size() == 0)
            return "";

        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (String s : stringList) {
            if (i > 0)
                builder.append(junction);
            builder.append(s);
            i ++;
        }
        return builder.toString();
    }
}
