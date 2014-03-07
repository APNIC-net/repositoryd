package net.apnic.rpki.server;

import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
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
    private boolean protocolInitialising = false;

    private final ProtocolFactory protocolFactory;

    public RsyncHandler(ProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    private class WireException extends Exception {
        public WireException(String msg) { super(msg); }
        public WireException(Throwable cause) { super(cause); }
        public WireException(String msg, Throwable cause) { super(msg, cause); }
    }

    /* Avoid creating large numbers of one-byte arrays to queue for delivery */
    private final static byte[] bytes = {
            (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07,
            (byte)0x08, (byte)0x09, (byte)0x0a, (byte)0x0b, (byte)0x0c, (byte)0x0d, (byte)0x0e, (byte)0x0f,
            (byte)0x10, (byte)0x11, (byte)0x12, (byte)0x13, (byte)0x14, (byte)0x15, (byte)0x16, (byte)0x17,
            (byte)0x18, (byte)0x19, (byte)0x1a, (byte)0x1b, (byte)0x1c, (byte)0x1d, (byte)0x1e, (byte)0x1f,
            (byte)0x20, (byte)0x21, (byte)0x22, (byte)0x23, (byte)0x24, (byte)0x25, (byte)0x26, (byte)0x27,
            (byte)0x28, (byte)0x29, (byte)0x2a, (byte)0x2b, (byte)0x2c, (byte)0x2d, (byte)0x2e, (byte)0x2f,
            (byte)0x30, (byte)0x31, (byte)0x32, (byte)0x33, (byte)0x34, (byte)0x35, (byte)0x36, (byte)0x37,
            (byte)0x38, (byte)0x39, (byte)0x3a, (byte)0x3b, (byte)0x3c, (byte)0x3d, (byte)0x3e, (byte)0x3f,
            (byte)0x40, (byte)0x41, (byte)0x42, (byte)0x43, (byte)0x44, (byte)0x45, (byte)0x46, (byte)0x47,
            (byte)0x48, (byte)0x49, (byte)0x4a, (byte)0x4b, (byte)0x4c, (byte)0x4d, (byte)0x4e, (byte)0x4f,
            (byte)0x50, (byte)0x51, (byte)0x52, (byte)0x53, (byte)0x54, (byte)0x55, (byte)0x56, (byte)0x57,
            (byte)0x58, (byte)0x59, (byte)0x5a, (byte)0x5b, (byte)0x5c, (byte)0x5d, (byte)0x5e, (byte)0x5f,
            (byte)0x60, (byte)0x61, (byte)0x62, (byte)0x63, (byte)0x64, (byte)0x65, (byte)0x66, (byte)0x67,
            (byte)0x68, (byte)0x69, (byte)0x6a, (byte)0x6b, (byte)0x6c, (byte)0x6d, (byte)0x6e, (byte)0x6f,
            (byte)0x70, (byte)0x71, (byte)0x72, (byte)0x73, (byte)0x74, (byte)0x75, (byte)0x76, (byte)0x77,
            (byte)0x78, (byte)0x79, (byte)0x7a, (byte)0x7b, (byte)0x7c, (byte)0x7d, (byte)0x7e, (byte)0x7f,
            (byte)0x80, (byte)0x81, (byte)0x82, (byte)0x83, (byte)0x84, (byte)0x85, (byte)0x86, (byte)0x87,
            (byte)0x88, (byte)0x89, (byte)0x8a, (byte)0x8b, (byte)0x8c, (byte)0x8d, (byte)0x8e, (byte)0x8f,
            (byte)0x90, (byte)0x91, (byte)0x92, (byte)0x93, (byte)0x94, (byte)0x95, (byte)0x96, (byte)0x97,
            (byte)0x98, (byte)0x99, (byte)0x9a, (byte)0x9b, (byte)0x9c, (byte)0x9d, (byte)0x9e, (byte)0x9f,
            (byte)0xa0, (byte)0xa1, (byte)0xa2, (byte)0xa3, (byte)0xa4, (byte)0xa5, (byte)0xa6, (byte)0xa7,
            (byte)0xa8, (byte)0xa9, (byte)0xaa, (byte)0xab, (byte)0xac, (byte)0xad, (byte)0xae, (byte)0xaf,
            (byte)0xb0, (byte)0xb1, (byte)0xb2, (byte)0xb3, (byte)0xb4, (byte)0xb5, (byte)0xb6, (byte)0xb7,
            (byte)0xb8, (byte)0xb9, (byte)0xba, (byte)0xbb, (byte)0xbc, (byte)0xbd, (byte)0xbe, (byte)0xbf,
            (byte)0xc0, (byte)0xc1, (byte)0xc2, (byte)0xc3, (byte)0xc4, (byte)0xc5, (byte)0xc6, (byte)0xc7,
            (byte)0xc8, (byte)0xc9, (byte)0xca, (byte)0xcb, (byte)0xcc, (byte)0xcd, (byte)0xce, (byte)0xcf,
            (byte)0xd0, (byte)0xd1, (byte)0xd2, (byte)0xd3, (byte)0xd4, (byte)0xd5, (byte)0xd6, (byte)0xd7,
            (byte)0xd8, (byte)0xd9, (byte)0xda, (byte)0xdb, (byte)0xdc, (byte)0xdd, (byte)0xde, (byte)0xdf,
            (byte)0xe0, (byte)0xe1, (byte)0xe2, (byte)0xe3, (byte)0xe4, (byte)0xe5, (byte)0xe6, (byte)0xe7,
            (byte)0xe8, (byte)0xe9, (byte)0xea, (byte)0xeb, (byte)0xec, (byte)0xed, (byte)0xee, (byte)0xef,
            (byte)0xf0, (byte)0xf1, (byte)0xf2, (byte)0xf3, (byte)0xf4, (byte)0xf5, (byte)0xf6, (byte)0xf7,
            (byte)0xf8, (byte)0xf9, (byte)0xfa, (byte)0xfb, (byte)0xfc, (byte)0xfd, (byte)0xfe, (byte)0xff
};
    private class BufferedMessageSender implements MessageSender {
        private final ChannelHandlerContext ctx;
        private CompositeByteBuf buffer;
        private final static int capacity = 8192;

        @Override
        public void sendBytes(byte[] bytes) {
            sendBytes(bytes, 0, bytes.length);
        }

        @Override
        public void sendByte(int datum) {
            sendBytes(bytes, datum & 0xff, 1);
        }

        @Override
        public void sendBytes(final byte[] bytes, final int from, final int length) {
            if (buffer.numComponents() >= capacity) {
                LOGGER.debug("About to require consolidation in composite buffer {}", buffer);
            }
            buffer.addComponent(Unpooled.wrappedBuffer(bytes, from, length));
            buffer.writerIndex(buffer.writerIndex() + length);
        }

        @Override
        public void sendInformation(String message) {
            flush();
            ctx.writeAndFlush(new ErrorMessage(message, ProtocolError.ErrorType.FINFO.getCode()));
        }

        void flush() {
            if (buffer.readableBytes() > 0) {
                ctx.writeAndFlush(new ProtocolMessage(buffer));
            } else {
                buffer.release();
            }
            buffer = ctx.alloc().compositeBuffer(capacity);
        }

        BufferedMessageSender(ChannelHandlerContext ctx) {
            this.ctx = ctx;
            buffer = ctx.alloc().compositeBuffer(capacity);
        }

        @Override
        protected void finalize() throws Throwable {
            buffer.release();
            super.finalize();
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
                throw new WireException(ex.getMessage(), ex);
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
                throw new WireException("Unknown command '" + command + "'");
            } else {
                // specific module
                try {
                    protocol.selectModule(command);
                } catch (NoSuchModuleException ex) {
                    throw new WireException("unknown module '" + command + "'");
                }

                ctx.writeAndFlush(new ResponseMessage("@RSYNCD: OK\n"));
            }
        } else if (msg instanceof ArgumentsMessage) {
            protocolInitialising = true;
            List<String> arguments = ((ArgumentsMessage)msg).getArguments();

            try {
                commandLine = new PosixParser().parse(Arguments.rsyncOptions(), arguments.toArray(new String[arguments.size()]));
            } catch (ParseException e) {
                throw new WireException("bad arguments (" + e.getMessage() + ")", e);
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

            try {
                protocol.setProperties(properties);
            } catch (ProtocolError protocolError) {
                throw new WireException(protocolError);
            }

            if (!ctx.channel().isOpen()) return;

            // Finish setting up the protocol
            ctx.writeAndFlush(new SetupMessage(protocol.getCompatibilityFlags(), protocol.getChecksumSeed()));
            protocolInitialising = false;
        } else if (msg instanceof FiltersMessage) {
            if (((FiltersMessage) msg).getFilters().size() > 0)
                throw new WireException("Filters not permitted on this server");

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
        LOGGER.info("Client connection ended.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // Need to finish initialising the protocol to allow the remote end to read the error
        if (protocolInitialising) {
            ctx.write(new SetupMessage((byte)0, 0));
            protocolInitialising = false;
        }

        LOGGER.error("Exception caught in RsyncHandler: {}", cause.getMessage());
        LOGGER.debug("Exception caught in RsyncHandler: ", cause);
        if (cause instanceof WireException) {
            WireException ex = (WireException)cause;
            final ErrorMessage error;
            if (ex.getCause() instanceof ProtocolError) {
               error = new ErrorMessage((ProtocolError)ex.getCause());
            } else {
                error = new ErrorMessage(new ProtocolError(ProtocolError.ErrorType.FERROR, cause.getMessage()));
            }
            ctx.write(error);
            ctx.writeAndFlush(new ProtocolMessage(Unpooled.wrappedBuffer(bytes, 0, 1)))
                    .addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.close();
        }
    }
}
