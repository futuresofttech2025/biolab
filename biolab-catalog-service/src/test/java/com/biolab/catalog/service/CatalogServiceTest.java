package com.biolab.catalog.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CatalogService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogService Unit Tests")
class CatalogServiceTest {

    @InjectMocks
    private CatalogService service;


    @Mock private com.biolab.catalog.repository.ServiceRepository serviceRepo;
    @Mock private com.biolab.catalog.repository.ServiceCategoryRepository categoryRepo;
    @Mock private com.biolab.catalog.repository.ServiceRequestRepository requestRepo;

    @Test
    @DisplayName("listServices returns paginated results")
    void listServices_returnsPaginated() {
        when(serviceRepo.findByIsActiveTrue(any(Pageable.class)))
            .thenReturn(Page.empty());
        Page<?> result = service.listServices(null, null, PageRequest.of(0, 10));
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(serviceRepo).findByIsActiveTrue(any(Pageable.class));
    }

    @Test
    @DisplayName("getService throws when not found")
    void getService_throwsNotFound() {
        java.util.UUID id = java.util.UUID.randomUUID();
        when(serviceRepo.findById(id)).thenReturn(java.util.Optional.empty());
        assertThrows(java.util.NoSuchElementException.class, () -> service.getService(id));
    }

    @Test
    @DisplayName("listCategories returns active categories")
    void listCategories_returnsActive() {
        when(categoryRepo.findByIsActiveTrueOrderBySortOrder()).thenReturn(java.util.List.of());
        var result = service.listCategories();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

}
