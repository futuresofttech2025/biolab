package com.biolab.auth.controller;

import com.biolab.auth.dto.request.UserCreateRequest;
import com.biolab.auth.dto.request.UserUpdateRequest;
import com.biolab.auth.dto.response.PageResponse;
import com.biolab.auth.dto.response.UserResponse;
import com.biolab.auth.service.UserCrudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * REST controller for user account CRUD — admin-level management.
 *
 * <h3>Base Path:</h3> {@code /api/users}
 *
 * <p>Self-registration goes through /api/auth/register.
 * This controller is for admin CRUD operations on user accounts.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User account CRUD — admin-level management")
public class UserCrudController {

    private final UserCrudService userCrudService;

    /** Admin: creates a new user account. */
    @PostMapping
    @Operation(summary = "Create user", description = "Admin: create user account with specified details")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        log.info("Admin creating user: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(userCrudService.create(request));
    }

    /** Retrieves a user profile by UUID (password hash never returned). */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Returns user profile — password hash never exposed")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getById(
            @Parameter(description = "User UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(userCrudService.getById(id));
    }

    /** Search users with keyword (email/name) and active status filter. Paginated. */
    @GetMapping
    @Operation(summary = "Search users", description = "Paginated search by keyword and active status")
    @ApiResponse(responseCode = "200", description = "Users retrieved")
    public ResponseEntity<PageResponse<UserResponse>> search(
            @Parameter(description = "Keyword: email, first name, or last name")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userCrudService.search(search, isActive, pageable));
    }

    /** Partial update — only non-null fields are applied. */
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Partial update — only provided fields change")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> update(
            @Parameter(description = "User UUID") @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("Updating user: {}", id);
        return ResponseEntity.ok(userCrudService.update(id, request));
    }

    /** Soft delete — sets is_active=false (retains record for HIPAA audit). */
    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate user", description = "Soft delete — record retained for HIPAA compliance")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deactivated"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "User UUID") @PathVariable UUID id) {
        log.info("Deactivating user: {}", id);
        userCrudService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
