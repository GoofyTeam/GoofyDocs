package com.goofy.GoofyDocs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goofy.GoofyDocs.model.FileChunkEntity;

public interface FileChunkRepository extends JpaRepository<FileChunkEntity, Long> {
}