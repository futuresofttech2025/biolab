package com.biolab.gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtUtil}.
 *
 * <p>Tests token validation including valid tokens, expired tokens,
 * invalid signatures, and claims extraction.</p>
 *
 * @author BioLab Engineering Team
 */
class JwtUtilTest {

    private static final String SECRET = "BioLabTestSecretKeyForJWTSigningMustBeAtLeast256BitsLongForTesting!";
    private static final String ISSUER = "biolab-auth-service";

    private JwtUtil jwtUtil;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expectedIssuer", ISSUER);
        jwtUtil.init();
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String generateToken(long expirationMs) {
        return Jwts.builder()
                .subject("user-uuid-123")
                .claim("email", "test@biolab.com")
                .claim("roles", List.of("BUYER"))
                .claim("orgId", "org-uuid-456")
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    @Test
    @DisplayName("[TC-GW-001] Should validate a valid JWT token")
    void shouldValidateValidToken() {
        String token = generateToken(60000); // 1 minute
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("[TC-GW-002] Should reject an expired JWT token")
    void shouldRejectExpiredToken() {
        String token = generateToken(-1000); // Already expired
        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("[TC-GW-003] Should reject a token with invalid signature")
    void shouldRejectInvalidSignature() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "WrongKeyThatIsAlsoAtLeast256BitsLongForHMACSHA256Signing!!".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("hacker")
                .issuer(ISSUER)
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(wrongKey)
                .compact();
        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("[TC-GW-004] Should extract user ID from token")
    void shouldExtractUserId() {
        String token = generateToken(60000);
        assertEquals("user-uuid-123", jwtUtil.extractUserId(token));
    }

    @Test
    @DisplayName("[TC-GW-005] Should extract email from token")
    void shouldExtractEmail() {
        String token = generateToken(60000);
        assertEquals("test@biolab.com", jwtUtil.extractEmail(token));
    }

    @Test
    @DisplayName("[TC-GW-006] Should extract roles from token")
    void shouldExtractRoles() {
        String token = generateToken(60000);
        List<String> roles = jwtUtil.extractRoles(token);
        assertEquals(1, roles.size());
        assertEquals("BUYER", roles.get(0));
    }

    @Test
    @DisplayName("[TC-GW-007] Should extract organization ID from token")
    void shouldExtractOrgId() {
        String token = generateToken(60000);
        assertEquals("org-uuid-456", jwtUtil.extractOrgId(token));
    }

    @Test
    @DisplayName("[TC-GW-008] Should reject malformed token string")
    void shouldRejectMalformedToken() {
        assertFalse(jwtUtil.validateToken("not.a.valid.jwt"));
    }

    @Test
    @DisplayName("[TC-GW-009] Should reject empty token")
    void shouldRejectEmptyToken() {
        assertFalse(jwtUtil.validateToken(""));
    }
}
