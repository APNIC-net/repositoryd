package net.apnic.rpki.protocol;

/**
 * Provide message delivery capabilities for an RSYNC protocol instance.
 *
 * @author Byron Ellacott
 * @since 1.0
 */
public interface MessageSender {
    /**
     * Send some outbound bytes.
     *
     * @param bytes the bytes to send
     * @since 1.0
     */
    public void sendBytes(byte[] bytes);

    /**
     * Send a single outbound byte
     *
     * @param datum the byte to send
     * @since 1.0
     */
    public void sendByte(int datum);

    /**
     * Send some outbound bytes.
     *
     * @param bytes  the bytes to send
     * @param from   the starting position in the array
     * @param length the number of bytes to send
     * @since 1.0
     */
    public void sendBytes(byte[] bytes, int from, int length);

    /**
     * Send an informational message.  This will cause any buffered data to be flushed.
     *
     * @param message the message to send
     * @since 1.0
     */
    public void sendInformation(String message);
}
