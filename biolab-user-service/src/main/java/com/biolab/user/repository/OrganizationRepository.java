package com.biolab.user.repository;

import com.biolab.user.entity.Organization;
import com.biolab.user.entity.enums.OrganizationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.UUID;

/**
 * Repository for {@code app_schema.organizations}.
 *
 * @author BioLab Engineering Team
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Page<Organization> findByType(OrganizationType type, Pageable pageable);
    Page<Organization> findByIsActiveTrue(Pageable pageable);

    /** Search orgs by name and optional type filter. */
    @Query("SELECT o FROM Organization o WHERE " +
           "(:search IS NULL OR LOWER(o.name) LIKE LOWER(CONCAT('%',:search,'%'))) " +
           "AND (:type IS NULL OR o.type = :type) " +
           "AND (:isActive IS NULL OR o.isActive = :isActive)")
    Page<Organization> searchOrganizations(String search, OrganizationType type, Boolean isActive, Pageable pageable);
}
