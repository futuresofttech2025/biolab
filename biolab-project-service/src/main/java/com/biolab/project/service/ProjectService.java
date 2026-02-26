package com.biolab.project.service;

import com.biolab.project.dto.*;
import com.biolab.project.entity.*;
import com.biolab.project.repository.*;
import com.biolab.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepo;
    private final ProjectMemberRepository memberRepo;
    private final ProjectMilestoneRepository milestoneRepo;

    public ProjectService(ProjectRepository pr, ProjectMemberRepository mr, ProjectMilestoneRepository mlr) {
        this.projectRepo = pr; this.memberRepo = mr; this.milestoneRepo = mlr;
    }

    @Transactional(readOnly = true)
    public Page<ProjectDto> listByOrg(UUID orgId, Pageable pageable) {
        log.debug("Listing projects for org={}, page={}", orgId, pageable.getPageNumber());
        return projectRepo.findByOrg(orgId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ProjectDto> listByBuyer(UUID buyerOrgId, Pageable pageable) {
        log.debug("Listing projects for buyer org={}", buyerOrgId);
        return projectRepo.findByBuyerOrgId(buyerOrgId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ProjectDto> listBySupplier(UUID supplierOrgId, Pageable pageable) {
        log.debug("Listing projects for supplier org={}", supplierOrgId);
        return projectRepo.findBySupplierOrgId(supplierOrgId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ProjectDto> listAll(Pageable pageable) {
        log.debug("Listing all projects, page={}", pageable.getPageNumber());
        return projectRepo.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ProjectDto get(UUID id) {
        log.debug("Fetching project id={}", id);
        return toDto(projectRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id)));
    }

    public ProjectDto create(CreateProjectRequest req) {
        Project p = new Project();
        p.setTitle(req.title()); p.setBuyerOrgId(req.buyerOrgId());
        p.setSupplierOrgId(req.supplierOrgId()); p.setServiceRequestId(req.serviceRequestId());
        p.setBudget(req.budget()); p.setStartDate(req.startDate()); p.setDeadline(req.deadline());
        Project saved = projectRepo.save(p);
        log.info("Project created: id={}, title='{}', buyer={}, supplier={}", saved.getId(), saved.getTitle(), req.buyerOrgId(), req.supplierOrgId());
        return toDto(saved);
    }

    public ProjectDto update(UUID id, UpdateProjectRequest req) {
        Project p = projectRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        if (req.title() != null) p.setTitle(req.title());
        if (req.status() != null) {
            String oldStatus = p.getStatus();
            p.setStatus(req.status());
            if ("COMPLETED".equals(req.status())) { p.setCompletedAt(Instant.now()); p.setProgressPct(100); }
            log.info("Project status changed: id={}, {} → {}", id, oldStatus, req.status());
        }
        if (req.progressPct() != null) p.setProgressPct(req.progressPct());
        if (req.budget() != null) p.setBudget(req.budget());
        if (req.deadline() != null) p.setDeadline(req.deadline());
        return toDto(projectRepo.save(p));
    }

    @Transactional(readOnly = true)
    public List<MilestoneDto> listMilestones(UUID projectId) {
        log.debug("Listing milestones for project={}", projectId);
        return milestoneRepo.findByProjectIdOrderBySortOrder(projectId).stream()
            .map(m -> new MilestoneDto(m.getId(), m.getTitle(), m.getDescription(),
                m.getMilestoneDate(), m.getIsCompleted(), m.getSortOrder()))
            .collect(Collectors.toList());
    }

    public MilestoneDto addMilestone(UUID projectId, CreateMilestoneRequest req) {
        ProjectMilestone m = new ProjectMilestone();
        m.setProjectId(projectId); m.setTitle(req.title()); m.setDescription(req.description());
        m.setMilestoneDate(req.milestoneDate()); m.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
        m = milestoneRepo.save(m);
        log.info("Milestone added: project={}, title='{}'", projectId, req.title());
        return new MilestoneDto(m.getId(), m.getTitle(), m.getDescription(),
            m.getMilestoneDate(), m.getIsCompleted(), m.getSortOrder());
    }

    public void completeMilestone(UUID milestoneId) {
        ProjectMilestone m = milestoneRepo.findById(milestoneId)
            .orElseThrow(() -> new ResourceNotFoundException("Milestone", "id", milestoneId));
        m.setIsCompleted(true); m.setCompletedAt(Instant.now());
        milestoneRepo.save(m);
        log.info("Milestone completed: id={}, title='{}'", milestoneId, m.getTitle());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats(UUID orgId, String role) {
        log.debug("Fetching project stats for org={}, role={}", orgId, role);
        Map<String, Object> s = new HashMap<>();
        if ("SUPPLIER".equals(role)) {
            s.put("active", projectRepo.countBySupplierOrgIdAndStatus(orgId, "IN_PROGRESS"));
            s.put("completed", projectRepo.countBySupplierOrgIdAndStatus(orgId, "COMPLETED"));
            s.put("overdue", projectRepo.countBySupplierOrgIdAndStatus(orgId, "OVERDUE"));
        } else if ("BUYER".equals(role)) {
            s.put("active", projectRepo.countByBuyerOrgIdAndStatus(orgId, "IN_PROGRESS"));
            s.put("completed", projectRepo.countByBuyerOrgIdAndStatus(orgId, "COMPLETED"));
        } else {
            s.put("total", projectRepo.count());
            s.put("active", projectRepo.countByStatus("IN_PROGRESS"));
        }
        return s;
    }

    private ProjectDto toDto(Project p) {
        List<MilestoneDto> milestones = milestoneRepo.findByProjectIdOrderBySortOrder(p.getId()).stream()
            .map(m -> new MilestoneDto(m.getId(), m.getTitle(), m.getDescription(),
                m.getMilestoneDate(), m.getIsCompleted(), m.getSortOrder()))
            .collect(Collectors.toList());
        return new ProjectDto(p.getId(), p.getTitle(), p.getBuyerOrgId(), p.getSupplierOrgId(),
            p.getStatus(), p.getProgressPct(), p.getBudget(), p.getStartDate(), p.getDeadline(),
            p.getCreatedAt(), milestones);
    }
}
