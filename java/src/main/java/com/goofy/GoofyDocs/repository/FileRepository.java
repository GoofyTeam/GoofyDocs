package com.goofy.GoofyDocs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goofy.GoofyDocs.model.FileEntity;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
  Optional<FileEntity> findById(Long id);
}