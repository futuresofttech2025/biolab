package com.biolab.catalog.repository;

import com.biolab.catalog.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID> {
    List<ServiceCategory> findByIsActiveTrueOrderBySortOrder();
    Optional<ServiceCategory> findBySlug(String slug);
}
