package com.biolab.document.repository;

import com.biolab.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    long countByProjectId(UUID projectId);
}
