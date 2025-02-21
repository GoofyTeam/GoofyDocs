package com.goofy.GoofyDocs.duplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.goofy.GoofyDocs.chunking.Chunk;
import com.goofy.GoofyDocs.chunking.ChunkingService;
import com.goofy.GoofyDocs.compression.CompressionService;
import com.goofy.GoofyDocs.model.ChunkEntity;
import com.goofy.GoofyDocs.model.FileChunkEntity;
import com.goofy.GoofyDocs.model.FileEntity;
import com.goofy.GoofyDocs.repository.ChunkRepository;
import com.goofy.GoofyDocs.repository.FileChunkRepository;
import com.goofy.GoofyDocs.repository.FileRepository;

class DuplicationPerformanceTest {

    private DuplicationService duplicationService;
    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        chunkingService = new ChunkingService();
        duplicationService = new DuplicationService(chunkingService);
    }

    @Test
    void testDuplicationDetectionWithDifferentAlgorithms(@TempDir Path tempDir) throws IOException {
        File testFile = createTestFile(tempDir, 1024 * 1024);

        long startTime = System.nanoTime();
        Map<String, Object> sha1Results = duplicationService.analyzeFile(testFile, HashingAlgorithm.SHA1);
        long sha1Time = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        Map<String, Object> sha256Results = duplicationService.analyzeFile(testFile, HashingAlgorithm.SHA256);
        long sha256Time = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        Map<String, Object> blake3Results = duplicationService.analyzeFile(testFile, HashingAlgorithm.BLAKE3);
        long blake3Time = System.nanoTime() - startTime;

        System.out.println("=== Performance Test Results ===");
        System.out.println("SHA-1:");
        System.out.println("  - Execution time: " + sha1Time / 1_000_000.0 + " ms");
        System.out.println("  - Unique chunks: " + sha1Results.get("uniqueChunks"));
        System.out.println("  - Duplicated chunks: " + sha1Results.get("duplicateChunks"));
        System.out.println("  - Duplicate details: " + sha1Results.get("duplicateDetails"));

        System.out.println("\nSHA-256:");
        System.out.println("  - Execution time: " + sha256Time / 1_000_000.0 + " ms");
        System.out.println("  - Unique chunks: " + sha256Results.get("uniqueChunks"));
        System.out.println("  - Duplicated chunks: " + sha256Results.get("duplicateChunks"));
        System.out.println("  - Duplicate details: " + sha256Results.get("duplicateDetails"));

        System.out.println("\nBLAKE3:");
        System.out.println("  - Execution time: " + blake3Time / 1_000_000.0 + " ms");
        System.out.println("  - Unique chunks: " + blake3Results.get("uniqueChunks"));
        System.out.println("  - Duplicated chunks: " + blake3Results.get("duplicateChunks"));
        System.out.println("  - Duplicate details: " + blake3Results.get("duplicateDetails"));

        assertTrue((Long) sha1Results.get("duplicatedChunks") > 0, "Duplicates should be detected for SHA-1");
        assertTrue((Long) blake3Results.get("duplicatedChunks") > 0, "Duplicates should be detected for BLAKE3");
        assertEquals(sha1Results.get("uniqueChunks"), sha256Results.get("uniqueChunks"),
                "The number of unique chunks must be equal for SHA-1 and SHA-256");
        assertEquals(sha1Results.get("uniqueChunks"), blake3Results.get("uniqueChunks"),
                "The number of unique chunks must be equal for SHA-1 and BLAKE3");
    }

    @Test
    void testProcessAndStoreFileCompressed(@TempDir Path tempDir) throws IOException {
        File testFile = createTestFile(tempDir, 1024 * 1024);

        FileRepository fileRepo = mock(FileRepository.class);
        when(fileRepo.save(any(FileEntity.class))).thenAnswer(invocation -> {
            FileEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        ChunkRepository chunkRepo = mock(ChunkRepository.class);
        when(chunkRepo.findByHashSha1(anyString())).thenReturn(Optional.empty());
        when(chunkRepo.findByHashSha256(anyString())).thenReturn(Optional.empty());
        when(chunkRepo.findByHashBlake3(anyString())).thenReturn(Optional.empty());
        when(chunkRepo.save(any(ChunkEntity.class))).thenAnswer(invocation -> {
            ChunkEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        FileChunkRepository fileChunkRepo = mock(FileChunkRepository.class);
        when(fileChunkRepo.save(any(FileChunkEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompressionService compressionService = new CompressionService();

        duplicationService = new DuplicationService(chunkingService, fileRepo, chunkRepo, fileChunkRepo,
                compressionService);

        Map<String, Object> result = duplicationService.processAndStoreFileCompressed(
                testFile,
                testFile.getName(),
                testFile.length(),
                HashingAlgorithm.SHA256,
                CompressionService.CompressionType.LZ4);

        assertNotNull(result.get("fileId"));
        assertNotNull(result.get("fileName"));
        assertNotNull(result.get("totalChunks"));
        assertNotNull(result.get("uniqueChunks"));
        assertNotNull(result.get("duplicateChunks"));
        assertNotNull(result.get("totalCompressedSize"));

        assertEquals(CompressionService.CompressionType.LZ4.name(), result.get("compressionType"));

        List<Chunk> chunks = chunkingService.chunkFile(testFile);
        long totalOriginalSize = chunks.stream().mapToLong(chunk -> chunk.getData().length).sum();
        long totalCompressedSize = ((Number) result.get("totalCompressedSize")).longValue();
        assertTrue(totalCompressedSize < totalOriginalSize,
                "Compressed size (" + totalCompressedSize + " bytes) must be less than original size ("
                        + totalOriginalSize + " bytes)");

        System.out.println("ProcessAndStoreFileCompressed result: " + result);
    }

    private File createTestFile(Path tempDir, int size) throws IOException {
        File file = tempDir.resolve("test.dat").toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[][] patterns = new byte[4][];
            for (int i = 0; i < patterns.length; i++) {
                patterns[i] = new byte[8192];
                Arrays.fill(patterns[i], (byte) i);
            }
            Random random = new Random(42);
            int written = 0;
            while (written < size) {
                byte[] pattern = patterns[random.nextInt(patterns.length)];
                fos.write(pattern);
                written += pattern.length;
            }
        }
        return file;
    }
}
