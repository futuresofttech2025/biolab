package com.biolab.auth.controller;

import com.biolab.auth.dto.response.LoginAuditLogResponse;
import com.biolab.auth.dto.response.PageResponse;
import com.biolab.auth.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Read-only controller for login audit log queries.
 * Immutable append-only log — HIPAA §164.312(b) and SOC 2 compliance.
 *
 * <h3>Base Path:</h3> {@code /api/audit/login}
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/audit/login")
@RequiredArgsConstructor
@Tag(name = "Login Audit Log", description = "Immutable authentication event log — HIPAA/SOC2")
public class AuditLogController {

    private final AuditLogService auditLogService;

    /** All audit entries (paginated, newest first). */
    @GetMapping
    @Operation(summary = "List all audit entries", description = "Paginated authentication events")
    @ApiResponse(responseCode = "200", description = "Audit entries retrieved")
    public ResponseEntity<PageResponse<LoginAuditLogResponse>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getAll(pageable));
    }

    /** Audit entries for a specific user. */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get by user", description = "Authentication events for a specific user")
    @ApiResponse(responseCode = "200", description = "Audit entries retrieved")
    public ResponseEntity<PageResponse<LoginAuditLogResponse>> getByUser(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getByUserId(userId, pageable));
    }

    /** Audit entries by email (useful for tracking failed attempts). */
    @GetMapping("/email/{email}")
    @Operation(summary = "Get by email", description = "Authentication events for a specific email")
    @ApiResponse(responseCode = "200", description = "Audit entries retrieved")
    public ResponseEntity<PageResponse<LoginAuditLogResponse>> getByEmail(
            @Parameter(description = "Email address") @PathVariable String email,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getByEmail(email, pageable));
    }
}
