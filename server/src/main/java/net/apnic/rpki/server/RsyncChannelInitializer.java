package net.apnic.rpki.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import net.apnic.rpki.protocol.Module;
import net.apnic.rpki.protocol.ProtocolFactory;
import org.slf4j.MDC;

/**
 * Initialize an rsync channel on connect.
 *
 * @author bje
 * @since 0.9
 */
class RsyncChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final ProtocolFactory protocolFactory;

    /**
     * Constructs a new channel initializer with the given modules.
     *
     * @param modules the modules to serve
     * @since 0.9
     */
    public RsyncChannelInitializer(Module... modules) {
        protocolFactory = new ProtocolFactory(modules);
    }

    @Override
    public void initChannel(SocketChannel ch) {
        MDC.put("remote", String.format("%s %d", ch.remoteAddress().getHostString(), ch.remoteAddress().getPort()));
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("codec", new RsyncCodec());
        pipeline.addLast("handler", new RsyncHandler(protocolFactory));
    }
}
