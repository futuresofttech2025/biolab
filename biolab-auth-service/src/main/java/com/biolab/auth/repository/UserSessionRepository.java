package com.biolab.auth.repository;

import com.biolab.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository for {@code sec_schema.user_sessions} — session management. */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    
    List<UserSession> findByUserIdAndIsActiveTrue(UUID userId);

    Optional<UserSession> findBySessionToken(String sessionToken);
    
    long countByUserIdAndIsActiveTrue(UUID userId);
    
    /** Count all active sessions platform-wide. */
    long countByIsActiveTrue();
    
    /** Count distinct users with active sessions. */
    @Query("SELECT COUNT(DISTINCT s.user.id) FROM UserSession s WHERE s.isActive = true")
    long countDistinctUsersByIsActiveTrue();
    
    /** Count sessions created after a certain time. */
    long countByCreatedAtAfter(Instant timestamp);

    /** Deactivate all sessions for a user and return count. */
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.user.id = :userId AND s.isActive = true")
    int deactivateAllUserSessions(UUID userId);

    List<UserSession> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId);
}
