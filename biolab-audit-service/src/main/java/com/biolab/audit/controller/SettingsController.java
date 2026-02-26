package com.biolab.audit.controller;

import com.biolab.audit.dto.PlatformSettingDto;
import com.biolab.audit.service.AuditService;
import com.biolab.common.security.CurrentUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/audit/settings")
@Tag(name = "Platform Settings")
@PreAuthorize("@perm.isAdmin()")
public class SettingsController {

    private final AuditService service;
    public SettingsController(AuditService s) { this.service = s; }

    @GetMapping
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List platform settings")
    public ResponseEntity<List<PlatformSettingDto>> list(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(service.listSettings(category));
    }

    @PutMapping("/{key}")
    @Operation(summary = "Update a setting")
    public ResponseEntity<PlatformSettingDto> update(@PathVariable String key, @RequestBody Map<String, String> body) {
        UUID userId = CurrentUserContext.get().get().userId();
        return ResponseEntity.ok(service.updateSetting(key, body.get("value"), userId));
    }
}
