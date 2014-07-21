package net.apnic.rpki.rsync.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

class ServerTestBase {
    public static final Charset UTF8 = Charset.forName("UTF-8");

    ByteBuf bufferForString(String s) {
        return Unpooled.wrappedBuffer(s.getBytes(UTF8));
    }

    void checkOutput(byte[] expected, ByteBuf output) {
        assertThat("output has data pending", output.isReadable(), is(equalTo(true)));
        assertThat("output has expected number of bytes", output.readableBytes(), is(greaterThanOrEqualTo(expected.length)));
        byte[] given = new byte[expected.length];
        output.readBytes(given);
        assertThat("output has expected data", given, is(equalTo(expected)));
    }

    void checkOutput(String expected, ByteBuf output) {
        checkOutput(expected.getBytes(UTF8), output);
    }
}
