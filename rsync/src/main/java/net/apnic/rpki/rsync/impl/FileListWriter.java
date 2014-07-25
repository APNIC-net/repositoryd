package net.apnic.rpki.rsync.impl;

import io.netty.buffer.ByteBuf;

/**
 * A writer of a non-incremental file list.
 *
 * @author Byron Ellacott
 * @since 2.0
 */
class FileListWriter implements Writer {

    FileListWriter() {

    }

    @Override
    public boolean write(ByteBuf into) {
        return false;
    }
}
