package net.apnic.rpki.rsync;

import io.netty.buffer.ByteBufAllocator;

/**
 * Construct a default Protocol instance.
 *
 * @author Byron Ellacott
 * @since 2.0
 */
public class DefaultProtocolBuilder implements ProtocolBuilder {
    @Override
    public Protocol build() {
        return null;
    }

    @Override
    public ProtocolBuilder withAllocator(ByteBufAllocator allocator) {
        return this;
    }

    @Override
    public ProtocolBuilder serveModule(Module module) {
        return this;
    }
}
