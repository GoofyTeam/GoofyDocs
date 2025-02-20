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
        // Pour le test d'analyse, on utilise le constructeur simplifié
        duplicationService = new DuplicationService(chunkingService);
    }

    @Test
    void testDuplicationDetectionWithDifferentAlgorithms(@TempDir Path tempDir) throws IOException {
        // Créer un fichier de test avec des données répétitives (1MB)
        File testFile = createTestFile(tempDir, 1024 * 1024);

        // Tester avec SHA-1
        long startTime = System.nanoTime();
        Map<String, Object> sha1Results = duplicationService.analyzeFile(testFile, HashingAlgorithm.SHA1);
        long sha1Time = System.nanoTime() - startTime;

        // Tester avec SHA-256
        startTime = System.nanoTime();
        Map<String, Object> sha256Results = duplicationService.analyzeFile(testFile, HashingAlgorithm.SHA256);
        long sha256Time = System.nanoTime() - startTime;

        // Tester avec BLAKE3
        startTime = System.nanoTime();
        Map<String, Object> blake3Results = duplicationService.analyzeFile(testFile, HashingAlgorithm.BLAKE3);
        long blake3Time = System.nanoTime() - startTime;

        // Affichage des résultats
        System.out.println("=== Résultats des tests de performance ===");
        System.out.println("SHA-1:");
        System.out.println("  - Temps d'exécution: " + sha1Time / 1_000_000.0 + " ms");
        System.out.println("  - Chunks uniques: " + sha1Results.get("uniqueChunks"));
        System.out.println("  - Chunks dupliqués: " + sha1Results.get("duplicateChunks"));
        System.out.println("  - Détails des doublons: " + sha1Results.get("duplicateDetails"));

        System.out.println("\nSHA-256:");
        System.out.println("  - Temps d'exécution: " + sha256Time / 1_000_000.0 + " ms");
        System.out.println("  - Chunks uniques: " + sha256Results.get("uniqueChunks"));
        System.out.println("  - Chunks dupliqués: " + sha256Results.get("duplicateChunks"));
        System.out.println("  - Détails des doublons: " + sha256Results.get("duplicateDetails"));

        System.out.println("\nBLAKE3:");
        System.out.println("  - Temps d'exécution: " + blake3Time / 1_000_000.0 + " ms");
        System.out.println("  - Chunks uniques: " + blake3Results.get("uniqueChunks"));
        System.out.println("  - Chunks dupliqués: " + blake3Results.get("duplicateChunks"));
        System.out.println("  - Détails des doublons: " + blake3Results.get("duplicateDetails"));

        // Vérifications
        assertTrue((Long) sha1Results.get("duplicatedChunks") > 0, "Des doublons devraient être détectés pour SHA-1");
        assertTrue((Long) blake3Results.get("duplicatedChunks") > 0,
                "Des doublons devraient être détectés pour BLAKE3");
        // Le nombre de chunks uniques doit être identique pour tous les algorithmes
        assertEquals(sha1Results.get("uniqueChunks"), sha256Results.get("uniqueChunks"),
                "Le nombre de chunks uniques doit être le même pour SHA-1 et SHA-256");
        assertEquals(sha1Results.get("uniqueChunks"), blake3Results.get("uniqueChunks"),
                "Le nombre de chunks uniques doit être le même pour SHA-1 et BLAKE3");
    }

    @Test
    void testProcessAndStoreFileCompressed(@TempDir Path tempDir) throws IOException {
        // Créer un fichier de test avec des données répétitives (1MB)
        File testFile = createTestFile(tempDir, 1024 * 1024);

        // Utilisation de mocks pour les repositories
        FileRepository fileRepo = mock(FileRepository.class);
        when(fileRepo.save(any(FileEntity.class))).thenAnswer(invocation -> {
            FileEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        ChunkRepository chunkRepo = mock(ChunkRepository.class);
        // Par défaut, aucun chunk n'est trouvé (pour simuler des chunks nouveaux)
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

        // Ré-instancier le service avec toutes les dépendances
        duplicationService = new DuplicationService(chunkingService, fileRepo, chunkRepo, fileChunkRepo,
                compressionService);

        Map<String, Object> result = duplicationService.processAndStoreFileCompressed(
                testFile,
                testFile.getName(),
                testFile.length(),
                HashingAlgorithm.SHA256,
                CompressionService.CompressionType.LZ4);

        // Vérifier que le résultat contient les clés attendues
        assertNotNull(result.get("fileId"));
        assertNotNull(result.get("fileName"));
        assertNotNull(result.get("totalChunks"));
        assertNotNull(result.get("uniqueChunks"));
        assertNotNull(result.get("duplicateChunks"));
        assertNotNull(result.get("totalCompressedSize"));

        // Nouvelle assertion pour la méthode de compression
        assertEquals(CompressionService.CompressionType.LZ4.name(), result.get("compressionType"));

        // Vérifier que la taille compressée totale est inférieure à la somme des
        // tailles originales
        List<Chunk> chunks = chunkingService.chunkFile(testFile);
        long totalOriginalSize = chunks.stream().mapToLong(chunk -> chunk.getData().length).sum();
        long totalCompressedSize = ((Number) result.get("totalCompressedSize")).longValue();
        assertTrue(totalCompressedSize < totalOriginalSize,
                "La taille compressée (" + totalCompressedSize + " octets) doit être inférieure à la taille originale ("
                        + totalOriginalSize + " octets)");

        System.out.println("ProcessAndStoreFileCompressed result: " + result);
    }

    private File createTestFile(Path tempDir, int size) throws IOException {
        File file = tempDir.resolve("test.dat").toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // Créer quelques patterns fixes pour garantir des doublons
            byte[][] patterns = new byte[4][];
            for (int i = 0; i < patterns.length; i++) {
                patterns[i] = new byte[8192]; // 8KB par pattern
                Arrays.fill(patterns[i], (byte) i);
            }
            // Écrire les patterns de manière répétitive
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
