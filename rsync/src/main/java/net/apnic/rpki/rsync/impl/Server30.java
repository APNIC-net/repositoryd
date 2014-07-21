package net.apnic.rpki.rsync.impl;

import io.netty.buffer.ByteBuf;
import net.apnic.rpki.rsync.Module;
import net.apnic.rpki.rsync.Protocol;
import net.apnic.rpki.rsync.RsyncException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version 30 protocol implementation
 *
 * @author Byron Ellacott
 * @since 2.0
 */
public class Server30 implements Protocol {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server30.class);

    private enum ReadState {
        READ_HANDSHAKE,
        READ_COMMAND
    }

    private ReadState readState;
    private Queue<AbstractBaseMessage> writeQueue;

    private final List<Module> modules;

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Pattern versionPattern = Pattern.compile("@RSYNCD: (\\d+)(?:\\.(\\d+))?");

    public Server30(List<Module> modules) {
        this.readState = ReadState.READ_HANDSHAKE;
        this.writeQueue = new ArrayDeque<>();
        this.writeQueue.add(new VersionMessage(30, 0));
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
        switch (readState) {
            case READ_HANDSHAKE:
                String handshake = delineatedString(input, 16, (byte)'\n');
                if (handshake == null) return;

                LOGGER.debug("Handshake: {}", handshake);

                Matcher m = versionPattern.matcher(handshake);
                if (!m.matches())
                    throw new RsyncException("version handshake failure");

                int major = Integer.parseInt(m.group(1), 10);
                int minor = m.group(2) != null ? Integer.parseInt(m.group(2), 10) : 0;

                LOGGER.debug("Remote version: {}.{}", major, minor);

                if (major < 30 || (major == 30 && minor != 0))
                    throw new RsyncException("version mismatch: 30 or greater expected");

                readState = ReadState.READ_COMMAND;
                break;
            case READ_COMMAND:
                String command = delineatedString(input, 40, (byte)'\n');
                if (command == null) return;

                // #list or a module name
                if (command.equals("") || command.equals("#list")) {
                    writeQueue.add(new ModuleListMessage(modules));
                }
        }
    }

    @Override
    public boolean write(ByteBuf buffer) {
        if (writeQueue.isEmpty()) return false;

        if (writeQueue.peek().write(buffer)) {
            writeQueue.remove();
            return true;
        }
        return false;
    }

    @Override
    public List<Module> getModules() {
        return modules;
    }
}
