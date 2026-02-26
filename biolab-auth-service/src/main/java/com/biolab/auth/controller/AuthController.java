package com.biolab.auth.controller;

import com.biolab.auth.dto.request.*;
import com.biolab.auth.dto.response.*;
import com.biolab.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller — login, register, token rotation, logout, password management.
 *
 * <h3>Token Rotation Endpoints:</h3>
 * <ul>
 *   <li>POST /api/auth/login — creates new token family (gen=0)</li>
 *   <li>POST /api/auth/refresh-token — rotates: revokes old, issues new (gen++)</li>
 *   <li>POST /api/auth/logout — revokes ALL tokens for user</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, JWT token rotation, MFA, password management")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    @ApiResponses({@ApiResponse(responseCode="201"), @ApiResponse(responseCode="400"), @ApiResponse(responseCode="409")})
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login — returns JWT tokens with token family for rotation")
    @ApiResponses({@ApiResponse(responseCode="200"), @ApiResponse(responseCode="401"), @ApiResponse(responseCode="423")})
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(authService.login(request, extractIp(http), http.getHeader("User-Agent")));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh token with rotation — old token revoked, new issued in same family")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(authService.refreshToken(request, extractIp(http), http.getHeader("User-Agent")));
    }

    @PostMapping("/mfa/verify")
    @Operation(summary = "Verify MFA code")
    public ResponseEntity<AuthResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyMfa(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout — blacklists access token, revokes all refresh tokens")
    public ResponseEntity<Void> logout(@RequestHeader(value="Authorization",required=false) String auth,
                                       @RequestBody(required=false) RefreshTokenRequest request) {
        String at = auth!=null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
        authService.logout(at, request!=null ? request.getRefreshToken() : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password (authenticated)")
    public ResponseEntity<MessageResponse> changePassword(@RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(userId, request));
    }

    @GetMapping("/validate-token")
    @Operation(summary = "Validate JWT — used by API Gateway")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestHeader("Authorization") String auth) {
        return ResponseEntity.ok(authService.validateToken(auth.replace("Bearer ","")));
    }

    private String extractIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return xff != null ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }
}
