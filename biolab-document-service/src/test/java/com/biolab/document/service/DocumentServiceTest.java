package com.biolab.document.service;

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
 * Unit tests for DocumentService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService Unit Tests")
class DocumentServiceTest {

    @InjectMocks
    private DocumentService service;


    @Mock private com.biolab.document.repository.DocumentRepository repo;

    @Test
    @DisplayName("listByProject returns documents for project")
    void listByProject_returns() {
        java.util.UUID projectId = java.util.UUID.randomUUID();
        when(repo.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(java.util.List.of());
        var result = service.listByProject(projectId);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getMetadata throws when document not found")
    void getMetadata_throwsNotFound() {
        java.util.UUID id = java.util.UUID.randomUUID();
        when(repo.findById(id)).thenReturn(java.util.Optional.empty());
        assertThrows(java.util.NoSuchElementException.class, () -> service.getMetadata(id));
    }

}
