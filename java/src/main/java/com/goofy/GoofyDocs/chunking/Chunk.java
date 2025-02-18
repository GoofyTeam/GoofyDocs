package com.goofy.GoofyDocs.chunking;

import java.util.Arrays;

public class Chunk {
    private final byte[] data;
    private final String hash;
    private final int originalSize;
    private final int position;

    public Chunk(byte[] data, String hash, int position) {
        this.data = Arrays.copyOf(data, data.length);
        this.hash = hash;
        this.originalSize = data.length;
        this.position = position;
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    public String getHash() {
        return hash;
    }

    public int getOriginalSize() {
        return originalSize;
    }

    public int getPosition() {
        return position;
    }
}
