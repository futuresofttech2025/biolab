package com.biolab.project.service;

import com.biolab.project.dto.*;
import com.biolab.project.entity.*;
import com.biolab.project.repository.*;
import com.biolab.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Unit Tests")
class ProjectServiceTest {

    @InjectMocks private ProjectService service;
    @Mock private ProjectRepository projectRepo;
    @Mock private ProjectMemberRepository memberRepo;
    @Mock private ProjectMilestoneRepository milestoneRepo;

    private Project project;
    private final UUID projId = UUID.randomUUID(), buyerOrgId = UUID.randomUUID(), suppOrgId = UUID.randomUUID();

    @BeforeEach void setUp() {
        project = new Project(); project.setId(projId); project.setTitle("Enzyme Kinetics Study");
        project.setBuyerOrgId(buyerOrgId); project.setSupplierOrgId(suppOrgId);
        project.setStatus("PENDING"); project.setProgressPct(0);
        project.setBudget(BigDecimal.valueOf(5000)); project.setCreatedAt(Instant.now());
    }

    // ── Projects CRUD ──

    @Test @DisplayName("[TC-PRJ-001] ✅ List all projects paginated") void listAll_Ok() {
        when(projectRepo.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(project)));
        when(milestoneRepo.findByProjectIdOrderBySortOrder(projId)).thenReturn(List.of());
        assertThat(service.listAll(PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }

    @Test @DisplayName("[TC-PRJ-002] ✅ List projects by buyer org") void listByBuyer_Ok() {
        when(projectRepo.findByBuyerOrgId(eq(buyerOrgId), any())).thenReturn(new PageImpl<>(List.of(project)));
        when(milestoneRepo.findByProjectIdOrderBySortOrder(any())).thenReturn(List.of());
        assertThat(service.listByBuyer(buyerOrgId, PageRequest.of(0, 10)).getContent()).hasSize(1);
    }

    @Test @DisplayName("[TC-PRJ-003] ✅ List projects by supplier org") void listBySupplier_Ok() {
        when(projectRepo.findBySupplierOrgId(eq(suppOrgId), any())).thenReturn(new PageImpl<>(List.of(project)));
        when(milestoneRepo.findByProjectIdOrderBySortOrder(any())).thenReturn(List.of());
        assertThat(service.listBySupplier(suppOrgId, PageRequest.of(0, 10)).getContent()).hasSize(1);
    }

    @Test @DisplayName("[TC-PRJ-004] ✅ Get project by ID") void get_Ok() {
        when(projectRepo.findById(projId)).thenReturn(Optional.of(project));
        when(milestoneRepo.findByProjectIdOrderBySortOrder(projId)).thenReturn(List.of());
        ProjectDto dto = service.get(projId);
        assertThat(dto.title()).isEqualTo("Enzyme Kinetics Study");
        assertThat(dto.status()).isEqualTo("PENDING");
    }

    @Test @DisplayName("[TC-PRJ-005] ❌ Get project not found") void get_404() {
        when(projectRepo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("[TC-PRJ-006] ✅ Create project") void create_Ok() {
        CreateProjectRequest req = new CreateProjectRequest("New Project", buyerOrgId, suppOrgId, null,
                BigDecimal.valueOf(10000), LocalDate.now(), LocalDate.now().plusDays(30));
        when(projectRepo.save(any())).thenReturn(project);
        when(milestoneRepo.findByProjectIdOrderBySortOrder(any())).thenReturn(List.of());
        assertThat(service.create(req).title()).isEqualTo("Enzyme Kinetics Study");
    }

    @Test @DisplayName("[TC-PRJ-007] ✅ Update project status to COMPLETED") void update_Complete() {
        UpdateProjectRequest req = new UpdateProjectRequest(null, "COMPLETED", null, null, null);
        when(projectRepo.findById(projId)).thenReturn(Optional.of(project));
        when(projectRepo.save(any())).thenReturn(project);
        when(milestoneRepo.findByProjectIdOrderBySortOrder(any())).thenReturn(List.of());

        service.update(projId, req);

        ArgumentCaptor<Project> cap = ArgumentCaptor.forClass(Project.class);
        verify(projectRepo).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo("COMPLETED");
        assertThat(cap.getValue().getProgressPct()).isEqualTo(100);
        assertThat(cap.getValue().getCompletedAt()).isNotNull();
    }

    @Test @DisplayName("[TC-PRJ-008] ✅ Update project partial fields") void update_Partial() {
        UpdateProjectRequest req = new UpdateProjectRequest("Updated Title", null, 50, null, null);
        when(projectRepo.findById(projId)).thenReturn(Optional.of(project));
        when(projectRepo.save(any())).thenReturn(project);
        when(milestoneRepo.findByProjectIdOrderBySortOrder(any())).thenReturn(List.of());

        service.update(projId, req);
        ArgumentCaptor<Project> cap = ArgumentCaptor.forClass(Project.class);
        verify(projectRepo).save(cap.capture());
        assertThat(cap.getValue().getTitle()).isEqualTo("Updated Title");
        assertThat(cap.getValue().getProgressPct()).isEqualTo(50);
    }

    @Test @DisplayName("[TC-PRJ-009] ❌ Update non-existent project") void update_404() {
        when(projectRepo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(UUID.randomUUID(), new UpdateProjectRequest(null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Milestones ──

    @Test @DisplayName("[TC-PRJ-010] ✅ Add milestone") void addMilestone_Ok() {
        CreateMilestoneRequest req = new CreateMilestoneRequest("Sample Collection", "Desc", LocalDate.now().plusDays(7), 1);
        ProjectMilestone ms = new ProjectMilestone(); ms.setId(UUID.randomUUID()); ms.setProjectId(projId);
        ms.setTitle("Sample Collection"); ms.setIsCompleted(false); ms.setSortOrder(1);
        when(milestoneRepo.save(any())).thenReturn(ms);

        MilestoneDto dto = service.addMilestone(projId, req);
        assertThat(dto.title()).isEqualTo("Sample Collection");
        assertThat(dto.isCompleted()).isFalse();
    }

    @Test @DisplayName("[TC-PRJ-011] ✅ Add milestone defaults sortOrder to 0") void addMilestone_DefaultSort() {
        CreateMilestoneRequest req = new CreateMilestoneRequest("MS", null, null, null);
        ProjectMilestone ms = new ProjectMilestone(); ms.setId(UUID.randomUUID()); ms.setProjectId(projId);
        ms.setTitle("MS"); ms.setIsCompleted(false); ms.setSortOrder(0);
        ArgumentCaptor<ProjectMilestone> cap = ArgumentCaptor.forClass(ProjectMilestone.class);
        when(milestoneRepo.save(cap.capture())).thenReturn(ms);
        service.addMilestone(projId, req);
        assertThat(cap.getValue().getSortOrder()).isEqualTo(0);
    }

    @Test @DisplayName("[TC-PRJ-012] ✅ Complete milestone") void completeMilestone_Ok() {
        ProjectMilestone ms = new ProjectMilestone(); ms.setId(UUID.randomUUID()); ms.setTitle("MS"); ms.setIsCompleted(false);
        when(milestoneRepo.findById(ms.getId())).thenReturn(Optional.of(ms));
        service.completeMilestone(ms.getId());
        assertThat(ms.getIsCompleted()).isTrue();
        assertThat(ms.getCompletedAt()).isNotNull();
    }

    @Test @DisplayName("[TC-PRJ-013] ❌ Complete non-existent milestone") void completeMilestone_404() {
        when(milestoneRepo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.completeMilestone(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("[TC-PRJ-014] ✅ List milestones for project") void listMilestones_Ok() {
        ProjectMilestone ms = new ProjectMilestone(); ms.setId(UUID.randomUUID()); ms.setTitle("MS"); ms.setIsCompleted(false); ms.setSortOrder(0);
        when(milestoneRepo.findByProjectIdOrderBySortOrder(projId)).thenReturn(List.of(ms));
        assertThat(service.listMilestones(projId)).hasSize(1);
    }

    @Test @DisplayName("[TC-PRJ-015] ✅ List milestones empty for project") void listMilestones_Empty() {
        when(milestoneRepo.findByProjectIdOrderBySortOrder(any())).thenReturn(List.of());
        assertThat(service.listMilestones(projId)).isEmpty();
    }

    // ── Stats ──

    @Test @DisplayName("[TC-PRJ-016] ✅ Stats for SUPPLIER role") void stats_Supplier() {
        when(projectRepo.countBySupplierOrgIdAndStatus(suppOrgId, "IN_PROGRESS")).thenReturn(3L);
        when(projectRepo.countBySupplierOrgIdAndStatus(suppOrgId, "COMPLETED")).thenReturn(7L);
        when(projectRepo.countBySupplierOrgIdAndStatus(suppOrgId, "OVERDUE")).thenReturn(1L);
        Map<String, Object> s = service.stats(suppOrgId, "SUPPLIER");
        assertThat(s).containsEntry("active", 3L).containsEntry("completed", 7L).containsEntry("overdue", 1L);
    }

    @Test @DisplayName("[TC-PRJ-017] ✅ Stats for BUYER role") void stats_Buyer() {
        when(projectRepo.countByBuyerOrgIdAndStatus(buyerOrgId, "IN_PROGRESS")).thenReturn(2L);
        when(projectRepo.countByBuyerOrgIdAndStatus(buyerOrgId, "COMPLETED")).thenReturn(5L);
        Map<String, Object> s = service.stats(buyerOrgId, "BUYER");
        assertThat(s).containsEntry("active", 2L).containsEntry("completed", 5L);
    }

    @Test @DisplayName("[TC-PRJ-018] ✅ Stats for ADMIN role") void stats_Admin() {
        when(projectRepo.count()).thenReturn(50L);
        when(projectRepo.countByStatus("IN_PROGRESS")).thenReturn(20L);
        Map<String, Object> s = service.stats(null, "ADMIN");
        assertThat(s).containsEntry("total", 50L).containsEntry("active", 20L);
    }
}
