package com.biolab.audit.controller;

import com.biolab.audit.dto.*;
import com.biolab.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/audit/compliance")
@Tag(name = "Compliance")
public class ComplianceController {

    private final AuditService service;
    public ComplianceController(AuditService s) { this.service = s; }

    @GetMapping("/audits")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List compliance audits")
    public ResponseEntity<Page<ComplianceAuditDto>> listAudits(Pageable pageable) {
        return ResponseEntity.ok(service.listComplianceAudits(pageable));
    }

    @PostMapping("/audits")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Record compliance audit")
    public ResponseEntity<ComplianceAuditDto> create(@RequestBody ComplianceAuditDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createComplianceAudit(dto));
    }

    @GetMapping("/policies")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List policy documents")
    public ResponseEntity<Page<PolicyDocumentDto>> listPolicies(Pageable pageable) {
        return ResponseEntity.ok(service.listPolicies(pageable));
    }

    @GetMapping("/stats")
    @Operation(summary = "Compliance dashboard stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.dashboardStats());
    }
}
