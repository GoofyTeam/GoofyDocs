package com.goofy.GoofyDocs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.goofy.GoofyDocs.model.FileChunkEntity;

@Repository
public interface FileChunkRepository extends JpaRepository<FileChunkEntity, Long> {
    List<FileChunkEntity> findByFileIdOrderByPosition(Long fileId);
}