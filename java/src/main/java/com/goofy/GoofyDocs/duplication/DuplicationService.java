package com.goofy.GoofyDocs.duplication;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.Blake3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goofy.GoofyDocs.chunking.Chunk;
import com.goofy.GoofyDocs.chunking.ChunkingService;
import com.goofy.GoofyDocs.compression.CompressionService;
import com.goofy.GoofyDocs.compression.CompressionService.CompressionType;
import com.goofy.GoofyDocs.model.ChunkEntity;
import com.goofy.GoofyDocs.model.FileChunkEntity;
import com.goofy.GoofyDocs.model.FileEntity;
import com.goofy.GoofyDocs.repository.ChunkRepository;
import com.goofy.GoofyDocs.repository.FileChunkRepository;
import com.goofy.GoofyDocs.repository.FileRepository;
import com.google.common.hash.Hashing;

@Service
public class DuplicationService {

  private static final Logger logger = LoggerFactory.getLogger(DuplicationService.class);

  private final ChunkingService chunkingService;
  private final FileRepository fileRepository;
  private final ChunkRepository chunkRepository;
  private final FileChunkRepository fileChunkRepository;
  private final CompressionService compressionService;

  @Autowired
  public DuplicationService(
      ChunkingService chunkingService,
      FileRepository fileRepository,
      ChunkRepository chunkRepository,
      FileChunkRepository fileChunkRepository,
      CompressionService compressionService) {
    this.chunkingService = chunkingService;
    this.fileRepository = fileRepository;
    this.chunkRepository = chunkRepository;
    this.fileChunkRepository = fileChunkRepository;
    this.compressionService = compressionService;
  }

  public DuplicationService(ChunkingService chunkingService) {
    this(chunkingService, null, null, null, null);
  }

