package net.apnic.rpki.rsync.impl;

import net.apnic.rpki.rsync.RsyncException;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

public class P30HandshakeTest {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    @Test
    public void handshakeTest() throws Exception {
        byte[] data = "@RSYNCD: 30.0\n!".getBytes(UTF8);
        Protocol30 p30 = new Protocol30();
        ByteBuffer input = ByteBuffer.wrap(data);
        p30.consume(input);
        assertThat("Input not fully consumed", input.hasRemaining(), is(equalTo(true)));
        assertThat("one character remains", input.remaining(), is(equalTo(1)));
    }

    @Test
    public void overrunHandshakeTest() throws Exception {
        byte[] data = "@RSYNCD: overrun attempt\n".getBytes(UTF8);
        Protocol30 p30 = new Protocol30();
        ByteBuffer input = ByteBuffer.wrap(data);
        boolean excepted = false;
        try {
            p30.consume(input);
        } catch (RsyncException ex) {
            excepted = true;
        }
        assertThat("an overrun exception was triggered", excepted, is(equalTo(true)));
    }

    @Test
    public void versionMatchingTest() throws Exception {
        Protocol30 p30 = new Protocol30();
        boolean excepted = false;
        try {
            p30.consume(ByteBuffer.wrap("@RSYNCD: 29\n".getBytes(UTF8)));
        } catch (RsyncException ex) {
            excepted = true;
        }
        assertTrue("version < 30 fails", excepted);

        excepted = false;
        try {
            p30.consume(ByteBuffer.wrap("@RSYNCD: 30.1\n".getBytes(UTF8)));
        } catch (RsyncException ex) {
            excepted = true;
        }
        assertTrue("minor != 0 fails", excepted);

        p30.consume(ByteBuffer.wrap("@RSYNCD: 31.5\n".getBytes(UTF8)));

    }
}