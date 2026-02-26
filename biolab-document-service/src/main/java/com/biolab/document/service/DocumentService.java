package com.biolab.document.service;

import com.biolab.document.dto.DocumentDto;
import com.biolab.document.entity.Document;
import com.biolab.document.repository.DocumentRepository;
import com.biolab.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository repo;
    @Value("${app.storage.local-path:/tmp/biolab-files}") private String storagePath;

    public DocumentService(DocumentRepository r) { this.repo = r; }

    @Transactional(readOnly = true)
    public List<DocumentDto> listByProject(UUID projectId) {
        log.debug("Listing documents for project={}", projectId);
        return repo.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public DocumentDto upload(UUID projectId, UUID uploadedBy, MultipartFile file) throws Exception {
        Path dir = Paths.get(storagePath, projectId.toString());
        Files.createDirectories(dir);
        String key = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path target = dir.resolve(key);
        file.transferTo(target);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(Files.readAllBytes(target));
        String checksum = Base64.getEncoder().encodeToString(hash);

        Document doc = new Document();
        doc.setProjectId(projectId);
        doc.setUploadedBy(uploadedBy);
        doc.setFileName(file.getOriginalFilename());
        doc.setFileType(extractFileType(file.getOriginalFilename()));
        doc.setFileSize(file.getSize());
        doc.setStorageKey(key);
        doc.setMimeType(file.getContentType());
        doc.setChecksum(checksum);
        Document saved = repo.save(doc);
        log.info("Document uploaded: project={}, file='{}', size={} bytes, type={}", projectId, file.getOriginalFilename(), file.getSize(), file.getContentType());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public byte[] download(UUID docId) throws IOException {
        Document doc = repo.findById(docId)
            .orElseThrow(() -> new ResourceNotFoundException("Document", "id", docId));
        Path file = Paths.get(storagePath, doc.getProjectId().toString(), doc.getStorageKey());
        log.debug("Document download: id={}, file='{}'", docId, doc.getFileName());
        return Files.readAllBytes(file);
    }

    @Transactional(readOnly = true)
    public DocumentDto getMetadata(UUID docId) {
        log.debug("Fetching document metadata: id={}", docId);
        return toDto(repo.findById(docId)
            .orElseThrow(() -> new ResourceNotFoundException("Document", "id", docId)));
    }

    private String extractFileType(String name) {
        if (name == null) return "UNKNOWN";
        int idx = name.lastIndexOf('.');
        return idx >= 0 ? name.substring(idx + 1).toUpperCase() : "UNKNOWN";
    }

    private DocumentDto toDto(Document d) {
        return new DocumentDto(d.getId(), d.getProjectId(), d.getUploadedBy(), d.getFileName(),
            d.getFileType(), d.getFileSize(), d.getMimeType(), d.getVersion(), d.getCreatedAt());
    }
}
