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

public class HandshakeTest extends ServerTestBase {

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

    void versionTest(String version, boolean succeeds) {
        Server server = new Server(new ArrayList<Module>());

        boolean excepted = false;
        try {
            server.read(bufferForString(version));
        } catch (RsyncException ex) {
            excepted = true;
        }
        assertThat(version, excepted, is(equalTo(!succeeds)));
    }

    @Test
    public void versionMatchingTest() throws Exception {
        versionTest("@RSYNCD: 28\n", false);
        versionTest("@RSYNCD: 29\n", true);
        versionTest("@RSYNCD: 30\n", false);
        versionTest("@RSYNCD: 30.0\n", true);
        versionTest("@RSYNCD: 30.1\n", true);
        versionTest("@RSYNCD: 31.0\n", true);
    }

    @Test
    public void versionWrittenOnConnect() throws Exception {
        Server p30 = new Server(new ArrayList<Module>());
        ByteBuf output = Unpooled.buffer(128, 128);
        assertTrue("The server had something to write", p30.write(output));

        checkOutput("@RSYNCD: 30.0\n", output);
    }
}