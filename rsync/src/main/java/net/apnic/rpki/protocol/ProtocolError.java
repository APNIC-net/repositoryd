package net.apnic.rpki.protocol;

/**
 * Thrown when there is an error in the use of the Protocol.
 *
 * @author bje
 * @since 0.9
 */
public class ProtocolError extends Exception {
    /**
     * The type of the error.
     *
     * @author bje
     * @since 0.9
     */
    public enum ErrorType {
        /**
         * Informational "error".
         */
        FINFO (2),

        /**
         * There was a general error.
         */
        FERROR (3);

        private final int code;
        private ErrorType(int code) {
            this.code = code;
        }

        /**
         * Returns the numeric code for this error type.
         *
         * @return the numeric code for this error type
         * @since ince 0.9
         */
        public int getCode() { return code; }
    }

    private final ErrorType type;

    /**
     * Constructs a new ProtocolError with the given error type and detail message.
     *
     * @param type the error type
     * @param msg the detail message
     * @since 0.9
     */
    public ProtocolError(ErrorType type, String msg) {
        super(msg);
        this.type = type;
    }

    /**
     * Returns the type of error that occurred.
     *
     * @return the type of error that occurred.
     * @since 0.0
     */
    public ErrorType getType() {
        return type;
    }
}
