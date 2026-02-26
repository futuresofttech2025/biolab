package com.biolab.auth.service.impl;

import com.biolab.auth.dto.request.*;
import com.biolab.auth.dto.response.*;
import com.biolab.auth.entity.*;
import com.biolab.auth.entity.enums.*;
import com.biolab.auth.exception.*;
import com.biolab.auth.repository.*;
import com.biolab.auth.security.JwtTokenProvider;
import com.biolab.auth.service.AuthService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core authentication service with <b>Refresh Token Rotation</b>.
 *
 * <h3>Token Rotation Flow:</h3>
 * <pre>
 *   LOGIN:
 *     1. Verify credentials → create token_family (UUID), generation=0
 *     2. Issue access_token + refresh_token
 *     3. Store refresh token hash + family + generation in DB
 *
 *   REFRESH (rotation):
 *     1. Look up refresh token by hash
 *     2. If token is revoked → REUSE DETECTED → revoke entire family
 *     3. If token is valid → revoke it (reason=ROTATED), issue new pair
 *        in same family with generation++
 *
 *   LOGOUT:
 *     1. Blacklist access token (by JTI)
 *     2. Revoke all refresh tokens for the user
 *
 *   PASSWORD CHANGE:
 *     1. Revoke all refresh tokens (forces re-authentication)
 * </pre>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenBlacklistRepository blacklistRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final LoginAuditLogRepository auditLogRepository;
    private final MfaSettingsRepository mfaSettingsRepository;
    private final UserRoleRepository userRoleRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Value("${app.security.password-history-count:5}")
    private int passwordHistoryCount;

    // ─── REGISTER ────────────────────────────────────────────────────

    @Override
    public RegisterResponse register(RegisterRequest request) {
        log.info("Registration attempt: {}", request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phone(request.getPhone())
                .passwordChangedAt(Instant.now())
                .build();

        User saved = userRepository.save(user);

        // Store initial password in history
        passwordHistoryRepository.save(PasswordHistory.builder()
                .user(saved).passwordHash(saved.getPasswordHash()).build());

        log.info("User registered: id={}", saved.getId());

        return RegisterResponse.builder()
                .id(saved.getId()).email(saved.getEmail())
                .firstName(saved.getFirstName()).lastName(saved.getLastName())
                .isEmailVerified(false).createdAt(saved.getCreatedAt()).build();
    }

    // ─── LOGIN ───────────────────────────────────────────────────────

    @Override
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Login attempt: {}", request.getEmail());

        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> {
                    logAudit(null, request.getEmail(), ipAddress, userAgent,
                            LoginAction.FAILED_LOGIN, LoginStatus.FAILURE, "User not found");
                    return new AuthException("Invalid email or password", HttpStatus.UNAUTHORIZED);
                });

        // Account status checks
        if (!user.getIsActive()) {
            logAudit(user, request.getEmail(), ipAddress, userAgent,
                    LoginAction.FAILED_LOGIN, LoginStatus.FAILURE, "Account deactivated");
            throw new AuthException("Account is deactivated", HttpStatus.FORBIDDEN);
        }
        if (user.isAccountLocked()) {
            logAudit(user, request.getEmail(), ipAddress, userAgent,
                    LoginAction.FAILED_LOGIN, LoginStatus.FAILURE, "Account locked");
            throw new AuthException("Account locked until " + user.getLockedUntil(), HttpStatus.LOCKED);
        }

        // Password verification
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.recordFailedLogin(maxLoginAttempts, lockoutDurationMinutes);
            userRepository.save(user);
            logAudit(user, request.getEmail(), ipAddress, userAgent,
                    LoginAction.FAILED_LOGIN, LoginStatus.FAILURE, "Invalid password");
            throw new AuthException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        // Check MFA
        if (mfaSettingsRepository.existsByUserIdAndIsEnabledTrue(user.getId())) {
            String mfaToken = UUID.randomUUID().toString();
            logAudit(user, request.getEmail(), ipAddress, userAgent,
                    LoginAction.MFA_CHALLENGE, LoginStatus.SUCCESS, null);
            return AuthResponse.builder().mfaRequired(true).mfaToken(mfaToken).build();
        }

        // Successful login → create new token family
        return issueTokenPair(user, ipAddress, userAgent);
    }

    // ─── REFRESH TOKEN (WITH ROTATION) ──────────────────────────────

    /**
     * Refreshes tokens using the Token Rotation strategy.
     *
     * <p><b>Rotation flow:</b></p>
     * <ol>
     *   <li>Hash the incoming refresh token and look it up in the DB</li>
     *   <li>If the token is already revoked → <b>REUSE DETECTED</b>:
     *       an attacker (or stale client) is replaying a used token.
     *       Revoke the ENTIRE token family for safety.</li>
     *   <li>If the token is valid → revoke it (mark as ROTATED),
     *       issue a new token in the same family with generation+1</li>
     * </ol>
     */
    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        log.debug("Token refresh attempt");

        String tokenHash = JwtTokenProvider.hashToken(request.getRefreshToken());

        // Look up the refresh token by its hash
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        // ═══ REUSE DETECTION ═══
        // If this token was already used (revoked), it means someone is replaying it.
        // This could be an attacker who stole the old token. Revoke the ENTIRE family.
        if (storedToken.getIsRevoked()) {
            log.warn("🚨 REFRESH TOKEN REUSE DETECTED! Family={}, Generation={}, User={}",
                    storedToken.getTokenFamily(), storedToken.getGeneration(),
                    storedToken.getUser().getId());

            // Revoke ALL tokens in this family
            int revoked = refreshTokenRepository.revokeAllByTokenFamily(storedToken.getTokenFamily());
            log.warn("Revoked {} tokens in family {}", revoked, storedToken.getTokenFamily());

            // Audit log the reuse detection
            logAudit(storedToken.getUser(), storedToken.getUser().getEmail(),
                    ipAddress, userAgent, LoginAction.REUSE_DETECTED, LoginStatus.FAILURE,
                    "Family=" + storedToken.getTokenFamily() + " Gen=" + storedToken.getGeneration());

            throw new TokenReusedException(storedToken.getTokenFamily().toString());
        }

        // Check expiration
        if (storedToken.isExpired()) {
            storedToken.revoke(RevokedReason.EXPIRED_CLEANUP);
            refreshTokenRepository.save(storedToken);
            throw new AuthException("Refresh token expired", HttpStatus.UNAUTHORIZED);
        }

        // Validate the JWT signature
        if (!jwtTokenProvider.isTokenValid(request.getRefreshToken())) {
            throw new AuthException("Invalid refresh token signature", HttpStatus.UNAUTHORIZED);
        }

        User user = storedToken.getUser();
        if (!user.getIsActive()) {
            throw new AuthException("Account is deactivated", HttpStatus.FORBIDDEN);
        }

        // ═══ ROTATION: Revoke old, issue new in same family ═══
        storedToken.revoke(RevokedReason.ROTATED);
        refreshTokenRepository.save(storedToken);

        int newGeneration = storedToken.getGeneration() + 1;
        UUID family = storedToken.getTokenFamily();

        // Fetch user roles
        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        if (roles.isEmpty()) roles = List.of("BUYER");

        // Generate new token pair
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), roles, null);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), family, newGeneration);

        // Store the new refresh token
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(JwtTokenProvider.hashToken(newRefreshToken))
                .tokenFamily(family)
                .generation(newGeneration)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                .build());

        logAudit(user, user.getEmail(), ipAddress, userAgent,
                LoginAction.TOKEN_ROTATION, LoginStatus.SUCCESS,
                "Family=" + family + " Gen=" + newGeneration);

        log.info("Token rotated: user={}, family={}, gen={}", user.getId(), family, newGeneration);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .tokenFamily(family.toString())
                .tokenGeneration(newGeneration)
                .build();
    }

    // ─── LOGOUT ──────────────────────────────────────────────────────

    @Override
    public void logout(String accessToken, String refreshToken) {
        log.info("Logout request");

        // Blacklist the access token
        if (accessToken != null) {
            try {
                Claims claims = jwtTokenProvider.parseToken(accessToken);
                User user = userRepository.findById(UUID.fromString(claims.getSubject())).orElse(null);
                if (user != null) {
                    blacklistRepository.save(JwtTokenBlacklist.builder()
                            .jti(claims.getId()).user(user).tokenType(TokenType.ACCESS)
                            .expiresAt(claims.getExpiration().toInstant()).reason("User logout").build());

                    // Revoke ALL refresh tokens for this user
                    int revoked = refreshTokenRepository.revokeAllByUserId(user.getId());
                    log.info("Logout: blacklisted access token, revoked {} refresh tokens for user {}", revoked, user.getId());
                }
            } catch (Exception e) {
                log.warn("Could not blacklist access token: {}", e.getMessage());
            }
        }

        // Also revoke the specific refresh token if provided
        if (refreshToken != null) {
            String hash = JwtTokenProvider.hashToken(refreshToken);
            refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
                rt.revoke(RevokedReason.LOGOUT);
                refreshTokenRepository.save(rt);
            });
        }
    }

    // ─── MFA ─────────────────────────────────────────────────────────

    @Override
    public AuthResponse verifyMfa(MfaVerifyRequest request) {
        // TODO: Verify TOTP/email code against stored secret
        throw new AuthException("MFA verification not yet implemented", HttpStatus.NOT_IMPLEMENTED);
    }

    // ─── PASSWORD MANAGEMENT ─────────────────────────────────────────

    @Override
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        log.info("Password reset requested for: {}", request.getEmail());
        // Always return success to prevent email enumeration attacks
        return MessageResponse.builder()
                .message("If the email exists, a password reset link has been sent")
                .expiresIn("15 minutes").build();
    }

    @Override
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        // TODO: Validate reset token and set new password
        throw new AuthException("Password reset not yet implemented", HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public MessageResponse changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AuthException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }

        // Check password history (no reuse of last N passwords)
        List<PasswordHistory> history = passwordHistoryRepository
                .findTop5ByUserIdOrderByCreatedAtDesc(user.getId());
        for (PasswordHistory ph : history) {
            if (passwordEncoder.matches(request.getNewPassword(), ph.getPasswordHash())) {
                throw new AuthException(
                        "Cannot reuse any of your last " + passwordHistoryCount + " passwords",
                        HttpStatus.BAD_REQUEST);
            }
        }

        String newHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(newHash);
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        passwordHistoryRepository.save(PasswordHistory.builder()
                .user(user).passwordHash(newHash).build());

        // Revoke ALL refresh tokens → forces re-authentication with new password
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("Password changed for user: {}, all refresh tokens revoked", userId);
        return MessageResponse.builder().message("Password changed successfully. Please log in again.").build();
    }

    // ─── TOKEN VALIDATION (for API Gateway) ──────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TokenValidationResponse validateToken(String token) {
        try {
            Claims claims = jwtTokenProvider.parseToken(token);
            if (blacklistRepository.existsByJti(claims.getId())) {
                return TokenValidationResponse.builder().valid(false).build();
            }
            return TokenValidationResponse.builder().valid(true)
                    .userId(claims.getSubject())
                    .email(claims.get("email", String.class))
                    .roles(claims.get("roles", List.class))
                    .orgId(claims.get("orgId", String.class)).build();
        } catch (Exception e) {
            return TokenValidationResponse.builder().valid(false).build();
        }
    }

    // ─── HELPER: Issue token pair (login + create family) ────────────

    private AuthResponse issueTokenPair(User user, String ipAddress, String userAgent) {
        user.recordSuccessfulLogin();
        userRepository.save(user);

        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        if (roles.isEmpty()) roles = List.of("BUYER");

        // Create new token family for this login session
        UUID tokenFamily = UUID.randomUUID();
        int generation = 0;

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), roles, null);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), tokenFamily, generation);

        // Persist refresh token (hashed) with family metadata
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(JwtTokenProvider.hashToken(refreshToken))
                .tokenFamily(tokenFamily)
                .generation(generation)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                .build());

        logAudit(user, user.getEmail(), ipAddress, userAgent,
                LoginAction.LOGIN, LoginStatus.SUCCESS, null);

        log.info("Login successful: user={}, family={}", user.getId(), tokenFamily);

        return AuthResponse.builder()
                .accessToken(accessToken).refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .mfaRequired(false).tokenFamily(tokenFamily.toString())
                .tokenGeneration(generation).build();
    }

    // ─── HELPER: Audit logging ───────────────────────────────────────

    private void logAudit(User user, String email, String ip, String ua,
                          LoginAction action, LoginStatus status, String reason) {
        auditLogRepository.save(LoginAuditLog.builder()
                .user(user).email(email).ipAddress(ip).userAgent(ua)
                .action(action).status(status).failureReason(reason).build());
    }

    // ─── SCHEDULED: Cleanup expired tokens ───────────────────────────

    /** Runs every hour — removes expired refresh tokens and blacklist entries. */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        int refreshCleaned = refreshTokenRepository.deleteExpiredTokens(now.minusSeconds(86400));
        int blacklistCleaned = blacklistRepository.deleteExpiredTokens(now);
        if (refreshCleaned > 0 || blacklistCleaned > 0) {
            log.info("Token cleanup: {} expired refresh tokens, {} expired blacklist entries removed",
                    refreshCleaned, blacklistCleaned);
        }
    }
}
