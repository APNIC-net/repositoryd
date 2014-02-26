package net.apnic.rpki.protocol;

/**
 * Thrown to indicate that the version requested cannot be supported.
 *
 * @author bje
 * @since 0.9
 */
public class IncompatibleVersionException extends Exception {
    /**
     * Constructs an IncompatibleVersionException with the specified detail message.
     *
     * @param msg the detail message
     */
    public IncompatibleVersionException(String msg) { super(msg); }
}