package net.apnic.rpki.rsync.impl;

import net.apnic.rpki.rsync.Protocol;
import net.apnic.rpki.rsync.RsyncException;

import java.nio.ByteBuffer;

/**
 * Version 30 protocol implementation
 *
 * @author Byron Ellacott
 * @since 2.0
 */
public class Protocol30 implements Protocol {
    private enum State {
        RSYNC_HANDSHAKING,
    }

    private State state;

    public Protocol30() {
        this.state = State.RSYNC_HANDSHAKING;
    }

    @Override
    public void receive(ByteBuffer input) throws RsyncException {
        // if previous data is retained, add "some" of this new one to it and keep
        // attempting to consume messages until either the internal buffer is too large,
        // or the input is fully consumed.  Once previous data is fully consumed, start
        // processing directly from the given buffer.  Once no more messages are available
        // to be read, copy the remaining data to a new internal buffer.
    }

    @Override
    public ByteBuffer transmit() {
        return null;
    }
}
