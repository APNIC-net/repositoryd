package net.apnic.rpki.server;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.Test;

public class IndexReaderTest {
    @Test
    public void positiveIndices() {
        IndexReader reader = new IndexReader();
        ByteBuf b = Unpooled.wrappedBuffer(new byte[] { 0x1 });

        b.markReaderIndex();
        assertThat("Reading 0x01 gives index value 0", reader.readIndex(b), is(equalTo(0)));
        b.resetReaderIndex();
        assertThat("A second reading of 0x01 gives index 1", reader.readIndex(b), is(equalTo(1)));
        b.resetReaderIndex();

        b = Unpooled.wrappedBuffer(new byte[] { (byte)0xfe, 0x70, 0x00 });
        assertThat("Reading 0xfe 0x70 0x00 gives index value 0x7001", reader.readIndex(b), is(equalTo(0x7001)));

        b = Unpooled.wrappedBuffer(new byte[] { (byte)0xfe, (byte)0x83, 0x10, 0x20, 0x30 });
        assertThat("Reading 0xfe 0x83 0x10 0x20 0x30 gives index value 0x03302010",
                reader.readIndex(b), is(equalTo(0x03302010)));
    }

    @Test
    public void negativeIndices() {
        IndexReader reader = new IndexReader();

        ByteBuf b = Unpooled.wrappedBuffer(new byte[] { -1, 0x65 });
        assertThat("Reading 0xff 0x65 gives index -102", reader.readIndex(b), is(equalTo(-102)));

        b = Unpooled.wrappedBuffer(new byte[] { -1, -2, -128, 2, 0, 0 });
        assertThat("Reading 0xff 0xfe 0x80 0x02 0x00 0x00 gives index -2",
                reader.readIndex(b), is(equalTo(-2)));
        assertThat("After reading -2 from a six-byte input, there are no unread bytes",
                b.readableBytes(), is(equalTo(0)));
    }
}
