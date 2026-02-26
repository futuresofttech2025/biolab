package com.biolab.auth.repository;

import com.biolab.auth.entity.PasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/** Repository for {@code sec_schema.password_history} — no-reuse enforcement. */
@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, UUID> {
    List<PasswordHistory> findTop5ByUserIdOrderByCreatedAtDesc(UUID userId);
    List<PasswordHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
