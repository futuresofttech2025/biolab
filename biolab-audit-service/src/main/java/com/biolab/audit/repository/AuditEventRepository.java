package com.biolab.audit.repository;
import com.biolab.audit.entity.AuditEvent;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    Page<AuditEvent> findByOrderByCreatedAtDesc(Pageable pageable);
    Page<AuditEvent> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Page<AuditEvent> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
    Page<AuditEvent> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);
}