  public Map<String, Object> analyzeFile(File file, HashingAlgorithm algorithm) throws IOException {
    List<Chunk> chunks = chunkingService.chunkFile(file);
    Map<String, Integer> duplicates = new HashMap<>();

    for (Chunk chunk : chunks) {
      String hash = calculateHash(chunk.getData(), algorithm);
      duplicates.merge(hash, 1, Integer::sum);
      logger.debug("Chunk at position {} with size {} bytes has hash: {}",
          chunk.getPosition(), chunk.getData().length, hash);
    }

    duplicates.entrySet().stream()
        .filter(e -> e.getValue() > 1);

    long uniqueChunks = duplicates.size();
    long totalChunks = chunks.size();
    long duplicatedChunks = duplicates.entrySet().stream()
        .filter(e -> e.getValue() > 1)
        .count();

    return Map.of(
        "fileName", file.getName(),
        "totalChunks", totalChunks,
        "uniqueChunks", uniqueChunks,
        "duplicatedChunks", duplicatedChunks,
        "algorithm", algorithm.name(),
        "duplicateDetails", duplicates.entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue)));
  }

  private String calculateHash(byte[] data, HashingAlgorithm algorithm) {
    try {
      switch (algorithm) {
        case SHA1:
          return Hashing.sha1().hashBytes(data).toString();
        case SHA256:
          return Hashing.sha256().hashBytes(data).toString();
        case BLAKE3:
          byte[] hashBytes = Blake3.hash(data);
          return Hex.encodeHexString(hashBytes);
        default:
          throw new IllegalArgumentException("Unsupported hashing algorithm: " + algorithm);
      }
    } catch (Exception e) {
      throw new RuntimeException("Error while calculating hash", e);
    }
  }

  @Transactional
  public Map<String, Object> processAndStoreFile(
      File file,
      String fileName,
      long fileSize,
      HashingAlgorithm algorithm) throws IOException {
    if (fileRepository == null || chunkRepository == null || fileChunkRepository == null) {
      throw new UnsupportedOperationException(
          "This method requires the repositories that have not been injected. " +
              "Use the constructor with all parameters for this functionality.");
    }

    String name = fileName;
    String extension = "";
    int lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex > 0) {
      name = fileName.substring(0, lastDotIndex);
      extension = fileName.substring(lastDotIndex + 1);
    }

    FileEntity fileEntity = new FileEntity();
    fileEntity.setName(name);
    fileEntity.setExtension(extension);
    fileEntity.setSize(fileSize);
    fileEntity = fileRepository.save(fileEntity);

    List<Chunk> chunks = chunkingService.chunkFile(file);

    int totalChunks = chunks.size();
    int duplicateChunks = 0;
    int uniqueChunks = 0;
    long savedStorage = 0;

    for (Chunk chunk : chunks) {
      String hash = calculateHash(chunk.getData(), algorithm);

      Optional<ChunkEntity> existingChunk;
      switch (algorithm) {
        case SHA1:
          existingChunk = chunkRepository.findByHashSha1(hash);
          break;
        case SHA256:
          existingChunk = chunkRepository.findByHashSha256(hash);
          break;
        case BLAKE3:
          existingChunk = chunkRepository.findByHashBlake3(hash);
          break;
        default:
          existingChunk = Optional.empty();
      }

      ChunkEntity chunkEntity;
      if (existingChunk.isPresent()) {
        chunkEntity = existingChunk.get();
        duplicateChunks++;
        savedStorage += chunk.getOriginalSize();
        logger.info("Duplicate chunk found: {}", hash);
      } else {
        chunkEntity = new ChunkEntity();
        chunkEntity.setData(chunk.getData());

        switch (algorithm) {
          case SHA1:
            chunkEntity.setHashSha1(hash);
            break;
          case SHA256:
            chunkEntity.setHashSha256(hash);
            break;
          case BLAKE3:
            chunkEntity.setHashBlake3(hash);
            break;
        }

        chunkEntity = chunkRepository.save(chunkEntity);
        uniqueChunks++;
      }

      FileChunkEntity fileChunk = new FileChunkEntity();
      fileChunk.setFile(fileEntity);
      fileChunk.setChunk(chunkEntity);
      fileChunk.setPosition(chunk.getPosition());
      fileChunkRepository.save(fileChunk);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("fileId", fileEntity.getId());
    result.put("fileName", fileEntity.getName());
    result.put("extension", fileEntity.getExtension());
    result.put("fileSize", fileEntity.getSize());
    result.put("algorithm", algorithm.name());
    result.put("totalChunks", totalChunks);
    result.put("uniqueChunks", uniqueChunks);
    result.put("duplicateChunks", duplicateChunks);
    result.put("savedStorage", savedStorage);
    result.put("deduplicationRatio", totalChunks > 0 ? (double) duplicateChunks / totalChunks : 0);

    logger.info("Processed file: id={}, name={}, chunks={}, uniqueChunks={}, duplicateChunks={}",
        fileEntity.getId(), fileName, totalChunks, uniqueChunks, duplicateChunks);

    return result;
  }

  @Transactional
  public Map<String, Object> processAndStoreFileCompressed(
      File file,
      String fileName,
      long fileSize,
      HashingAlgorithm algorithm,
      CompressionType compressionType) throws IOException {
    if (fileRepository == null || chunkRepository == null || fileChunkRepository == null
        || compressionService == null) {
      throw new UnsupportedOperationException(
          "This method requires the repositories and compression service that have not been injected. " +
              "Use the constructor with all parameters for this functionality.");
    }

    String name = fileName;
    String extension = "";
    int lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex > 0) {
      name = fileName.substring(0, lastDotIndex);
      extension = fileName.substring(lastDotIndex + 1);
    }

    FileEntity fileEntity = new FileEntity();
    fileEntity.setName(name);
    fileEntity.setExtension(extension);
    fileEntity.setSize(fileSize);
    fileEntity = fileRepository.save(fileEntity);

    List<Chunk> chunks = chunkingService.chunkFile(file);

    int totalChunks = chunks.size();
    int duplicateChunks = 0;
    int uniqueChunks = 0;
    long savedStorage = 0;
    long totalCompressedSize = 0;

    for (Chunk chunk : chunks) {
      String hash = calculateHash(chunk.getData(), algorithm);

      Optional<ChunkEntity> existingChunk;
      switch (algorithm) {
        case SHA1:
          existingChunk = chunkRepository.findByHashSha1(hash);
          break;
        case SHA256:
          existingChunk = chunkRepository.findByHashSha256(hash);
          break;
        case BLAKE3:
          existingChunk = chunkRepository.findByHashBlake3(hash);
          break;
        default:
          existingChunk = Optional.empty();
      }

      ChunkEntity chunkEntity;
      if (existingChunk.isPresent()) {
        chunkEntity = existingChunk.get();
        duplicateChunks++;
        savedStorage += chunk.getOriginalSize();
        logger.info("Duplicate chunk found: {}", hash);
      } else {
        byte[] compressedData = compressionService.compress(chunk.getData(), compressionType);
        totalCompressedSize += compressedData.length;

        chunkEntity = new ChunkEntity();
        chunkEntity.setData(compressedData);
        chunkEntity.setCompressionType(compressionType.name());

        switch (algorithm) {
          case SHA1:
            chunkEntity.setHashSha1(hash);
            break;
          case SHA256:
            chunkEntity.setHashSha256(hash);
            break;
          case BLAKE3:
            chunkEntity.setHashBlake3(hash);
            break;
        }

        chunkEntity = chunkRepository.save(chunkEntity);
        uniqueChunks++;
      }

      FileChunkEntity fileChunk = new FileChunkEntity();
      fileChunk.setFile(fileEntity);
      fileChunk.setChunk(chunkEntity);
      fileChunk.setPosition(chunk.getPosition());
      fileChunkRepository.save(fileChunk);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("fileId", fileEntity.getId());
    result.put("fileName", fileEntity.getName());
    result.put("extension", fileEntity.getExtension());
    result.put("fileSize", fileEntity.getSize());
    result.put("algorithm", algorithm.name());
    result.put("compressionType", compressionType.name());
    result.put("totalChunks", totalChunks);
    result.put("uniqueChunks", uniqueChunks);
    result.put("duplicateChunks", duplicateChunks);
    result.put("savedStorage", savedStorage);
    result.put("deduplicationRatio", totalChunks > 0 ? (double) duplicateChunks / totalChunks : 0);
    result.put("totalCompressedSize", totalCompressedSize);

    logger.info(
        "Processed compressed file: id={}, name={}, chunks={}, uniqueChunks={}, duplicateChunks={}, compressedSize={}",
        fileEntity.getId(), fileName, totalChunks, uniqueChunks, duplicateChunks, totalCompressedSize);

    return result;
  }
}
