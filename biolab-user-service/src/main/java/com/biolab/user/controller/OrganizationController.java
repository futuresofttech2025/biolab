package com.biolab.user.controller;

import com.biolab.user.dto.request.OrganizationCreateRequest;
import com.biolab.user.dto.request.OrganizationUpdateRequest;
import com.biolab.user.dto.response.OrganizationResponse;
import com.biolab.user.dto.response.PageResponse;
import com.biolab.user.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Organization Management Controller.
 *
 * <h3>Endpoints (Slide 13 — LLD User Service):</h3>
 * <ul>
 *   <li>{@code POST /api/organizations} — Create (Admin only)</li>
 *   <li>{@code GET /api/organizations/{id}} — Get by ID (authenticated)</li>
 *   <li>{@code GET /api/organizations} — Search (admin: all, others: own org)</li>
 *   <li>{@code PUT /api/organizations/{id}} — Update (Admin only)</li>
 *   <li>{@code DELETE /api/organizations/{id}} — Soft delete (Admin only)</li>
 * </ul>
 *
 * <h3>RBAC (Slide 6):</h3>
 * <p>Organization management restricted to SUPER_ADMIN/ADMIN roles.
 * Suppliers and Buyers can view their own organization details.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organizations", description = "BUYER/SUPPLIER organization management")
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Create organization (Admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Organization created"),
        @ApiResponse(responseCode = "403", description = "Admin access required"),
        @ApiResponse(responseCode = "409", description = "Duplicate name")
    })
    public ResponseEntity<OrganizationResponse> create(
            @Valid @RequestBody OrganizationCreateRequest request) {
        log.info("POST /api/organizations — name={} type={}", request.getName(), request.getType());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Organization found"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<OrganizationResponse> getById(
            @Parameter(description = "Organization UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(organizationService.getById(id));
    }

    @GetMapping
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Search organizations (Admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<PageResponse<OrganizationResponse>> search(
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by type") @RequestParam(required = false) String type,
            @Parameter(description = "Filter by active") @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(organizationService.search(keyword, type, isActive, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Update organization (Admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated"),
        @ApiResponse(responseCode = "403", description = "Admin access required"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<OrganizationResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody OrganizationUpdateRequest request) {
        log.info("PUT /api/organizations/{}", id);
        return ResponseEntity.ok(organizationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Deactivate organization (Admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deactivated"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        log.info("DELETE /api/organizations/{} — soft delete", id);
        organizationService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
