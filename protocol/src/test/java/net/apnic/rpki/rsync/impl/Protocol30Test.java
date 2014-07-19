package net.apnic.rpki.rsync.impl;

import net.apnic.rpki.rsync.RsyncException;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

public class Protocol30Test {
    @Test
    public void handshakeTest() throws Exception {
        byte[] data = "@RSYNCD: 30.0\n!".getBytes(Charset.forName("UTF-8"));
        Protocol30 p30 = new Protocol30();
        ByteBuffer input = ByteBuffer.wrap(data);
        p30.consume(input);
        assertThat("Input not fully consumed", input.hasRemaining(), is(equalTo(true)));
        assertThat("one character remains", input.remaining(), is(equalTo(1)));
    }

    @Test
    public void overrunHandshakeTest() throws Exception {
        byte[] data = "@RSYNCD: overrun attempt\n".getBytes(Charset.forName("UTF-8"));
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
}