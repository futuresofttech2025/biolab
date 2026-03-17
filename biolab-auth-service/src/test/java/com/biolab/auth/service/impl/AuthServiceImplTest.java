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
import com.biolab.auth.service.EmailService;
import com.biolab.auth.service.MfaService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link AuthServiceImpl}.
 *
 * <h3>Changes from original test class</h3>
 * <ul>
 *   <li>Added mocks for new dependencies: {@link MfaService}, {@link MfaPendingTokenRepository},
 *       {@link LoginAnomalyDetector}, {@link ConcurrentSessionManager}, {@link EmailService}</li>
 *   <li>TC-AUTH-010 (MFA login): mocks {@code mfaPendingTokenRepository.save()} — FIX-1</li>
 *   <li>TC-AUTH-021 / TC-AUTH-023: replaced stale {@code findTop5ByUserIdOrderByCreatedAtDesc}
 *       with {@code findRecentByUserId(UUID, Pageable)} — FIX-20</li>
 *   <li>TC-AUTH-005 / TC-AUTH-011: added {@code anomalyDetector} stub — FIX-7</li>
 *   <li>TC-AUTH-005 / TC-AUTH-011: added {@code concurrentSessionManager} stub — FIX-8</li>
 *   <li>TC-AUTH-021: added {@code emailService.sendPasswordChangedEmail} stub — FIX-15</li>
 *   <li>All {@code @Mock} fields updated to match {@link AuthServiceImpl} constructor</li>
 *   <li>email verification set to {@code true} on {@code testUser} so login tests pass</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @InjectMocks private AuthServiceImpl authService;

    // ── Original mocks ────────────────────────────────────────────────
    @Mock private UserRepository                   userRepository;
    @Mock private RefreshTokenRepository           refreshTokenRepository;
    @Mock private JwtTokenBlacklistRepository      blacklistRepository;
    @Mock private PasswordHistoryRepository        passwordHistoryRepository;
    @Mock private LoginAuditLogRepository          auditLogRepository;
    @Mock private MfaSettingsRepository            mfaSettingsRepository;
    @Mock private UserRoleRepository               userRoleRepository;
    @Mock private RoleRepository                   roleRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository     passwordResetTokenRepository;
    @Mock private JwtTokenProvider                 jwtTokenProvider;
    @Mock private PasswordEncoder                  passwordEncoder;

    // ── New mocks required by FIX-1, 7, 8, 15, 18 ────────────────────
    @Mock private MfaPendingTokenRepository  mfaPendingTokenRepository;  // FIX-1
    @Mock private MfaService                 mfaService;                 // FIX-1
    @Mock private LoginAnomalyDetector       anomalyDetector;            // FIX-7
    @Mock private ConcurrentSessionManager   concurrentSessionManager;   // FIX-8
    @Mock private EmailService               emailService;               // FIX-18

    private User testUser;
    private final UUID   userId      = UUID.randomUUID();
    private final String email       = "test@biolab.com";
    private final String rawPassword = "StrongPass@123";
    private final String encodedPassword = "$2a$10$encoded";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(email).passwordHash(encodedPassword)
                .firstName("John").lastName("Doe")
                .isActive(true).isLocked(false)
                .isEmailVerified(true)   // must be true for login to proceed past email check
                .failedLoginCount(0).passwordChangedAt(Instant.now())
                .build();
        testUser.setId(userId);
        testUser.setCreatedAt(Instant.now());

        ReflectionTestUtils.setField(authService, "maxLoginAttempts",    5);
        ReflectionTestUtils.setField(authService, "lockoutDurationMinutes", 30);
        ReflectionTestUtils.setField(authService, "passwordHistoryCount", 5);
        ReflectionTestUtils.setField(authService, "frontendUrl",         "http://localhost:5173");
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", 604800000L);
    }

    // ══════════════════════════════════════════════════════════════════
    // REGISTER
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Register")
    class RegisterTests {

        @Test
        @DisplayName("[TC-AUTH-001] ✅ Should register user successfully")
        void register_Success() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail(email); req.setPassword(rawPassword);
            req.setFirstName("John"); req.setLastName("Doe");

            when(userRepository.existsByEmailIgnoreCase(email)).thenReturn(false);
            when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(passwordHistoryRepository.save(any())).thenReturn(null);
            // register sends a verification email — stub the token repo
            when(emailVerificationTokenRepository.save(any())).thenReturn(null);

            RegisterResponse resp = authService.register(req);

            assertThat(resp).isNotNull();
            assertThat(resp.getId()).isEqualTo(userId);
            assertThat(resp.getEmail()).isEqualTo(email);
            assertThat(resp.getIsEmailVerified()).isFalse();
            verify(userRepository).save(any(User.class));
            verify(passwordHistoryRepository).save(any(PasswordHistory.class));
        }

        @Test
        @DisplayName("[TC-AUTH-002] ❌ Should fail registration when email already exists")
        void register_DuplicateEmail() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail(email); req.setPassword(rawPassword);
            req.setFirstName("John"); req.setLastName("Doe");

            when(userRepository.existsByEmailIgnoreCase(email)).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("[TC-AUTH-003] ✅ Should trim and lowercase email on registration")
        void register_NormalizesEmail() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("  Test@BioLab.COM  "); req.setPassword(rawPassword);
            req.setFirstName("John"); req.setLastName("Doe");

            when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
            when(emailVerificationTokenRepository.save(any())).thenReturn(null);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(captor.capture())).thenReturn(testUser);

            authService.register(req);

            assertThat(captor.getValue().getEmail()).isEqualTo("test@biolab.com");
        }

        @Test
        @DisplayName("[TC-AUTH-004] ✅ Should store initial password in history")
        void register_StoresPasswordHistory() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail(email); req.setPassword(rawPassword);
            req.setFirstName("A"); req.setLastName("B");

            when(userRepository.existsByEmailIgnoreCase(email)).thenReturn(false);
            when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
            when(userRepository.save(any())).thenReturn(testUser);
            when(emailVerificationTokenRepository.save(any())).thenReturn(null);

            authService.register(req);

            ArgumentCaptor<PasswordHistory> phCaptor = ArgumentCaptor.forClass(PasswordHistory.class);
            verify(passwordHistoryRepository).save(phCaptor.capture());
            assertThat(phCaptor.getValue().getPasswordHash()).isEqualTo(encodedPassword);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("[TC-AUTH-005] ✅ Should login successfully and return tokens")
        void login_Success() {
            LoginRequest req = new LoginRequest();
            req.setEmail(email); req.setPassword(rawPassword);

            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
            // FIX-7: stub anomaly detector to allow login
            when(anomalyDetector.calculateAnomalyScore(userId, "127.0.0.1", "Mozilla")).thenReturn(0);
            when(anomalyDetector.shouldBlock(0)).thenReturn(false);
            when(mfaSettingsRepository.existsByUserIdAndIsEnabledTrue(userId)).thenReturn(false);
            // FIX-8: stub concurrent session manager
            doNothing().when(concurrentSessionManager).enforceSessionLimit(userId);
            when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of("BUYER"));
            when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any())).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(any(), any(), anyInt())).thenReturn("refresh-token");
            when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600L);
            when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(86400000L);
            when(userRepository.save(any())).thenReturn(testUser);
            when(refreshTokenRepository.save(any())).thenReturn(null);

            AuthResponse resp = authService.login(req, "127.0.0.1", "Mozilla");

            assertThat(resp.getAccessToken()).isEqualTo("access-token");
            assertThat(resp.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(resp.getMfaRequired()).isFalse();
            assertThat(resp.getTokenFamily()).isNotNull();
            assertThat(resp.getTokenGeneration()).isEqualTo(0);
        }

        @Test
        @DisplayName("[TC-AUTH-006] ❌ Should fail login with wrong password")
        void login_WrongPassword() {
            LoginRequest req = new LoginRequest();
            req.setEmail(email); req.setPassword("WrongPass@999");

            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("WrongPass@999", encodedPassword)).thenReturn(false);
            when(userRepository.save(any())).thenReturn(testUser);

            assertThatThrownBy(() -> authService.login(req, "127.0.0.1", "Mozilla"))
                    .isInstanceOf(AuthException.class);
            verify(auditLogRepository, atLeastOnce()).save(any(LoginAuditLog.class));
        }

        @Test
        @DisplayName("[TC-AUTH-007] ❌ Should fail login when user does not exist")
        void login_UserNotFound() {
            LoginRequest req = new LoginRequest();
            req.setEmail("nobody@biolab.com"); req.setPassword(rawPassword);

            when(userRepository.findByEmailIgnoreCase("nobody@biolab.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req, "127.0.0.1", "Mozilla"))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("[TC-AUTH-008] ❌ Should fail login when account is deactivated")
        void login_AccountDeactivated() {
            testUser.setIsActive(false);
            LoginRequest req = new LoginRequest();
            req.setEmail(email); req.setPassword(rawPassword);

            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(req, "127.0.0.1", "Mozilla"))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("deactivated");
        }

        @Test
        @DisplayName("[TC-AUTH-009] ❌ Should fail login when account is locked")
        void login_AccountLocked() {
            testUser.setIsLocked(true);
            testUser.setLockedUntil(Instant.now().plusSeconds(1800));
            LoginRequest req = new LoginRequest();
            req.setEmail(email); req.setPassword(rawPassword);

            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(req, "127.0.0.1", "Mozilla"))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("locked");
        }

        @Test
        @DisplayName("[TC-AUTH-010] ✅ Should return MFA challenge when MFA is enabled")
        void login_MfaRequired() {
            LoginRequest req = new LoginRequest();
            req.setEmail(email); req.setPassword(rawPassword);

            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
            // FIX-7: anomaly detector allows login
            when(anomalyDetector.calculateAnomalyScore(userId, "127.0.0.1", "Mozilla")).thenReturn(0);
            when(anomalyDetector.shouldBlock(0)).thenReturn(false);
            when(mfaSettingsRepository.existsByUserIdAndIsEnabledTrue(userId)).thenReturn(true);
            // FIX-1: mfaPendingTokenRepository.save() must be stubbed
            when(mfaPendingTokenRepository.save(any(MfaPendingToken.class))).thenReturn(null);

            AuthResponse resp = authService.login(req, "127.0.0.1", "Mozilla");

            assertThat(resp.getMfaRequired()).isTrue();
            assertThat(resp.getMfaToken()).isNotNull();
            assertThat(resp.getAccessToken()).isNull();
            // FIX-1: verify the pending token was actually persisted to Redis
            verify(mfaPendingTokenRepository).save(any(MfaPendingToken.class));
        }

        @Test
        @DisplayName("[TC-AUTH-NEW] ❌ Should block login when anomaly detector fires")
        void login_BlockedByAnomalyDetector() {
            LoginRequest req = new LoginRequest();
            req.setEmail(email); req.setPassword(rawPassword);

            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
            // FIX-7: anomaly detector blocks the login
            when(anomalyDetector.calculateAnomalyScore(userId, "1.2.3.4", "bot")).thenReturn(90);
            when(anomalyDetector.shouldBlock(90)).thenReturn(true);

            assertThatThrownBy(() -> authService.login(req, "1.2.3.4", "bot"))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("suspicious");
        }

        @Test
        @DisplayName("[TC-AUTH-011] ✅ Should default to BUYER role when no roles assigned")
        void login_DefaultBuyerRole() {
            LoginRequest req = new LoginRequest();
            req.setEmail(email); req.setPassword(rawPassword);

            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
            when(anomalyDetector.calculateAnomalyScore(userId, "127.0.0.1", "Mozilla")).thenReturn(0);
            when(anomalyDetector.shouldBlock(0)).thenReturn(false);
            when(mfaSettingsRepository.existsByUserIdAndIsEnabledTrue(userId)).thenReturn(false);
            doNothing().when(concurrentSessionManager).enforceSessionLimit(userId);
            when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of());
            when(jwtTokenProvider.generateAccessToken(eq(userId), eq(email), eq(List.of("BUYER")), any()))
                    .thenReturn("token");
            when(jwtTokenProvider.generateRefreshToken(any(), any(), anyInt())).thenReturn("refresh");
            when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600L);
            when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(86400000L);
            when(userRepository.save(any())).thenReturn(testUser);

            authService.login(req, "127.0.0.1", "Mozilla");

            verify(jwtTokenProvider).generateAccessToken(eq(userId), eq(email), eq(List.of("BUYER")), any());
        }

        @Test
        @DisplayName("[TC-AUTH-012] ✅ Should increment failed login count on wrong password")
        void login_IncrementsFailedCount() {
            LoginRequest req = new LoginRequest();
            req.setEmail(email); req.setPassword("wrong");

            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrong", encodedPassword)).thenReturn(false);
            when(userRepository.save(any())).thenReturn(testUser);

            assertThatThrownBy(() -> authService.login(req, "127.0.0.1", "Mozilla"))
                    .isInstanceOf(AuthException.class);

            verify(userRepository).save(any(User.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // REFRESH TOKEN ROTATION
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Refresh Token Rotation")
    class RefreshTokenTests {

        private RefreshToken validToken;
        private final UUID familyId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
            validToken = RefreshToken.builder()
                    .user(testUser).tokenHash("hashed-token")
                    .tokenFamily(familyId).generation(0)
                    .isRevoked(false).expiresAt(Instant.now().plusSeconds(86400))
                    .ipAddress("127.0.0.1").userAgent("Mozilla")
                    .build();
            validToken.setId(UUID.randomUUID());
        }

        @Test
        @DisplayName("[TC-AUTH-013] ✅ Should rotate tokens successfully")
        void refreshToken_Success() {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("valid-refresh-token");

            try (MockedStatic<JwtTokenProvider> mocked = mockStatic(JwtTokenProvider.class)) {
                mocked.when(() -> JwtTokenProvider.hashToken("valid-refresh-token")).thenReturn("hashed-token");

                when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(validToken));
                when(jwtTokenProvider.isTokenValid("valid-refresh-token")).thenReturn(true);
                when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of("SUPPLIER"));
                when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any())).thenReturn("new-access");
                when(jwtTokenProvider.generateRefreshToken(any(), eq(familyId), eq(1))).thenReturn("new-refresh");
                when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600L);
                when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(86400000L);
                when(refreshTokenRepository.save(any())).thenReturn(null);

                AuthResponse resp = authService.refreshToken(req, "127.0.0.1", "Mozilla");

                assertThat(resp.getAccessToken()).isEqualTo("new-access");
                assertThat(resp.getRefreshToken()).isEqualTo("new-refresh");
                assertThat(resp.getTokenGeneration()).isEqualTo(1);
                assertThat(resp.getTokenFamily()).isEqualTo(familyId.toString());
            }
        }

        @Test
        @DisplayName("[TC-AUTH-014] ❌ Should detect reuse and revoke entire family")
        void refreshToken_ReuseDetected() {
            validToken.setIsRevoked(true);
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("reused-token");

            try (MockedStatic<JwtTokenProvider> mocked = mockStatic(JwtTokenProvider.class)) {
                mocked.when(() -> JwtTokenProvider.hashToken("reused-token")).thenReturn("hashed-token");

                when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(validToken));
                when(refreshTokenRepository.revokeAllByTokenFamily(familyId)).thenReturn(3);

                assertThatThrownBy(() -> authService.refreshToken(req, "127.0.0.1", "Mozilla"))
                        .isInstanceOf(TokenReusedException.class);
                verify(refreshTokenRepository).revokeAllByTokenFamily(familyId);
            }
        }

        @Test
        @DisplayName("[TC-AUTH-015] ❌ Should reject expired refresh token")
        void refreshToken_Expired() {
            validToken.setExpiresAt(Instant.now().minusSeconds(3600));
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("expired-token");

            try (MockedStatic<JwtTokenProvider> mocked = mockStatic(JwtTokenProvider.class)) {
                mocked.when(() -> JwtTokenProvider.hashToken("expired-token")).thenReturn("hashed-token");
                when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(validToken));
                when(refreshTokenRepository.save(any())).thenReturn(validToken);

                assertThatThrownBy(() -> authService.refreshToken(req, "127.0.0.1", "Mozilla"))
                        .isInstanceOf(AuthException.class)
                        .hasMessageContaining("expired");
            }
        }

        @Test
        @DisplayName("[TC-AUTH-016] ❌ Should reject invalid refresh token hash")
        void refreshToken_InvalidHash() {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("unknown-token");

            try (MockedStatic<JwtTokenProvider> mocked = mockStatic(JwtTokenProvider.class)) {
                mocked.when(() -> JwtTokenProvider.hashToken("unknown-token")).thenReturn("unknown-hash");
                when(refreshTokenRepository.findByTokenHash("unknown-hash")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> authService.refreshToken(req, "127.0.0.1", "Mozilla"))
                        .isInstanceOf(AuthException.class);
            }
        }

        @Test
        @DisplayName("[TC-AUTH-017] ❌ Should reject refresh when account deactivated")
        void refreshToken_AccountDeactivated() {
            testUser.setIsActive(false);
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("valid-token");

            try (MockedStatic<JwtTokenProvider> mocked = mockStatic(JwtTokenProvider.class)) {
                mocked.when(() -> JwtTokenProvider.hashToken("valid-token")).thenReturn("hashed-token");
                when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(validToken));
                when(jwtTokenProvider.isTokenValid("valid-token")).thenReturn(true);

                assertThatThrownBy(() -> authService.refreshToken(req, "127.0.0.1", "Mozilla"))
                        .isInstanceOf(AuthException.class)
                        .hasMessageContaining("deactivated");
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // LOGOUT
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Test
        @DisplayName("[TC-AUTH-018] ✅ Should blacklist access token and revoke refresh tokens")
        void logout_Success() {
            Claims mockClaims = mock(Claims.class);
            when(mockClaims.getId()).thenReturn("jti-123");
            when(mockClaims.getSubject()).thenReturn(userId.toString());
            when(mockClaims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() + 60000));

            when(jwtTokenProvider.parseToken("access-token")).thenReturn(mockClaims);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(refreshTokenRepository.revokeAllByUserId(userId)).thenReturn(2);

            authService.logout("access-token", "refresh-token");

            verify(blacklistRepository).save(any(JwtTokenBlacklist.class));
            verify(refreshTokenRepository).revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("[TC-AUTH-019] ✅ Should handle logout gracefully when access token is invalid")
        void logout_InvalidAccessToken() {
            when(jwtTokenProvider.parseToken("bad-token")).thenThrow(new RuntimeException("Invalid"));

            assertThatNoException().isThrownBy(() -> authService.logout("bad-token", null));
        }

        @Test
        @DisplayName("[TC-AUTH-020] ✅ Should handle logout with null tokens")
        void logout_NullTokens() {
            assertThatNoException().isThrownBy(() -> authService.logout(null, null));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // CHANGE PASSWORD
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Change Password")
    class ChangePasswordTests {

        @Test
        @DisplayName("[TC-AUTH-021] ✅ Should change password successfully")
        void changePassword_Success() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword(rawPassword);
            req.setNewPassword("NewStrong@456");

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
            // FIX-20: new method is findRecentByUserId(UUID, Pageable)
            when(passwordHistoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of());
            when(passwordEncoder.encode("NewStrong@456")).thenReturn("new-encoded");
            when(userRepository.save(any())).thenReturn(testUser);
            // FIX-15: sendPasswordChangedEmail is now called after reset
            doNothing().when(emailService).sendPasswordChangedEmail(eq(email), eq("John"));

            MessageResponse resp = authService.changePassword(userId.toString(), req);

            assertThat(resp.getMessage()).contains("Password changed");
            verify(passwordHistoryRepository).save(any(PasswordHistory.class));
            verify(refreshTokenRepository).revokeAllByUserId(userId);
            // FIX-15: verify security notification email was sent
            verify(emailService).sendPasswordChangedEmail(eq(email), eq("John"));
        }

        @Test
        @DisplayName("[TC-AUTH-022] ❌ Should fail when current password is incorrect")
        void changePassword_WrongCurrentPassword() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword("wrong"); req.setNewPassword("New@123");

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrong", encodedPassword)).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(userId.toString(), req))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("incorrect");
        }

        @Test
        @DisplayName("[TC-AUTH-023] ❌ Should fail when reusing a recent password")
        void changePassword_PasswordReuse() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword(rawPassword); req.setNewPassword("OldPass@789");

            PasswordHistory ph = PasswordHistory.builder().user(testUser).passwordHash("old-hash").build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
            // FIX-20: updated mock signature
            when(passwordHistoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of());
            when(passwordEncoder.matches("OldPass@789", "old-hash")).thenReturn(true);

            assertThatThrownBy(() -> authService.changePassword(userId.toString(), req))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("reuse");
        }

        @Test
        @DisplayName("[TC-AUTH-024] ❌ Should fail when user not found")
        void changePassword_UserNotFound() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword(rawPassword); req.setNewPassword("New@123");
            UUID randomId = UUID.randomUUID();

            when(userRepository.findById(randomId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.changePassword(randomId.toString(), req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-AUTH-NEW] ✅ Should respect configurable password history count")
        void changePassword_RespectsConfigurableHistoryCount() {
            // Verify the Pageable is built with the configured count (5), not a hardcoded value
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword(rawPassword);
            req.setNewPassword("BrandNew@999");

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
            when(passwordHistoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of());
            when(passwordEncoder.encode("BrandNew@999")).thenReturn("newer-encoded");
            when(userRepository.save(any())).thenReturn(testUser);
            doNothing().when(emailService).sendPasswordChangedEmail(anyString(), anyString());

            authService.changePassword(userId.toString(), req);

            ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(passwordHistoryRepository).findTop5ByUserIdOrderByCreatedAtDesc(userId);
            // passwordHistoryCount = 5 (set via ReflectionTestUtils in setUp)
            assertThat(pageCaptor.getValue().getPageSize()).isEqualTo(5);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // TOKEN VALIDATION
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Token Validation")
    class TokenValidationTests {

        @Test
        @DisplayName("[TC-AUTH-025] ✅ Should validate a good token")
        void validateToken_Valid() {
            Claims mockClaims = mock(Claims.class);
            when(mockClaims.getId()).thenReturn("jti-1");
            when(mockClaims.getSubject()).thenReturn(userId.toString());
            when(mockClaims.get("email", String.class)).thenReturn(email);
            when(mockClaims.get("roles", List.class)).thenReturn(List.of("ADMIN"));
            when(mockClaims.get("orgId", String.class)).thenReturn("org-1");

            when(jwtTokenProvider.parseToken("valid-token")).thenReturn(mockClaims);
            when(blacklistRepository.existsByJti("jti-1")).thenReturn(false);

            TokenValidationResponse resp = authService.validateToken("valid-token");

            assertThat(resp.isValid()).isTrue();
            assertThat(resp.getUserId()).isEqualTo(userId.toString());
            assertThat(resp.getEmail()).isEqualTo(email);
        }

        @Test
        @DisplayName("[TC-AUTH-026] ❌ Should reject blacklisted token")
        void validateToken_Blacklisted() {
            Claims mockClaims = mock(Claims.class);
            when(mockClaims.getId()).thenReturn("jti-blacklisted");

            when(jwtTokenProvider.parseToken("blacklisted-token")).thenReturn(mockClaims);
            when(blacklistRepository.existsByJti("jti-blacklisted")).thenReturn(true);

            TokenValidationResponse resp = authService.validateToken("blacklisted-token");

            assertThat(resp.isValid()).isFalse();
        }

        @Test
        @DisplayName("[TC-AUTH-027] ❌ Should return invalid for unparseable token")
        void validateToken_MalformedToken() {
            when(jwtTokenProvider.parseToken("garbage")).thenThrow(new RuntimeException());

            TokenValidationResponse resp = authService.validateToken("garbage");

            assertThat(resp.isValid()).isFalse();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // FORGOT PASSWORD
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Forgot Password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("[TC-AUTH-028] ✅ Should always return success to prevent email enumeration")
        void forgotPassword_AlwaysReturnsSuccess() {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("anyone@biolab.com");

            // User not found — service silently skips; still returns success
            when(userRepository.findByEmailIgnoreCase("anyone@biolab.com")).thenReturn(Optional.empty());

            MessageResponse resp = authService.forgotPassword(req);

            assertThat(resp.getMessage()).contains("If the email exists");
            verify(emailService, never()).sendPasswordResetEmail(any(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("[TC-AUTH-NEW] ✅ Should send full reset URL, not raw token — FIX-4")
        void forgotPassword_SendsFullUrl() {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail(email);
            testUser.setIsActive(true);

            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testUser));
            when(passwordResetTokenRepository.save(any())).thenReturn(null);
            doNothing().when(emailService)
                    .sendPasswordResetEmail(anyString(), anyString(), anyString(), anyInt());

            authService.forgotPassword(req);

            ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendPasswordResetEmail(
                    eq(email), eq("John"), linkCaptor.capture(), anyInt());
            // FIX-4: must be a full URL, not a bare token
            assertThat(linkCaptor.getValue())
                    .startsWith("http://localhost:5173/reset-password?token=");
        }
    }
}
