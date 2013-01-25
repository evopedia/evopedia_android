package info.evopedia;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.util.Arrays;

public class ByteLineReader {
    private final static int bufSize = 512;
    private final RandomAccessFile file;
    private long filePosition;
    private final long fileLength;
    private int bufPosition;
    private final byte[] buffer;

    public ByteLineReader(RandomAccessFile file) throws IOException {
        this.file = file;
        fileLength = file.length();
        filePosition = file.getFilePointer();
        bufPosition = bufSize;
        buffer = new byte[bufSize];
    }

    public byte[] nextLine() throws IOException {
        for (int i = bufPosition; i < bufSize; i ++) {
            if (buffer[i] == '\n') {
                byte[] line = new byte[i - bufPosition];
                System.arraycopy(buffer, bufPosition, line, 0, i - bufPosition);
                filePosition += i + 1 - bufPosition;
                bufPosition = i + 1;
                return line;
            }
        }

        System.arraycopy(buffer, bufPosition, buffer, 0, bufSize - bufPosition);
        int i = bufSize - bufPosition;
        bufPosition = 0;
        /* TODO problems at file end */
        file.readFully(buffer, i, bufSize - i);

        for (; i < bufSize; i ++) {
            if (buffer[i] == '\n') {
                byte[] line = new byte[i - bufPosition];
                System.arraycopy(buffer, bufPosition, line, 0, i - bufPosition);
                filePosition += i + 1 - bufPosition;
                bufPosition = i + 1;
                return line;
            }
        }
        return null;
    }

    public long getPosition() {
        return filePosition;
    }
}
