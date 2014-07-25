package net.apnic.rpki.protocol;

import java.util.EnumSet;

/**
 * The attributes of a transfer request from a generator.
 *
 * @author bje
 * @since 0.9
 */
public class TransferAttributes {
    private final int fileIndex;
    private final EnumSet<ItemFlag> iflags;
    private final byte basisType;
    private final String xname;

    /**
     * Constructs a new TransferAttributes with the specified attributes.
     *
     * @param fileIndex the file index being requested
     * @param iflags the flags of the transfer request
     * @param basisType the basis type used for the name
     * @param xname the fuzzy match name, which may be null
     * @since 0.9
     */
    public TransferAttributes(int fileIndex, EnumSet<ItemFlag> iflags, byte basisType, String xname) {
        this.fileIndex = fileIndex;
        this.iflags = iflags;
        this.basisType = basisType;
        this.xname = xname;
    }

    /**
     * Returns the file index of the request.
     *
     * @return the file index of the request
     * @since 0.9
     */
    public int getFileIndex() {
        return fileIndex;
    }

    /**
     * Returns the item flags of the request.
     *
     * @return the item flags of the request
     * @since 0.9
     */
    public EnumSet<ItemFlag> getItemFlags() {
        return iflags;
    }

    /**
     * Returns the basis type of the request.
     *
     * @return the basis type of the request
     * @since 0.9
     */
    public byte getBasisType() {
        return basisType;
    }

    /**
     * Returns the fuzzy match name of the request, or null if none was set.
     *
     * @return the fuzzy match name of the request
     * @since 0.9
     */
    public String getFuzzyName() {
        return xname;
    }
}
