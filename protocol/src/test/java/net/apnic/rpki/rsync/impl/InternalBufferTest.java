package net.apnic.rpki.rsync.impl;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class InternalBufferTest {
    byte[] consumed = new byte[15];
    int consumedCount = 0;

    InternalBuffer.Consumer consumer = new InternalBuffer.Consumer() {
        @Override
        public void consume(ByteBuffer src) {
            if (src.remaining() >= 15) {
                src.get(consumed);
                consumedCount++;
            }
        }
    };

    @Test
    public void testBuffering() throws Exception {
        InternalBuffer buffer = new InternalBuffer(20, 256);

        consumedCount = 0;

        // 40 bytes of data consumed 15 bytes at a time will be read twice
        buffer.buffer(ByteBuffer.wrap(new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38,
                39, 40
        }), consumer);
        assertEquals(2, consumedCount);
        assertArrayEquals(new byte[] {
                16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30
        }, consumed);

        // Adding another 4 bytes will not consume anything
        buffer.buffer(ByteBuffer.wrap(new byte[] { 41, 42, 43, 44 }), consumer);
        assertEquals(2, consumedCount);
        assertArrayEquals(new byte[] {
                16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30
        }, consumed);

        // But add that last byte, and another message should pop out
        buffer.buffer(ByteBuffer.wrap(new byte[] { 45 }), consumer);
        assertEquals(3, consumedCount);
        assertArrayEquals(new byte[] {
                31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45
        }, consumed);
    }
}