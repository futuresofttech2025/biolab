package com.biolab.auth.controller;

import com.biolab.auth.dto.request.PermissionCreateRequest;
import com.biolab.auth.dto.request.PermissionUpdateRequest;
import com.biolab.auth.dto.response.PermissionResponse;
import com.biolab.auth.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for permission management — full CRUD operations.
 *
 * <h3>Base Path:</h3> {@code /api/permissions}
 *
 * <h3>Endpoints:</h3>
 * <pre>
 *   POST   /api/permissions                — Create permission
 *   GET    /api/permissions                — List all permissions
 *   GET    /api/permissions/{id}           — Get by ID
 *   GET    /api/permissions/module/{module} — Get by module
 *   PUT    /api/permissions/{id}           — Update permission
 *   DELETE /api/permissions/{id}           — Delete permission
 * </pre>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Permissions", description = "Granular permission management — CRUD for action-level permissions")
public class PermissionController {

    private final PermissionService permissionService;

    /** Creates a new permission definition. */
    @PostMapping
    @Operation(summary = "Create permission", description = "Creates a new granular permission definition")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Permission created"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "409", description = "Permission name exists")
    })
    public ResponseEntity<PermissionResponse> create(@Valid @RequestBody PermissionCreateRequest request) {
        log.info("Creating permission: {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(permissionService.create(request));
    }

    /** Lists all permissions. */
    @GetMapping
    @Operation(summary = "List all permissions")
    @ApiResponse(responseCode = "200", description = "Permissions retrieved")
    public ResponseEntity<List<PermissionResponse>> getAll() {
        return ResponseEntity.ok(permissionService.getAll());
    }

    /** Retrieves a permission by UUID. */
    @GetMapping("/{id}")
    @Operation(summary = "Get permission by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Permission found"),
        @ApiResponse(responseCode = "404", description = "Permission not found")
    })
    public ResponseEntity<PermissionResponse> getById(
            @Parameter(description = "Permission UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(permissionService.getById(id));
    }

    /** Lists permissions filtered by module (USER, SERVICE, PROJECT, etc.). */
    @GetMapping("/module/{module}")
    @Operation(summary = "Get by module", description = "Filter by module: USER, SERVICE, PROJECT, DOCUMENT, etc.")
    @ApiResponse(responseCode = "200", description = "Permissions retrieved")
    public ResponseEntity<List<PermissionResponse>> getByModule(
            @Parameter(description = "Module name") @PathVariable String module) {
        return ResponseEntity.ok(permissionService.getByModule(module));
    }

    /** Updates permission description/module. */
    @PutMapping("/{id}")
    @Operation(summary = "Update permission")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Permission updated"),
        @ApiResponse(responseCode = "404", description = "Permission not found")
    })
    public ResponseEntity<PermissionResponse> update(
            @Parameter(description = "Permission UUID") @PathVariable UUID id,
            @Valid @RequestBody PermissionUpdateRequest request) {
        log.info("Updating permission: {}", id);
        return ResponseEntity.ok(permissionService.update(id, request));
    }

    /** Deletes a permission. */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete permission")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Permission deleted"),
        @ApiResponse(responseCode = "404", description = "Permission not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Permission UUID") @PathVariable UUID id) {
        log.info("Deleting permission: {}", id);
        permissionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
