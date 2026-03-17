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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * Authentication controller — login, register, token rotation, logout,
 * password management, email verification.
 *
 * <h3>FIX-11 — Trusted-proxy IP extraction</h3>
 * <p>The previous {@code extractIp()} took the first value of
 * {@code X-Forwarded-For} unconditionally. Any client can set this header,
 * so an attacker sending {@code X-Forwarded-For: 1.2.3.4} from their real IP
 * would bypass anomaly detection and account lockout.</p>
 *
 * <p>The fixed approach: only honour {@code X-Forwarded-For} when the
 * <em>actual TCP connection</em> comes from a known trusted proxy CIDR.
 * Configure your proxy CIDRs in {@code app.security.trusted-proxy-cidrs}
 * (comma-separated). When the connection is from an untrusted source, the
 * real {@code request.getRemoteAddr()} is used instead.</p>
 *
 * <p><strong>Production example:</strong></p>
 * <pre>
 *   app.security.trusted-proxy-cidrs: 10.0.0.0/8,172.16.0.0/12,192.168.0.0/16
 * </pre>
 *
 * @author BioLab Engineering Team
 * @version 2.0.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication",
     description = "Login, JWT token rotation, MFA, password management")
public class AuthController {

    private final AuthService authService;

    /**
     * FIX-11: Comma-separated list of trusted proxy CIDRs.
     * X-Forwarded-For is only trusted when the TCP connection originates from one of these.
     * Defaults to localhost only — set proper CIDRs in production.
     */
    @Value("${app.security.trusted-proxy-cidrs:127.0.0.1/32,::1/128}")
    private String trustedProxyCidrs;

    // ─── Auth endpoints ───────────────────────────────────────────────────

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    @ApiResponses({@ApiResponse(responseCode = "201"),
                   @ApiResponse(responseCode = "400"),
                   @ApiResponse(responseCode = "409")})
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login — returns JWT tokens with token family for rotation")
    @ApiResponses({@ApiResponse(responseCode = "200"),
                   @ApiResponse(responseCode = "401"),
                   @ApiResponse(responseCode = "423")})
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest http) {
        return ResponseEntity.ok(
                authService.login(request, extractIp(http), http.getHeader("User-Agent")));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh token with rotation — old token revoked, new issued in same family")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest http) {
        return ResponseEntity.ok(
                authService.refreshToken(request, extractIp(http), http.getHeader("User-Agent")));
    }

    @PostMapping("/mfa/verify")
    @Operation(summary = "Verify MFA code — completes step-up login")
    public ResponseEntity<AuthResponse> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyMfa(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout — blacklists access token, revokes all refresh tokens")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody(required = false) RefreshTokenRequest request) {
        String at = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;
        authService.logout(at, request != null ? request.getRefreshToken() : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password (authenticated)")
    public ResponseEntity<MessageResponse> changePassword(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(userId, request));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email address via token link")
    @ApiResponses({@ApiResponse(responseCode = "200"),
                   @ApiResponse(responseCode = "400", description = "Invalid or expired token")})
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam("token") String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend email verification link")
    public ResponseEntity<MessageResponse> resendVerification(
            @RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(authService.resendVerificationEmail(request.getEmail()));
    }

    @GetMapping("/validate-token")
    @Operation(summary = "Validate JWT — used by API Gateway")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @RequestHeader("Authorization") String auth) {
        return ResponseEntity.ok(authService.validateToken(auth.replace("Bearer ", "")));
    }

    // ─── FIX-11: IP extraction with trusted-proxy guard ──────────────────

    /**
     * Extracts the real client IP address.
     *
     * <p>Only honours {@code X-Forwarded-For} when the TCP connection comes
     * from a configured trusted proxy (e.g. your load balancer or API gateway).
     * If the connection is from an untrusted source, the header is ignored and
     * {@code request.getRemoteAddr()} is used directly.</p>
     *
     * <p>This prevents IP spoofing via forged {@code X-Forwarded-For} headers
     * from external clients trying to bypass anomaly detection or lockout.</p>
     */
    private String extractIp(HttpServletRequest req) {
        String remoteAddr = req.getRemoteAddr();

        if (isTrustedProxy(remoteAddr)) {
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                // X-Forwarded-For: client, proxy1, proxy2 — leftmost is the real client
                String clientIp = xff.split(",")[0].trim();
                if (!clientIp.isEmpty()) {
                    log.debug("Using XFF IP {} (trusted proxy {})", clientIp, remoteAddr);
                    return clientIp;
                }
            }
        } else {
            log.debug("Ignoring X-Forwarded-For from untrusted source {}", remoteAddr);
        }

        return remoteAddr;
    }

    /**
     * Returns true if {@code remoteAddr} matches any configured trusted CIDR.
     * Supports IPv4 exact-match and /32 notation; IPv6 ::1 exact match.
     * For production, replace this with a proper CIDR library if subnets are needed.
     */
    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null) return false;
        List<String> cidrs = Arrays.stream(trustedProxyCidrs.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        for (String cidr : cidrs) {
            // Exact match or /32 (strip suffix for simplicity)
            String base = cidr.contains("/") ? cidr.substring(0, cidr.indexOf('/')) : cidr;
            if (remoteAddr.equals(base) || remoteAddr.equals(cidr)) return true;
            // Handle common loopback variants
            if ("127.0.0.1/32".equals(cidr) && "127.0.0.1".equals(remoteAddr)) return true;
            if ("::1/128".equals(cidr) && ("::1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr))) return true;
        }
        return false;
    }
}
