package net.apnic.rpki.rsync.impl;

import net.apnic.rpki.rsync.Module;
import net.apnic.rpki.rsync.Protocol;
import net.apnic.rpki.rsync.RsyncException;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version 30 protocol implementation
 *
 * @author Byron Ellacott
 * @since 2.0
 */
public class Protocol30 implements Protocol, InternalBuffer.Consumer {
    private enum State {
        RSYNC_HANDSHAKE,
        RSYNC_COMMAND
    }

    private State state;

    private final InternalBuffer buffer;
    private final ByteBuffer output;
    private final List<Module> modules;

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Pattern versionPattern = Pattern.compile("@RSYNCD: (\\d+)(?:\\.(\\d+))?");

    public Protocol30(List<Module> modules) {
        this.state = State.RSYNC_HANDSHAKE;
        this.buffer = new InternalBuffer(256, 16384);
        this.modules = modules;
        this.output = ByteBuffer.allocateDirect(16384);
        output.put("@RSYNCD: 30.0\n".getBytes(UTF8));
        output.flip();
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
                throw new RsyncException("buffer overrun detected");
            }
            if (src.get() == (byte)marker) {
                byte[] data = new byte[src.position() - startPosition - 1];
                src.reset();
                src.get(data);
                src.get();
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
            case RSYNC_HANDSHAKE:
                String handshake = delineatedString(src, 16, '\n');
                if (handshake == null) return;

                Matcher m = versionPattern.matcher(handshake);
                if (!m.matches())
                    throw new RsyncException("version handshake failure");

                int major = Integer.parseInt(m.group(1), 10);
                int minor = m.group(2) != null ? Integer.parseInt(m.group(2), 10) : 0;

                if (major < 30 || (major == 30 && minor != 0))
                    throw new RsyncException("version mismatch: 30 or greater expected");

                state = State.RSYNC_COMMAND;

                break;
            case RSYNC_COMMAND:
                String command = delineatedString(src, 40, 'n');
                if (command == null) return;

                // #list or a module name
                if (command.equals("#list")) {

                } else {

                }
        }
    }

    @Override
    public ByteBuffer transmit() {
        if (output.hasRemaining()) return output;
        return null;
    }
}
