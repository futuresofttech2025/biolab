package com.biolab.auth.controller;

import com.biolab.auth.dto.request.DataAccessLogRequest;
import com.biolab.auth.dto.response.DataAccessLogResponse;
import com.biolab.auth.dto.response.PageResponse;
import com.biolab.auth.service.DataAccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for PHI/PII data access audit logging.
 * HIPAA §164.312(b) — Audit Controls.
 *
 * <h3>Base Path:</h3> {@code /api/audit/data-access}
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/audit/data-access")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Data Access Log", description = "PHI/PII access audit trail — HIPAA §164.312(b)")
public class DataAccessLogController {

    private final DataAccessLogService dataAccessLogService;

    /** Logs a data access event (called by other microservices). */
    @PostMapping
    @Operation(summary = "Log data access", description = "Records a PHI/PII data access event")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Access event logged"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<DataAccessLogResponse> log(
            @Parameter(description = "Accessing user UUID (from gateway)")
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody DataAccessLogRequest request,
            HttpServletRequest http) {
        log.info("Data access: user={}, resource={}/{}, action={}",
                userId, request.getResourceType(), request.getResourceId(), request.getAction());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dataAccessLogService.log(UUID.fromString(userId), request, http.getRemoteAddr()));
    }

    /** Access logs by user (paginated). */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get by user", description = "Data access history for a specific user")
    @ApiResponse(responseCode = "200", description = "Access logs retrieved")
    public ResponseEntity<PageResponse<DataAccessLogResponse>> getByUser(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(dataAccessLogService.getByUserId(userId, pageable));
    }

    /** Access logs by resource. */
    @GetMapping("/resource/{type}/{id}")
    @Operation(summary = "Get by resource", description = "All access events for a specific resource")
    @ApiResponse(responseCode = "200", description = "Access logs retrieved")
    public ResponseEntity<PageResponse<DataAccessLogResponse>> getByResource(
            @Parameter(description = "Resource type") @PathVariable String type,
            @Parameter(description = "Resource UUID") @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(dataAccessLogService.getByResource(type, id, pageable));
    }
}
