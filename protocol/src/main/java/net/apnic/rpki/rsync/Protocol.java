package net.apnic.rpki.rsync;

import java.nio.ByteBuffer;

/**
 * RSYNC Protocol interface.  Defines the core behaviour of an rsync (sender) system, separate to communications.
 *
 * @author Byron Ellacott
 * @since 2.0
 */
public interface Protocol {
    /**
     * Receive data from another rsync instance.
     *
     * The Protocol will process the received data and queue any output needed.  If communication
     * fails due to a protocol error, receive() will throw an RsyncException.
     *
     * @param input The remote data to process
     * @throws RsyncException if the remote data cannot be processed
     * @since 2.0
     */
    void receive(ByteBuffer input) throws RsyncException;

    /**
     * Provide data to transmit to another rsync instance.
     *
     * The Protocol will return chunks of data to be transmitted to the remote end, or NULL
     * if no data can be sent at this time.  Further input may cause more data to be available
     * to transmit.
     *
     * @return A block of data to transmit, or NULL if there is none pending.
     */
    ByteBuffer transmit();
}
