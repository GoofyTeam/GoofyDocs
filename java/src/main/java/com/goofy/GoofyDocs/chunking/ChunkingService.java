package com.goofy.GoofyDocs.chunking;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ChunkingService {
    private static final int MIN_CHUNK_SIZE = 4 * 1024;
    private static final int MAX_CHUNK_SIZE = 64 * 1024;
    private static final String HASH_ALGORITHM = "SHA-256";

    private final RabinKarp rabinKarp;
    private final MessageDigest messageDigest;

    public ChunkingService() throws NoSuchAlgorithmException {
        this.rabinKarp = new RabinKarp();
        this.messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
    }

    public List<Chunk> chunkFile(File file) throws IOException {
        List<Chunk> chunks = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file)) {
            ByteArrayOutputStream currentChunk = new ByteArrayOutputStream();
            int currentPosition = 0;
            int b;
            int currentChunkSize = 0;

            while ((b = fis.read()) != -1) {
                currentChunk.write(b);
                currentChunkSize++;

                if (currentChunkSize >= MIN_CHUNK_SIZE && rabinKarp.pushByte((byte) b) ||
                        currentChunkSize >= MAX_CHUNK_SIZE) {

                    byte[] chunkData = currentChunk.toByteArray();
                    String hash = calculateHash(chunkData);
                    chunks.add(new Chunk(chunkData, hash, currentPosition));

                    currentPosition += currentChunkSize;
                    currentChunkSize = 0;
                    currentChunk.reset();
                    rabinKarp.reset();
                }
            }

            if (currentChunkSize > 0) {
                byte[] chunkData = currentChunk.toByteArray();
                String hash = calculateHash(chunkData);
                chunks.add(new Chunk(chunkData, hash, currentPosition));
            }
        }

        return chunks;
    }

    private String calculateHash(byte[] data) {
        messageDigest.reset();
        byte[] hash = messageDigest.digest(data);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
