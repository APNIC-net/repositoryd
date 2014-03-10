package net.apnic.rpki.server.messages;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapsulates a version number handshake message.
 *
 * @author bje
 * @since 0.9
 */
public class HandshakeMessage extends WireMessage {
    private static final Pattern versionPattern = Pattern.compile("@RSYNCD: (\\d+)(?:\\.(\\d+))?");

    private final int major;
    private final int minor;

    /**
     * Constructs a HandshakeMessage with the given version specifications.
     *
     * @param major the major version number
     * @param minor the minor (beta) version number
     * @since 0.9
     */
    public HandshakeMessage(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * Returns the major version number.
     *
     * @return the major version number
     * @since 0.9
     */
    public int getMajor() { return major; }

    /**
     * Returns the minor (beta) version number.
     * @return the minor (beta) version number
     * @since 0.9
     */
    public int getMinor() { return minor; }

    /**
     * Creates a new HandshakeMessage from the given handshake string.
     *
     * TODO: create a specific exception
     *
     * @param handshake the handshake string to parse
     * @return a new HandshakeMessage
     * @throws Exception if the handshake string is invalid
     */
    public static HandshakeMessage parseHandshake(String handshake) throws Exception {
        Matcher m = versionPattern.matcher(handshake);
        if (!m.matches())
            throw new Exception("protocol startup error");

        return new HandshakeMessage(Integer.parseInt(m.group(1), 10),
                m.groupCount() > 2 ? Integer.parseInt(m.group(3), 10) : 0);
    }
}
