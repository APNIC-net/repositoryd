package net.apnic.rpki.rsync.impl;

import java.nio.ByteBuffer;

/**
 * Created by bje on 19/07/2014.
 */
class InternalBuffer {
    private ByteBuffer retained;
    private ByteBuffer provided;
    private final int nomSize;
    private final int maxSize;

    InternalBuffer(int chunkSize, int maxSize) {
        retained = ByteBuffer.allocate(maxSize);
        this.nomSize = chunkSize;
        this.maxSize = maxSize;
    }

    void add(ByteBuffer input) {
        this.provided = input;
    }

    boolean consume() {
        if (this.retained.
    }
}
