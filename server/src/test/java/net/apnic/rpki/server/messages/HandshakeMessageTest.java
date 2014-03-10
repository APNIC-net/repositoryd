package net.apnic.rpki.server.messages;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author bje
 */
public class HandshakeMessageTest {
    @Test
    public void parseHandshakes() throws Exception {
        HandshakeMessage message = HandshakeMessage.parseHandshake("@RSYNCD: 30.0");
        assertThat("Version is major 30", message.getMajor(), is(equalTo(30)));

        message = HandshakeMessage.parseHandshake("@RSYNCD: 29");
        assertThat("Version is major 29", message.getMajor(), is(equalTo(29)));
        assertThat("Version is minor 0", message.getMinor(), is(equalTo(0)));
    }
}
