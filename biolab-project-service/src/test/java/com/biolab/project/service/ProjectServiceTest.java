package com.biolab.project.service;

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
 * Unit tests for ProjectService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Unit Tests")
class ProjectServiceTest {

    @InjectMocks
    private ProjectService service;


    @Mock private com.biolab.project.repository.ProjectRepository projectRepo;
    @Mock private com.biolab.project.repository.ProjectMilestoneRepository milestoneRepo;
    @Mock private com.biolab.project.repository.ProjectMemberRepository memberRepo;

    @Test
    @DisplayName("listAll returns paginated projects")
    void listAll_returnsPaginated() {
        when(projectRepo.findAll(any(Pageable.class))).thenReturn(Page.empty());
        Page<?> result = service.listAll(PageRequest.of(0, 10));
        assertNotNull(result);
        verify(projectRepo).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("get throws when project not found")
    void get_throwsNotFound() {
        java.util.UUID id = java.util.UUID.randomUUID();
        when(projectRepo.findById(id)).thenReturn(java.util.Optional.empty());
        assertThrows(java.util.NoSuchElementException.class, () -> service.get(id));
    }

}
