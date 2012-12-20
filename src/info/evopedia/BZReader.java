package info.evopedia;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.bzip2.CBZip2InputStream;

import android.util.Log;

public class BZReader {

    public static byte[] readAt(InputStream stream, long blockStart, long blockOffset, long dataLength)
                throws IOException {
        if (stream == null) {
            throw new NullPointerException("input stream is null");
        }
        /* also skip "BZ" header */
        for (long i = 0; i < blockStart + 2; ) {
            i += stream.skip(blockStart + 2 - i);
        }

        byte[] data = new byte[(int) dataLength];
        try {
            CBZip2InputStream bzip = new CBZip2InputStream(new BufferedInputStream(stream, 20480));

            for (long read = 0; read < blockOffset; ) {
                read += bzip.skip(blockOffset - read);
            }

            for (int read = 0; read < dataLength; ) {
                int r = bzip.read(data, read, data.length - read);
                if (r == -1)
                    break;
                read += r;
            }
        } catch (NullPointerException exc) {
            Log.d("BZReader", "Null pointer exception from CBZip2InputStream", exc);
            return new byte[0];
        }
        return data;
    }
}
