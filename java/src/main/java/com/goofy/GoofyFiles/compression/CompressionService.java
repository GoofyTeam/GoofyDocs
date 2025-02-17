package com.goofy.GoofyFiles.compression;

import org.springframework.stereotype.Service;
import org.xerial.snappy.Snappy;

import com.github.luben.zstd.Zstd;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

@Service
public class CompressionService {

    private final LZ4Factory lz4Factory;
    private final LZ4Compressor lz4Compressor;
    private final LZ4FastDecompressor lz4Decompressor;

    public enum CompressionType {
        LZ4,
        ZSTD,
        SNAPPY
    }

    public CompressionService() {
        this.lz4Factory = LZ4Factory.fastestInstance();
        this.lz4Compressor = lz4Factory.fastCompressor();
        this.lz4Decompressor = lz4Factory.fastDecompressor();
    }

    public byte[] compress(byte[] data, CompressionType type) {
        try {
            return switch (type) {
                case LZ4 -> compressLZ4(data);
                case ZSTD -> compressZstd(data);
                case SNAPPY -> compressSnappy(data);
            };
        } catch (Exception e) {
            throw new RuntimeException("Compression failed", e);
        }
    }

    public byte[] decompress(byte[] compressedData, CompressionType type, int originalLength) {
        try {
            return switch (type) {
                case LZ4 -> decompressLZ4(compressedData, originalLength);
                case ZSTD -> decompressZstd(compressedData);
                case SNAPPY -> decompressSnappy(compressedData);
            };
        } catch (Exception e) {
            throw new RuntimeException("Decompression failed", e);
        }
    }

    private byte[] compressLZ4(byte[] data) {
        return lz4Compressor.compress(data);
    }

    private byte[] decompressLZ4(byte[] compressedData, int originalLength) {
        return lz4Decompressor.decompress(compressedData, originalLength);
    }   

    private byte[] compressZstd(byte[] data) {
        return Zstd.compress(data);
    }

    private byte[] decompressZstd(byte[] compressedData) {
        long originalSize = Zstd.getFrameContentSize(compressedData);
            return Zstd.decompress(compressedData, (int) originalSize);
    }

    private byte[] compressSnappy(byte[] data) throws Exception {
        return Snappy.compress(data);
    }

    private byte[] decompressSnappy(byte[] compressedData) throws Exception {
        return Snappy.uncompress(compressedData);
    }
}
