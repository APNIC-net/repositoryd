package net.apnic.rpki.rsync.impl;

/**
 * An RSYNCD version message.
 *
 * This message is the first one sent by both servers and clients to negotiate the protocol version.
 *
 * @author Byron Ellacott
 * @since 2.0
 */
class VersionWriter extends AbstractByteWriter {
    VersionWriter(int major, int minor) {
        setData(String.format("@RSYNCD: %d.%d\n", major, minor).getBytes(UTF8));
    }
}
