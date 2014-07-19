package net.apnic.rpki.rsync.impl;

import net.apnic.rpki.rsync.RsyncException;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class P30HandshakeTest {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    @Test
    public void handshakeTest() throws Exception {
        byte[] data = "@RSYNCD: 30.0\n!".getBytes(UTF8);
        Protocol30 p30 = new Protocol30(null);
        ByteBuffer input = ByteBuffer.wrap(data);
        p30.consume(input);
        assertThat("Input not fully consumed", input.hasRemaining(), is(equalTo(true)));
        assertThat("one character remains", input.remaining(), is(equalTo(1)));
    }

    @Test
    public void overrunHandshakeTest() throws Exception {
        byte[] data = "@RSYNCD: overrun attempt\n".getBytes(UTF8);
        Protocol30 p30 = new Protocol30(null);
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
        Protocol30 p30 = new Protocol30(null);
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

    @Test
    public void versionWrittenOnConnect() throws Exception {
        Protocol30 p30 = new Protocol30(null);
        ByteBuffer out = p30.transmit();
        assertThat("out has data pending", out.hasRemaining(), is(equalTo(true)));
        byte[] expected = "@RSYNCD: 30.0\n".getBytes(UTF8);
        byte[] given = new byte[expected.length];
        out.get(given);
        assertThat("out has a version pending", given, is(equalTo(expected)));
    }
}