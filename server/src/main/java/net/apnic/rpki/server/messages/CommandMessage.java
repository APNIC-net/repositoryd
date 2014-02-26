package net.apnic.rpki.server.messages;

/**
 * Encapsulates an rsync command message.
 *
 * This message is sent after the version negotiation is complete, and
 * is either a module list request or a module name.
 *
 * @author bje
 * @since 0.9
 */
public class CommandMessage extends WireMessage {
    private final String command;

    /**
     * Constructs a new CommandMessage with the given command.
     *
     * @param command the command
     * @since 0.9
     */
    public CommandMessage(String command) {
        this.command = command;
    }

    /**
     * Returns the command.
     *
     * @return the command
     * @since 0.9
     */
    public String getCommand() { return command; }
}
