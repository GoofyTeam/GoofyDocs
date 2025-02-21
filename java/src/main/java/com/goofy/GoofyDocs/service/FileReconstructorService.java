package com.goofy.GoofyDocs.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goofy.GoofyDocs.compression.CompressionService;
import com.goofy.GoofyDocs.model.FileChunkEntity;
import com.goofy.GoofyDocs.model.FileEntity;
import com.goofy.GoofyDocs.repository.FileChunkRepository;
import com.goofy.GoofyDocs.repository.FileRepository;

@Service
public class FileReconstructorService {
    private static final Logger logger = LoggerFactory.getLogger(FileReconstructorService.class);

    private final FileRepository fileRepository;
    private final FileChunkRepository fileChunkRepository;
    private final CompressionService compressionService;

    @Autowired
    public FileReconstructorService(
            FileRepository fileRepository,
            FileChunkRepository fileChunkRepository,
            CompressionService compressionService) {
        this.fileRepository = fileRepository;
        this.fileChunkRepository = fileChunkRepository;
        this.compressionService = compressionService;
    }

    @Transactional(readOnly = true)
    public byte[] reconstructFile(Long fileId) throws IOException {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        List<FileChunkEntity> chunks = fileChunkRepository.findByFileIdOrderByPosition(fileId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No chunks found for the file: " + fileId);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (FileChunkEntity chunk : chunks) {
            byte[] chunkData = chunk.getChunk().getData();

            String compressionType = chunk.getChunk().getCompressionType();
            if (compressionType != null) {
                try {
                    int originalSize = chunk.getChunk().getOriginalSize() != null
                            ? chunk.getChunk().getOriginalSize()
                            : chunkData.length * 2;

                    chunkData = compressionService.decompress(
                            chunkData,
                            CompressionService.CompressionType.valueOf(compressionType),
                            originalSize);

                    logger.debug("Decompressed chunk at position {}: original size={}, decompressed size={}",
                            chunk.getPosition(),
                            originalSize,
                            chunkData.length);

                } catch (Exception e) {
                    logger.error("Error while decompressing chunk at position {}", chunk.getPosition(), e);
                    throw new IOException("Decompression error", e);
                }
            }

            outputStream.write(chunkData);
        }

        byte[] reconstructedFile = outputStream.toByteArray();
        logger.info("File reconstructed: id={}, name={}, size={} bytes",
                fileId, file.getName(), reconstructedFile.length);

        return reconstructedFile;
    }
}
