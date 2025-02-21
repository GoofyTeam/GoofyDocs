package com.goofy.GoofyDocs.controller.api;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.goofy.GoofyDocs.compression.CompressionService;
import com.goofy.GoofyDocs.duplication.DuplicationService;
import com.goofy.GoofyDocs.duplication.HashingAlgorithm;

@RestController
@RequestMapping("api/duplication")
public class DuplicationController {

    private final DuplicationService duplicationService;

    public DuplicationController(DuplicationService duplicationService) {
        this.duplicationService = duplicationService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "algorithm", defaultValue = "SHA256") HashingAlgorithm algorithm) {
        try {
            File tempFile = File.createTempFile("upload-", "-" + file.getOriginalFilename());
            file.transferTo(tempFile);

            Map<String, Object> result = duplicationService.analyzeFile(tempFile, algorithm);

            tempFile.delete();
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Échec du traitement du fichier: " + e.getMessage()));
        }
    }

    @PostMapping("/process")
    public ResponseEntity<?> processFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "algorithm", defaultValue = "SHA256") HashingAlgorithm algorithm) {
        try {
            File tempFile = File.createTempFile("upload-", "-" + file.getOriginalFilename());
            file.transferTo(tempFile);

            Map<String, Object> result = duplicationService.processAndStoreFile(
                    tempFile,
                    file.getOriginalFilename(),
                    file.getSize(),
                    algorithm);

            tempFile.delete();
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process and save the file: " + e.getMessage()));
        }
    }

    @PostMapping("/process-compressed")
    public ResponseEntity<?> processFileCompressed(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "algorithm", defaultValue = "SHA256") HashingAlgorithm algorithm,
            @RequestParam(value = "compression", defaultValue = "LZ4") CompressionService.CompressionType compression) {
        try {
            File tempFile = File.createTempFile("upload-", "-" + file.getOriginalFilename());
            file.transferTo(tempFile);

            Map<String, Object> result = duplicationService.processAndStoreFileCompressed(
                    tempFile,
                    file.getOriginalFilename(),
                    file.getSize(),
                    algorithm,
                    compression);

            tempFile.delete();
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process and store the compressed file: " + e.getMessage()));
        }
    }
}
