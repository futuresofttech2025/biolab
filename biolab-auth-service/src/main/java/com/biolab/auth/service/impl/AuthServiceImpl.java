package com.biolab.auth.service.impl;

import com.biolab.auth.dto.request.*;
import com.biolab.auth.dto.response.*;
import com.biolab.auth.entity.*;
import com.biolab.auth.entity.enums.*;
import com.biolab.auth.exception.*;
import com.biolab.auth.repository.*;
import com.biolab.auth.security.ConcurrentSessionManager;
import com.biolab.auth.security.JwtTokenProvider;
import com.biolab.auth.security.LoginAnomalyDetector;
import com.biolab.auth.service.AuthService;
import com.biolab.auth.service.EmailService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core authentication service.
 *
 * <h3>Sprint 1 changes:</h3>
 * <ul>
 *   <li><b>GAP-05</b>: MFA pending sessions moved from in-memory
 *       {@code ConcurrentHashMap} to Redis. Sessions survive gateway restarts
 *       and work correctly in a multi-instance deployment. TTL is enforced
 *       server-side by Redis — no scheduled cleanup needed.</li>
 *   <li><b>GAP-06</b>: Email OTP single-use enforcement in
 *       {@code verifyMfa()} — {@code emailOtpExpiresAt} is checked; the OTP
 *       secret is cleared immediately after successful verification via
 *       {@code MfaServiceImpl.verifyOtp()}.</li>
 *   <li><b>GAP-07</b>: Per-token MFA brute-force lockout — a Redis counter
 *       keyed by {@code mfaToken} tracks failed attempts. After 5 failures
 *       the pending session is invalidated and the counter is deleted.
 *       Regardless of attempt count, the session auto-expires after 5 minutes
 *       via Redis TTL.</li>
 *   <li><b>GAP-08</b>: {@link LoginAnomalyDetector} and
 *       {@link ConcurrentSessionManager} are now wired and called on every
 *       successful credential verification:
 *       <ul>
 *         <li>Anomaly score ≥ 5 → login blocked outright.</li>
 *         <li>Anomaly score 3–4 → step-up MFA forced even when not configured.</li>
 *         <li>Anomaly score 0–2 → normal flow.</li>
 *         <li>Session limit enforced in {@code issueTokenPair}.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * @author BioLab Engineering Team
 * @version 3.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    // ── Repositories ─────────────────────────────────────────────────────
    private final UserRepository                   userRepository;
    private final RefreshTokenRepository           refreshTokenRepository;
    private final JwtTokenBlacklistRepository      blacklistRepository;
    private final PasswordHistoryRepository        passwordHistoryRepository;
    private final LoginAuditLogRepository          auditLogRepository;
    private final MfaSettingsRepository            mfaSettingsRepository;
    private final RoleRepository                   roleRepository;
    private final UserRoleRepository               userRoleRepository;
    private final PasswordResetTokenRepository     passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    /** SESSION FIX: required to create UserSession records on every login */
    private final UserSessionRepository            userSessionRepository;

    // ── Services / components ────────────────────────────────────────────
    private final JwtTokenProvider         jwtTokenProvider;
    private final PasswordEncoder          passwordEncoder;
    private final EmailService             emailService;

    /**
     * GAP-08: wired anomaly detector — was dead code before Sprint 1.
     * Detects new IP logins, brute-force from IP, and credential stuffing.
     */
    private final LoginAnomalyDetector     loginAnomalyDetector;

    /**
     * GAP-08: wired session manager — was dead code before Sprint 1.
     * Terminates oldest sessions when the per-user limit is reached.
     */
    private final ConcurrentSessionManager concurrentSessionManager;

    /**
     * GAP-05: Redis template used for two purposes:
     * <ol>
     *   <li>MFA pending session store (replaces in-memory ConcurrentHashMap)</li>
     *   <li>MFA attempt counter for per-token brute-force lockout (GAP-07)</li>
     * </ol>
     * Keys used:
     * <ul>
     *   <li>{@code mfa:session:{token}}  → JSON-serialised {@link MfaPendingToken}</li>
     *   <li>{@code mfa:attempts:{token}} → integer attempt counter</li>
     * </ul>
     */
    private final RedisTemplate<String, Object> redisTemplate;

    // ── Config ────────────────────────────────────────────────────────────
    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Value("${app.security.password-history-count:5}")
    private int passwordHistoryCount;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /** When false (local dev), skip email verification on login + auto-verify on register. */
    @Value("${app.security.require-email-verification:false}")
    private boolean requireEmailVerification;

    /** When true (local dev), completely skip MFA step-up on login regardless of anomaly score or MFA settings. */
    @Value("${app.security.skip-mfa:true}")
    private boolean skipMfa;

    // ── Redis key prefixes ────────────────────────────────────────────────
    private static final String MFA_SESSION_PREFIX  = "mfa:session:";
    private static final String MFA_ATTEMPTS_PREFIX = "mfa:attempts:";

    /** GAP-05: MFA pending session TTL — 5 minutes, enforced by Redis. */
    private static final Duration MFA_SESSION_TTL = Duration.ofMinutes(5);

    /** GAP-07: Max failed MFA attempts before the pending session is killed. */
    private static final int MFA_MAX_ATTEMPTS = 5;

    // ─────────────────────────────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────────────────────────────

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
                // Auto-verify when email verification is disabled (local dev)
                .isEmailVerified(!requireEmailVerification)
                .build();

        User saved = userRepository.save(user);
        passwordHistoryRepository.save(
                PasswordHistory.builder().user(saved).passwordHash(saved.getPasswordHash()).build());

        String requestedRole = (request.getRole() != null)
                ? request.getRole().toUpperCase().trim() : "BUYER";
        if (!requestedRole.equals("SUPPLIER")) requestedRole = "BUYER";
        final String roleName = requestedRole;
        roleRepository.findByName(roleName).ifPresentOrElse(
                r -> userRoleRepository.save(UserRole.builder().user(saved).role(r).build()),
                () -> roleRepository.findByName("BUYER").ifPresent(
                        r -> userRoleRepository.save(UserRole.builder().user(saved).role(r).build())));

        sendNewVerificationToken(saved);
        log.info("User registered: id={}", saved.getId());

        return RegisterResponse.builder()
                .id(saved.getId()).email(saved.getEmail())
                .firstName(saved.getFirstName()).lastName(saved.getLastName())
                .isEmailVerified(false).createdAt(saved.getCreatedAt()).build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Login attempt: {}", request.getEmail());

        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> {
                    logAudit(null, request.getEmail(), ipAddress, userAgent,
                            LoginAction.FAILED_LOGIN, LoginStatus.FAILURE, "User not found");
                    return new AuthException("Invalid email or password", HttpStatus.UNAUTHORIZED);
                });

        // Account status gates
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
        if (requireEmailVerification && !user.getIsEmailVerified()) {
            logAudit(user, request.getEmail(), ipAddress, userAgent,
                    LoginAction.FAILED_LOGIN, LoginStatus.FAILURE, "Email not verified");
            throw new AuthException(
                    "Please verify your email before logging in.", HttpStatus.FORBIDDEN);
        }

        // Password check
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.recordFailedLogin(maxLoginAttempts, lockoutDurationMinutes);
            userRepository.save(user);
            logAudit(user, request.getEmail(), ipAddress, userAgent,
                    LoginAction.FAILED_LOGIN, LoginStatus.FAILURE, "Invalid password");
            throw new AuthException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        // GAP-08: anomaly detection (was dead code before Sprint 1)
        int anomalyScore = loginAnomalyDetector.calculateAnomalyScore(
                user.getId(), ipAddress, userAgent);

        if (loginAnomalyDetector.shouldBlock(anomalyScore)) {
            log.warn("Login BLOCKED by anomaly detector: user={} ip={} score={}",
                    user.getId(), ipAddress, anomalyScore);
            logAudit(user, request.getEmail(), ipAddress, userAgent,
                    LoginAction.FAILED_LOGIN, LoginStatus.FAILURE,
                    "Anomaly score=" + anomalyScore + " (blocked)");
            throw new AuthException(
                    "Login blocked due to suspicious activity. Please try again later.",
                    HttpStatus.FORBIDDEN);
        }

        // Determine if MFA is required: either user has MFA enabled or anomaly score forces step-up
        boolean mfaEnabled = mfaSettingsRepository.existsByUserIdAndIsEnabledTrue(user.getId());
        boolean forceMfa   = loginAnomalyDetector.shouldRequireMfa(anomalyScore);

        if (!skipMfa && (mfaEnabled || forceMfa)) {
            String mfaToken = UUID.randomUUID().toString();

            // GAP-05: store in Redis instead of in-memory ConcurrentHashMap
            MfaPendingToken pending = MfaPendingToken.builder()
                    .token(mfaToken)
                    .userId(user.getId())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .ttlSeconds(MFA_SESSION_TTL.getSeconds())
                    .build();
            redisTemplate.opsForValue().set(
                    MFA_SESSION_PREFIX + mfaToken, pending, MFA_SESSION_TTL);

            // Send EMAIL OTP if user has EMAIL MFA type enabled
            if (mfaEnabled) {
                List<MfaSettings> mfaList = mfaSettingsRepository.findByUserId(user.getId());
                for (MfaSettings mfa : mfaList) {
                    if (mfa.getIsEnabled() && mfa.getMfaType() == MfaType.EMAIL) {
                        // BUG-1 FIX: generate a fresh 6-digit numeric OTP, store it as
                        // secretKey (overwriting any stale value), and set emailOtpExpiresAt.
                        // Previously this called TotpUtil.generateCurrentCode(secretKey) which
                        // produces a TOTP code derived from the secret — but verifyMfa() does
                        // a plain string equals() check against secretKey, so they NEVER match.
                        String otp = String.format("%06d",
                                new java.security.SecureRandom().nextInt(1_000_000));
                        mfa.setSecretKey(otp);
                        mfa.setEmailOtpExpiresAt(
                                Instant.now().plusSeconds(600)); // 10-minute window
                        mfa.setUpdatedAt(Instant.now());
                        mfaSettingsRepository.save(mfa);
                        emailService.sendMfaCode(user.getEmail(), user.getFirstName(), otp);
                        log.info("MFA email OTP generated and sent to: {}", user.getEmail());
                    }
                }
            }

            if (forceMfa && !mfaEnabled) {
                log.warn("Step-up MFA forced for user {} due to anomaly score {}",
                        user.getId(), anomalyScore);
            }

            logAudit(user, request.getEmail(), ipAddress, userAgent,
                    LoginAction.MFA_CHALLENGE, LoginStatus.SUCCESS, null);

            // BUG-2 FIX: expiresIn was left at the long default (0) on the MFA challenge
            // response because the builder call never set it.  The frontend reported
            // "login expire in is 0".  Set it to the Redis pending-session TTL (300 s)
            // so the UI can show a countdown and the client knows when to re-login.
            return AuthResponse.builder()
                    .mfaRequired(true)
                    .mfaToken(mfaToken)
                    .expiresIn(MFA_SESSION_TTL.getSeconds())   // 300 seconds
                    .build();
        }

        return issueTokenPair(user, ipAddress, userAgent);
    }

    // ─────────────────────────────────────────────────────────────────────
    // VERIFY MFA — GAP-05, GAP-06, GAP-07
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public AuthResponse verifyMfa(MfaVerifyRequest request) {
        String mfaToken = request.getMfaToken();
        log.info("MFA verification attempt");

        // GAP-05: Redis lookup — null means session expired (Redis TTL) or never existed
        MfaPendingToken pending = (MfaPendingToken) redisTemplate.opsForValue()
                .get(MFA_SESSION_PREFIX + mfaToken);

        if (pending == null) {
            throw new AuthException(
                    "MFA session expired or invalid. Please log in again.",
                    HttpStatus.UNAUTHORIZED);
        }

        if (request.getCode() == null || request.getCode().trim().length() < 6) {
            throw new AuthException("Invalid MFA code format.", HttpStatus.BAD_REQUEST);
        }

        // GAP-07: per-token attempt counter
        String attemptsKey = MFA_ATTEMPTS_PREFIX + mfaToken;
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts == 1) {
            // Set TTL on the counter to match the session TTL
            redisTemplate.expire(attemptsKey, MFA_SESSION_TTL);
        }
        if (attempts > MFA_MAX_ATTEMPTS) {
            log.warn("MFA brute-force detected: token={}, attempts={}", mfaToken, attempts);
            // Invalidate the pending session immediately
            redisTemplate.delete(MFA_SESSION_PREFIX + mfaToken);
            redisTemplate.delete(attemptsKey);
            throw new AuthException(
                    "Too many incorrect attempts. Please log in again.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        User user = userRepository.findById(pending.getUserId())
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.UNAUTHORIZED));

        // GAP-06: OTP validation delegated to MfaServiceImpl which enforces
        // emailOtpExpiresAt and clears the secret after single use.
        List<MfaSettings> mfaList = mfaSettingsRepository.findByUserId(user.getId());
        boolean codeValid = false;

        for (MfaSettings mfa : mfaList) {
            if (!mfa.getIsEnabled() || mfa.getSecretKey() == null) continue;

            // GAP-06: enforce email OTP expiry
            if (mfa.getMfaType() == MfaType.EMAIL) {
                if (mfa.getEmailOtpExpiresAt() != null
                        && Instant.now().isAfter(mfa.getEmailOtpExpiresAt())) {
                    log.warn("Email OTP expired for user {}", user.getId());
                    continue; // try next method
                }
                if (mfa.getSecretKey().equals(request.getCode().trim())) {
                    // GAP-06: single-use — clear secret and expiry immediately
                    mfa.setSecretKey(null);
                    mfa.setEmailOtpExpiresAt(null);
                    mfa.setUpdatedAt(Instant.now());
                    mfaSettingsRepository.save(mfa);
                    codeValid = true;
                    break;
                }
            } else {
                // TOTP validation
                if (com.biolab.auth.security.TotpUtil.validateCode(
                        mfa.getSecretKey(), request.getCode())) {
                    codeValid = true;
                    break;
                }
            }

            // Backup codes
            if (mfa.getBackupCodes() != null) {
                String normalized = request.getCode().toUpperCase().trim();
                String[] codes = mfa.getBackupCodes();
                for (int i = 0; i < codes.length; i++) {
                    if (codes[i] != null && codes[i].equals(normalized)) {
                        codes[i] = null;
                        mfa.setBackupCodes(codes);
                        mfaSettingsRepository.save(mfa);
                        codeValid = true;
                        log.info("Backup code consumed for user: {}", user.getId());
                        break;
                    }
                }
                if (codeValid) break;
            }
        }

        if (!codeValid) {
            logAudit(user, user.getEmail(), pending.getIpAddress(), pending.getUserAgent(),
                    LoginAction.MFA_CHALLENGE, LoginStatus.FAILURE,
                    "Invalid MFA code (attempt " + attempts + ")");
            throw new AuthException(
                    "Invalid verification code. Please try again.", HttpStatus.BAD_REQUEST);
        }

        // Success — clean up Redis session and attempt counter
        redisTemplate.delete(MFA_SESSION_PREFIX + mfaToken);
        redisTemplate.delete(attemptsKey);

        logAudit(user, user.getEmail(), pending.getIpAddress(), pending.getUserAgent(),
                LoginAction.LOGIN, LoginStatus.SUCCESS, null);
        log.info("MFA verified for user: {}", user.getId());

        return issueTokenPair(user, pending.getIpAddress(), pending.getUserAgent());
    }

    // ─────────────────────────────────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        log.debug("Token refresh attempt");

        String tokenHash = JwtTokenProvider.hashToken(request.getRefreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (storedToken.getIsRevoked()) {
            log.warn("REFRESH TOKEN REUSE DETECTED! Family={}, Generation={}, User={}",
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

        RefreshToken newSavedToken = refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(JwtTokenProvider.hashToken(newRefreshToken))
                .tokenFamily(family)
                .generation(newGeneration)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                .build());

        // SESSION FIX: on token rotation, update the existing UserSession
        // (lastAccessedAt + new refreshToken link) rather than creating a new session.
        // This keeps the active session count accurate and the sessions list meaningful.
        userSessionRepository.findByRefreshTokenId(storedToken.getId())
                .ifPresent(sess -> {
                    sess.setRefreshToken(newSavedToken);
                    sess.setLastAccessedAt(Instant.now());
                    sess.setExpiresAt(newSavedToken.getExpiresAt());
                    userSessionRepository.save(sess);
                });

        logAudit(user, user.getEmail(), ipAddress, userAgent,
                LoginAction.TOKEN_ROTATION, LoginStatus.SUCCESS,
                "Family=" + family + " Gen=" + newGeneration);

        return AuthResponse.builder()
                .accessToken(newAccessToken).refreshToken(newRefreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .tokenFamily(family.toString()).tokenGeneration(newGeneration)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void logout(String accessToken, String refreshToken) {
        log.info("Logout request");
        if (accessToken != null) {
            try {
                Claims claims = jwtTokenProvider.parseToken(accessToken);
                User user = userRepository.findById(
                        UUID.fromString(claims.getSubject())).orElse(null);
                if (user != null) {
                    blacklistRepository.save(JwtTokenBlacklist.builder()
                            .jti(claims.getId()).user(user).tokenType(TokenType.ACCESS)
                            .expiresAt(claims.getExpiration().toInstant())
                            .reason("User logout").build());
                    int revoked = refreshTokenRepository.revokeAllByUserId(user.getId());
                    // SESSION FIX: deactivate all UserSession records on logout
                    int deactivated = userSessionRepository.deactivateAllUserSessions(user.getId());
                    log.info("Logout: blacklisted access token, revoked {} refresh tokens, deactivated {} sessions",
                            revoked, deactivated);
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
                // SESSION FIX: also deactivate the specific session linked to this refresh token
                userSessionRepository.findByRefreshTokenId(rt.getId())
                        .ifPresent(sess -> {
                            sess.setIsActive(false);
                            userSessionRepository.save(sess);
                        });
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PASSWORD MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        log.info("Password reset requested for: {}", request.getEmail());
        userRepository.findByEmailIgnoreCase(request.getEmail()).ifPresent(user -> {
            passwordResetTokenRepository.invalidateAllByUserId(user.getId(), Instant.now());
            String rawToken  = UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", "");
            String tokenHash = hashToken(rawToken);
            passwordResetTokenRepository.save(PasswordResetToken.builder()
                    .user(user).tokenHash(tokenHash)
                    .expiresAt(Instant.now().plusSeconds(
                            PasswordResetToken.EXPIRY_MINUTES * 60L))
                    .build());
            emailService.sendPasswordResetEmail(
                    user.getEmail(), user.getFirstName(),
                    frontendUrl + "/reset-password?token=" + rawToken,
                    PasswordResetToken.EXPIRY_MINUTES);
            log.info("Password reset email dispatched for user: {}", user.getId());
        });
        return MessageResponse.builder()
                .message("If the email exists, a password reset link has been sent")
                .expiresIn(PasswordResetToken.EXPIRY_MINUTES + " minutes").build();
    }

    @Override
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        log.info("Password reset attempt");
        String tokenHash = hashToken(request.getToken());
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthException(
                        "Invalid or expired password reset link. Please request a new one.",
                        HttpStatus.BAD_REQUEST));

        if (!resetToken.isValid()) {
            throw new AuthException(
                    Boolean.TRUE.equals(resetToken.getUsed())
                            ? "This link has already been used."
                            : "This link has expired. Please request a new one.",
                    HttpStatus.BAD_REQUEST);
        }

        User user = resetToken.getUser();
        if (!user.getIsActive()) {
            throw new AuthException("Account is deactivated.", HttpStatus.FORBIDDEN);
        }

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
        user.clearLockout();
        userRepository.save(user);

        passwordHistoryRepository.save(
                PasswordHistory.builder().user(user).passwordHash(newHash).build());

        resetToken.setUsed(true);
        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        int revokedCount = refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("Password reset: user={}, {} refresh tokens revoked", user.getId(), revokedCount);

        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());
        return MessageResponse.builder()
                .message("Password reset successful. Please log in with your new password.")
                .build();
    }

    @Override
    public MessageResponse changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AuthException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }

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

        passwordHistoryRepository.save(
                PasswordHistory.builder().user(user).passwordHash(newHash).build());
        refreshTokenRepository.revokeAllByUserId(user.getId());
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());

        log.info("Password changed for user: {}", userId);
        return MessageResponse.builder()
                .message("Password changed successfully. Please log in again.").build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // EMAIL VERIFICATION
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public MessageResponse verifyEmail(String rawToken) {
        String hash = hashToken(rawToken);
        EmailVerificationToken evt = emailVerificationTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AuthException(
                        "Invalid or expired verification link.", HttpStatus.BAD_REQUEST));

        if (!evt.isValid()) {
            throw new AuthException(
                    evt.getUsedAt() != null
                            ? "This link has already been used."
                            : "This link has expired. Please request a new one.",
                    HttpStatus.BAD_REQUEST);
        }

        evt.setUsedAt(Instant.now());
        emailVerificationTokenRepository.save(evt);

        User user = evt.getUser();
        user.setIsEmailVerified(true);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getId());
        return MessageResponse.builder()
                .message("Email verified successfully. You can now log in.").build();
    }

    @Override
    public MessageResponse resendVerificationEmail(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (!user.getIsEmailVerified()) {
                sendNewVerificationToken(user);
                log.info("Verification email resent for user: {}", user.getId());
            }
        });
        return MessageResponse.builder()
                .message("If that email is registered and unverified, a new link has been sent.")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // TOKEN VALIDATION
    // ─────────────────────────────────────────────────────────────────────

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
                    .orgId(claims.get("orgId", String.class))
                    .build();
        } catch (Exception e) {
            return TokenValidationResponse.builder().valid(false).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Issues a new access+refresh token pair for a successfully-authenticated user.
     * GAP-08: session limit is enforced here via {@link ConcurrentSessionManager}.
     */
    private AuthResponse issueTokenPair(User user, String ipAddress, String userAgent) {
        user.recordSuccessfulLogin();
        userRepository.save(user);

        // GAP-08: enforce concurrent session limit (was dead code before Sprint 1)
        concurrentSessionManager.enforceSessionLimit(user.getId());

        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        if (roles.isEmpty()) roles = List.of("BUYER");

        UUID tokenFamily = UUID.randomUUID();
        int  generation  = 0;

        String accessToken  = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), roles, null);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), tokenFamily, generation);

        RefreshToken savedRefreshToken = refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(JwtTokenProvider.hashToken(refreshToken))
                .tokenFamily(tokenFamily)
                .generation(generation)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(Instant.now().plusMillis(
                        jwtTokenProvider.getRefreshTokenExpirationMs()))
                .build());

        // SESSION FIX: create a UserSession record so the Sessions UI shows active devices.
        // Previously issueTokenPair() only saved a RefreshToken — UserSession was never
        // written, so user_sessions table was always empty and the UI showed an error.
        userSessionRepository.save(UserSession.builder()
                .user(user)
                .refreshToken(savedRefreshToken)
                .sessionToken(JwtTokenProvider.hashToken(refreshToken)) // reuse RT hash as session token
                .ipAddress(ipAddress != null ? ipAddress : "unknown")
                .userAgent(userAgent != null ? userAgent : "unknown")
                .isActive(true)
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                .lastAccessedAt(Instant.now())
                .build());

        logAudit(user, user.getEmail(), ipAddress, userAgent,
                LoginAction.LOGIN, LoginStatus.SUCCESS, null);

        log.info("Login successful: user={}, family={}", user.getId(), tokenFamily);
        return AuthResponse.builder()
                .accessToken(accessToken).refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .mfaRequired(false).tokenFamily(tokenFamily.toString())
                .tokenGeneration(generation)
                .build();
    }

    private void sendNewVerificationToken(User user) {
        emailVerificationTokenRepository.revokeAllByUserId(user.getId());
        String rawToken = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .user(user).tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().plusSeconds(24 * 60 * 60))
                .build());
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), rawToken);
    }

    private void logAudit(User user, String email, String ip, String ua,
                          LoginAction action, LoginStatus status, String reason) {
        auditLogRepository.save(LoginAuditLog.builder()
                .user(user).email(email).ipAddress(ip).userAgent(ua)
                .action(action).status(status).failureReason(reason).build());
    }

    private String hashToken(String raw) {
        try {
            java.security.MessageDigest md =
                    java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(
                    raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SCHEDULED CLEANUP
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Hourly cleanup of expired DB tokens.
     * GAP-05: MFA session cleanup is now handled automatically by Redis TTL —
     * no manual cleanup needed for that store.
     */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        int refreshCleaned   = refreshTokenRepository.deleteExpiredTokens(now);
        int blacklistCleaned = blacklistRepository.deleteExpiredTokens(now);
        int resetCleaned     = passwordResetTokenRepository.deleteExpiredAndUsed(now);
        if (refreshCleaned > 0 || blacklistCleaned > 0 || resetCleaned > 0) {
            log.info("Token cleanup: {} refresh, {} blacklist, {} reset tokens removed",
                    refreshCleaned, blacklistCleaned, resetCleaned);
        }
    }
}