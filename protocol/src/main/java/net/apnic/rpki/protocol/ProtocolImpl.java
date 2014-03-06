package net.apnic.rpki.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static net.apnic.rpki.protocol.RsyncUtils.write_varlong;

// private implementation; do not dig too deeply into this, you'll have nightmares.
class ProtocolImpl implements Protocol {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolImpl.class);

    private final int version;
    private final List<Module> modules;

    private Module activeModule = null;
    private Map<String, List<String>> properties;
    private int checksumSeed = (int)(System.currentTimeMillis() / 1000);

    private FileList fileList;
    private int phase;

    ProtocolImpl(int version, List<Module> modules) {
        this.version = version;

        this.modules = modules;
    }

    @Override public int getVersion() { return version; }

    private final static int NDX_DONE = -1;

    private int previous_positive = -1, previous_negative = 1;
    private void writeNdx(MessageSender sender, int ndx) {
        int diff;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        if (ndx >= 0) {
            diff = ndx - previous_positive;
            previous_positive = ndx;
        } else if (ndx == NDX_DONE) {
            sender.sendByte(0);
            return;
        } else {
            buf.write(0xFF);
            ndx = -ndx;
            diff = ndx - previous_negative;
            previous_negative = ndx;
        }

        if (diff < 0xFE && diff > 0) {
            buf.write((byte) diff);
        } else if (diff < 0 || diff > 0x7fff) {
            buf.write((byte) 0xFE);
            buf.write((byte) ((ndx >> 24) | 0x80));
            buf.write((byte) ndx);
            buf.write((byte) (ndx >> 8));
            buf.write((byte) (ndx >> 16));
        } else {
            buf.write((byte) 0xFE);
            buf.write((byte) (diff >> 8));
            buf.write((byte) diff);
        }
        sender.sendBytes(buf.toByteArray());
    }

    private void writeInt(MessageSender sender, int value) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte)(value & 0xff);
        bytes[1] = (byte)((value >> 8) & 0xff);
        bytes[2] = (byte)((value >> 16) & 0xff);
        bytes[3] = (byte)((value >> 24) & 0xff);
        sender.sendBytes(bytes);
    }

    @Override
    public void sendFileList(List<String> paths, MessageSender sender) throws ProtocolError {
        if (paths.size() != 1) throw new ProtocolError(ProtocolError.ErrorType.FERROR, "Requesting multiple paths is not supported");

        phase = 0;

        for (String path: paths) {
            try {
                fileList = activeModule.getFileList(path, isRecursive());

                if (fileList.getRoot().isDirectory() && !isRecursive() && !xferDirs()) {
                    sender.sendInformation("skipping directory " + path);
                    continue;
                }

                sender.sendBytes(fileList.getFileListData());
            } catch (NoSuchPathException ex) {
                throw new ProtocolError(ProtocolError.ErrorType.FERROR, "the requested path does not exist.");
            }
        }


        sender.sendByte(0);
    }

    @Override
    public boolean sendExtraFileList(MessageSender sender) {
        return true;
    }

    @Override
    public void transferFile(TransferAttributes attributes, Checksums checksums, MessageSender sender) throws ProtocolError {
        writeNdx(sender, attributes.getFileIndex());

        EnumSet<ItemFlag> flags = attributes.getItemFlags();

        int flagBits = ItemFlag.intFromSet(flags);
        sender.sendByte(flagBits & 0xff);
        sender.sendByte(flagBits >> 8);

        if (flags.contains(ItemFlag.ITEM_BASIS_TYPE_FOLLOWS)) {
            sender.sendByte(attributes.getBasisType());
        }

        if (flags.contains(ItemFlag.ITEM_XNAME_FOLLOWS)) {
            byte[] bytes = attributes.getFuzzyName().getBytes(Charset.forName("UTF-8"));
            int length = bytes.length;
            if (length > 0x7f) {
                if (length > 0x7fff) throw new ProtocolError(ProtocolError.ErrorType.FERROR,
                        "Attempting to send over-long vstring");
                sender.sendByte((length >> 8) + 0x80);
            }
            sender.sendByte(length & 0xff);
        }

        if (attributes.getFileIndex() >= fileList.getSize())
            throw new ProtocolError(ProtocolError.ErrorType.FERROR,
                    String.format("RsyncFile-list index %d not in %d - %d [repositoryd]",
                            attributes.getFileIndex(), 0, fileList.getSize() - 1));

        int position = attributes.getFileIndex() - fileList.getFirstIndex();
        RsyncFile file = (position >= 0) ? fileList.getFile(position) : fileList.getRoot();

        LOGGER.debug("send_file({}, {}, {})", attributes.getFileIndex(), attributes.getItemFlags(), file);

        if (flags.contains(ItemFlag.ITEM_TRANSFER)) {
            writeInt(sender, checksums.getBlockCount());
            writeInt(sender, checksums.getBlockSize());
            writeInt(sender, checksums.getSecondaryLength());
            writeInt(sender, checksums.getRemainder());

            if (file == null) {
                throw new ProtocolError(ProtocolError.ErrorType.FERROR, "Invalid transfer index");
            }

            if (properties.containsKey("compress")) {
                byte[] data = file.getCompressedContents();
                for (int l = 0; l < data.length; l += 16383) {
                    int nextSize = Math.min(16383, data.length - l);
                    sender.sendByte(0x40 + (nextSize >> 8));
                    sender.sendByte(nextSize & 0xff);
                    sender.sendBytes(data, l, nextSize);
                }
                sender.sendByte(0);
            } else {
                byte[] data = file.getContents();
                for (int l = 0; l < data.length; l += 32*1024) {
                    int nextSize = Math.min(32*1024, data.length - l);
                    writeInt(sender, nextSize);
                    sender.sendBytes(data, l, nextSize);
                }
                writeInt(sender, 0);
            }
            sender.sendBytes(file.getChecksum());
        }
    }

    @Override
    public boolean completedList(MessageSender sender) throws ProtocolError {
        phase++;
        LOGGER.debug("All file lists completed, sender at phase {}", phase);
        if (phase == 2) {
            writeNdx(sender, NDX_DONE);
            LOGGER.debug("Sending transfer statistics");
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            write_varlong(os, 0, 3); // total_read
            write_varlong(os, 0, 3); // total_written
            write_varlong(os, 0, 3); // total_size
            write_varlong(os, 0, 3); // flist_buildtime
            write_varlong(os, 0, 3); // flist_xfertime
            sender.sendBytes(os.toByteArray());
            return false;
        } else if (phase > 2) {
            writeNdx(sender, NDX_DONE);
            return true;
        }
        writeNdx(sender, NDX_DONE);
        return false;
    }

    @Override
    public Iterable<Module> getModuleList() {
        return modules;
    }

    @Override
    public Module getModuleByName(String module) {
        for (Module mod : modules) {
            if (mod.getName().equals(module)) return mod;
        }
        return null;
    }

    @Override
    public void selectModule(String module) throws NoSuchModuleException {
        Module mod = getModuleByName(module);
        if (mod == null) throw new NoSuchModuleException();
        activeModule = mod;
    }

    @Override
    public void setProperties(Map<String, List<String>> properties) throws ProtocolError {
        this.properties = properties;

        // Confirm that the remote end thinks they're the receiver
        if (!properties.containsKey("sender")) {
            throw new ProtocolError(ProtocolError.ErrorType.FERROR, "Module is read only");
        }

        // Confirm that only understood arguments have been supplied
        List<String> badProperties = PropertiesValidator.validateProperties(properties);
        if (!badProperties.isEmpty()) {
            LOGGER.debug("Invalid arguments supplied: {}", badProperties);

            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (String error: badProperties) {
                if (first)
                    first = false;
                else
                    builder.append(", ");
                builder.append(error);
            }
            throw new ProtocolError(ProtocolError.ErrorType.FERROR, "Unsupported arguments given: " + builder);
        }

        // Confirm that either recursion is disabled, or incremental recursion is supported
        if (isRecursive() && !getClientInfo().contains("i")) {
            throw new ProtocolError(ProtocolError.ErrorType.FERROR, "Incremental recursion is required");
        }

        // Set the checksum seed straight away
        if (properties.containsKey("checksum_seed")) {
            checksumSeed = Integer.parseInt(properties.get("checksum_seed").get(0), 10);
        }
    }

    private boolean isRecursive() {
        return properties.containsKey("recurse");
    }

    private boolean xferDirs() {
        return properties.containsKey("xfer_dirs");
    }

    private String getClientInfo() {
        List<String> flags = properties.get("shell_cmd");
        return flags == null ? "" : flags.get(0);
    }

    @Override
    public byte getCompatibilityFlags() {
        byte flags = 0;

        // We don't support incremental recursion
        // We don't support symlink times
        // We don't support symlink iconv

        // Safe FLIST supported if client sends 'f' in client_info (only 3.0.9 did this, so protocol 30 isn't uniform!)
        flags |= getClientInfo().contains("f") ? (1 << 3) : 0;

        return flags;
    }

    @Override
    public int getChecksumSeed() {
        return checksumSeed;
    }
}