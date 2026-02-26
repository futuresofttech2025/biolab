package com.biolab.catalog.repository;

import com.biolab.catalog.entity.Service;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.*;

public interface ServiceRepository extends JpaRepository<Service, UUID> {
    Page<Service> findByIsActiveTrue(Pageable pageable);
    Page<Service> findByCategoryIdAndIsActiveTrue(UUID categoryId, Pageable pageable);
    Page<Service> findBySupplierOrgId(UUID supplierOrgId, Pageable pageable);
    @Query("SELECT s FROM Service s WHERE s.isActive = true AND " +
           "(LOWER(s.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Service> search(String q, Pageable pageable);
    long countBySupplierOrgId(UUID orgId);
}
