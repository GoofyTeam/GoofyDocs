package com.goofy.GoofyDocs.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "chunk")
public class ChunkEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(columnDefinition = "bytea")
  private byte[] data;

  @Column(name = "hash_sha_1")
  private String hashSha1;

  @Column(name = "hash_sha_256")
  private String hashSha256;

  @Column(name = "hash_blake3")
  private String hashBlake3;

  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now();

  @OneToMany(mappedBy = "chunk")
  private List<FileChunkEntity> fileChunks = new ArrayList<>();

  @Column(name = "compression_type")
  private String compressionType;

  @Column(name = "original_size")
  private Integer originalSize;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public String getHashSha1() {
    return hashSha1;
  }

  public void setHashSha1(String hashSha1) {
    this.hashSha1 = hashSha1;
  }

  public String getHashSha256() {
    return hashSha256;
  }

  public void setHashSha256(String hashSha256) {
    this.hashSha256 = hashSha256;
  }

  public String getHashBlake3() {
    return hashBlake3;
  }

  public void setHashBlake3(String hashBlake3) {
    this.hashBlake3 = hashBlake3;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public List<FileChunkEntity> getFileChunks() {
    return fileChunks;
  }

  public String getCompressionType() {
    return compressionType;
  }

  public void setCompressionType(String compressionType) {
    this.compressionType = compressionType;
  }

  public Integer getOriginalSize() {
    return originalSize;
  }

  public void setOriginalSize(Integer originalSize) {
    this.originalSize = originalSize;
  }
}