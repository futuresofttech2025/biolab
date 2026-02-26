package com.biolab.audit.repository;
import com.biolab.audit.entity.PolicyDocument;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, UUID> {
    Page<PolicyDocument> findByOrderByUpdatedAtDesc(Pageable pageable);
    Page<PolicyDocument> findByStatusOrderByUpdatedAtDesc(String status, Pageable pageable);
}
