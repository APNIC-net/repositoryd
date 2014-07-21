package net.apnic.rpki.protocol;

import java.util.List;

/**
 * Represents a file within a list.
 *
 * @author bje
 * @since 0.9
 */
public interface RsyncFile {
    /**
     * Returns the name of the file, relative to the repository root.
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
     * Returns the last modified time of the file.
     *
     * @return the last modified time of the file
     * @since 1.0
     */
    public long getLastModifiedTime();

    /**
     * Returns true if the file is a directory
     *
     * @return true if the file is a directory
     * @since 1.0
     */
    public boolean isDirectory();

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

    /**
     * Returns the File children of a directory.
     *
     * @return the File children of a directory, or null if not a directory
     * @since 1.0
     */
    public List<RsyncFile> getChildren();
}