package net.apnic.rpki.protocol;

/**
 * Thrown when a Module is asked to serve a path which is not contained in its Repository.
 *
 * @author bje
 * @since 0.9
 */
public class NoSuchPathException extends Exception {
}
