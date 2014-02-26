package net.apnic.rpki.server.messages;

import java.util.List;

/**
 * A WireMessage containing an argument list sent from the client.
 *
 * @author bje
 * @since 0.9
 */
public class ArgumentsMessage extends WireMessage {
    private final List<String> arguments;

    /**
     * Constructs a new ArgumentsMessage.
     *
     * @param arguments the arguments
     * @since 0.9
     */
    public ArgumentsMessage(List<String>arguments) {
        this.arguments = arguments;
    }

    /**
     * Returns the arguments of this message.
     *
     * @return the arguments of this message
     * @since 0.9
     */
    public List<String> getArguments() { return arguments; }
}
