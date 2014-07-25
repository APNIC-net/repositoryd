package net.apnic.rpki.protocol;

/**
 * A Module is a set of files to be served by a Protocol object. The Module interface
 * provides a layer of abstraction over the source of files.
 *
 * A Module will issue a notifyAll() on itself whenever its content has been updated, should other threads wish to
 * wait() on the Module.
 */
public interface Module {
    /**
     * Gets the name of this Module.
     *
     * @return the name of this Module
     * @since 0.9
     */
    public String getName();

    /**
     * Gets the description of this Module.
     *
     * @return the description of this Module.
     * @since 0.9
     */
    public String getDescription();

    /**
     * Gets the file list of the data rooted at the given path.
     *
     * @param rootPath the path from which to root the file list
     * @param recursive whether to recurse into subdirectories or not
     * @return the file list of the data rooted at the given path
     * @throws NoSuchPathException if the given path does not exist in this module
     * @since 0.9
     */
    public FileList getFileList(String rootPath, boolean recursive) throws NoSuchPathException;

}
