package net.apnic.rpki.rsync.impl;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

/**
 * The abstract base class for outbound messages waiting in the queue.
 *
 * Message subclasses need only implement a constructor that calls setData if they
 * don't have any special needs.
 *
 * @author Byron Ellacott
 * @since 2.0
 */
abstract class AbstractBaseMessage {
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

    /**
     * Write into an output buffer.
     *
     * @param into the buffer to write into
     * @return true if the message was completely written, false otherwise
     * @since 2.0
     */
    boolean write(ByteBuf into) {
        if (data == null) return true;

        if (data.length > into.writableBytes()) return false;
        into.writeBytes(data);
        return true;
    }
}
