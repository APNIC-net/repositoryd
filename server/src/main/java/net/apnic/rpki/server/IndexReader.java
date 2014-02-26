package net.apnic.rpki.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

class IndexReader {
    static final int NDX_DONE = -1;

    private int[] prev_positive = { -1 };
    private int[] prev_negative = { 1 };

    IndexReader() {}

    Integer readIndex(ByteBuf in) {
        if (in.readableBytes() < 1) return null;

        int[] prev = prev_positive;

        int b = in.readUnsignedByte();
        if (b == 0xff) {
            prev = prev_negative;
            if (in.readableBytes() < 1) return null;
            b = in.readUnsignedByte();
        } else if (b == 0) {
            return NDX_DONE;
        }

        int num;
        if (b == 0xfe) {
            if (in.readableBytes() < 2) return null;
            b = in.readUnsignedByte();
            if ((b & 0x80) == 0x80) {
                if (in.readableBytes() < 3) return null;
                num = ByteBufUtil.swapMedium(in.readMedium()) + ((b & ~0x80) << 24);
            } else {
                num = (b << 8) + in.readByte() + prev[0];
            }
        } else {
            num = b + prev[0];
        }
        prev[0] = num;
        if (prev == prev_negative)
            num = -num;

        return num;
    }
}
