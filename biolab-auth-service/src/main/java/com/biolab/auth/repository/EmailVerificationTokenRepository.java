package com.biolab.auth.repository;

import com.biolab.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Repository for {@code sec_schema.email_verification_tokens}. */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    /** Invalidate any previous unused tokens for this user before issuing a new one. */
    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.usedAt = CURRENT_TIMESTAMP " +
            "WHERE t.user.id = :userId AND t.usedAt IS NULL")
    void revokeAllByUserId(UUID userId);
}