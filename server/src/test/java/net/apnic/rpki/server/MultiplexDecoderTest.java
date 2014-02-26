package net.apnic.rpki.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

/**
 * @author bje
 */
public class MultiplexDecoderTest {
    @Test
    public void decodeMessageData() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new MultiplexDecoder());
        ch.writeInbound(Unpooled.wrappedBuffer(new byte[] { 0x04, 0x00, 0x00, 0x07 } ));
        ch.writeInbound(Unpooled.wrappedBuffer(new byte[] { 0x00, 0x00, 0x00, 0x00 }));
    }
}
