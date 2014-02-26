package net.apnic.rpki.protocol;

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
    public byte[] getFileListData();

    /**
     * Represents a file within a list.
     *
     * @author bje
     * @since 0.9
     */
    public interface File {
        /**
         * Returns the name of the file, relative to the repository root.
         *
         * Note that this name is not relative to this file list, but to the overall repository root.
         *
         * @return the name of the file
         * @since 0.9
         */
        public String getName();

        /**
         * Returns the size of the file.
         *
         * Note that an RPKI repository limits file sizes to a 32-bit signed integer.
         *
         * @return the size of the file
         * @since 0.9
         */
        public int getSize();   // beware the integer overflow; this system expects RPKI-sized files, tens of kb max.

        /**
         * Returns the contents of the file, as a byte array.
         *
         * @return the contents of the file as a byte array
         * @since 0.9
         */
        public byte[] getContents();

        /**
         * Returns the contents of the file, compressed with the rsync-specific zlib settings.
         *
         * @return the compressed contents of the file as a byte array
         * @since 0.9
         */
        public byte[] getCompressedContents();

        /**
         * Returns the MD5 checksum of the file.
         *
         * @return the MD5 checksum of the file
         * @since 0.9
         */
        public byte[] getChecksum();
    }

    /**
     * Returns the File at the specified index.
     *
     * @param index the index of the File to return
     * @return the File at the specified index
     * @throws IndexOutOfBoundsException if the index is out of the range (0, getSize() - 1)
     * @since 0.9
     */
    public File getFile(int index);

    /**
     * Returns a File representing the parent directory of this fileList.
     *
     * @return a File representing the parent directory of this fileList
     * @since 0.9
     */
    public File getRoot();
}
