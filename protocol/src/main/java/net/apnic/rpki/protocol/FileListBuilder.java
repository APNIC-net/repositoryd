package net.apnic.rpki.protocol;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static net.apnic.rpki.protocol.RsyncUtils.writeVarint;
import static net.apnic.rpki.protocol.RsyncUtils.writeVarlong;

// CHECKSTYLE:OFF MagicNumber
class FileListBuilder {
    FileListBuilder() {

    }

    /**
     * Make a file list for the given root, node, and recursion type.
     *
     * If the node is at the root, the first entry in the first list will be '.' to represent the parent.
     *
     * @param root the root directory for the list
     * @param node the starting node for the list
     * @param recursive whether to expand directories
     * @return a file list for the given node
     */
    FileList makeList(final String root, final RsyncFile node, boolean recursive) {
        ByteArrayOutputStream listData = new ByteArrayOutputStream();
        List<RsyncFile> files = new ArrayList<>();

        makeList(root, node, null, recursive, listData, files);

        return new ImmutableFileList(files.size(), 0, listData.toByteArray(), files, files.get(0));
    }

    // Implementation details follow.
    private RsyncFile makeList(final String root, final RsyncFile node, RsyncFile lastNode, boolean recursive,
                                 ByteArrayOutputStream listData, List<RsyncFile> files) {
        lastNode = writeListEntry(listData, node, lastNode, root, recursive);
        files.add(node);

        if (node.isDirectory() && (recursive || node.getName().equals(root))) {
            List<RsyncFile> children = node.getChildren();
            Collections.sort(children, rsyncComparator);

            for (RsyncFile child : children) {
                lastNode = makeList(root, child, lastNode, recursive, listData, files);
            }
        }

        return lastNode;
    }

    private class ImmutableFileList implements FileList {
        private final int size;
        private final int firstIndex;
        private final byte[] fileListData;
        private final List<RsyncFile> files;
        private final RsyncFile root;

        private ImmutableFileList(int size, int firstIndex, byte[] fileListData, List<RsyncFile> files, RsyncFile root) {
            this.size = size;
            this.firstIndex = firstIndex;
            this.fileListData = fileListData;
            this.files = files;
            this.root = root;
        }

        @Override public int getSize() { return size; }
        @Override public int getFirstIndex() { return firstIndex; }
        @Override public byte[] getFileListData() { return fileListData; }
        @Override public RsyncFile getFile(int index) { return files.get(index); }
        @Override public RsyncFile getRoot() { return root; }
    }

    private class Flags {
        static final int TOP_DIR =          0b00000001;
        static final int SAME_MODE =        0b00000010;
        static final int XFLAGS =           0b00000100;
        static final int SAME_UID =         0b00001000;
        static final int SAME_GID =         0b00010000;
        static final int SAME_NAME =        0b00100000;
        static final int LONG_NAME =        0b01000000;
        static final int SAME_TIME =        0b10000000;

        // extended flags
        static final int NO_CONTENT_DIR =   0b00000001 << 8;
    }

    // Sort such that:
    //  - Directories appear immediately before files within those directories
    //  - Directories appear after all files at the same level
    //  - A directory with the name '.' always compares before anything else
    private final Comparator<RsyncFile> rsyncComparator = new Comparator<RsyncFile>() {
        @Override
        public int compare(RsyncFile o1, RsyncFile o2) {
            if (o1.isDirectory()) {
                if (o2.isDirectory()) {
                    // both are directories, lexical order applies
                    return o1.getName().compareTo(o2.getName());
                }

                // o1 is greater than o2 because directories sort after files
                return 1;
            } else {
                if (o2.isDirectory()) {
                    // o2 is greater than o1 because directories sort after files
                    return -1;
                }

                // both are files, lexical order applies
                return o1.getName().compareTo(o2.getName());
            }
        }
    };

    private static final byte[] directoryMode = new byte[] { (byte)0xfd, 0x41, 0x00, 0x00 };    // 0040775
    private static final byte[] fileMode = new byte[] { (byte)0xb4, (byte)0x81, 0x00, 0x00 };   // 0100664
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private RsyncFile writeListEntry(ByteArrayOutputStream data, RsyncFile node, RsyncFile lastNode, String root, boolean recursive) {
        int flags = Flags.SAME_GID | Flags.SAME_UID;

        String name = node.getName();
        if (name.equals(root)) {
            name = ".";
            if (node.isDirectory()) flags |= Flags.TOP_DIR;
        } else {
            name = name.substring(root.length() + 1);
            if (node.isDirectory() && !recursive)
                flags |= Flags.NO_CONTENT_DIR | Flags.XFLAGS;
        }

        byte[] nameBytes = name.getBytes(UTF8);

        int lastCount = 0;
        if (lastNode != null) {
            byte[] lastName = lastNode.getName().getBytes(UTF8);
            lastCount = Math.min(longestMatch(lastName, nameBytes), 255);
            if (lastCount > 1) flags |= Flags.SAME_NAME;

            if (lastNode.isDirectory() == node.isDirectory()) flags |= Flags.SAME_MODE;
            if (lastNode.getLastModifiedTime() == node.getLastModifiedTime()) flags |= Flags.SAME_TIME;
        }

        if (nameBytes.length - lastCount > 255) flags |= Flags.LONG_NAME;

        data.write(flags);
        if ((flags & Flags.XFLAGS) != 0) {
            data.write(flags >> 8);
        }

        if ((flags & Flags.SAME_NAME) != 0) {
            data.write(lastCount);
        }

        if ((flags & Flags.LONG_NAME) != 0) {
            writeVarint(data, nameBytes.length - lastCount);
        } else {
            data.write(nameBytes.length - lastCount);
        }

        data.write(nameBytes, lastCount, nameBytes.length - lastCount);

        writeVarlong(data, node.getSize(), 3);

        if ((flags & Flags.SAME_TIME) == 0) {
            writeVarlong(data, node.getLastModifiedTime(), 4);
        }

        if ((flags & Flags.SAME_MODE) == 0) {
            byte[] mode = node.isDirectory() ? directoryMode : fileMode;
            data.write(mode, 0, mode.length);
        }

        return node;
    }

    private int longestMatch(byte[] left, byte[] right) {
        int i;

        for (i = 0; i < left.length && i < right.length && i < 256; i++) {
            if (left[i] != right[i]) return i;
        }

        return i;
    }

}
