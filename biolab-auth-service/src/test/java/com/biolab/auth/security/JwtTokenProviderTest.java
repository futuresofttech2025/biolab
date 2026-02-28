package com.biolab.auth.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider provider;
    private final UUID userId = UUID.randomUUID();
    private final String email = "test@biolab.com";

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "BioLabTestSecretKeyForJWTSigningMustBeAtLeast256BitsLongForTesting!");
        ReflectionTestUtils.setField(provider, "accessTokenExpirationMs", 900000L);
        ReflectionTestUtils.setField(provider, "refreshTokenExpirationMs", 604800000L);
        ReflectionTestUtils.setField(provider, "issuer", "biolab-auth-service");
        provider.init();
    }

    @Nested @DisplayName("generateAccessToken")
    class AccessTokenTests {
        @Test @DisplayName("[TC-AUTH-115] ✅ Should generate valid access token with all claims")
        void generate_Success() {
            String token = provider.generateAccessToken(userId, email, List.of("ADMIN", "BUYER"), "org-123");
            assertThat(token).isNotBlank();
            Claims claims = provider.parseToken(token);
            assertThat(claims.getSubject()).isEqualTo(userId.toString());
            assertThat(claims.get("email", String.class)).isEqualTo(email);
            assertThat(claims.get("roles", List.class)).containsExactly("ADMIN", "BUYER");
            assertThat(claims.get("orgId", String.class)).isEqualTo("org-123");
            assertThat(claims.getIssuer()).isEqualTo("biolab-auth-service");
            assertThat(claims.getId()).isNotBlank();
        }

        @Test @DisplayName("[TC-AUTH-116] ✅ Should handle null orgId")
        void generate_NullOrg() {
            String token = provider.generateAccessToken(userId, email, List.of("BUYER"), null);
            assertThat(provider.parseToken(token).get("orgId", String.class)).isNull();
        }

        @Test @DisplayName("[TC-AUTH-117] ✅ Should handle empty roles")
        void generate_EmptyRoles() {
            String token = provider.generateAccessToken(userId, email, List.of(), null);
            assertThat(provider.parseToken(token).get("roles", List.class)).isEmpty();
        }

        @Test @DisplayName("[TC-AUTH-118] ✅ Should generate unique JTI per token")
        void generate_UniqueJti() {
            String t1 = provider.generateAccessToken(userId, email, List.of(), null);
            String t2 = provider.generateAccessToken(userId, email, List.of(), null);
            assertThat(provider.parseToken(t1).getId()).isNotEqualTo(provider.parseToken(t2).getId());
        }
    }

    @Nested @DisplayName("generateRefreshToken")
    class RefreshTokenTests {
        @Test @DisplayName("[TC-AUTH-119] ✅ Should include family and generation claims")
        void generate_WithFamily() {
            UUID family = UUID.randomUUID();
            String token = provider.generateRefreshToken(userId, family, 3);
            Claims claims = provider.parseToken(token);
            assertThat(claims.get("family", String.class)).isEqualTo(family.toString());
            assertThat(claims.get("gen", Integer.class)).isEqualTo(3);
            assertThat(claims.get("type", String.class)).isEqualTo("REFRESH");
        }
    }

    @Nested @DisplayName("isTokenValid")
    class ValidationTests {
        @Test @DisplayName("[TC-AUTH-120] ✅ Valid token") void valid() {
            assertThat(provider.isTokenValid(provider.generateAccessToken(userId, email, List.of(), null))).isTrue();
        }
        @Test @DisplayName("[TC-AUTH-121] ❌ Malformed") void malformed() { assertThat(provider.isTokenValid("not.a.jwt")).isFalse(); }
        @Test @DisplayName("[TC-AUTH-122] ❌ Empty") void empty() { assertThat(provider.isTokenValid("")).isFalse(); }
        @Test @DisplayName("[TC-AUTH-123] ❌ Null") void nullToken() { assertThat(provider.isTokenValid(null)).isFalse(); }
        @Test @DisplayName("[TC-AUTH-124] ❌ Wrong signing key") void wrongKey() {
            JwtTokenProvider other = new JwtTokenProvider();
            ReflectionTestUtils.setField(other, "jwtSecret", "DifferentKeyThatIsAlsoLongEnoughForHMACSHA256Algorithm!!");
            ReflectionTestUtils.setField(other, "accessTokenExpirationMs", 900000L);
            ReflectionTestUtils.setField(other, "refreshTokenExpirationMs", 604800000L);
            ReflectionTestUtils.setField(other, "issuer", "biolab-auth-service");
            other.init();
            assertThat(provider.isTokenValid(other.generateAccessToken(userId, email, List.of(), null))).isFalse();
        }
    }

    @Nested @DisplayName("hashToken")
    class HashTests {
        @Test @DisplayName("[TC-AUTH-125] ✅ Consistent") void consistent() {
            assertThat(JwtTokenProvider.hashToken("t")).isEqualTo(JwtTokenProvider.hashToken("t"));
        }
        @Test @DisplayName("[TC-AUTH-126] ✅ Different inputs") void different() {
            assertThat(JwtTokenProvider.hashToken("a")).isNotEqualTo(JwtTokenProvider.hashToken("b"));
        }
        @Test @DisplayName("[TC-AUTH-127] ✅ Base64 output") void base64() {
            assertThat(JwtTokenProvider.hashToken("x")).matches("^[A-Za-z0-9+/=]+$");
        }
    }

    @Nested @DisplayName("Expiration helpers")
    class ExpirationTests {
        @Test @DisplayName("[TC-AUTH-128] ✅ Access token expiration in seconds") void access() { assertThat(provider.getAccessTokenExpirationSeconds()).isEqualTo(900); }
        @Test @DisplayName("[TC-AUTH-129] ✅ Refresh token expiration in ms") void refresh() { assertThat(provider.getRefreshTokenExpirationMs()).isEqualTo(604800000L); }
    }
}
