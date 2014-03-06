package net.apnic.rpki.protocol;

import com.jcraft.jzlib.GZIPException;
import com.jcraft.jzlib.JZlib;
import net.apnic.rpki.data.Repository;

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
 * bytes each, depending on the number of entries in each directory.  Each RsyncFile carries its raw and compressed bytes.
 *
 * The state of a FileList returned from @{getFileList} is immutable.  A Repository rebuild will cause future calls
 * to return a new FileList.
 *
 * @author bje
 * @since 0.9
 */
public class MemoryCachedModule implements Module, Repository.Watcher {
    private final String name;
    private final String description;
    private final MessageDigest messageDigest;
    private final FileListBuilder fileListBuilder = new FileListBuilder();

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

    private class CachedFile implements RsyncFile {
        private final byte[] contents;
        private final byte[] zipped;
        private final byte[] checksum;
        private final int size;
        private final String name;
        private final long lastModifiedTime;
        private final boolean isDirectory;
        private final List<RsyncFile> children;

        public CachedFile(Repository.Node node) {
            name = node.getName();
            size = (int)node.getSize();
            contents = node.getContent();
            lastModifiedTime = node.getLastModifiedTime();
            isDirectory = node.isDirectory();
            zipped = zipper.zip(contents);
            messageDigest.reset();
            checksum = contents == null ? null : messageDigest.digest(contents);
            if (isDirectory) {
                children = new ArrayList<>();
                for (Repository.Node child : node.getChildren()) {
                    children.add(new CachedFile(child));
                }
            } else {
                children = null;
            }
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
        @Override public long getLastModifiedTime() { return lastModifiedTime; }
        @Override public boolean isDirectory() { return isDirectory; }
        @Override public List<RsyncFile> getChildren() { return children; }

        @Override
        public String toString() {
            return String.format(
                    "RsyncFile(%s, %d, %d zipped)",
                    name, size, zipped == null ? 0 : zipped.length
            );
        }
    }

    @Override
    public FileList getFileList(String rootPath, boolean recursive) throws NoSuchPathException {
        // 0. The module name becomes the module name plus '/'
        if (rootPath.equals(name)) {
            rootPath = name + "/";
        }

        // 1. All paths requested should start with the module name followed by '/', or be the module name
        if (!rootPath.startsWith(name + "/")) {
            throw new NoSuchPathException();
        }

        // 2. A path consists of a root, up to the last /, and a name, after the last /
        String root = rootPath.substring(0, rootPath.lastIndexOf("/"));

        // 4. Find the RsyncFile associated with this
        RsyncFile file = paths.get(rootPath);
        if (file == null) {
            throw new NoSuchPathException();
        }

        return fileListBuilder.makeList(root, file, recursive);
    }

    private Map<String, RsyncFile> paths = new HashMap<>();

    @Override
    public void repositoryUpdated(Repository repository) {
        // Convert repository nodes into RsyncFiles
        Map<String, RsyncFile> newPaths = new HashMap<>();
        updatePaths(newPaths, new CachedFile(repository.getRepositoryRoot()));
        paths = newPaths;
        synchronized (this) {
            this.notifyAll();
        }
    }

    private void updatePaths(Map<String, RsyncFile> paths, RsyncFile path) {
        paths.put(path.getName(), path);
        if (path.isDirectory()) {
            paths.put(path.getName() + "/", path);
            for (RsyncFile child : path.getChildren()) {
                updatePaths(paths, child);
            }
        }
    }

 }
