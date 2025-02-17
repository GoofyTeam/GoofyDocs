package com.goofy.GoofyFiles.chunking;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

public class ChunkingPerformanceTest {

    private ChunkingService chunkingService;
    private static final int[] FILE_SIZES = {
            1 * 1024 * 1024,     // 1 MB
            10 * 1024 * 1024,    // 10 MB
            50 * 1024 * 1024     // 50 MB
    };

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        chunkingService = new ChunkingService();
    }

    @Test
    void testChunkingPerformance() throws IOException {
        System.out.println("\n=== Test de Performance du Chunking ===");
        System.out.println("Format: Taille | Temps | Vitesse | Nb Chunks | Taille Moy | Duplication");
        System.out.println("--------------------------------------------------------");

        for (int fileSize : FILE_SIZES) {
            File testFile = createTestFile(fileSize);
            
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            List<Chunk> chunks = chunkingService.chunkFile(testFile);
            stopWatch.stop();

            double timeInSeconds = stopWatch.getTotalTimeSeconds();
            double speedMBps = (fileSize / (1024.0 * 1024.0)) / timeInSeconds;
            double avgChunkSize = fileSize / (double) chunks.size();
            long uniqueChunks = chunks.stream()
                    .map(Chunk::getHash)
                    .distinct()
                    .count();
            double duplicationRate = 1.0 - ((double) uniqueChunks / chunks.size());

            System.out.printf("%5.1f MB | %6.3f s | %6.1f MB/s | %8d | %7.0f B | %6.2f%%%n",
                    fileSize / (1024.0 * 1024.0),
                    timeInSeconds,
                    speedMBps,
                    chunks.size(),
                    avgChunkSize,
                    duplicationRate * 100);

            testFile.delete();

            assertNotNull(chunks);
            assertTrue(chunks.size() > 0);
        }
    }

    private File createTestFile(int size) throws IOException {
        File file = File.createTempFile("perf-test-", ".dat");
        file.deleteOnExit();

        // Créer des données avec un certain degré de répétition
        byte[] repeatingPattern = new byte[1024]; // 1KB pattern
        new Random().nextBytes(repeatingPattern);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            int written = 0;
            while (written < size) {
                // 70% chance d'écrire le pattern répétitif, 30% chance de données aléatoires
                if (Math.random() < 0.7) {
                    fos.write(repeatingPattern);
                } else {
                    byte[] randomData = new byte[1024];
                    new Random().nextBytes(randomData);
                    fos.write(randomData);
                }
                written += 1024;
            }
        }

        return file;
    }
}
