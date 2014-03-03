package net.apnic.rpki.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.apnic.rpki.protocol.FileList;
import net.apnic.rpki.protocol.Module;
import net.apnic.rpki.protocol.NoSuchPathException;
import net.apnic.rpki.protocol.ProtocolFactory;
import net.apnic.rpki.server.messages.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Queue;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class RsyncHandlerTest {
    private EmbeddedChannel channel;

    private final Module dummyModule = new Module() {
        @Override
        public String getName() {
            return "member_repository";
        }

        @Override
        public String getDescription() {
            return "member repository";
        }

        @Override
        public FileList getFileList(String rootPath, boolean recursive) throws NoSuchPathException {
            return null;
        }
    };

    @Before
    public void createHandler() {
        channel = new EmbeddedChannel(new RsyncHandler(new ProtocolFactory(dummyModule)));
    }
    @Test
    public void sendsInitialVersion() {
        assertThat("There should be data available on 'connect'", channel.outboundMessages().size(), equalTo(1));
        Object object = channel.readOutbound();
        assertThat("The message available is a HandshakeMessage", object, is(instanceOf(HandshakeMessage.class)));
        assertThat("The message is for major version 30", ((HandshakeMessage)object).getMajor(), is(equalTo(30)));
        assertThat("The message is for minor version 0", ((HandshakeMessage)object).getMinor(), is(equalTo(0)));
    }

    @Test
    public void negotiateProtocolVersions() {
        // Get the server's version out of the way
        channel.readOutbound();

        channel.writeInbound(new HandshakeMessage(29, 0));

        assertThat("There should be a message waiting now", channel.outboundMessages().size(), equalTo(1));
        Object object = channel.readOutbound();
        assertThat("The message available is an ErrorMessage", object, is(instanceOf(ErrorMessage.class)));
        assertThat("The error message is an @ERROR", ((ErrorMessage)object).getError(), startsWith("@ERROR: "));
        assertFalse("The channel should no longer be open", channel.isOpen());
    }

    @Test
    public void listModules() {
        channel.readOutbound(); // version
        channel.writeInbound(new HandshakeMessage(31, 0));
        channel.writeInbound(new CommandMessage(""));
        assertThat("There should be messages waiting now", channel.outboundMessages().size(), greaterThanOrEqualTo(1));
        Queue<Object> messages = channel.outboundMessages();
        for (Object object: messages) {
            assertThat("Each message available is a ResponseMessage", object, is(instanceOf(ResponseMessage.class)));
            if (messages.isEmpty()) {
                assertThat("The last message is @RSYNCD: EXIT", ((ResponseMessage)object).getResponse(), is(equalTo("@RSYNCD: EXIT\n")));
            }
        }
        assertFalse("The channel should no longer be open", channel.isOpen());
    }

    @Test
    public void awaitFileList() {
        channel.readOutbound(); // version
        channel.writeInbound(new HandshakeMessage(31, 0));
        channel.writeInbound(new CommandMessage("member_repository"));

        // check that we're all sorted
        assertThat("There should be one message waiting now", channel.outboundMessages().size(), equalTo(1));
        Object message = channel.readOutbound();
        assertThat("The waiting message should be a ResponseMessage", message, is(instanceOf(ResponseMessage.class)));
        assertThat("Everything should be OK", ((ResponseMessage)message).getResponse(), equalTo("@RSYNCD: OK\n"));

        // send an empty filter list
        channel.writeInbound(new ProtocolMessage(Unpooled.wrappedBuffer(new byte[] { 0x00, 0x00, 0x00, 0x00})));
    }
}
