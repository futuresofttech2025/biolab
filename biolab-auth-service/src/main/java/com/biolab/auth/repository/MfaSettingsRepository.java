package com.biolab.auth.repository;

import com.biolab.auth.entity.MfaSettings;
import com.biolab.auth.entity.enums.MfaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository for {@code sec_schema.mfa_settings} — MFA CRUD. */
@Repository
public interface MfaSettingsRepository extends JpaRepository<MfaSettings, UUID> {
    Optional<MfaSettings> findByUserIdAndMfaType(UUID userId, MfaType mfaType);
    List<MfaSettings> findByUserId(UUID userId);
    boolean existsByUserIdAndIsEnabledTrue(UUID userId);
}
