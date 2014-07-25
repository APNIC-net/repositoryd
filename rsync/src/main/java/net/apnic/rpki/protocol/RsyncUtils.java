package net.apnic.rpki.protocol;

import java.io.ByteArrayOutputStream;

/**
 * A set of encoding tools for producing byte data compatible with rsync.
 *
 * @author bje
 * @since 0.9
 */
public class RsyncUtils {
    private static void write_varnum(ByteArrayOutputStream data, long num, int maxBytes, int minBytes) {
        byte[] buf = new byte[maxBytes+1];
        int bit;
        int cnt;

        buf[0] = 0;

        for (cnt = 1; cnt <= maxBytes; cnt++) {
            buf[cnt] = (byte)(num & 0xff);
            num >>= 8;
            if (num == 0 && cnt >= minBytes) break;
        }

        bit = 1 << (7 - cnt + minBytes);
        if (buf[cnt] >= bit) {
            cnt++;
            buf[0] = (byte)~(bit - 1);
        } else if (cnt > minBytes) {
            buf[0] = (byte)(buf[cnt] | ~(bit * 2 - 1));
        } else {
            buf[0] = buf[cnt];
        }
        data.write(buf, 0, cnt);
    }

    /**
     * Writes the given int value to the data stream as a variable length field.
     *
     * @param data where to write the bytes
     * @param value what value to write
     * @since 0.9
     */
    public static void write_varint(ByteArrayOutputStream data, int value) {
        write_varnum(data, value, 4, 1);
    }

    /**
     * Writes the given long value to the datastream as a variable length field.
     *
     * @param data where to write the bytes
     * @param value what value to write
     * @param minBytes how many bytes to initially use
     * @since 0.9
     */
    public static void write_varlong(ByteArrayOutputStream data, long value, int minBytes) {
        write_varnum(data, value, 8, minBytes);
    }
}
