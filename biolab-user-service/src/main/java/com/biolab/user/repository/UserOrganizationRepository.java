package com.biolab.user.repository;

import com.biolab.user.entity.UserOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@code app_schema.user_organizations}.
 *
 * @author BioLab Engineering Team
 */
@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, UUID> {
    List<UserOrganization> findByUserId(UUID userId);
    List<UserOrganization> findByOrganizationId(UUID orgId);
    Optional<UserOrganization> findByUserIdAndOrganizationId(UUID userId, UUID orgId);
    boolean existsByUserIdAndOrganizationId(UUID userId, UUID orgId);
    Optional<UserOrganization> findByUserIdAndIsPrimaryTrue(UUID userId);
    void deleteByUserIdAndOrganizationId(UUID userId, UUID orgId);
}
