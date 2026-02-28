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
import java.util.UUID;

@RestController
@RequestMapping("/catalog/requests")
@Tag(name = "Service Requests")
public class ServiceRequestController {

    private final CatalogService catalogService;
    public ServiceRequestController(CatalogService cs) { this.catalogService = cs; }

    @GetMapping("/supplier")
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','ADMIN','SUPER_ADMIN')")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List incoming requests for supplier")
    public ResponseEntity<Page<ServiceRequestDto>> listForSupplier(Pageable pageable) {
        UUID orgId = UUID.fromString(CurrentUserContext.require().orgId());
        return ResponseEntity.ok(catalogService.listRequests(orgId, pageable));
    }

    @PostMapping
    @PreAuthorize("@perm.hasAnyRole('BUYER','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Submit a service request")
    public ResponseEntity<ServiceRequestDto> create(@Valid @RequestBody CreateServiceRequestRequest req) {
        var user = CurrentUserContext.require();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(catalogService.createRequest(req, user.userId(), UUID.fromString(user.orgId())));
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Accept a service request")
    public ResponseEntity<ServiceRequestDto> accept(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogService.updateRequestStatus(id, "ACCEPTED"));
    }

    @PatchMapping("/{id}/decline")
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Decline a service request")
    public ResponseEntity<ServiceRequestDto> decline(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogService.updateRequestStatus(id, "DECLINED"));
    }
}
