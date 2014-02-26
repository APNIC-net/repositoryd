package net.apnic.rpki.server.messages;

/**
 * Encapsulates a protocol setup message to send to the client.
 *
 * @author bje
 * @since 0.9
 */
public class SetupMessage extends WireMessage {
    private final byte flags;
    private final int seed;

    /**
     * Constructs a new SetupMessage with the given protocol parameters.
     *
     * @param flags the compatibility flags
     * @param seed the checksum seed
     * @since 0.9
     */
    public SetupMessage(byte flags, int seed) {
        this.flags = flags;
        this.seed = seed;
    }

    /**
     * Returns the compatibility flags.
     *
     * @return the compatibility flags
     * @since 0.9
     */
    public byte getFlags() { return flags; }

    /**
     * Returns the checksum seed.
     *
     * @return the checksum seed
     * @since 0.9
     */
    public int getSeed() { return seed; }
}
