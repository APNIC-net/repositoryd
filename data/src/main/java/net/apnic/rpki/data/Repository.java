package net.apnic.rpki.data;

import java.util.List;

/**
 * An independently updating data source for an RPKI repository.  A repository is
 * a tree of files and directories, containing certificates, manifests, and RPKI
 * products.
 *
 * The Repository interface specifies a mechanism that informs a Repository owner
 * of changes to the Repository.
 *
 * @since 0.9
 * @author bje
 */
public interface Repository {
    /**
     * A repository's owner, to be notified of changes.
     *
     * @since 0.9
     */
    public interface Watcher {
        /**
         * Receives a notification of an updated repository.
         *
         * @param repository the repository which was updated
         */
        public void repositoryUpdated(Repository repository);
    }

    /**
     * A Node in the repository.
     *
     * @since 0.9
     */
    public interface Node {
        /**
         * Gets the name of this Node.  This name is relative to the repository root.
         *
         * @return the name of this Node
         * @since 0.9
         */
        public String getName();

        /**
         * Gets the last-modified-time of this Node.
         *
         * @return the last-modified-time of this Node
         * @since 0.9
         */
        public long getLastModifiedTime();

        /**
         * Returns true if this Node is a directory, else false.
         *
         * @return true if this Node is a directory, else false
         * @since 0.9
         */
        public boolean isDirectory();

        /**
         * Gets the content of this Node.  Always null for directories.
         *
         * @return the content of this Node
         * @since 0.9
         */
        public byte[] getContent();

        /**
         * Gets the size of this Node.  This will not equal getContent().size() for a directory.
         *
         * @return the size of this Node
         * @since 0.9
         */
        public long getSize();

        /**
         * Gets the children of this Node.
         *
         * @return the children of this Node
         * @since 0.9
         */
        public List<Node> getChildren();
    }

    /**
     * Sets the Watcher to be informed of changes to this Repository
     * @param watcher the Watcher to be informed of changes to this Repository
     * @since 0.9
     */
    public void setWatcher(Watcher watcher);

    /**
     * Gets the top level Repository.Node entry.
     *
     * @return the top level Repository.Node entry
     * @since 0.9
     */
    public Node getRepositoryRoot();
}
