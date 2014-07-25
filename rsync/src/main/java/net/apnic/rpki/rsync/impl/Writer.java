package net.apnic.rpki.rsync.impl;

import io.netty.buffer.ByteBuf;

/**
 * Write data to a remote end when requested.
 *
 * The connectivity layer will provide its preferred buffer size via the argument
 * to into.
 *
 * @author Byron Ellacott
 * @since 2.0
 */
interface Writer {
    /**
     * Write into an output buffer.
     *
     * @param into the buffer to write into
     * @return true if the message was completely written, false otherwise
     * @since 2.0
     */
    boolean write(ByteBuf into);
}
