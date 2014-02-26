package net.apnic.rpki.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.util.CharsetUtil;
import net.apnic.rpki.server.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

class RsyncCodec extends ByteToMessageCodec<WireMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RsyncCodec.class);

    private enum RsyncState {
        HANDSHAKE,
        COMMAND,
        ARGUMENTS,
        FILTER_LIST,
        SEND_FILES
    }

    private final MultiplexDecoder decoder = new MultiplexDecoder();

    private final IndexReader indexReader = new IndexReader();
    private GeneratorMessage generatorMessage = null;

    private RsyncState state;
    private boolean multiplexing = false;

    private final List<String> arguments = new ArrayList<>();
    private final List<String> filters = new ArrayList<>();

    public RsyncCodec() {
        state = RsyncState.HANDSHAKE;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, WireMessage msg, ByteBuf out) throws Exception {
        if (msg instanceof HandshakeMessage) {
            HandshakeMessage handshake = (HandshakeMessage)msg;
            String message = String.format("@RSYNCD: %d.%d\n", handshake.getMajor(), handshake.getMinor());
            final ByteBuf data = ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(message), CharsetUtil.UTF_8);
            out.writeBytes(data);
            data.release();
        } else if (msg instanceof ResponseMessage) {
            final ByteBuf data = ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(((ResponseMessage)msg).getResponse()),
                    CharsetUtil.UTF_8);
            if (multiplexing) {
                int header = data.readableBytes() + ((MessageType.MSG_ERROR.getCodeValue() + 7) << 24);
                out.writeInt(ByteBufUtil.swapInt(header));
            }
            out.writeBytes(data);
            data.release();
        } else if (msg instanceof SetupMessage) {
            SetupMessage setupMessage = (SetupMessage)msg;
            out.writeByte(setupMessage.getFlags());
            out.writeInt(setupMessage.getSeed());
        } else if (msg instanceof ProtocolMessage) {
            final ByteBuf data = ((ProtocolMessage)msg).getBytes();
            if (multiplexing) {
                int header = data.readableBytes() + ((MessageType.MSG_DATA.getCodeValue() + 7) << 24);
                out.writeInt(ByteBufUtil.swapInt(header));
            }
            out.writeBytes(data);
        } else if (msg instanceof ErrorMessage) {
            ErrorMessage errorMessage = (ErrorMessage)msg;
            final ByteBuf data = ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(errorMessage.getError()),
                    CharsetUtil.UTF_8);

            if (multiplexing) {
                int header = data.readableBytes() + ((errorMessage.getCode() + 7) << 24);
                out.writeInt(ByteBufUtil.swapInt(header));
            }
            out.writeBytes(data);
            data.release();
        } else {
            System.out.println("Wire message: " + msg);
        }
    }

    private ChannelFuture writeString(ChannelHandlerContext ctx, String msg) {
        return ctx.writeAndFlush( ByteBufUtil.encodeString(ctx.alloc(),
                CharBuffer.wrap(msg),
                CharsetUtil.UTF_8));
    }

    private class MissingMessageException extends Exception {
        public MissingMessageException() { super(); }
//        public MissingMessageException(String msg) { super(msg); }
//        public MissingMessageException(Throwable cause) { super(cause); }
//        public MissingMessageException(String msg, Throwable cause) { super(msg, cause); }
    }

    private String delineatedString(ByteBuf in, int sizeCap, byte delimiter) throws Exception {
        int messageSize = in.bytesBefore(delimiter);

        if (messageSize == -1 && in.readableBytes() > sizeCap)
            throw new MissingMessageException();

        if (messageSize == -1) return null;

        byte[] data = new byte[messageSize];
        in.readBytes(data);
        in.skipBytes(1); // skip delimiter

        return new String(data, "UTF-8");
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state) {
            case HANDSHAKE:
                LOGGER.debug("Handshaking a new connection with this: {}", this);
                try {
                    String handshake = delineatedString(in, 16, (byte) '\n');
                    if (handshake == null) return;

                    out.add(HandshakeMessage.parseHandshake(handshake));
                    LOGGER.debug("Handshake received: {}", handshake);
                } catch (MissingMessageException ex) {
                    writeString(ctx, "@ERROR: protocol startup error\n")
                            .addListener(ChannelFutureListener.CLOSE);
                } catch (Exception ex) { // includes IncompatibleVersionException
                    writeString(ctx, "@ERROR: " + ex.getMessage())
                            .addListener(ChannelFutureListener.CLOSE);
                }

                // Switch state
                state = RsyncState.COMMAND;

                break;

            case COMMAND:
                try {
                    String command = delineatedString(in, 40, (byte)'\n');
                    if (command == null) return;

                    out.add(new CommandMessage(command));
                    LOGGER.debug("Command received: {}", command);
                } catch (MissingMessageException ex) {
                    writeString(ctx, "@ERROR: protocol startup error\n")
                            .addListener(ChannelFutureListener.CLOSE);
                } catch (Exception ex) {
                    writeString(ctx, "@ERROR: " + ex.getMessage() + "\n")
                            .addListener(ChannelFutureListener.CLOSE);
                }

                state = RsyncState.ARGUMENTS;

                break;

            case ARGUMENTS:
                multiplexing = true;

                try {
                    String argument = delineatedString(in, 128, (byte)'\0');
                    if (argument == null) return;

                    if (argument.isEmpty()) {
                        out.add(new ArgumentsMessage(arguments));
                        LOGGER.debug("Arguments received: " + arguments);

                        // enable multiplex decoding of input
                        ctx.pipeline().addFirst("mplex-decoder", decoder);
                        LOGGER.debug("Multiplexing mode engaged");

                        state = RsyncState.FILTER_LIST;
                    } else {
                        arguments.add(argument);

                        if (arguments.size() > 20) {
                            writeString(ctx, "@ERROR: argument list too long\n")
                                    .addListener(ChannelFutureListener.CLOSE);
                        }
                    }
                } catch (MissingMessageException ex) {
                    writeString(ctx, "@ERROR: argument too long\n")
                            .addListener(ChannelFutureListener.CLOSE);
                }

                break;

            case FILTER_LIST:
                if (in.readableBytes() < 4) return;

                int filterExpressionSize = ByteBufUtil.swapInt((int)in.readUnsignedInt());

                if (filterExpressionSize == 0) {
                    out.add(new FiltersMessage(filters));
                    state = RsyncState.SEND_FILES;
                    break;
                }

                if (in.readableBytes() < filterExpressionSize) {
                    in.resetReaderIndex();
                    return;
                }

                String filter = in.readBytes(filterExpressionSize).toString(CharsetUtil.UTF_8);
                LOGGER.debug("I'm probably going to ingore this filter: {}", filter);
                filters.add(filter);

                break;

            case SEND_FILES:
                if (generatorMessage == null) {
                    Integer ndx = indexReader.readIndex(in);
                    if (ndx == null) {
                        in.resetReaderIndex();
                        return;
                    }

                    if (ndx >= 0) {
                        generatorMessage = new GeneratorMessage(ndx);
                    } else if (ndx == IndexReader.NDX_DONE) {
                        out.add(new ListDoneMessage());
                    } else {
                        // fail.
                        throw new Exception("index fail");
                    }
                }

                if (generatorMessage != null && generatorMessage.constructWithBytes(in)) {
                    out.add(generatorMessage);
                    generatorMessage = null;
                }

                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("Exception caught: " + cause.getMessage());
        super.exceptionCaught(ctx, cause);
    }
}
