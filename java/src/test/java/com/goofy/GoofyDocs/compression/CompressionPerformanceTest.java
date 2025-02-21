package com.goofy.GoofyDocs.compression;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.StopWatch;

public class CompressionPerformanceTest {

    private CompressionService compressionService;
    private static final int[] IMAGE_SIZES = { 100, 500, 1000 };
    private static final CompressionService.CompressionType[] COMPRESSION_TYPES = {
            CompressionService.CompressionType.LZ4,
            CompressionService.CompressionType.ZSTD,
            CompressionService.CompressionType.SNAPPY
    };

    @BeforeEach
    void setup() {
        compressionService = new CompressionService();
    }

    @Test
    void testImageCompressionPerformance(@TempDir Path tempDir) throws IOException {
        System.out.println("\n=== Performance Test on Images ===");
        System.out.println("Format: Size | Algorithm | Compression Time | Compression Ratio | Decompression Time");
        System.out.println("------------------------------------------------------------------------");

        for (int size : IMAGE_SIZES) {
            File imageFile = createTestImage(tempDir, size);
            byte[] originalData = readFileToBytes(imageFile);

            for (CompressionService.CompressionType type : COMPRESSION_TYPES) {
                StopWatch compressionWatch = new StopWatch();
                compressionWatch.start();
                byte[] compressedData = compressionService.compress(originalData, type);
                compressionWatch.stop();

                StopWatch decompressionWatch = new StopWatch();
                decompressionWatch.start();
                decompressionWatch.stop();

                double compressionRatio = (double) compressedData.length / originalData.length * 100;
                double compressionSpeed = originalData.length / (compressionWatch.getTotalTimeMillis() / 1000.0)
                        / (1024 * 1024);
                double decompressionSpeed = originalData.length / (decompressionWatch.getTotalTimeMillis() / 1000.0)
                        / (1024 * 1024);

                System.out.printf("%dx%d pixels with %s:\n", size, size, type);
                System.out.printf("  - Original size: %.2f MB\n", originalData.length / (1024.0 * 1024.0));
                System.out.printf("  - Compressed size: %.2f MB\n", compressedData.length / (1024.0 * 1024.0));
                System.out.printf("  - Compression ratio: %.2f%%\n", compressionRatio);
                System.out.printf("  - Compression speed: %.2f MB/s\n", compressionSpeed);
                System.out.printf("  - Decompression speed: %.2f MB/s\n", decompressionSpeed);
                System.out.printf("  - Compression time: %d ms\n", compressionWatch.getTotalTimeMillis());
                System.out.printf("  - Decompression time: %d ms\n", decompressionWatch.getTotalTimeMillis());
                System.out.println("------------------------------------------------------------------------");
            }
        }
    }

    @Test
    void testCompressionDashboard(@TempDir Path tempDir) throws IOException {
        Map<String, Map<String, Double>> metrics = new HashMap<>();
        String[] fileTypes = { "text", "image", "binary" };
        int[] fileSizes = { 1, 10, 50 };

        System.out.println("\n=== Compression Performance Dashboard ===");
        System.out.println(
                "Format: Type | Size | Algorithm | Compression (ms) | Decompression (ms) | Ratio | Speed (MB/s)");
        System.out.println("--------------------------------------------------------------------------------");

        for (String fileType : fileTypes) {
            metrics.put(fileType, new HashMap<>());
            for (int size : fileSizes) {
                File testFile = createTestFile(tempDir, fileType, size * 1024 * 1024);
                byte[] originalData = readFileToBytes(testFile);

                for (CompressionService.CompressionType type : COMPRESSION_TYPES) {
                    PerformanceMetrics perfMetrics = measureCompression(originalData, type);

                    System.out.printf("%s (%dMB) with %s:\n", fileType, size, type);
                    System.out.printf("  - Compression time: %.2f ms\n", perfMetrics.compressionTime);
                    System.out.printf("  - Decompression time: %.2f ms\n", perfMetrics.decompressionTime);
                    System.out.printf("  - Compression ratio: %.2f%%\n", perfMetrics.compressionRatio);
                    System.out.printf("  - Average speed: %.2f MB/s\n", perfMetrics.averageSpeed);
                    System.out.println(
                            "--------------------------------------------------------------------------------");
                }
            }
        }
    }

    private File createTestImage(Path tempDir, int size) throws IOException {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                image.setRGB(x, y, (x + y) % 256 * 65536);
            }
        }
        File imageFile = tempDir.resolve("test_" + size + ".png").toFile();
        ImageIO.write(image, "PNG", imageFile);
        return imageFile;
    }

    private File createTestFile(Path tempDir, String type, int size) throws IOException {
        File file = tempDir.resolve("test_" + type + "_" + size + ".dat").toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int remaining = size;
            while (remaining > 0) {
                int toWrite = Math.min(remaining, buffer.length);
                if ("text".equals(type)) {
                    for (int i = 0; i < toWrite; i++) {
                        buffer[i] = (byte) ((i % 26) + 'a');
                    }
                } else if ("binary".equals(type)) {
                    for (int i = 0; i < toWrite; i++) {
                        buffer[i] = (byte) (Math.random() * 256);
                    }
                }
                fos.write(buffer, 0, toWrite);
                remaining -= toWrite;
            }
        }
        return file;
    }

    private byte[] readFileToBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes();
        }
    }

    private static class PerformanceMetrics {
        double compressionTime;
        double decompressionTime;
        double compressionRatio;
        double averageSpeed;
    }

    private PerformanceMetrics measureCompression(byte[] originalData, CompressionService.CompressionType type)
            throws IOException {
        PerformanceMetrics metrics = new PerformanceMetrics();

        StopWatch watch = new StopWatch();
        watch.start();
        byte[] compressedData = compressionService.compress(originalData, type);
        watch.stop();
        metrics.compressionTime = watch.getTotalTimeMillis();

        watch = new StopWatch();
        watch.start();
        watch.stop();
        metrics.decompressionTime = watch.getTotalTimeMillis();

        metrics.compressionRatio = (double) compressedData.length / originalData.length * 100;
        double totalTime = (metrics.compressionTime + metrics.decompressionTime) / 1000.0;
        metrics.averageSpeed = (originalData.length / (1024.0 * 1024.0)) / totalTime;

        return metrics;
    }
}
