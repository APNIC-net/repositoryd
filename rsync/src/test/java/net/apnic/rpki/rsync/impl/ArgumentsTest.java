package net.apnic.rpki.rsync.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.apnic.rpki.rsync.Module;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ArgumentsTest extends ServerTestBase {

    Server handshakenServer() throws Exception {
        Server p30 = new Server(Arrays.asList(new TestModule("module", "")));
        p30.read(bufferForString("@RSYNCD: 30.0\n"));

        ByteBuf output = Unpooled.buffer(128, 256);
        p30.write(output);

        output.clear();
        p30.read(bufferForString("module\n"));
        p30.write(output);
        return p30;
    }

    @Test
    public void typicalArgumentsAcceptedTest() throws Exception {
        Server server = handshakenServer();
        feed(server, bufferForString("--server\0--sender\0--list-only\0-vvvve.Lix\0.\0module\0\0"));

        ByteBuf output = Unpooled.buffer(128, 256);
        server.write(output);
        assertThat("five bytes of setup data exist", output.readableBytes(), is(greaterThanOrEqualTo(5)));
    }

    @Test
    public void protectedArgumentsTest() throws Exception {
        Server server = handshakenServer();
        feed(server, bufferForString("--server\0--sender\0-s\0\0rsync\0.\0module\0\0"));

        ByteBuf output = Unpooled.buffer(128, 256);
        server.write(output);
        assertThat("five bytes of setup data exist", output.readableBytes(), is(greaterThanOrEqualTo(5)));
    }
}
