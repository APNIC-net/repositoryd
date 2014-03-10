package net.apnic.rpki.protocol;

import java.util.List;
import java.util.Map;

/**
 * RSYNC Protocol interface.  Defines the core behaviour of an rsync (sender) system, separate to communications.
 *
 * @author Byron Ellacott
 * @since 0.9
 */
public interface Protocol {
    /**
     * Get the negotiated (major) version of this instance.
     *
     * @return the negotiated (major) version of this instance
     * @since 0.9
     */
    public int getVersion();

    /**
     * Returns the list of Modules served by this Protocol.
     *
     * @return the list of Modules served by this Protocol
     * @since 0.9
     */
    public Iterable<Module> getModuleList();

    /**
     * Returns the named module, or null if the module does not exist.
     *
     * @param module the module to return
     * @return the named module, or null if the module does not exist
     * @since 0.9
     */
    public Module getModuleByName(String module);

    /**
     * Sets the module being served by this Protocol.
     *
     * @param module the module to serve
     * @throws NoSuchModuleException if the named module is not served by this Protocol
     * @since 0.9
     */
    public void selectModule(String module) throws NoSuchModuleException;

    /**
     * Sets the protocol properties requested by the client.
     *
     *
     * @param properties the protocol properties requested by the client
     * @throws ProtocolError if the requested properties are invalid or unsupported
     * @since 0.9
     */
    public void setProperties(Map<String,List<String>> properties) throws ProtocolError;

    /**
     * Returns the compatibility flags resulting from the properties negiation.
     *
     * The return value is not defined if this is called before setProperties().
     *
     * @return the compatibility flags for the current properties set
     * @since 0.9
     */
    public byte getCompatibilityFlags();

    /**
     * Returns the checksum seed to be used.
     *
     * @return the checksum seed to be used.
     * @since 0.9
     */
    public int getChecksumSeed();

    /**
     * Send the file list through the given MessageSender.
     *
     * Note that incremental recursion means this may not send the entire file list.  The @{sendExtraFileList} method
     * should be called repeatedly until there are no more files to send.
     *
     * @param paths the path(s) to send lists for
     * @param sender the MessageSender to use
     * @throws ProtocolError if the path(s) do not exist
     * @since 0.9
     */
    public void sendFileList(List<String> paths, MessageSender sender) throws ProtocolError;

    /**
     * Transfer file content through the message sender.
     *
     * @param attributes the attributes of the transfer request
     * @param checksums the checksums of the remote item
     * @param sender the MessageSender to use
     * @throws ProtocolError if the transfer request is invalid
     * @since 0.9
     */
    public void transferFile(TransferAttributes attributes, Checksums checksums, MessageSender sender) throws ProtocolError;

    /**
     * Note that file list has been processed by receiver
     *
     * @param sender the MessageSender to use
     * @return true when the exchange is finalised
     * @since 0.9
     */
    public boolean completedList(MessageSender sender) throws ProtocolError;
}