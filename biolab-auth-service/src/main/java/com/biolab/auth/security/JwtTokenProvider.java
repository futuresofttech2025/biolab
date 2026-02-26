package com.biolab.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * JWT Token Provider — issues and validates access/refresh tokens.
 *
 * <h3>Access Token Claims:</h3>
 * <pre>
 *   sub:   user UUID          jti:   unique token ID
 *   email: user email         iss:   biolab-auth-service
 *   roles: [BUYER,ADMIN]      iat:   issued timestamp
 *   orgId: organization UUID  exp:   expiration timestamp
 * </pre>
 *
 * <h3>Token Rotation Support:</h3>
 * <p>Refresh tokens include a {@code family} claim (UUID) and a {@code gen}
 * claim (generation number) to support the rotation strategy. The actual
 * rotation logic is in {@code AuthServiceImpl}; this class only handles
 * token creation and parsing.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret:BioLabSecretKeyForJWTSigningMustBeAtLeast256BitsLong2026!}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Value("${app.jwt.issuer:biolab-auth-service}")
    private String issuer;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT provider initialized — issuer={}, accessTTL={}ms, refreshTTL={}ms",
                issuer, accessTokenExpirationMs, refreshTokenExpirationMs);
    }

    /**
     * Generates a JWT access token with user identity and RBAC claims.
     *
     * @param userId user UUID
     * @param email  user email
     * @param roles  list of role names
     * @param orgId  primary organization UUID (nullable)
     * @return signed JWT string
     */
    public String generateAccessToken(UUID userId, String email, List<String> roles, String orgId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .claim("orgId", orgId)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpirationMs)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a JWT refresh token with token family and generation for rotation.
     *
     * @param userId      user UUID
     * @param tokenFamily the family UUID (created at login, preserved across rotations)
     * @param generation  the rotation generation (0 = first, incremented on each refresh)
     * @return signed JWT string
     */
    public String generateRefreshToken(UUID userId, UUID tokenFamily, int generation) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("family", tokenFamily.toString())
                .claim("gen", generation)
                .claim("type", "REFRESH")
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshTokenExpirationMs)))
                .signWith(signingKey)
                .compact();
    }

    /** Parses and validates a JWT, returning its claims. Throws on invalid token. */
    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
    }

    /** Validates token structure and signature. Returns false on any error. */
    public boolean isTokenValid(String token) {
        try { parseToken(token); return true; }
        catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /** SHA-256 hash of a token string — used to store refresh tokens securely. */
    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    public long getAccessTokenExpirationSeconds() { return accessTokenExpirationMs / 1000; }
    public long getRefreshTokenExpirationMs() { return refreshTokenExpirationMs; }
}
