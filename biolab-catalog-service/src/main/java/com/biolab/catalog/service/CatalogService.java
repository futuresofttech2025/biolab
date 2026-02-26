package com.biolab.catalog.service;

import com.biolab.catalog.dto.*;
import com.biolab.catalog.entity.*;
import com.biolab.catalog.repository.*;
import com.biolab.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final ServiceRepository serviceRepo;
    private final ServiceCategoryRepository categoryRepo;
    private final ServiceRequestRepository requestRepo;

    public CatalogService(ServiceRepository sr, ServiceCategoryRepository cr, ServiceRequestRepository rr) {
        this.serviceRepo = sr; this.categoryRepo = cr; this.requestRepo = rr;
    }

    // ── Categories ──
    @Transactional(readOnly = true)
    public List<CategoryDto> listCategories() {
        log.debug("Listing all active categories");
        return categoryRepo.findByIsActiveTrueOrderBySortOrder().stream()
            .map(c -> new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getIcon(), c.getIsActive()))
            .collect(Collectors.toList());
    }

    // ── Services ──
    @Transactional(readOnly = true)
    public Page<ServiceDto> listServices(UUID categoryId, String query, Pageable pageable) {
        log.debug("Listing services — categoryId={}, query='{}', page={}", categoryId, query, pageable.getPageNumber());
        Page<com.biolab.catalog.entity.Service> page;
        if (query != null && !query.isBlank()) page = serviceRepo.search(query, pageable);
        else if (categoryId != null) page = serviceRepo.findByCategoryIdAndIsActiveTrue(categoryId, pageable);
        else page = serviceRepo.findByIsActiveTrue(pageable);
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ServiceDto> listBySupplier(UUID supplierOrgId, Pageable pageable) {
        log.debug("Listing services for supplier org={}", supplierOrgId);
        return serviceRepo.findBySupplierOrgId(supplierOrgId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ServiceDto getService(UUID id) {
        return toDto(serviceRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id)));
    }

    public ServiceDto createService(CreateServiceRequest req, UUID supplierOrgId) {
        ServiceCategory cat = categoryRepo.findById(req.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", req.categoryId()));
        com.biolab.catalog.entity.Service s = new com.biolab.catalog.entity.Service();
        s.setName(req.name());
        s.setSlug(req.name().toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        s.setCategory(cat);
        s.setSupplierOrgId(supplierOrgId);
        s.setDescription(req.description());
        s.setMethodology(req.methodology());
        s.setPriceFrom(req.priceFrom());
        s.setTurnaround(req.turnaround());
        log.info("Service created: name='{}', supplier={}, category='{}'", s.getName(), supplierOrgId, cat.getName());
        return toDto(serviceRepo.save(s));
    }

    public ServiceDto updateService(UUID id, CreateServiceRequest req) {
        com.biolab.catalog.entity.Service s = serviceRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));
        s.setName(req.name());
        if (req.description() != null) s.setDescription(req.description());
        if (req.priceFrom() != null) s.setPriceFrom(req.priceFrom());
        if (req.turnaround() != null) s.setTurnaround(req.turnaround());
        log.info("Service updated: id={}, name='{}'", id, s.getName());
        return toDto(serviceRepo.save(s));
    }

    public void toggleService(UUID id, boolean active) {
        com.biolab.catalog.entity.Service s = serviceRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));
        s.setIsActive(active);
        serviceRepo.save(s);
        log.info("Service toggled: id={}, active={}", id, active);
    }

    // ── Service Requests ──
    @Transactional(readOnly = true)
    public Page<ServiceRequestDto> listRequests(UUID supplierOrgId, Pageable pageable) {
        log.debug("Listing service requests for supplier org={}", supplierOrgId);
        return requestRepo.findByServiceSupplierOrgId(supplierOrgId, pageable).map(this::toReqDto);
    }

    public ServiceRequestDto createRequest(CreateServiceRequestRequest req, UUID buyerId, UUID buyerOrgId) {
        com.biolab.catalog.entity.Service svc = serviceRepo.findById(req.serviceId())
            .orElseThrow(() -> new ResourceNotFoundException("Service", "id", req.serviceId()));
        ServiceRequest sr = new ServiceRequest();
        sr.setService(svc); sr.setBuyerId(buyerId); sr.setBuyerOrgId(buyerOrgId);
        sr.setSampleType(req.sampleType()); sr.setTimeline(req.timeline() != null ? req.timeline() : "STANDARD");
        sr.setRequirements(req.requirements()); sr.setPriority(req.priority() != null ? req.priority() : "MEDIUM");
        log.info("Service request created: service='{}', buyer={}", svc.getName(), buyerOrgId);
        return toReqDto(requestRepo.save(sr));
    }

    public ServiceRequestDto updateRequestStatus(UUID id, String status) {
        ServiceRequest sr = requestRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", "id", id));
        sr.setStatus(status);
        log.info("Service request status updated: id={}, status={}", id, status);
        return toReqDto(requestRepo.save(sr));
    }

    // ── Stats ──
    @Transactional(readOnly = true)
    public Map<String, Object> supplierStats(UUID supplierOrgId) {
        log.debug("Fetching supplier stats for org={}", supplierOrgId);
        return Map.of(
            "totalServices", serviceRepo.countBySupplierOrgId(supplierOrgId),
            "pendingRequests", requestRepo.countByServiceSupplierOrgIdAndStatus(supplierOrgId, "PENDING")
        );
    }

    private ServiceDto toDto(com.biolab.catalog.entity.Service s) {
        return new ServiceDto(s.getId(), s.getName(), s.getSlug(),
            s.getCategory().getName(), s.getCategory().getSlug(),
            s.getSupplierOrgId(), s.getDescription(), s.getMethodology(),
            s.getPriceFrom(), s.getTurnaround(), s.getRating(), s.getReviewCount(), s.getIsActive());
    }

    private ServiceRequestDto toReqDto(ServiceRequest r) {
        return new ServiceRequestDto(r.getId(), r.getService().getId(), r.getService().getName(),
            r.getService().getCategory().getName(), r.getBuyerId(), r.getBuyerOrgId(),
            r.getSampleType(), r.getTimeline(), r.getRequirements(), r.getPriority(),
            r.getStatus(), r.getCreatedAt());
    }
}
