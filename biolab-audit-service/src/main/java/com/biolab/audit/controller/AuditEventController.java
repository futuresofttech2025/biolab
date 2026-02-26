package com.biolab.audit.controller;

import com.biolab.audit.dto.*;
import com.biolab.audit.service.AuditService;
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
@RequestMapping("/audit/events")
@Tag(name = "Audit Events")
@PreAuthorize("@perm.isAdmin()")
public class AuditEventController {

    private final AuditService service;
    public AuditEventController(AuditService s) { this.service = s; }

    @GetMapping
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List all audit events (admin)")
    public ResponseEntity<Page<AuditEventDto>> list(Pageable pageable) {
        return ResponseEntity.ok(service.listEvents(pageable));
    }

    @GetMapping("/user/{userId}")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List events for a specific user")
    public ResponseEntity<Page<AuditEventDto>> listByUser(@PathVariable UUID userId, Pageable pageable) {
        return ResponseEntity.ok(service.listEventsByUser(userId, pageable));
    }

    @PostMapping
    @Operation(summary = "Log an audit event")
    public ResponseEntity<AuditEventDto> log(@Valid @RequestBody CreateAuditEventRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.logEvent(req));
    }
}
