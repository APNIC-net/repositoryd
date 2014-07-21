package net.apnic.rpki.rsync.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.apnic.rpki.rsync.Module;
import net.apnic.rpki.rsync.RsyncException;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.ArrayList;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class P30HandshakeTest {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    @Test
    public void handshakeTest() throws Exception {
        byte[] data = "@RSYNCD: 30.0\n!".getBytes(UTF8);
        Server p30 = new Server(new ArrayList<Module>());
        ByteBuf input = Unpooled.wrappedBuffer(data);
        p30.read(input);
        assertThat("Input not fully consumed", input.isReadable(), is(equalTo(true)));
        assertThat("one character remains", input.readableBytes(), is(equalTo(1)));
    }

    @Test
    public void overrunHandshakeTest() throws Exception {
        byte[] data = "@RSYNCD: overrun attempt\n".getBytes(UTF8);
        Server p30 = new Server(new ArrayList<Module>());
        ByteBuf input = Unpooled.wrappedBuffer(data);
        boolean excepted = false;
        try {
            p30.read(input);
        } catch (RsyncException ex) {
            excepted = true;
        }
        assertThat("an overrun exception was triggered", excepted, is(equalTo(true)));
    }

    @Test
    public void versionMatchingTest() throws Exception {
        Server p30 = new Server(new ArrayList<Module>());
        boolean excepted = false;
        try {
            p30.read(Unpooled.wrappedBuffer("@RSYNCD: 29\n".getBytes(UTF8)));
        } catch (RsyncException ex) {
            excepted = true;
        }
        assertTrue("version < 30 fails", excepted);

        excepted = false;
        try {
            p30.read(Unpooled.wrappedBuffer("@RSYNCD: 30.1\n".getBytes(UTF8)));
        } catch (RsyncException ex) {
            excepted = true;
        }
        assertTrue("minor != 0 fails", excepted);

        p30.read(Unpooled.wrappedBuffer("@RSYNCD: 31.5\n".getBytes(UTF8)));
    }

    @Test
    public void versionWrittenOnConnect() throws Exception {
        Server p30 = new Server(new ArrayList<Module>());
        ByteBuf output = Unpooled.buffer(128, 128);
        assertTrue("The server had something to write", p30.write(output));
        assertThat("out has data pending", output.isReadable(), is(equalTo(true)));
        byte[] expected = "@RSYNCD: 30.0\n".getBytes(UTF8);
        assertThat("there are the right number of bytes written", output.readableBytes(), is(equalTo(expected.length)));
        byte[] given = new byte[expected.length];
        output.readBytes(given);
        assertThat("out has a version pending", given, is(equalTo(expected)));
    }
}