package com.biolab.user.service.impl;

import com.biolab.user.dto.request.OrganizationCreateRequest;
import com.biolab.user.dto.request.OrganizationUpdateRequest;
import com.biolab.user.dto.response.OrganizationResponse;
import com.biolab.user.dto.response.PageResponse;
import com.biolab.user.entity.Organization;
import com.biolab.user.entity.enums.OrganizationType;
import com.biolab.user.exception.ResourceNotFoundException;
import com.biolab.user.repository.OrganizationRepository;
import com.biolab.user.repository.UserOrganizationRepository;
import com.biolab.user.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link OrganizationService} — CRUD for organizations.
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrgRepository;

    @Override
    public OrganizationResponse create(OrganizationCreateRequest request) {
        Organization org = Organization.builder()
                .name(request.getName().trim())
                .type(OrganizationType.valueOf(request.getType()))
                .address(request.getAddress())
                .phone(request.getPhone())
                .website(request.getWebsite())
                .logoUrl(request.getLogoUrl())
                .build();

        Organization saved = organizationRepository.save(org);
        log.info("Organization created: id={}, name={}, type={}", saved.getId(), saved.getName(), saved.getType());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getById(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
        return toResponseWithCount(org);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrganizationResponse> search(String search, String type, Boolean isActive, Pageable pageable) {
        OrganizationType orgType = type != null ? OrganizationType.valueOf(type) : null;
        Page<Organization> page = organizationRepository.searchOrganizations(search, orgType, isActive, pageable);

        List<OrganizationResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<OrganizationResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @Override
    public OrganizationResponse update(UUID id, OrganizationUpdateRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));

        if (request.getName() != null) org.setName(request.getName().trim());
        if (request.getAddress() != null) org.setAddress(request.getAddress());
        if (request.getPhone() != null) org.setPhone(request.getPhone());
        if (request.getWebsite() != null) org.setWebsite(request.getWebsite());
        if (request.getLogoUrl() != null) org.setLogoUrl(request.getLogoUrl());
        if (request.getIsActive() != null) org.setIsActive(request.getIsActive());
        org.setUpdatedAt(Instant.now());

        Organization saved = organizationRepository.save(org);
        log.info("Organization updated: {}", id);
        return toResponseWithCount(saved);
    }

    @Override
    public void deactivate(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
        org.setIsActive(false);
        org.setUpdatedAt(Instant.now());
        organizationRepository.save(org);
        log.info("Organization deactivated: {}", id);
    }

    private OrganizationResponse toResponse(Organization org) {
        return OrganizationResponse.builder()
                .id(org.getId()).name(org.getName()).type(org.getType().name())
                .address(org.getAddress()).phone(org.getPhone()).website(org.getWebsite())
                .logoUrl(org.getLogoUrl()).isActive(org.getIsActive())
                .createdAt(org.getCreatedAt()).updatedAt(org.getUpdatedAt())
                .build();
    }

    private OrganizationResponse toResponseWithCount(Organization org) {
        OrganizationResponse resp = toResponse(org);
        long count = userOrgRepository.findByOrganizationId(org.getId()).size();
        resp.setMemberCount(count);
        return resp;
    }
}
