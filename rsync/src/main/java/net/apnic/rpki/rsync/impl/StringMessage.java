package net.apnic.rpki.rsync.impl;

/**
 * Send an error to the remote end.
 *
 * @author Byron Ellacott
 * @since 2.0
 */
public class StringMessage extends AbstractBaseMessage {
    StringMessage(String content) {
        setData(content.getBytes(UTF8));
    }
}
