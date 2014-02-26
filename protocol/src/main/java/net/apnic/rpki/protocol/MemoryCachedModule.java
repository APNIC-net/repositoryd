package net.apnic.rpki.protocol;

import com.jcraft.jzlib.GZIPException;
import com.jcraft.jzlib.JZlib;
import net.apnic.rpki.data.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of a Module that caches all data in memory.
 *
 * A recursive set of FileLists for each possible requested path is maintained, taking a few hundred to a few thousand
 * bytes each, depending on the number of entries in each directory.  Each File carries its raw and compressed bytes.
 *
 * The state of a FileList returned from @{getFileList} is immutable.  A Repository rebuild will cause future calls
 * to return a new FileList.
 *
 * @author bje
 * @since 0.9
 */
public class MemoryCachedModule implements Module, Repository.Watcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryCachedModule.class);

    private final String name;
    private final String description;
    private final MessageDigest messageDigest;

    /**
     * Constructs a MemoryCachedModule with the specified name, description, and source repository.
     *
     * @param name the name of the module; the base of all apparent paths.
     * @param description the description of the module.
     * @param source the Repository providing data for this module
     * @since 0.9
     */
    public MemoryCachedModule(String name, String description, Repository source) {
        this.name = name;
        this.description = description;

        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            // MD5 is guaranteed available by spec, this only happens if your JRE is broken
            throw new RuntimeException(ex);
        }

        source.setWatcher(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    private class Zip {
        byte[] zip(byte[] input) {
            if (input == null) return null;

            try {
                com.jcraft.jzlib.Deflater deflater = new com.jcraft.jzlib.Deflater(6, -15, 8);
                deflater.params(6, JZlib.Z_DEFAULT_STRATEGY);

                final byte[] output = new byte[(int)(input.length * 1.1 + 16)];
                deflater.setOutput(output);
                deflater.setInput(input);
                if (deflater.deflate(JZlib.Z_SYNC_FLUSH) != JZlib.Z_OK)
                    throw new GZIPException(deflater.getMessage());
                if (deflater.total_out < 4)
                    throw new GZIPException("deflated output doesn't have sync marker bytes");

                final byte[] result = new byte[(int)deflater.total_out - 4];
                System.arraycopy(output, 0, result, 0, result.length);
                return result;
            } catch (GZIPException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private final Zip zipper = new Zip();

    private class CachedFile implements FileList.File {
        private final byte[] contents;
        private final byte[] zipped;
        private final byte[] checksum;
        private final int size;
        private final String name;

        public CachedFile(Repository.Node node) {
            name = node.getName();
            size = (int)node.getSize();
            contents = node.getContent();
            zipped = zipper.zip(contents);
            messageDigest.reset();
            checksum = contents == null ? null : messageDigest.digest(contents);
        }

        @Override public byte[] getContents() {
            return contents;
        }
        @Override public byte[] getCompressedContents() {
            return zipped;
        }
        @Override public byte[] getChecksum() { return checksum; }
        @Override public String getName() {
            return name;
        }
        @Override public int getSize() {
            return size;
        }

        @Override
        public String toString() {
            return String.format(
                    "File(%s, %d, %d zipped)",
                    name, size, zipped == null ? 0 : zipped.length
            );
        }
    }

    private class CachedFileList implements FileList {
        private final byte[] listData;
        private final List<CachedFile> contents = new ArrayList<>();
        private final int size;
        private final int base;
        private final File root;

        @Override public int getSize() { return size; }
        @Override public int getFirstIndex() { return base; }
        @Override public byte[] getFileListData() { return listData; }
        @Override public File getFile(int index) { return contents.get(index); }
        @Override public File getRoot() { return root; }

        CachedFileList(byte[] listData, List<Repository.Node> contents, int base, Repository.Node root) {
            this.listData = listData;
            this.size = contents.size();
            this.base = base;
            this.root = new CachedFile(root);

            for (Repository.Node node : contents) {
                this.contents.add(new CachedFile(node));
            }
        }
    }

    private Map<String, List<FileList>> contents = new HashMap<>();

    @Override
    public List<FileList> getFileList(String rootPath) throws NoSuchPathException {
        if (!rootPath.startsWith(name)) {
            throw new NoSuchPathException();
        }

        rootPath = rootPath.substring(Math.min(name.length() + 1, rootPath.length()));
        if (rootPath.isEmpty()) rootPath = ".";

        if (!contents.containsKey(rootPath)) {
            throw new NoSuchPathException();
        }

        return contents.get(rootPath);
    }

    private long bytes;

    private void updateCache(Map<String, List<FileList>> cache, Repository.Node node) {
        String prefix = node.getName();
        prefix = prefix.substring(0, prefix.lastIndexOf('/') + 1);
        cache.put(node.getName(), FileListBuilder.build(node, prefix, new FileListBuilder.FileListFactory() {
            @Override
            public FileList makeFileList(byte[] fileListData, List<Repository.Node> contents, int firstIndex, Repository.Node root) {
                bytes += fileListData.length;
                return new CachedFileList(fileListData, contents, firstIndex, root);
            }
        }));

        if (node.isDirectory()) {
            for (Repository.Node sub : node.getChildren()) {
                updateCache(cache, sub);
            }
        }
    }

    @Override
    public void repositoryUpdated(Repository repository) {
        Map<String, List<FileList>> cache = new HashMap<>();

        bytes = 0;
        updateCache(cache, repository.getRepositoryRoot());
        LOGGER.debug("Constructed module '{}' cache, approx. {} bytes used for indices", name, bytes);

        if (LOGGER.isDebugEnabled()) {
            long dataBytes = 0;
            for (FileList fileList : cache.get(".")) {
                for (int i = 0; i < fileList.getSize(); i++) {
                    FileList.File content = fileList.getFile(i);
                    dataBytes += content == null ? 0 : content.getSize();
                }
            }
            LOGGER.debug("Approx. {} bytes used for file content", dataBytes);
        }

        contents = cache;
    }

 }
