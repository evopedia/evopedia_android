package info.evopedia;

import java.io.ByteArrayOutputStream;

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

}
