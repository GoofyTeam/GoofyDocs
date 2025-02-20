package com.goofy.GoofyDocs.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "files")
public class FileEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String extension;
  private Long size;

  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now();

  @OneToMany(mappedBy = "file", cascade = CascadeType.ALL)
  private List<FileChunkEntity> fileChunks = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public List<FileChunkEntity> getFileChunks() {
    return fileChunks;
  }

  public void addFileChunk(FileChunkEntity fileChunk) {
    this.fileChunks.add(fileChunk);
    fileChunk.setFile(this);
  }
}