package com.biolab.auth.repository;

import com.biolab.auth.entity.JwtTokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Repository for {@code sec_schema.jwt_token_blacklist} — revoked access tokens. */
@Repository
public interface JwtTokenBlacklistRepository extends JpaRepository<JwtTokenBlacklist, UUID> {
    boolean existsByJti(String jti);
    List<JwtTokenBlacklist> findByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM JwtTokenBlacklist t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(Instant now);
}
