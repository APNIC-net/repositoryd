package net.apnic.rpki.rsync.impl;

import java.nio.ByteBuffer;

/**
 * Assist reassembly of input messages.
 */
class InternalBuffer {
    private ByteBuffer retained;
    private final int nomSize;

    InternalBuffer(int chunkSize, int maxSize) {
        retained = ByteBuffer.allocate(maxSize);
        retained.flip();    // set limit=0, position=0
        this.nomSize = chunkSize;
    }

    // A useful optimisation would be to detect when this function has already consumed
    // all bytes previously retained, and start feeding the consumer directly out of the
    // input buffer.
    void buffer(ByteBuffer input, Consumer consumer) throws Exception {
        // pre: there is less than a whole message in retained
        // pre: retained.position == 0
        // pre: retained.limit >= 0
        while (input.hasRemaining()) {
            // fill retained from limit with at most nomSize bytes from input
            int toEat = Integer.min(input.remaining(), nomSize);
            // don't overflow the retained buffer, either
            toEat = Integer.min(toEat, retained.capacity() - retained.limit());
            input.get(retained.array(), retained.arrayOffset() + retained.limit(), toEat);
            retained.limit(retained.limit() + toEat);
            int position;
            do {
                position = retained.position();
                consumer.consume(retained);
            } while (retained.position() > position);
            retained.compact().flip();
        }
        // post: input.remaining() == 0, retained.remaining() >= 0
        // post: there is less than a whole message in retained
    }

    interface Consumer {
        /**
         * Consume a message from src, if possible.
         *
         * This method should advance the position of the src ByteBuffer if it consumes a
         * message.  If the position is not advanced, it is assumed there is insufficient
         * data available in the src for an entire message.
         *
         * @param src the buffer from which to consume a message
         * @throws Exception when something goes wrong in consumption
         */
        public void consume(ByteBuffer src) throws Exception;
    }
}
