package net.apnic.rpki.rsync.impl;

import io.netty.buffer.ByteBuf;
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
public class Server30 implements Protocol {
    private enum State {
        RSYNC_HANDSHAKE,
        RSYNC_COMMAND
    }

    private State state;

    private final List<Module> modules;

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Pattern versionPattern = Pattern.compile("@RSYNCD: (\\d+)(?:\\.(\\d+))?");

    public Server30(List<Module> modules) {
        this.state = State.RSYNC_HANDSHAKE;
        this.modules = modules;
    }

    private String delineatedString(ByteBuf in, int sizeCap, byte delimiter) throws RsyncException {
        int messageSize = in.bytesBefore(delimiter);

        if (messageSize == -1 && in.readableBytes() > sizeCap)
            throw new RsyncException("buffer overrun attempt");

        if (messageSize == -1) return null;

        byte[] data = new byte[messageSize];
        in.readBytes(data);
        in.skipBytes(1); // skip delimiter

        return new String(data, UTF8);
    }

    @Override
    public void read(ByteBuf input) throws RsyncException {
        switch (state) {
            case RSYNC_HANDSHAKE:
                String handshake = delineatedString(input, 16, (byte)'\n');
                if (handshake == null) return;

                Matcher m = versionPattern.matcher(handshake);
                if (!m.matches())
                    throw new RsyncException("version handshake failure");

                int major = Integer.parseInt(m.group(1), 10);
                int minor = m.group(2) != null ? Integer.parseInt(m.group(2), 10) : 0;

                if (major < 30 || (major == 30 && minor != 0))
                    throw new RsyncException("version mismatch: 30 or greater expected");

                // TODO: a client implementation would send a command, not receive one
                state = State.RSYNC_COMMAND;

                break;
            case RSYNC_COMMAND:
                String command = delineatedString(input, 40, (byte)'\n');
                if (command == null) return;

                // #list or a module name
                if (command.equals("") || command.equals("#list")) {
                    StringBuilder builder = new StringBuilder();
//                    for (Module module : modules) {
//                        builder.append(module.getName());
//                        builder.append(": ");
//                        builder.append(module.getDescription());
//                        builder.append("\n");
//                    }
                    builder.append("@RSYNCD: EXIT\n");
//                    return ByteBuffer.wrap(builder.toString().getBytes(UTF8));
                }
        }
    }

    @Override
    public boolean write(ByteBuf buffer) {
        return false;
    }

    @Override
    public List<Module> getModules() {
        return modules;
    }
}
