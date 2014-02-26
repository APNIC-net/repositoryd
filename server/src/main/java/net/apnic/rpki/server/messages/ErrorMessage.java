package net.apnic.rpki.server.messages;

import net.apnic.rpki.protocol.ProtocolError;

/**
 * Encapsulates an error message to be sent to the client.
 *
 * @author bje
 * @since 0.9
 */
public class ErrorMessage extends WireMessage {
    private final String error;
    private final int code;

    /**
     * Constructs a new ErrorMessage from a given ProtocolError.
     *
     * @param error the triggering ProtocolError
     * @since 0.9
     */
    public ErrorMessage(ProtocolError error) {
        this(error.getMessage(), error.getType().getCode());
    }

    /**
     * Constructs a new ErrorMessage with the given message and code.
     *
     * @param error the error message
     * @param code the error code
     * @since 0.9
     */
    public ErrorMessage(String error, int code) {
        this.error = error;
        this.code = code;
    }

    /**
     * Returns the error code.
     *
     * @return the error code
     * @since 0.9
     */
    public int getCode() { return code; }

    /**
     * Returns the error message.
     *
     * @return the error message
     * @since 0.9
     */
    public String getError() { return error; }
}
