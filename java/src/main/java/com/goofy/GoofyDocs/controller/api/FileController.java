package com.goofy.GoofyDocs.controller.api;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goofy.GoofyDocs.model.FileEntity;
import com.goofy.GoofyDocs.repository.FileRepository;
import com.goofy.GoofyDocs.service.FileReconstructionService;

@RestController
@RequestMapping("api/files")
public class FileController {

    private final FileReconstructionService fileReconstructionService;
    private final FileRepository fileRepository;

    @Autowired
    public FileController(FileReconstructionService fileReconstructionService, FileRepository fileRepository) {
        this.fileReconstructionService = fileReconstructionService;
        this.fileRepository = fileRepository;
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<?> downloadFile(@PathVariable Long fileId) {
        try {
            FileEntity fileEntity = fileRepository.findById(fileId)
                    .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

            byte[] fileContent = fileReconstructionService.reconstructFile(fileId);

            String fileName = fileEntity.getName();
            if (fileEntity.getExtension() != null && !fileEntity.getExtension().isEmpty()) {
                fileName += "." + fileEntity.getExtension();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(new ByteArrayResource(fileContent));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Error during file reconstruction: " + e.getMessage());
        }
    }
}
