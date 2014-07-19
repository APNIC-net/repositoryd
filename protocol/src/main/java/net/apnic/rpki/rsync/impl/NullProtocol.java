package net.apnic.rpki.rsync.impl;

import net.apnic.rpki.rsync.Protocol;
import net.apnic.rpki.rsync.RsyncException;

import java.nio.ByteBuffer;

/**
 * The protocol you use when you don't know what to do.
 *
 * This Protocol is neither a sender nor a receiver.  It will simply throw an RsyncException
 * when it is communicated with.
 *
 */
class NullProtocol implements Protocol {
    @Override
    public void receive(ByteBuffer input) throws RsyncException {
        throw new RsyncException("Protocol configured as neither sender nor receiver");
    }

    @Override
    public ByteBuffer transmit() {
        return null;
    }
}
