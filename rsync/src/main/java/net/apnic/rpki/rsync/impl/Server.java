package net.apnic.rpki.rsync.impl;

import io.netty.buffer.ByteBuf;
import net.apnic.rpki.rsync.Module;
import net.apnic.rpki.rsync.Protocol;
import net.apnic.rpki.rsync.RsyncException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version 29/30/31 protocol implementation
 *
 * @author Byron Ellacott
 * @since 2.0
 */
public class Server implements Protocol {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    public enum Option {
        OPT_INC_RECURSIVE,
        OPT_ALLOW_DEFLATE
    }

    private enum ReadState {
        READ_HANDSHAKE,
        READ_COMMAND,
        READ_ARGUMENTS,
        READ_FILTERS,
        READ_GENERATORS,
        READ_GOODBYE,
        READ_NOTHING
    }

    // Current state of the protocol
    private ReadState readState = ReadState.READ_HANDSHAKE;

    // Output pending delivery
    private Queue<AbstractBaseMessage> writeQueue = new ArrayDeque<>();

    // Arguments provided by remote end
    private List<String> arguments = new ArrayList<>();

    // Filters requested by remote end
    private List<String> filters = new ArrayList<>();

    // Module requested by remote end
    private Module chosenModule = null;

    // True when the session is finished
    private boolean finished = false;

    // Seed for file checksums
    private int checksumSeed = (int)(System.currentTimeMillis() / 1000);

    private final int serverVersion = 30; // TODO: support protocol 31
    private int negotiatedVersion;

    private final List<? extends Module> modules;
    private final EnumSet<Option> options;

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Pattern versionPattern = Pattern.compile("@RSYNCD: (\\d+)(?:\\.(\\d+))?");

    public Server(List<? extends Module> modules) {
        this(modules, EnumSet.noneOf(Option.class));
    }

    public Server(List<? extends Module> modules, EnumSet<Option> options) {
        this.modules = modules;
        this.options = options;
        // send a version message as soon as a remote end is ready to read it
        this.writeQueue.add(new VersionMessage(serverVersion, 0));
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

                Matcher m = versionPattern.matcher(handshake);
                if (!m.matches())
                    throw new RsyncException("version handshake failure");

                int major = Integer.parseInt(m.group(1), 10);
                int minor = m.group(2) != null ? Integer.parseInt(m.group(2), 10) : 0;

                // Version 30+ clients must send the minor version, else they're a weird v30 beta
                if (major == 30 && m.group(2) == null)
                    throw new RsyncException("client is speaking an incompatible beta of protocol 30");

                // Negotiate version
                negotiatedVersion = serverVersion;
                if (negotiatedVersion > major)
                    negotiatedVersion = major;
                if (minor != 0)
                    negotiatedVersion--;

                if (negotiatedVersion < 29)
                    throw new RsyncException("client is an ancient version, try to upgrade it");

                LOGGER.debug("Negotiated version {}, remote {}.{}, local {}", negotiatedVersion, major, minor, serverVersion);

                readState = ReadState.READ_COMMAND;
                break;
            case READ_COMMAND:
                String command = delineatedString(input, 40, (byte)'\n');
                if (command == null) return;

                // #list or a module name
                if (command.equals("") || command.equals("#list")) {
                    LOGGER.debug("Remote called for a module list");
                    writeQueue.add(new ModuleListMessage(modules));
                    finished = true;
                    readState = ReadState.READ_NOTHING;
                } else {
                    for (Module module: modules) {
                        if (module.getName().equals(command)) {
                            chosenModule = module;
                            break;
                        }
                    }
                    if (chosenModule != null) {
                        LOGGER.debug("Module {} requested; valid", command);
                        writeQueue.add(new StringMessage("@RSYNCD: OK\n"));
                        readState = ReadState.READ_ARGUMENTS;
                    } else {
                        LOGGER.debug("Module {} requested; invalid", command);
                        writeQueue.add(new StringMessage(
                                String.format("@ERROR: Unknown module '%s'\n", command)));
                        finished = true;
                        readState = ReadState.READ_NOTHING;
                    }
                }
                break;
            case READ_ARGUMENTS:
                byte delimiter = negotiatedVersion >= 30 ? 0 : (byte)'\n';
                String argument = delineatedString(input, 40, delimiter);
                if (argument == null) return;

                LOGGER.debug("Argument read: {}", argument);

                if (argument.isEmpty()) {
                    LOGGER.debug("Argument list complete");
                    byte cFlag = 0;

                    // Incremental recursive file list
                    if (options.contains(Option.OPT_INC_RECURSIVE))
                        cFlag |= (1 << 0);

                    // symlink times not supported
                    // symlink iconv not supported
                    // safe flist TODO based on client flags
                    // avoid xattr optimisation not supported (TODO: protocol 31 option)

                    writeQueue.add(new SetupMessage(cFlag, checksumSeed));

                    // Server writes checksum seed int, client reads it
                    readState = ReadState.READ_FILTERS;
                } else {
                    arguments.add(argument);
                }
                break;
            case READ_FILTERS:
                String filter = delineatedString(input, 40, (byte)'\n');
                if (filter == null) return;

                if (filter.isEmpty()) {
                    // TODO: establish mode
                } else {
                    filters.add(filter);
                }
                break;
            case READ_GENERATORS:
                break;
            case READ_GOODBYE:
                break;
            case READ_NOTHING:
                // ignore all yo' jabberin.
                break;
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
    public boolean isFinished() {
        return finished;
    }
}
