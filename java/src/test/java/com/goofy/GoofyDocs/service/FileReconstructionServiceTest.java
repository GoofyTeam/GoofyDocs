package com.goofy.GoofyDocs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.StopWatch;

import com.goofy.GoofyDocs.compression.CompressionService;
import com.goofy.GoofyDocs.model.ChunkEntity;
import com.goofy.GoofyDocs.model.FileChunkEntity;
import com.goofy.GoofyDocs.model.FileEntity;
import com.goofy.GoofyDocs.repository.FileChunkRepository;
import com.goofy.GoofyDocs.repository.FileRepository;

class FileReconstructionServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileChunkRepository fileChunkRepository;

    @Mock
    private CompressionService compressionService;

    private FileReconstructionService service;

    private static final int[] FILE_SIZES = { 1, 10, 50, 100 };
    private static final String[] FILE_TYPES = { "text", "binary", "mixed" };
    private static final int[] CHUNK_SIZES = { 1024, 4096, 16384 };

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new FileReconstructionService(fileRepository, fileChunkRepository, compressionService);
    }

    @Test
    void testReconstructionPerformanceDashboard(@TempDir Path tempDir) throws IOException {
        System.out.println("\n=== File Reconstruction Performance Dashboard ===");
        System.out.println("Format: Type | File Size | Chunk Size | Total Time | Speed | Chunks/s");
        System.out.println("------------------------------------------------------------------------");

        for (String fileType : FILE_TYPES) {
            for (int fileSize : FILE_SIZES) {
                for (int chunkSize : CHUNK_SIZES) {
                    TestFileData testData = createTestFileWithChunks(tempDir, fileType, fileSize * 1024 * 1024,
                            chunkSize);

                    when(fileRepository.findById(testData.fileEntity.getId()))
                            .thenReturn(Optional.of(testData.fileEntity));
                    when(fileChunkRepository.findByFileIdOrderByPosition(testData.fileEntity.getId()))
                            .thenReturn(testData.fileChunks);

                    StopWatch watch = new StopWatch();
                    watch.start();
                    byte[] reconstructedFile = service.reconstructFile(testData.fileEntity.getId());
                    watch.stop();

                    double totalTimeSeconds = watch.getTotalTimeMillis() / 1000.0;
                    double speedMBps = fileSize / totalTimeSeconds;
                    double chunksPerSecond = testData.fileChunks.size() / totalTimeSeconds;

                    System.out.printf("%s (%dMB, chunks %dKB):\n", fileType, fileSize, chunkSize / 1024);
                    System.out.printf("  - Total time: %.2f s\n", totalTimeSeconds);
                    System.out.printf("  - Speed: %.2f MB/s\n", speedMBps);
                    System.out.printf("  - Chunks processed: %d chunks\n", testData.fileChunks.size());
                    System.out.printf("  - Throughput: %.2f chunks/s\n", chunksPerSecond);
                    System.out.printf("  - Final size: %.2f MB\n", reconstructedFile.length / (1024.0 * 1024.0));
                    System.out.println("------------------------------------------------------------------------");
                }
            }
        }
    }

    private static class TestFileData {
        FileEntity fileEntity;
        List<FileChunkEntity> fileChunks;
        byte[] originalData;
    }

    private TestFileData createTestFileWithChunks(Path tempDir, String type, int size, int chunkSize)
            throws IOException {
        TestFileData data = new TestFileData();

        data.originalData = new byte[size];
        Random random = new Random();
        if ("text".equals(type)) {
            for (int i = 0; i < size; i++) {
                data.originalData[i] = (byte) ((i % 26) + 'a');
            }
        } else if ("binary".equals(type)) {
            random.nextBytes(data.originalData);
        } else {
            for (int i = 0; i < size; i++) {
                data.originalData[i] = (byte) (i % 256);
            }
        }

        data.fileEntity = new FileEntity();
        data.fileEntity.setId(random.nextLong());
        data.fileEntity.setName("test_" + type);
        data.fileEntity.setExtension(type);

        data.fileChunks = new ArrayList<>();
        for (int offset = 0; offset < size; offset += chunkSize) {
            int length = Math.min(chunkSize, size - offset);
            byte[] chunkData = Arrays.copyOfRange(data.originalData, offset, offset + length);

            ChunkEntity chunk = new ChunkEntity();
            chunk.setId(random.nextLong());
            chunk.setData(chunkData);

            FileChunkEntity fileChunk = new FileChunkEntity();
            fileChunk.setFile(data.fileEntity);
            fileChunk.setChunk(chunk);
            fileChunk.setPosition(offset / chunkSize);

            data.fileChunks.add(fileChunk);
        }

        return data;
    }

    @Test
    void testReconstructFile() throws IOException {
        Long fileId = 1L;
        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(fileId);
        fileEntity.setName("test");
        fileEntity.setExtension("txt");

        byte[] chunk1Data = "Hello ".getBytes();
        byte[] chunk2Data = "World".getBytes();
        byte[] chunk3Data = "!".getBytes();

        ChunkEntity chunk1 = new ChunkEntity();
        chunk1.setData(chunk1Data);
        ChunkEntity chunk2 = new ChunkEntity();
        chunk2.setData(chunk2Data);
        ChunkEntity chunk3 = new ChunkEntity();
        chunk3.setData(chunk3Data);

        FileChunkEntity fileChunk1 = new FileChunkEntity();
        fileChunk1.setFile(fileEntity);
        fileChunk1.setChunk(chunk1);
        fileChunk1.setPosition(0);

        FileChunkEntity fileChunk2 = new FileChunkEntity();
        fileChunk2.setFile(fileEntity);
        fileChunk2.setChunk(chunk2);
        fileChunk2.setPosition(1);

        FileChunkEntity fileChunk3 = new FileChunkEntity();
        fileChunk3.setFile(fileEntity);
        fileChunk3.setChunk(chunk3);
        fileChunk3.setPosition(2);

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(fileEntity));
        when(fileChunkRepository.findByFileIdOrderByPosition(fileId))
                .thenReturn(Arrays.asList(fileChunk1, fileChunk2, fileChunk3));

        byte[] reconstructedFile = service.reconstructFile(fileId);

        assertNotNull(reconstructedFile);
        assertEquals("Hello World!", new String(reconstructedFile));
    }

    @Test
    void testReconstructCompressedFile() throws IOException {
        Long fileId = 1L;
        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(fileId);
        fileEntity.setName("test");
        fileEntity.setExtension("txt");

        byte[] compressedData = "compressed".getBytes();
        byte[] originalData = "Hello World!".getBytes();
        int originalSize = originalData.length;

        ChunkEntity chunk = new ChunkEntity();
        chunk.setData(compressedData);
        chunk.setCompressionType(CompressionService.CompressionType.LZ4.name());
        chunk.setOriginalSize(originalSize);

        FileChunkEntity fileChunk = new FileChunkEntity();
        fileChunk.setFile(fileEntity);
        fileChunk.setChunk(chunk);
        fileChunk.setPosition(0);

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(fileEntity));
        when(fileChunkRepository.findByFileIdOrderByPosition(fileId))
                .thenReturn(List.of(fileChunk));
        when(compressionService.decompress(compressedData, CompressionService.CompressionType.LZ4, originalSize))
                .thenReturn(originalData);

        byte[] reconstructedFile = service.reconstructFile(fileId);

        assertNotNull(reconstructedFile);
        assertEquals("Hello World!", new String(reconstructedFile));
        verify(compressionService).decompress(compressedData, CompressionService.CompressionType.LZ4, originalSize);
    }

    @Test
    void testFileNotFound() {
        Long fileId = 999L;
        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.reconstructFile(fileId);
        });
    }

    @Test
    void testNoChunksFound() {
        Long fileId = 1L;
        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(fileId);

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(fileEntity));
        when(fileChunkRepository.findByFileIdOrderByPosition(fileId)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> {
            service.reconstructFile(fileId);
        });
    }
}
