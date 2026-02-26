package com.biolab.project.controller;

import com.biolab.project.dto.*;
import com.biolab.project.service.ProjectService;
import com.biolab.common.security.CurrentUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/projects")
@Tag(name = "Projects")
public class ProjectController {

    private final ProjectService projectService;
    public ProjectController(ProjectService ps) { this.projectService = ps; }

    @GetMapping
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List projects for current user's org")
    public ResponseEntity<Page<ProjectDto>> listMine(Pageable pageable) {
        var user = CurrentUserContext.get();
        return ResponseEntity.ok(projectService.listByOrg(UUID.fromString(user.get().orgId()), pageable));
    }

    @GetMapping("/all")
    @PreAuthorize("@perm.isAdmin()")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List all projects (admin)")
    public ResponseEntity<Page<ProjectDto>> listAll(Pageable pageable) {
        return ResponseEntity.ok(projectService.listAll(pageable));
    }

    @GetMapping("/{id}")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "404", description = "Not found")})
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ProjectDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.get(id));
    }

    @PostMapping
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','ADMIN','SUPER_ADMIN')")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Created"), @ApiResponse(responseCode = "400", description = "Validation error")})
    @Operation(summary = "Create a project")
    public ResponseEntity<ProjectDto> create(@Valid @RequestBody CreateProjectRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Update project status/progress")
    public ResponseEntity<ProjectDto> update(@PathVariable UUID id, @RequestBody UpdateProjectRequest req) {
        return ResponseEntity.ok(projectService.update(id, req));
    }

    @GetMapping("/{id}/milestones")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List project milestones")
    public ResponseEntity<List<MilestoneDto>> milestones(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.listMilestones(id));
    }

    @PostMapping("/{id}/milestones")
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Add a milestone")
    public ResponseEntity<MilestoneDto> addMilestone(@PathVariable UUID id, @Valid @RequestBody CreateMilestoneRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.addMilestone(id, req));
    }

    @PatchMapping("/milestones/{milestoneId}/complete")
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Mark milestone complete")
    public ResponseEntity<Void> completeMilestone(@PathVariable UUID milestoneId) {
        projectService.completeMilestone(milestoneId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Project statistics")
    public ResponseEntity<Map<String, Object>> stats() {
        var user = CurrentUserContext.get();
        String role = user.get().roles() != null && !user.get().roles().isEmpty() ? user.get().roles().get(0) : "BUYER";
        return ResponseEntity.ok(projectService.stats(UUID.fromString(user.get().orgId()), role));
    }
}
