package net.apnic.rpki.rsync.impl;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

/**
 * An abstract base class for writing data to the remote end.
 *
 * Message subclasses need only implement a constructor that calls setData if they
 * don't have any special needs.
 *
 * @author Byron Ellacott
 * @since 2.0
 */
abstract class AbstractByteWriter implements Writer {
    // commonly used value for subclasses
    static final Charset UTF8 = Charset.forName("UTF8");

    // the data to be written
    private byte[] data;

    /**
     * Set data to be written.
     *
     * @param data the data to be written
     * @since 2.0
     */
    void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public boolean write(ByteBuf into) {
        if (data == null) return true;

        if (data.length > into.writableBytes()) return false;
        into.writeBytes(data);
        return true;
    }
}
