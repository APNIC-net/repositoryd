package net.apnic.rpki.protocol;

/**
 * A container for rsync file checksums, to use in the delta algorithm
 *
 * @author bje
 * @since 0.9
 */
public class Checksums {
    private final int blockCount;
    private final int blockSize;
    private final int secondaryLength;
    private final int remainder;

    /**
     * Constructs a new Checksums instane with the specified values.
     *
     * @param blockCount the number of blocks in the sum
     * @param blockSize the size of each block summed
     * @param secondaryLength the number of bytes of MD5 sum used for secondary checksums
     * @param remainder the number of bytes in the final block
     * @since 0.9
     */
    public Checksums(int blockCount, int blockSize, int secondaryLength, int remainder) {
        this.blockCount = blockCount;
        this.blockSize = blockSize;
        this.secondaryLength = secondaryLength;
        this.remainder = remainder;
    }

    /**
     * Returns the number of blocks summed.
     *
     * @return the number of blocks summed
     * @since 0.9
     */
    public int getBlockCount() {
        return blockCount;
    }

    /**
     * Returns the size of each block summed.
     *
     * @return the size of each block summed
     * @since 0.9
     */
    public int getBlockSize() {
        return blockSize;
    }

    /**
     * Returns the number of bytes of MD5 sum used in secondary sums.
     *
     * @return the number of bytes of MD5 sum used in secondary sums
     * @since 0.9
     */
    public int getSecondaryLength() {
        return secondaryLength;
    }

    /**
     * Returns the number of bytes contained in the final block.
     *
     * @return the number of bytes contained in the final block
     * @since 0.9
     */
    public int getRemainder() {
        return remainder;
    }
}
