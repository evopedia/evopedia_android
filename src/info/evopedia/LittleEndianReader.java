package info.evopedia;

import java.io.IOException;
import java.io.RandomAccessFile;

public class LittleEndianReader {
    public static final long readUInt32(byte[] data, int offset) {
        return (((long) data[offset + 3] & 0xffL) << 24) +
                (((long) data[offset + 2] & 0xffL) << 16) +
                (((long) data[offset + 1] & 0xffL) << 8) +
                (((long) data[offset + 0] & 0xffL));
    }

    public static final int readUInt16(byte[] data, int offset) {
        return (((int) data[offset + 1] & 0xff) << 8) +
                 ((int) data[offset + 0] & 0xff);
    }

    public static final short readUInt8(byte[] data, int offset) {
        return (short) ((int) data[offset] & 0xff);
    }

    public static final long readUInt32(RandomAccessFile f, long offset) throws IOException {
        f.seek(offset);
        return readUInt32(f);
    }

    public static final long readUInt32(RandomAccessFile f) throws IOException {
        byte[] data = new byte[4];
        f.readFully(data);
        return readUInt32(data, 0);
    }

    public static final long readUInt16(RandomAccessFile f, long offset) throws IOException {
        f.seek(offset);
        return readUInt16(f);
    }

    public static final long readUInt16(RandomAccessFile f) throws IOException {
        byte[] data = new byte[2];
        f.readFully(data);
        return readUInt16(data, 0);
    }

    public static final long readUInt8(RandomAccessFile f, long offset) throws IOException {
        f.seek(offset);
        return readUInt8(f);
    }

    public static final long readUInt8(RandomAccessFile f) throws IOException {
        byte[] data = new byte[1];
        f.readFully(data);
        return readUInt8(data, 0);
    }
}
