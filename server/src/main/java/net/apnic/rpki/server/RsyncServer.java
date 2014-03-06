package net.apnic.rpki.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.apnic.rpki.data.FileSystemRepository;
import net.apnic.rpki.protocol.MemoryCachedModule;
import net.apnic.rpki.protocol.Module;

import java.net.URI;
import java.nio.file.Paths;

/**
 * An rsync server.
 *
 * Listens for connections on the given port to serve the given modules.
 *
 * @author bje
 * @since 0.9
 */
public class RsyncServer {
    private final int port;

    private final RsyncChannelInitializer rsyncChannelInitializer;

    /**
     * Constructs a new RsyncServer with the given configuration.
     *
     * @param port the port to listen on
     * @param modules the modules to serve
     * @since 0.9
     */
    public RsyncServer(int port, Module... modules) {
        this.port = port;
        this.rsyncChannelInitializer = new RsyncChannelInitializer(modules);
    }

    /**
     * Begins the server.  Does not return.
     *
     * @throws InterruptedException if the server's execution is interrupted
     * @since 0.9
     */
    public void run() throws InterruptedException {
        EventLoopGroup listenGroup = new NioEventLoopGroup();
        EventLoopGroup serviceGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(listenGroup, serviceGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(rsyncChannelInitializer)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            bootstrap.bind(port).sync().channel().closeFuture().sync();
        } finally {
            serviceGroup.shutdownGracefully();
            listenGroup.shutdownGracefully();
        }
    }

    /**
     * Runs a sample server that serves out of /data/repositoryd/repository, listening on port 8730.
     *
     * @param args startup arguments, unused.
     * @throws Exception eventually, since there's no other way to exit
     * @since 0.9
     */
    public static void main(String[] args) throws Exception {
        Module simple = new MemoryCachedModule("repository", "simple repo", new FileSystemRepository(
                Paths.get(new URI("file:///data/repositoryd/repository"))
        ));
        new RsyncServer(8730, simple).run();
    }
}