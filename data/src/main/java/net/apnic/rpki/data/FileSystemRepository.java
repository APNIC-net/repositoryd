package net.apnic.rpki.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * A concrete implementation of Repository that scans a filesystem for changes, retaining them in an in-memory
 * tree.  Symbolic links are NOT followed.
 *
 * @author bje
 * @since 0.9
 */
public class FileSystemRepository implements Repository, Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemRepository.class);

    private final Path rootPath;
    private final WatchService watchService = FileSystems.getDefault().newWatchService();
    private final Map<WatchKey, Path> keys = new HashMap<>();
    private Watcher watcher;

    // This Node is immutable, allowing this value to be safely shared across threads without synchronisation.
    private Node rootNode;

    /**
     * Constructs a FileSystemRepository based at the specified root path.
     *
     * @param root a Path specifying the root of this repository
     * @throws IOException if a watch service cannot be created
     */
    public FileSystemRepository(Path root) throws IOException {
        this.rootPath = root;
        new Thread(this).start();
    }

    @Override
    public void setWatcher(Watcher watcher) {
        this.watcher = watcher;
    }

    @Override
    public Node getRepositoryRoot() {
        return rootNode;
    }

    private void registerPath(final Path top) throws IOException {
        Files.walkFileTree(top, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                keys.put(key, dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean registerRoot() {
        /* Reset all existing watches */
        for (WatchKey key : keys.keySet()) {
            key.cancel();
        }
        keys.clear();

        try {
            registerPath(rootPath);
        } catch (IOException ex) {
            LOGGER.error("Repository cannot (re-)register root path: {}", ex.getMessage());
            try {
                for (WatchKey key : keys.keySet()) {
                    key.cancel();
                    watchService.close();
                }
            } catch (Exception innerEx) {
                LOGGER.warn("And then, an error when closing the watchService: {}", innerEx.getMessage());
            }
            return false;
        }

        return true;
    }

    private final class FinalNode implements Node {
        private final String name;
        private final byte[] content;
        private final List<Node> children;
        private final BasicFileAttributes attrs;

        protected FinalNode(String name, byte[] content, List<Node> children, BasicFileAttributes attrs) {
            this.name = name;
            this.content = content;
            this.children = children;
            this.attrs = attrs;
        }

        @Override public String getName() { return name; }
        @Override public byte[] getContent() { return content; }
        @Override public long getSize() { return attrs.size(); }
        @Override public List<Node> getChildren() { return children; }
        @Override public long getLastModifiedTime() { return attrs.lastModifiedTime().to(TimeUnit.SECONDS); }
        @Override public boolean isDirectory() { return attrs.isDirectory(); }

        @Override public String toString() { return name; }
    }

    private String trimName(Path path) {
        return path.subpath(rootPath.getNameCount() - 1, path.getNameCount()).toString();
    }

    private void rebuildNodes() {
        class VisitorState {
            final List<Node> children = new ArrayList<>();
            BasicFileAttributes attrs;
            Path dir;

            VisitorState(Path dir, BasicFileAttributes attrs) {
                this.dir = dir;
                this.attrs = attrs;
            }

            @Override
            public String toString() {
                return String.format("{%s dir=%s mode=%s}", super.toString(),
                        dir != null ? dir.toString() : "<none>",
                        attrs != null ? attrs.toString() : "<none>");
            }
        }

        try {
            final Deque<VisitorState> state = new ArrayDeque<>();
            state.add(new VisitorState(rootPath, null));

            long startTime = System.currentTimeMillis();
            final long totalBytes[] = {0};

            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    state.add(new VisitorState(dir, attrs));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    final FinalNode node = new FinalNode(trimName(file), Files.readAllBytes(file), null, attrs);
                    state.peekLast().children.add(node);
                    totalBytes[0] += node.getContent().length;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    VisitorState currentState = state.removeLast();
                    Node node = new FinalNode(trimName(dir), null, currentState.children, currentState.attrs);
                    state.peekLast().children.add(node);

                    if (exc != null) throw exc;
                    return FileVisitResult.CONTINUE;
                }
            });

            long tookTime = System.currentTimeMillis() - startTime;
            LOGGER.debug("Filesystem tree rebuilt in {} seconds, approx. {} bytes of file data", tookTime / 1000, totalBytes[0]);

            rootNode = state.peek().children.get(0);
            if (watcher != null)
                watcher.repositoryUpdated(this);

        } catch (IOException ex) {
            LOGGER.warn("Failed to rebuild node tree, data will become stale: {}", ex.getMessage());
        }
    }

    @Override
    public void run() {
        String name = "repository-" + rootPath.getFileName().toString();
        Thread.currentThread().setName(name);
        if (!registerRoot()) return;

        rebuildNodes();

        for (;;) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException ex) {
                continue;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                LOGGER.warn("WatchService returned unregistered key {}", key);
                continue;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // we missed events, reset everything
                if (kind == OVERFLOW) {
                    if (!registerRoot()) return;
                    break;
                }

                LOGGER.info("Update to {} triggering FileSystemRepository({}) rebuild", event.context(), rootPath);

                if (kind == ENTRY_CREATE) {
                    Path child = dir.resolve(FileSystemRepository.<Path>cast(event).context());

                    if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                        try {
                            registerPath(child);
                        } catch (IOException ex) {
                            LOGGER.warn("IOException when registering new child path; ignoring this addition: {}", ex.getMessage());
                        }
                    }

                }
            }

            if (!key.reset()) {
                keys.remove(key);
            }

            // Something has changed, so rebuild the tree
            rebuildNodes();
        }
    }

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }
}
