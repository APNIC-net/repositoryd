package net.apnic.rpki.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.apnic.rpki.server.messages.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Decodes multiplexed messages.  Does not like anything other than MSG_DATA.
 *
 * @author bje
 */
class MultiplexDecoder extends ByteToMessageDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiplexDecoder.class);

    private static final int MPLEX_BASE = 7;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) return;

        int dataSize = ByteBufUtil.swapMedium(in.readUnsignedMedium());
        int tagByte = in.readUnsignedByte();
        MessageType tag = MessageType.typeForTag(tagByte - MPLEX_BASE);

        // todo: be better about this.
        if (tag == null) throw new Exception("Invalid tag: " + (tagByte - MPLEX_BASE));

        switch (tag) {
            case MSG_DATA:
                if (in.readableBytes() < dataSize) {
                    in.resetReaderIndex();
                    return;
                }
                if (dataSize > 0x10000)
                    LOGGER.debug("Very large data packet received, {} bytes", dataSize);
                ByteBuf content = ctx.alloc().heapBuffer(dataSize);
                content.writeBytes(in.readBytes(dataSize));
                out.add(content);

                break;
            default:
                throw new Exception("Unhandled tag: " + tag);
        }
    }
}
