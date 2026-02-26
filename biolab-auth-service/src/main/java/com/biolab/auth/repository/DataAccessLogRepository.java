package com.biolab.auth.repository;

import com.biolab.auth.entity.DataAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

/** Repository for {@code sec_schema.data_access_log} — PHI/PII access HIPAA audit. */
@Repository
public interface DataAccessLogRepository extends JpaRepository<DataAccessLog, UUID> {
    Page<DataAccessLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Page<DataAccessLog> findByResourceTypeAndResourceId(String resourceType, UUID resourceId, Pageable pageable);
}
