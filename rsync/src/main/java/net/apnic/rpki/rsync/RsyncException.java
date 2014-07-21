package net.apnic.rpki.rsync;

/**
 * Thrown when communication with a remote end fails.
 *
 * @author bje
 * @since 2.0
 */
public class RsyncException extends Exception {
    public RsyncException(String message) {
        super(message);
    }
}
