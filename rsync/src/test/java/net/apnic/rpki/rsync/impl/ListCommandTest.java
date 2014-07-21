package net.apnic.rpki.rsync.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.apnic.rpki.rsync.Module;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ListCommandTest {
    public static final Charset UTF8 = Charset.forName("UTF-8");

    ByteBuf bufferForString(String s) {
        return Unpooled.wrappedBuffer(s.getBytes(UTF8));
    }

    Server30 handshakenServer(List<Module> modules) throws Exception {
        Server30 p30 = new Server30(modules);
        p30.read(bufferForString("@RSYNCD: 30.0\n"));

        ByteBuf output = Unpooled.buffer(128, 256);
        p30.write(output);
        return p30;
    }

    @Test
    public void listEmptyModules() throws Exception {
        Server30 p30 = handshakenServer(new ArrayList<Module>());
        p30.read(bufferForString("\n"));
        ByteBuf output = Unpooled.buffer(128, 256);
        p30.write(output);
        assertNotNull("out is not null", output);
        assertThat("out has data pending", output.isReadable(), is(equalTo(true)));
        byte[] expected = "@RSYNCD: EXIT\n".getBytes(UTF8);
        assertThat("out has expected number of bytes", output.readableBytes(), is(equalTo(expected.length)));
        byte[] given = new byte[expected.length];
        output.readBytes(given);
        assertThat("out has a version pending", given, is(equalTo(expected)));
    }

}