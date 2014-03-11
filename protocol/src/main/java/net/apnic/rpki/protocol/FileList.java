package net.apnic.rpki.protocol;

import java.nio.ByteBuffer;

/**
 * A FileList represents a set of files being offered to a client for transfer.
 *
 * @author bje
 * @since 0.9
 */
public interface FileList {
    /**
     * Get the size of this file list.  The size of a file list is the number of file entries
     * it contains.
     *
     * @return the size of this file list
     * @since 0.9
     */
    public int getSize();

    /**
     * Get the first file index in this file list.
     *
     * @return the first file index in this file list
     * @since 0.9
     */
    public int getFirstIndex();

    /**
     * Get the file list data for exchange with an rsync receiver instance.  The data describes the
     * entire contents of the requested root path, including all directories and subdirectories.
     *
     * @return the file list data for exchange with an rsync receiver instance
     * @since 0.9
     */
    public ByteBuffer getFileListData();

    /**
     * Returns the RsyncFile at the specified index.
     *
     * @param index the index of the RsyncFile to return
     * @return the RsyncFile at the specified index
     * @throws IndexOutOfBoundsException if the index is out of the range (0, getSize() - 1)
     * @since 0.9
     */
    public RsyncFile getFile(int index);

    /**
     * Returns a RsyncFile representing the parent directory of this fileList.
     *
     * @return a RsyncFile representing the parent directory of this fileList
     * @since 0.9
     */
    public RsyncFile getRoot();
}
