package com.biolab.project.repository;

import com.biolab.project.entity.Project;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.*;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Page<Project> findByBuyerOrgId(UUID buyerOrgId, Pageable pageable);
    Page<Project> findBySupplierOrgId(UUID supplierOrgId, Pageable pageable);
    Page<Project> findByStatus(String status, Pageable pageable);
    @Query("SELECT p FROM Project p WHERE p.buyerOrgId = :orgId OR p.supplierOrgId = :orgId")
    Page<Project> findByOrg(UUID orgId, Pageable pageable);
    long countBySupplierOrgIdAndStatus(UUID orgId, String status);
    long countByBuyerOrgIdAndStatus(UUID orgId, String status);
    long countByStatus(String status);
}
