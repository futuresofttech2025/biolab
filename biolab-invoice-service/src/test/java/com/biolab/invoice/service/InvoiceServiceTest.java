package com.biolab.invoice.service;

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
 * Unit tests for InvoiceService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceService Unit Tests")
class InvoiceServiceTest {

    @InjectMocks
    private InvoiceService service;


    @Mock private com.biolab.invoice.repository.InvoiceRepository repo;

    @Test
    @DisplayName("listAll returns paginated invoices")
    void listAll_returnsPaginated() {
        when(repo.findAll(any(Pageable.class))).thenReturn(Page.empty());
        Page<?> result = service.listAll(PageRequest.of(0, 10));
        assertNotNull(result);
        verify(repo).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("get throws when invoice not found")
    void get_throwsNotFound() {
        java.util.UUID id = java.util.UUID.randomUUID();
        when(repo.findById(id)).thenReturn(java.util.Optional.empty());
        assertThrows(java.util.NoSuchElementException.class, () -> service.get(id));
    }

}
