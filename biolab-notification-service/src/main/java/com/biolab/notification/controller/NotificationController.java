package com.biolab.notification.controller;

import com.biolab.notification.dto.*;
import com.biolab.notification.service.NotificationService;
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
@RequestMapping("/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService service;
    public NotificationController(NotificationService s) { this.service = s; }

    @GetMapping
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List notifications for current user")
    public ResponseEntity<Page<NotificationDto>> list(Pageable pageable) {
        UUID userId = CurrentUserContext.get().get().userId();
        return ResponseEntity.ok(service.list(userId, pageable));
    }

    @GetMapping("/unread-count")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "404", description = "Not found")})
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        UUID userId = CurrentUserContext.get().get().userId();
        return ResponseEntity.ok(Map.of("count", service.unreadCount(userId)));
    }

    @PostMapping
    @PreAuthorize("@perm.isAdmin()")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Created"), @ApiResponse(responseCode = "400", description = "Validation error")})
    @Operation(summary = "Create notification (admin/system)")
    public ResponseEntity<NotificationDto> create(@Valid @RequestBody CreateNotificationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark single notification as read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        service.markRead(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllRead() {
        service.markAllRead(CurrentUserContext.get().get().userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "404", description = "Not found")})
    @Operation(summary = "Get notification preferences")
    public ResponseEntity<NotificationPreferenceDto> getPrefs() {
        return ResponseEntity.ok(service.getPreferences(CurrentUserContext.get().get().userId()));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update notification preferences")
    public ResponseEntity<NotificationPreferenceDto> updatePrefs(@RequestBody NotificationPreferenceDto dto) {
        return ResponseEntity.ok(service.updatePreferences(CurrentUserContext.get().get().userId(), dto));
    }
}
