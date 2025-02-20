package com.goofy.GoofyDocs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goofy.GoofyDocs.model.FileEntity;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
}