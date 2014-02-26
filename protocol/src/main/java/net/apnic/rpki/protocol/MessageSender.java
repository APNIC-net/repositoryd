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
     */
    public void sendBytes(byte[] bytes);

    /**
     * Send a single outbound byte
     *
     * @param datum the byte to send
     */
    public void sendByte(byte datum);

    /**
     * Send some outbound bytes.
     *
     * @param bytes  the bytes to send
     * @param from   the starting position in the array
     * @param length the number of bytes to send
     */
    public void sendBytes(byte[] bytes, int from, int length);
}
