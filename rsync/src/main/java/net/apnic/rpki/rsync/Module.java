package net.apnic.rpki.rsync;

/**
 * A Module is a set of files to be served by a Protocol object. The Module interface
 * provides a layer of abstraction over the source of files.
 *
 * @author Byron Ellacott
 * @since 2.0
 */
public interface Module {
    /**
     * Gets the name of this Module.
     *
     * @return the name of this Module
     * @since 0.9
     */
    public String getName();

    /**
     * Gets the description of this Module.
     *
     * @return the description of this Module.
     * @since 0.9
     */
    public String getDescription();


}
