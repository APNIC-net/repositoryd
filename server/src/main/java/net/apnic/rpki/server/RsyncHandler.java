package net.apnic.rpki.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.apnic.rpki.protocol.*;
import net.apnic.rpki.server.messages.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RsyncHandler extends SimpleChannelInboundHandler<WireMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RsyncHandler.class);
    private Protocol protocol;
    private CommandLine commandLine;

    private final ProtocolFactory protocolFactory;

    public RsyncHandler(ProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    private class WireException extends Exception {
        public WireException(String msg) { super(msg); }
        public WireException(Throwable cause) { super(cause); }
        public WireException(String msg, Throwable cause) { super(msg, cause); }
    }

    private class BufferedMessageSender implements MessageSender {
        private final ChannelHandlerContext ctx;
        private final ByteBuf buffer;
        private final static int capacity = 8192;

        @Override
        public void sendBytes(byte[] bytes) {
            sendBytes(bytes, 0, bytes.length);
        }

        @Override
        public void sendByte(byte datum) {
            ensureSpace(1);
            buffer.writeByte(datum);
        }

        @Override
        public void sendBytes(byte[] bytes, int from, int length) {
            while (length > capacity) {
                ensureSpace(capacity);
                buffer.writeBytes(bytes, from, capacity);
                from += capacity;
                length -= capacity;
            }
            ensureSpace(length);
            buffer.writeBytes(bytes, from, length);
        }

        private void ensureSpace(int needed) {
            if (buffer.writableBytes() < needed) {
                flush();
            }

            /* Still not enough!? */
            if (buffer.writableBytes() < needed) {
                throw new OutOfMemoryError("Buffer overrun");
            }
        }

        void flush() {
            if (buffer.readableBytes() > 0) {
                ctx.writeAndFlush(new ProtocolMessage(buffer));
                buffer.clear();
            }
        }

        BufferedMessageSender(ChannelHandlerContext ctx) {
            this.ctx = ctx;
            this.buffer = ctx.alloc().buffer(capacity, capacity);
        }
    }

    private BufferedMessageSender sender;

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, WireMessage msg) throws WireException {
        if (sender == null) sender = new BufferedMessageSender(ctx);

        // I expect a CommandMessage, then an ArgumentsMessage, then a FiltersMessage, then ... ?
        if (msg instanceof HandshakeMessage) {
            HandshakeMessage handshakeMessage = (HandshakeMessage)msg;
            try {
                protocol = protocolFactory.protocolForVersion(handshakeMessage.getMajor(), handshakeMessage.getMinor());
            } catch (IncompatibleVersionException ex) {
                throw new WireException("@ERROR: " + ex.getMessage(), ex);
            }
        } else if (msg instanceof CommandMessage) {
            String command = ((CommandMessage)msg).getCommand();

            if (command.equals("") || command.equals("#list")) {
                Iterable<Module> modules = protocol.getModuleList();
                for (Module module : modules) {
                    ctx.write(new ResponseMessage(String.format("%-15s\t%s\n", module.getName(), module.getDescription())));
                }
                ctx.writeAndFlush(new ResponseMessage("@RSYNCD: EXIT\n"))
                        .addListener(ChannelFutureListener.CLOSE);
            } else if (command.startsWith("#")) {
                throw new WireException("@ERROR: Unknown command '" + command + "'");
            } else {
                // specific module
                try {
                    protocol.selectModule(command);
                } catch (NoSuchModuleException ex) {
                    throw new WireException("@ERROR: unknown module '" + command + "'");
                }

                ctx.writeAndFlush(new ResponseMessage("@RSYNCD: OK\n"));
            }
        } else if (msg instanceof ArgumentsMessage) {
            List<String> arguments = ((ArgumentsMessage)msg).getArguments();

            try {
                commandLine = new PosixParser().parse(Arguments.rsyncOptions(), arguments.toArray(new String[arguments.size()]));
            } catch (ParseException e) {
                throw new WireException("@ERROR: bad arguments (" + e.getMessage() + ")", e);
            }

            // Convert the commandLine instance to a properties map
            Map<String, List<String>> properties = new HashMap<>();
            for (Option option : commandLine.getOptions()) {
                String argName = option.getArgName();
                if (argName == null) argName = option.getOpt();
                String[] values = option.getValues();
                if (values == null) values = new String[] { option.getDescription() };

                properties.put(argName, Arrays.asList(values));
            }

            // May trigger an error using the above MessageSender, and may close the connection
            protocol.setProperties(properties);

            if (!ctx.channel().isOpen()) return;

            // Finish setting up the protocol
            ctx.writeAndFlush(new SetupMessage(protocol.getCompatibilityFlags(), protocol.getChecksumSeed()));
        } else if (msg instanceof FiltersMessage) {
            if (((FiltersMessage) msg).getFilters().size() > 0)
                throw new WireException("@ERROR: Filters not permitted on this server");

            // Need all but one arg
            List<String> args = Arrays.asList(commandLine.getArgs());
            try {
                protocol.sendFileList(args.subList(1, args.size()), sender);
            } catch (ProtocolError error) {
                throw new WireException(error);
            }
        } else if (msg instanceof GeneratorMessage) {
            GeneratorMessage generatorMessage = (GeneratorMessage)msg;

            try {
                protocol.sendExtraFileList(sender);
                protocol.transferFile(generatorMessage.getAttributes(), generatorMessage.getChecksums(), sender);
            } catch (ProtocolError error) {
                throw new WireException(error);
            }
        } else if (msg instanceof ListDoneMessage) {
            try {
                protocol.sendExtraFileList(sender);
                protocol.completedList(sender);
            } catch (ProtocolError error) {
                throw new WireException(error);
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        int[] version = protocolFactory.supportedProtocolVersion();
        ctx.writeAndFlush(new HandshakeMessage(version[0], version[1]));

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        sender.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.debug("Connection closed.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Exception caught in RsyncHandler");
        LOGGER.debug("Error caught was:", cause);
        if (cause instanceof WireException) {
            WireException ex = (WireException)cause;
            if (ex.getCause() instanceof ProtocolError) {
                ctx.writeAndFlush(new ErrorMessage((ProtocolError)ex.getCause()));
            } else {
                ctx.writeAndFlush(new ErrorMessage(new ProtocolError(ProtocolError.ErrorType.FERROR, cause.getMessage())))
                        .addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }
}
