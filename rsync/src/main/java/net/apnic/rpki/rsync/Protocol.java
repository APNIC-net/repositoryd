package net.apnic.rpki.rsync;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * RSYNC Protocol interface.  Defines the core behaviour of an rsync (sender) system, separate to communications.
 *
 * @author Byron Ellacott
 * @since 2.0
 */
public interface Protocol {
    /**
     * Read data from another rsync instance.
     *
     * The Protocol will process the data and queue any output needed.  If the data
     * received is invalid, read() will throw an RsyncException.
     *
     * @param input the remote data to process
     * @throws RsyncException if the remote data cannot be processed
     * @since 2.0
     */
    void read(ByteBuf input) throws RsyncException;

    /**
     * Write output data into the given buffer.
     *
     * Write available output bytes into the output buffer, up to its maximum
     * capacity, and return whether any bytes were written.  This method allows
     * users of a Protocol to only fill output buffers when the remote end of a
     * connection can consume them.
     *
     * @param buffer the output buffer to write into
     * @return true if some data was written into the buffer
     * @since 2.0
     */
    boolean write(ByteBuf buffer);

    /**
     * Get the list of modules being served by this Protocol instance.
     *
     * @return the list of modules being served by this Protocol instance
     * @since 2.0
     */
    public List<Module> getModules();
}
