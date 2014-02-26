package net.apnic.rpki.server.messages;

import io.netty.buffer.ByteBuf;

/**
 * Encapsulates a raw protocol message to send to the client.
 *
 * @author bje
 * @since 0.9
 */
public class ProtocolMessage extends WireMessage {
    private final ByteBuf bytes;

    /**
     * Creates a new ProtocolMessage wrapping the given bytes.
     *
     * This does not claim ownership of the ByteBuf.
     *
     * @param bytes the bytes to wrap
     * @since 0.9
     */
    public ProtocolMessage(ByteBuf bytes) {
        this.bytes = bytes;
    }

    /**
     * Gets the bytes of the message.
     *
     * @return the bytes of the message
     * @since 0.9
     */
    public ByteBuf getBytes() { return bytes; }
}
