package com.biolab.auth.controller;

import com.biolab.auth.dto.request.RoleCreateRequest;
import com.biolab.auth.dto.request.RoleUpdateRequest;
import com.biolab.auth.dto.response.RoleResponse;
import com.biolab.auth.service.RoleService;
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
 * REST controller for role management — full CRUD operations.
 *
 * <h3>Base Path:</h3> {@code /api/roles}
 *
 * <h3>Endpoints:</h3>
 * <pre>
 *   POST   /api/roles           — Create a new role
 *   GET    /api/roles           — List all roles
 *   GET    /api/roles/{id}      — Get role by ID
 *   PUT    /api/roles/{id}      — Update role details
 *   DELETE /api/roles/{id}      — Delete a role
 * </pre>
 *
 * <p>System roles (SUPER_ADMIN, ADMIN, SUPPLIER, BUYER) are seeded at startup
 * and marked as {@code is_system_role = true}.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Roles", description = "RBAC role management — create, read, update, delete roles")
public class RoleController {

    private final RoleService roleService;

    /** Creates a new role definition. */
    @PostMapping
    @Operation(summary = "Create role", description = "Creates a new RBAC role definition")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Role created"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "409", description = "Role name already exists")
    })
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        log.info("Creating role: {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(request));
    }

    /** Lists all roles in the system. */
    @GetMapping
    @Operation(summary = "List all roles", description = "Returns all role definitions including system roles")
    @ApiResponse(responseCode = "200", description = "Roles retrieved")
    public ResponseEntity<List<RoleResponse>> getAll() {
        return ResponseEntity.ok(roleService.getAll());
    }

    /** Retrieves a single role by UUID. */
    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role found"),
        @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<RoleResponse> getById(
            @Parameter(description = "Role UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(roleService.getById(id));
    }

    /** Updates role display name and/or description (partial update). */
    @PutMapping("/{id}")
    @Operation(summary = "Update role", description = "Updates role display name and/or description")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role updated"),
        @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<RoleResponse> update(
            @Parameter(description = "Role UUID") @PathVariable UUID id,
            @Valid @RequestBody RoleUpdateRequest request) {
        log.info("Updating role: {}", id);
        return ResponseEntity.ok(roleService.update(id, request));
    }

    /** Deletes a role. System roles cannot be deleted. */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role", description = "Removes a role — system roles are protected")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Role deleted"),
        @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Role UUID") @PathVariable UUID id) {
        log.info("Deleting role: {}", id);
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
