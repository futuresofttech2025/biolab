package com.biolab.user.service;

import com.biolab.user.dto.request.OrganizationCreateRequest;
import com.biolab.user.dto.request.OrganizationUpdateRequest;
import com.biolab.user.dto.response.OrganizationResponse;
import com.biolab.user.entity.Organization;
import com.biolab.user.entity.enums.OrganizationType;
import com.biolab.user.exception.ResourceNotFoundException;
import com.biolab.user.repository.OrganizationRepository;
import com.biolab.user.repository.UserOrganizationRepository;
import com.biolab.user.service.impl.OrganizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrganizationServiceImpl}.
 *
 * @author BioLab Engineering Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationService Tests")
class OrganizationServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private UserOrganizationRepository userOrgRepository;
    @InjectMocks private OrganizationServiceImpl organizationService;

    private Organization sampleOrg;

    @BeforeEach
    void setUp() {
        sampleOrg = Organization.builder()
                .name("BioTech Labs Inc")
                .type(OrganizationType.SUPPLIER)
                .address("123 Science Dr")
                .phone("+1-555-1234")
                .website("https://biotech.example.com")
                .build();
        // Simulate persisted entity
        sampleOrg.setId(UUID.randomUUID());
        sampleOrg.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should create organization successfully")
    void createOrganization_Success() {
        // Given
        OrganizationCreateRequest request = OrganizationCreateRequest.builder()
                .name("BioTech Labs Inc")
                .type("SUPPLIER")
                .address("123 Science Dr")
                .build();

        when(organizationRepository.save(any(Organization.class))).thenReturn(sampleOrg);

        // When
        OrganizationResponse response = organizationService.create(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("BioTech Labs Inc");
        assertThat(response.getType()).isEqualTo("SUPPLIER");
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    @DisplayName("Should get organization by ID with member count")
    void getById_Success() {
        when(organizationRepository.findById(sampleOrg.getId())).thenReturn(Optional.of(sampleOrg));
        when(userOrgRepository.findByOrganizationId(sampleOrg.getId())).thenReturn(Collections.emptyList());

        OrganizationResponse response = organizationService.getById(sampleOrg.getId());

        assertThat(response.getId()).isEqualTo(sampleOrg.getId());
        assertThat(response.getMemberCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent org")
    void getById_NotFound() {
        UUID id = UUID.randomUUID();
        when(organizationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Organization");
    }

    @Test
    @DisplayName("Should update organization partially")
    void updateOrganization_PartialUpdate() {
        OrganizationUpdateRequest request = OrganizationUpdateRequest.builder()
                .name("Updated Name")
                .build();

        when(organizationRepository.findById(sampleOrg.getId())).thenReturn(Optional.of(sampleOrg));
        when(organizationRepository.save(any())).thenReturn(sampleOrg);
        when(userOrgRepository.findByOrganizationId(sampleOrg.getId())).thenReturn(Collections.emptyList());

        OrganizationResponse response = organizationService.update(sampleOrg.getId(), request);

        assertThat(response).isNotNull();
        verify(organizationRepository).save(any());
    }

    @Test
    @DisplayName("Should deactivate organization (soft delete)")
    void deactivateOrganization() {
        when(organizationRepository.findById(sampleOrg.getId())).thenReturn(Optional.of(sampleOrg));
        when(organizationRepository.save(any())).thenReturn(sampleOrg);

        organizationService.deactivate(sampleOrg.getId());

        verify(organizationRepository).save(any());
    }
}
