package com.biolab.auth.controller;

import com.biolab.auth.dto.request.ConsentRequest;
import com.biolab.auth.dto.response.ConsentRecordResponse;
import com.biolab.auth.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for GDPR/HIPAA/TOS consent management.
 *
 * <h3>Base Path:</h3> {@code /api/users/{userId}/consent}
 *
 * <p>All actions are recorded with timestamps, IP, and version for
 * regulatory compliance (GDPR Article 7, HIPAA).</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/users/{userId}/consent")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Consent Records", description = "GDPR/HIPAA/TOS consent management — grant, view, revoke")
public class ConsentController {

    private final ConsentService consentService;

    /** Records consent grant. Re-grants if same type already exists. */
    @PostMapping
    @Operation(summary = "Grant consent", description = "Records user consent for GDPR/HIPAA/TOS/MARKETING")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Consent granted"),
        @ApiResponse(responseCode = "400", description = "Invalid consent type"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ConsentRecordResponse> grant(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @Valid @RequestBody ConsentRequest request,
            HttpServletRequest http) {
        log.info("Granting {} consent for user {}", request.getConsentType(), userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consentService.grant(userId, request, http.getRemoteAddr()));
    }

    /** Lists all consent records for a user (granted and revoked). */
    @GetMapping
    @Operation(summary = "List consent records")
    @ApiResponse(responseCode = "200", description = "Consent records retrieved")
    public ResponseEntity<List<ConsentRecordResponse>> getByUser(
            @Parameter(description = "User UUID") @PathVariable UUID userId) {
        return ResponseEntity.ok(consentService.getByUserId(userId));
    }

    /** Revokes consent — record retained with revoked_at timestamp. */
    @DeleteMapping("/{type}")
    @Operation(summary = "Revoke consent", description = "Revokes consent — record retained for audit")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Consent revoked"),
        @ApiResponse(responseCode = "404", description = "Consent record not found")
    })
    public ResponseEntity<ConsentRecordResponse> revoke(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @Parameter(description = "Consent type: GDPR, HIPAA, TOS, MARKETING")
            @PathVariable String type) {
        log.info("Revoking {} consent for user {}", type, userId);
        return ResponseEntity.ok(consentService.revoke(userId, type));
    }
}
