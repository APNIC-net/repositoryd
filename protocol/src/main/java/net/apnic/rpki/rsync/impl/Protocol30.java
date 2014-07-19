package net.apnic.rpki.rsync.impl;

import net.apnic.rpki.rsync.Protocol;
import net.apnic.rpki.rsync.RsyncException;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Version 30 protocol implementation
 *
 * @author Byron Ellacott
 * @since 2.0
 */
public class Protocol30 implements Protocol, InternalBuffer.Consumer {
    private enum State {
        RSYNC_HANDSHAKING,
    }

    private State state;
    private InternalBuffer buffer;

    private final Charset UTF8;

    public Protocol30() {
        this.state = State.RSYNC_HANDSHAKING;
        this.buffer = new InternalBuffer(256, 16384);
         UTF8 = Charset.forName("UTF-8");
    }

    @Override
    public void receive(ByteBuffer input) throws RsyncException {
        try {
            buffer.buffer(input, this);
        } catch (RsyncException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String delineatedString(ByteBuffer src, int limit, char marker) throws RsyncException {
        src.mark();
        int startPosition = src.position();
        while (src.hasRemaining()) {
            if (src.position() > startPosition + limit) {
                throw new RsyncException("protocol error - buffer overrun attempted");
            }
            if (src.getChar() == marker) {
                byte[] data = new byte[src.position() - startPosition];
                src.reset();
                src.get(data);
                return new String(data, UTF8);
            }
        }
        src.reset();
        return null;
    }

    @Override
    public void consume(ByteBuffer src) throws Exception {
        if (!src.hasRemaining()) return;
        switch (state) {
            case RSYNC_HANDSHAKING:
                String handshake = delineatedString(src, 16, '\n');
                break;
        }
    }

    @Override
    public ByteBuffer transmit() {
        return null;
    }
}
