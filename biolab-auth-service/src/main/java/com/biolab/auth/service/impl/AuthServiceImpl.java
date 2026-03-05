package com.biolab.auth.service.impl;

import com.biolab.auth.dto.request.*;
import com.biolab.auth.dto.response.*;
import com.biolab.auth.entity.*;
import com.biolab.auth.entity.enums.*;
import com.biolab.auth.exception.*;
import com.biolab.auth.repository.*;
import com.biolab.auth.security.JwtTokenProvider;
import com.biolab.auth.service.AuthService;
import com.biolab.auth.service.EmailService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
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
 *   PASSWORD CHANGE / RESET:
 *     1. Revoke all refresh tokens (forces re-authentication)
 *     2. Send confirmation email
 * </pre>
 *
 * @author BioLab Engineering Team
 * @version 1.1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository                 userRepository;
    private final RefreshTokenRepository         refreshTokenRepository;
    private final JwtTokenBlacklistRepository    blacklistRepository;
    private final PasswordHistoryRepository      passwordHistoryRepository;
    private final LoginAuditLogRepository        auditLogRepository;
    private final MfaSettingsRepository          mfaSettingsRepository;
    private final UserRoleRepository             userRoleRepository;
    private final PasswordResetTokenRepository   passwordResetTokenRepository;
    private final JwtTokenProvider               jwtTokenProvider;
    private final PasswordEncoder                passwordEncoder;
    private final EmailService                   emailService;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Value("${app.security.password-history-count:5}")
    private int passwordHistoryCount;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

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

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        // ═══ REUSE DETECTION ═══
        if (storedToken.getIsRevoked()) {
            log.warn("🚨 REFRESH TOKEN REUSE DETECTED! Family={}, Generation={}, User={}",
                    storedToken.getTokenFamily(), storedToken.getGeneration(),
                    storedToken.getUser().getId());

            int revoked = refreshTokenRepository.revokeAllByTokenFamily(storedToken.getTokenFamily());
            log.warn("Revoked {} tokens in family {}", revoked, storedToken.getTokenFamily());

            logAudit(storedToken.getUser(), storedToken.getUser().getEmail(),
                    ipAddress, userAgent, LoginAction.REUSE_DETECTED, LoginStatus.FAILURE,
                    "Family=" + storedToken.getTokenFamily() + " Gen=" + storedToken.getGeneration());

            throw new TokenReusedException(storedToken.getTokenFamily().toString());
        }

        if (storedToken.isExpired()) {
            storedToken.revoke(RevokedReason.EXPIRED_CLEANUP);
            refreshTokenRepository.save(storedToken);
            throw new AuthException("Refresh token expired", HttpStatus.UNAUTHORIZED);
        }

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

        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        if (roles.isEmpty()) roles = List.of("BUYER");

        String newAccessToken  = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), roles, null);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), family, newGeneration);

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

        if (accessToken != null) {
            try {
                Claims claims = jwtTokenProvider.parseToken(accessToken);
                User user = userRepository.findById(UUID.fromString(claims.getSubject())).orElse(null);
                if (user != null) {
                    blacklistRepository.save(JwtTokenBlacklist.builder()
                            .jti(claims.getId()).user(user).tokenType(TokenType.ACCESS)
                            .expiresAt(claims.getExpiration().toInstant()).reason("User logout").build());

                    int revoked = refreshTokenRepository.revokeAllByUserId(user.getId());
                    log.info("Logout: blacklisted access token, revoked {} refresh tokens for user {}",
                            revoked, user.getId());
                }
            } catch (Exception e) {
                log.warn("Could not blacklist access token: {}", e.getMessage());
            }
        }

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

    /**
     * Initiates the forgot-password flow.
     *
     * <p>Always returns an identical generic response regardless of whether
     * the email exists — prevents user enumeration attacks.</p>
     *
     * <p>When the email exists:</p>
     * <ol>
     *   <li>Invalidates all previous unused reset tokens for this user</li>
     *   <li>Generates a cryptographically secure 256-bit URL-safe raw token</li>
     *   <li>Stores only the BCrypt hash — raw token is never persisted</li>
     *   <li>Sends the reset link asynchronously via email</li>
     * </ol>
     */
    @Override
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Password reset requested for: {}", email);

        // Generic response — identical regardless of email existence
        final MessageResponse GENERIC = MessageResponse.builder()
                .message("If that email is registered, a reset link has been sent.")
                .expiresIn(PasswordResetToken.EXPIRY_MINUTES + " minutes")
                .build();

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            log.debug("Forgot-password request for unknown email (not revealing): {}", email);
            return GENERIC;
        }

        User user = userOpt.get();

        // Invalidate all previous unused tokens — one active token per user at a time
        int invalidated = passwordResetTokenRepository.invalidateAllByUserId(user.getId(), Instant.now());
        if (invalidated > 0) {
            log.debug("Invalidated {} previous reset token(s) for user {}", invalidated, user.getId());
        }

        // Generate a cryptographically secure 256-bit URL-safe token
        String rawToken  = generateSecureToken();
        String tokenHash = passwordEncoder.encode(rawToken);
        Instant expiresAt = Instant.now().plus(PasswordResetToken.EXPIRY_MINUTES, ChronoUnit.MINUTES);

        PasswordResetToken prt = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();
        passwordResetTokenRepository.save(prt);

        // Build the reset URL — raw (un-hashed) token goes in the link
        String resetLink = frontendUrl + "/reset-password?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

        // Fire-and-forget async email — failure is logged, not re-thrown
        emailService.sendPasswordResetEmail(
                user.getEmail(), user.getFirstName(), resetLink, PasswordResetToken.EXPIRY_MINUTES);

        log.info("Password reset token issued for user: {}, expires: {}", user.getId(), expiresAt);
        return GENERIC;
    }

    /**
     * Completes the password reset using a token from the reset email.
     *
     * <p>Security properties:</p>
     * <ul>
     *   <li>Token is BCrypt-matched against stored hashes (raw token never stored)</li>
     *   <li>Token is single-use — marked used immediately on consumption</li>
     *   <li>All refresh tokens are revoked → forces re-login with new password</li>
     *   <li>Password history is enforced — cannot reuse last N passwords</li>
     * </ul>
     */
    @Override
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String rawToken   = request.getToken().trim();
        String newPassword = request.getNewPassword();

        // Load all currently-valid tokens, BCrypt-match the raw token against each hash
        List<PasswordResetToken> candidates = passwordResetTokenRepository
                .findAll()
                .stream()
                .filter(PasswordResetToken::isValid)
                .toList();

        PasswordResetToken matched = candidates.stream()
                .filter(t -> passwordEncoder.matches(rawToken, t.getTokenHash()))
                .findFirst()
                .orElseThrow(() -> new AuthException(
                        "Invalid or expired reset token. Please request a new one.",
                        HttpStatus.BAD_REQUEST));

        User user = matched.getUser();

        // Enforce password history — no reuse of last N passwords
        List<PasswordHistory> history = passwordHistoryRepository
                .findTop5ByUserIdOrderByCreatedAtDesc(user.getId());
        for (PasswordHistory ph : history) {
            if (passwordEncoder.matches(newPassword, ph.getPasswordHash())) {
                throw new AuthException(
                        "Cannot reuse any of your last " + passwordHistoryCount + " passwords.",
                        HttpStatus.BAD_REQUEST);
            }
        }

        // Consume the token — prevents replay within TTL window
        matched.setUsed(true);
        matched.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(matched);

        // Update password
        String newHash = passwordEncoder.encode(newPassword);
        user.setPasswordHash(newHash);
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        // Persist to history
        passwordHistoryRepository.save(PasswordHistory.builder()
                .user(user).passwordHash(newHash).build());

        // Revoke all refresh tokens — forces re-authentication with new password
        refreshTokenRepository.revokeAllByUserId(user.getId());

        // Notify user via async email
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());

        log.info("Password reset completed for user: {}, all sessions revoked", user.getId());
        return MessageResponse.builder()
                .message("Password reset successfully. Please log in with your new password.")
                .build();
    }

    /**
     * Changes a password for an authenticated user (from Settings).
     *
     * <p>Requires the current password for verification. All active sessions
     * are revoked and a confirmation email is sent.</p>
     */
    @Override
    public MessageResponse changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AuthException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }

        // Enforce password history — no reuse of last N passwords
        List<PasswordHistory> history = passwordHistoryRepository
                .findTop5ByUserIdOrderByCreatedAtDesc(user.getId());
        for (PasswordHistory ph : history) {
            if (passwordEncoder.matches(request.getNewPassword(), ph.getPasswordHash())) {
                throw new AuthException(
                        "Cannot reuse any of your last " + passwordHistoryCount + " passwords.",
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

        // Notify user via async email
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());

        log.info("Password changed for user: {}, all refresh tokens revoked", userId);
        return MessageResponse.builder()
                .message("Password changed successfully. Please log in again.")
                .build();
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

        UUID tokenFamily = UUID.randomUUID();
        int generation = 0;

        String accessToken  = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), roles, null);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), tokenFamily, generation);

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

    // ─── HELPER: Cryptographically secure token generation ───────────

    /** Generates a URL-safe Base64 encoded 256-bit secure random token. */
    private String generateSecureToken() {
        byte[] bytes = new byte[32]; // 256 bits
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ─── SCHEDULED: Cleanup expired tokens ───────────────────────────

    /** Runs every hour — removes expired refresh tokens and blacklist entries. */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        int refreshCleaned   = refreshTokenRepository.deleteExpiredTokens(now.minusSeconds(86400));
        int blacklistCleaned = blacklistRepository.deleteExpiredTokens(now);
        int resetCleaned     = passwordResetTokenRepository.deleteExpiredAndUsed(now);
        if (refreshCleaned > 0 || blacklistCleaned > 0 || resetCleaned > 0) {
            log.info("Token cleanup: {} refresh, {} blacklist, {} password-reset records removed",
                    refreshCleaned, blacklistCleaned, resetCleaned);
        }
    }
}