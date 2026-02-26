package com.biolab.auth.repository;

import com.biolab.auth.entity.ConsentRecord;
import com.biolab.auth.entity.enums.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository for {@code sec_schema.consent_records} — GDPR/HIPAA consent. */
@Repository
public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, UUID> {
    List<ConsentRecord> findByUserId(UUID userId);
    Optional<ConsentRecord> findByUserIdAndConsentType(UUID userId, ConsentType consentType);
}
