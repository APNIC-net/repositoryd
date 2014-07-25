package net.apnic.rpki.rsync.impl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Protocol setup - compatibility flags and checksum seed data
 *
 * @author Byron Ellacott
 * @since 2.0
 */
class SetupWriter extends AbstractByteWriter {
    SetupWriter(byte cFlags, int seed) {
        ByteBuffer buf = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN).put(cFlags).putInt(seed);
        setData(buf.array());
    }
}
