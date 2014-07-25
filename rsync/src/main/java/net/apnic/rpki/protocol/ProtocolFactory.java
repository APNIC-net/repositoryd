package net.apnic.rpki.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates new Protocol instances for each connecting client.
 *
 * @author bje
 * @since 0.9
 */
public class ProtocolFactory {
    private final List<Module> modules = new ArrayList<>();

    /**
     * Constructs a new ProtocolFactory serving the given list of modules.
     *
     * @param modules the list of modules to serve
     * @since 0.9
     */
    public ProtocolFactory(Module... modules) {
        Collections.addAll(this.modules, modules);
    }

    /**
     * Creates a Protocol instance to suit the requested protocol version.
     *
     * @param major the protocol major version
     * @param minor the protocol minor (beta) version
     * @return a Protocol instance to suit the requested protocol version
     * @throws IncompatibleVersionException if the requested protocol version is incompatible
     * @since 0.9
     */
    public Protocol protocolForVersion(int major, int minor) throws IncompatibleVersionException {
        if (major < 30)
            throw new IncompatibleVersionException(String.format("protocol version [%s] detected but 30 or greater is required",major));

        if (major == 30 && minor != 0)
            throw new IncompatibleVersionException(String.format("your client is speaking an incompatible beta of protocol 30 [minor=%s]",
                    minor));

        if (major > 30) major = 30;

        return new ProtocolImpl(major, modules);
    }

    /**
     * Returns the highest supported protocol version of this factory.
     *
     * The return value is a two-int array containing the major and minor versions supported.
     *
     * @return the highest supported protocol version of this factory
     * @since 0.9
     */
    public int[] supportedProtocolVersion() {
        return new int[] { 30, 0 };
    }
}
