package net.apnic.rpki.server.messages;

/**
 * Encapsulates a string response to send to the client.
 *
 * @author bje
 * @since 0.9
 */
public class ResponseMessage extends WireMessage {
    private final String response;

    /**
     * Constructs a new ResponseMessage with the given response.
     *
     * @param response the response.
     * @since 0.9
     */
    public ResponseMessage(String response) {
        this.response = response;
    }

    /**
     * Returns the response.
     *
     * @return the response
     * @since 0.9
     */
    public String getResponse() { return response; }
}
