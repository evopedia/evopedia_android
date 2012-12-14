package info.evopedia;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.bzip2.CBZip2InputStream;

public class BZReader {

    public static byte[] readAt(InputStream stream, long blockStart, long blockOffset, long dataLength)
                throws IOException {
        stream.skip(blockStart + 2); /* skip two header bytes */
        CBZip2InputStream bzip = new CBZip2InputStream(new BufferedInputStream(stream, 20480));

        for (long read = 0; read < blockOffset; ) {
            read += bzip.skip(blockOffset - read);
        }

        byte[] data = new byte[(int) dataLength];
        for (int read = 0; read < dataLength; ) {
            int r = bzip.read(data, read, data.length - read);
            if (r == -1)
                break;
            read += r;
        }
        return data;
    }
}
