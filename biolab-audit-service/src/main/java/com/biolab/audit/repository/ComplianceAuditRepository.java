package com.biolab.audit.repository;
import com.biolab.audit.entity.ComplianceAudit;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ComplianceAuditRepository extends JpaRepository<ComplianceAudit, UUID> {
    Page<ComplianceAudit> findByOrderByAuditDateDesc(Pageable pageable);
}
