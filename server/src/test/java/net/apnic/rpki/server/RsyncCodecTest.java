package net.apnic.rpki.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;
import net.apnic.rpki.server.messages.HandshakeMessage;
import org.junit.Test;

import java.util.Queue;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class RsyncCodecTest {
    private ByteBuf combineMessages(Queue<Object> messages) {
        ByteBuf output = Unpooled.buffer();
        for (Object message : messages) {
            output.writeBytes(confirmType(message, ByteBuf.class));
        }
        return output;
    }

    @SuppressWarnings("unchecked")
    private <T> T confirmType(Object object, Class<T> klass) {
        assertThat("The object given is of the correct type", object, is(instanceOf(klass)));
        return (T)object;
    }

    @Test
    public void goodHandshakeAccepted() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new RsyncCodec());

        ch.writeInbound(Unpooled.copiedBuffer("@RSYNCD: 30.0\n", CharsetUtil.UTF_8));
        assertEquals("There should be an inbound message waiting", 1, ch.inboundMessages().size());

        HandshakeMessage message = confirmType(ch.readInbound(), HandshakeMessage.class);
        assertEquals("The server should have received a version 30 message", 30, message.getMajor());
        assertEquals("The server should have received a sub-version 0 message", 0, message.getMinor());
    }

    @Test
    public void badHandshakeRejected() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new RsyncCodec());

        // Clear the connect message
        ch.outboundMessages().clear();

        ch.writeInbound(Unpooled.copiedBuffer("@RSYNCD is a silly protocol\n", CharsetUtil.UTF_8));
        assertEquals("There should be no inbound messages waiting", 0, ch.inboundMessages().size());

        assertThat("There should be outbound messages waiting'", ch.outboundMessages().size(), greaterThan(0));
        ByteBuf errorMessage = combineMessages(ch.outboundMessages());
        assertThat("The message is an error", errorMessage.toString(CharsetUtil.UTF_8), startsWith("@ERROR: "));
    }

}
