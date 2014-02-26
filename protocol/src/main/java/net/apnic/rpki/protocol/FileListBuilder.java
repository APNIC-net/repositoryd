package net.apnic.rpki.protocol;

import net.apnic.rpki.data.Repository;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.*;

import static net.apnic.rpki.protocol.RsyncUtils.write_varint;
import static net.apnic.rpki.protocol.RsyncUtils.write_varlong;

class FileListBuilder {
//    private static final Logger LOGGER = LoggerFactory.getLogger(FileListBuilder.class);

    public interface FileListFactory  {
        public FileList makeFileList(byte[] fileListData, List<Repository.Node> contents, int firstIndex, Repository.Node root);
    }

    private static class Flags {
        static final int TOP_DIR =      0b00000001;
        static final int SAME_MODE =    0b00000010;
        // static final int XFLAGS =    0b00000100;
        static final int SAME_UID =     0b00001000;
        static final int SAME_GID =     0b00010000;
        static final int SAME_NAME =    0b00100000;
        static final int LONG_NAME =    0b01000000;
        static final int SAME_TIME =    0b10000000;
    }

    // Sort such that:
    //  - Directories appear immediately before files within those directories
    //  - Directories appear after all files at the same level
    //  - A directory with the name '.' always compares before anything else
    private static final Comparator<Repository.Node> rsyncComparator = new Comparator<Repository.Node>() {
        @Override
        public int compare(Repository.Node o1, Repository.Node o2) {
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

    public static List<FileList> build(Repository.Node repoNode, String prefix, FileListFactory fileListFactory) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        // Use a queue of nodes to push directories to recurse through
        Queue<Repository.Node> nodes = new LinkedList<>();

        if (!repoNode.isDirectory()) {
            // Special case for a single file
            encodeNodeForList(data, prefix, repoNode, null);
            data.write(0);

            return Collections.singletonList(fileListFactory.makeFileList(data.toByteArray(),
                    Collections.singletonList(repoNode), 1, repoNode));
        }

        List<FileList> fileLists = new ArrayList<>();
        List<Repository.Node> fileContents = new ArrayList<>();

        Repository.Node lastNode = encodeNodeForList(data, prefix, repoNode, null);
        fileContents.add(repoNode);

        nodes.add(repoNode);

        int firstIndex = 1;

        while (!nodes.isEmpty()) {
            Repository.Node node = nodes.remove();

            List<Repository.Node> sorted = new ArrayList<>(node.getChildren());
            Collections.sort(sorted, rsyncComparator);

            // Only directories should be in the nodes queue
            for (Repository.Node sub : sorted) {
                lastNode = encodeNodeForList(data, prefix, sub, lastNode);
                fileContents.add(sub);

                // Add each directory to the queue of directories to encode
                if (sub.isDirectory()) {
                    nodes.add(sub);
                }
            }
            data.write(0);
            fileLists.add(fileListFactory.makeFileList(data.toByteArray(), fileContents, firstIndex, node));
            firstIndex += fileContents.size() + 1;
            data.reset();
            fileContents = new ArrayList<>();
        }

        return fileLists;
    }

    private static final byte[] directoryMode = new byte[] { (byte)0xfd, 0x41, 0x00, 0x00 };    // 0040775
    private static final byte[] fileMode = new byte[] { (byte)0xb4, (byte)0x81, 0x00, 0x00 };   // 0100664
    private static Repository.Node encodeNodeForList(ByteArrayOutputStream data, String prefix, Repository.Node node, Repository.Node lastNode) {
        // preserve-gid and preserve-uid are unsupported (and unwise in this context!), so these flags ensure
        // the flags byte is never zero, while also allowing unwise clients to just think everything is uid/gid 0.
        int flags = Flags.SAME_GID | Flags.SAME_UID;

        int sameLength = 0;
        String name = node.getName().substring(prefix.length());
        byte[] nodeNameBytes = name.getBytes(Charset.forName("UTF-8"));

        if (lastNode != null) {
            // If the last node is set, this one will be affected by it
            byte[] lastNameBytes = lastNode.getName().getBytes(Charset.forName("UTF-8"));
            sameLength = longestMatch(nodeNameBytes, lastNameBytes);

            if (lastNode.getLastModifiedTime() == node.getLastModifiedTime())
                flags |= Flags.SAME_TIME;

            if (lastNode.isDirectory() == node.isDirectory())
                flags |= Flags.SAME_MODE;

        } else {
            // if there was no last node, this is the TOP_DIR.  This is safe to set on a file, too.
            flags |= Flags.TOP_DIR;
        }

        int nameLength = nodeNameBytes.length - sameLength;

        if (sameLength > 0) flags |= Flags.SAME_NAME;
        if (nameLength > 255) flags |= Flags.LONG_NAME;

        data.write(flags);

        if ((flags & Flags.SAME_NAME) != 0) {
            data.write(sameLength);
        }

        if ((flags & Flags.LONG_NAME) != 0) {
            write_varint(data, nameLength);
        } else {
            data.write(nameLength);
        }
        data.write(nodeNameBytes, sameLength, nameLength);

        write_varlong(data, node.getSize(), 3);

        if ((flags & Flags.SAME_TIME) == 0) {
            write_varlong(data, node.getLastModifiedTime(), 4);
        }

        if ((flags & Flags.SAME_MODE) == 0) {
            byte[] mode = node.isDirectory() ? directoryMode : fileMode;
            data.write(mode, 0, mode.length);
        }

        return node;
    }

    private static int longestMatch(byte[] left, byte[] right) {
        int i;

        for (i = 0; i < left.length && i < right.length && i < 256; i++) {
            if (left[i] != right[i]) return i;
        }

        return i;
    }

}
