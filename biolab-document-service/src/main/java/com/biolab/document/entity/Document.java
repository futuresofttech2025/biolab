package com.biolab.document.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "documents", schema = "app_schema")
public class Document {
    @Id @GeneratedValue private UUID id;
    @Column(name = "project_id", nullable = false) private UUID projectId;
    @Column(name = "uploaded_by", nullable = false) private UUID uploadedBy;
    @Column(name = "file_name", nullable = false) private String fileName;
    @Column(name = "file_type") private String fileType;
    @Column(name = "file_size") private Long fileSize = 0L;
    @Column(name = "storage_key", nullable = false) private String storageKey;
    @Column(name = "mime_type") private String mimeType;
    private Integer version = 1;
    private String checksum;
    @Column(name = "created_at") private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID id) { this.projectId = id; }
    public UUID getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(UUID id) { this.uploadedBy = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String n) { this.fileName = n; }
    public String getFileType() { return fileType; }
    public void setFileType(String t) { this.fileType = t; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long s) { this.fileSize = s; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String k) { this.storageKey = k; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String m) { this.mimeType = m; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer v) { this.version = v; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String c) { this.checksum = c; }
    public Instant getCreatedAt() { return createdAt; }
}
