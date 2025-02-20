package com.goofy.GoofyDocs.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "file_chunk")
public class FileChunkEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "file_id")
  private FileEntity file;

  @ManyToOne
  @JoinColumn(name = "chunk_id")
  private ChunkEntity chunk;

  private Integer position;

  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public FileEntity getFile() {
    return file;
  }

  public void setFile(FileEntity file) {
    this.file = file;
  }

  public ChunkEntity getChunk() {
    return chunk;
  }

  public void setChunk(ChunkEntity chunk) {
    this.chunk = chunk;
  }

  public Integer getPosition() {
    return position;
  }

  public void setPosition(Integer position) {
    this.position = position;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}