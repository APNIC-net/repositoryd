package net.apnic.rpki.protocol;

import java.util.List;

/**
 * A Module is a set of files to be served by a Protocol object. The Module interface
 * provides a layer of abstraction over the source of files.
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
     * Gets the file lists of the incrementally recursive data rooted at the given path.
     *
     * @param rootPath the path from which to root the file list
     * @return the file lists of the incrementally recursive data rooted at the given path
     * @throws NoSuchPathException if the given path does not exist in this module
     * @since 0.9
     */
    public List<FileList> getFileList(String rootPath) throws NoSuchPathException;

}
