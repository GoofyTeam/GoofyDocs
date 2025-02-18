package com.goofy.GoofyDocs.controller.api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.goofy.GoofyDocs.chunking.Chunk;
import com.goofy.GoofyDocs.chunking.ChunkingService;

@RestController
@RequestMapping("api/chunking")
public class ChunkingController {

    private final ChunkingService chunkingService;

    public ChunkingController(ChunkingService chunkingService) {
        this.chunkingService = chunkingService;
    }
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeFile(@RequestParam("file") MultipartFile file) {
        try {
            File tempFile = File.createTempFile("upload-", "-" + file.getOriginalFilename());
            file.transferTo(tempFile);

            List<Chunk> chunks = chunkingService.chunkFile(tempFile);

            Map<String, Object> stats = Map.of(
                "fileName", file.getOriginalFilename(),
                "originalSize", file.getSize(),
                "numberOfChunks", chunks.size(),
                "averageChunkSize", file.getSize() / chunks.size(),
                "uniqueChunks", chunks.stream().map(Chunk::getHash).distinct().count()
            );

            Files.delete(tempFile.toPath());

            return ResponseEntity.ok(stats);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process file: " + e.getMessage()));
        }
    }
}
