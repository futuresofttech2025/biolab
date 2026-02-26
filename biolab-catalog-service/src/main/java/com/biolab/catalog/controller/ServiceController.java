package com.biolab.catalog.controller;

import com.biolab.catalog.dto.*;
import com.biolab.catalog.service.CatalogService;
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
@RequestMapping("/catalog/services")
@Tag(name = "Service Catalog")
public class ServiceController {

    private final CatalogService catalogService;
    public ServiceController(CatalogService cs) { this.catalogService = cs; }

    @GetMapping
    @Operation(summary = "Browse services (public catalog)")
    public ResponseEntity<Page<ServiceDto>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(catalogService.listServices(categoryId, q, pageable));
    }

    @GetMapping("/{id}")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "404", description = "Not found")})
    @Operation(summary = "Get service by ID")
    public ResponseEntity<ServiceDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogService.getService(id));
    }

    @GetMapping("/supplier")
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','ADMIN','SUPER_ADMIN')")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List services for current supplier org")
    public ResponseEntity<Page<ServiceDto>> listMine(Pageable pageable) {
        UUID orgId = UUID.fromString(CurrentUserContext.get().get().orgId());
        return ResponseEntity.ok(catalogService.listBySupplier(orgId, pageable));
    }

    @PostMapping
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','ADMIN','SUPER_ADMIN')")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Created"), @ApiResponse(responseCode = "400", description = "Validation error")})
    @Operation(summary = "Create a new service")
    public ResponseEntity<ServiceDto> create(@Valid @RequestBody CreateServiceRequest req) {
        UUID orgId = UUID.fromString(CurrentUserContext.get().get().orgId());
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createService(req, orgId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Update a service")
    public ResponseEntity<ServiceDto> update(@PathVariable UUID id, @Valid @RequestBody CreateServiceRequest req) {
        return ResponseEntity.ok(catalogService.updateService(id, req));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Activate/deactivate a service")
    public ResponseEntity<Void> toggle(@PathVariable UUID id, @RequestParam boolean active) {
        catalogService.toggleService(id, active);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Supplier catalog stats")
    public ResponseEntity<Map<String, Object>> stats() {
        UUID orgId = UUID.fromString(CurrentUserContext.get().get().orgId());
        return ResponseEntity.ok(catalogService.supplierStats(orgId));
    }
}
