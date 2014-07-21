package net.apnic.rpki.rsync.impl;

import io.netty.buffer.ByteBuf;
import net.apnic.rpki.rsync.Module;
import net.apnic.rpki.rsync.Protocol;
import net.apnic.rpki.rsync.RsyncException;

import java.util.List;

/**
 * The protocol you use when you don't know what to do.
 *
 * This Protocol is neither a sender nor a receiver.  It will simply throw an RsyncException
 * when it is communicated with.
 *
 * @author Byron Ellacott
 * @since 2.0
 */
class NullProtocol implements Protocol {
    @Override
    public void read(ByteBuf input) throws RsyncException {
        throw new RsyncException("Protocol configured as neither sender nor receiver");
    }

    @Override
    public boolean write(ByteBuf buffer) {
        return false;
    }

    @Override
    public boolean isFinished() {
        return true;
    }
}
