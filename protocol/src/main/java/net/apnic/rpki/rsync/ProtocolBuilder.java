package net.apnic.rpki.rsync;

/**
 * Constructs a Protocol instance to suit the needs of a calling system.
 *
 * @author Byron Ellacott
 * @since 2.0
 */
public interface ProtocolBuilder {
    /**
     * Build the protocol object with the configured options.
     * <p/>
     * If you don't specify either a target directory or at least one Module, this
     * will return a rather useless Protocol that will fail all connection negotiations.
     *
     * @return the built Protocol object.
     * @since 2.0
     */
    public Protocol build();

    /**
     * Add a module to the protocol object to be built.
     *
     * If at least one module has been added, the Protocol will successfully negotiate as
     * a sender and provide module list and module transfer operations to a remote receiver.
     *
     * @param module the module to add
     * @return the modified ProtocolBuilder object
     * @since 2.0
     */
    public ProtocolBuilder serveModule(Module module);

    // fetchModule(Module module)

}
