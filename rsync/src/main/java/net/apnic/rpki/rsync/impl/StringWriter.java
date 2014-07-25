package net.apnic.rpki.rsync.impl;

/**
 * Send an error to the remote end.
 *
 * @author Byron Ellacott
 * @since 2.0
 */
public class StringWriter extends AbstractByteWriter {
    StringWriter(String content) {
        setData(content.getBytes(UTF8));
    }
}
