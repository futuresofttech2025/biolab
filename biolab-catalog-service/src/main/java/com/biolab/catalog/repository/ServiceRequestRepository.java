package com.biolab.catalog.repository;

import com.biolab.catalog.entity.ServiceRequest;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {
    Page<ServiceRequest> findByBuyerId(UUID buyerId, Pageable pageable);
    Page<ServiceRequest> findByServiceSupplierOrgId(UUID supplierOrgId, Pageable pageable);
    Page<ServiceRequest> findByStatus(String status, Pageable pageable);
    long countByServiceSupplierOrgIdAndStatus(UUID supplierOrgId, String status);
}
