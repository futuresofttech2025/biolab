package com.biolab.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Utility class for JWT token parsing and validation.
 *
 * <p>Used by the {@link com.biolab.gateway.filter.JwtAuthenticationFilter}
 * to validate Bearer tokens on incoming requests. This class only
 * <strong>validates</strong> tokens — token <strong>issuance</strong> is handled
 * by the Auth Service.</p>
 *
 * <h3>Token Structure (expected claims):</h3>
 * <pre>
 * {
 *   "sub":   "user-uuid",
 *   "email": "user@biolab.com",
 *   "roles": ["BUYER"],
 *   "orgId": "org-uuid",
 *   "iss":   "biolab-auth-service",
 *   "iat":   1708012800,
 *   "exp":   1708013700
 * }
 * </pre>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${app.jwt.secret:BioLabSecretKeyForJWTSigningMustBeAtLeast256BitsLong2026!}")
    private String jwtSecret;

    @Value("${app.jwt.issuer:biolab-auth-service}")
    private String expectedIssuer;

    /** HMAC-SHA key derived from the secret string. */
    private SecretKey signingKey;

    /**
     * Initializes the HMAC signing key from the configured secret.
     * Called once after dependency injection.
     */
    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT signing key initialized (issuer: {})", expectedIssuer);
    }

    /**
     * Validates the given JWT token string.
     *
     * <p>Checks signature integrity, expiration, issuer, and structural validity.</p>
     *
     * @param token the raw JWT string (without "Bearer " prefix)
     * @return {@code true} if the token is valid, {@code false} otherwise
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);

            // Verify issuer matches expected auth service
            if (!expectedIssuer.equals(claims.getIssuer())) {
                log.warn("JWT issuer mismatch: expected={}, actual={}", expectedIssuer, claims.getIssuer());
                return false;
            }

            // Verify token is not expired
            if (claims.getExpiration().before(new Date())) {
                log.warn("JWT token expired for subject: {}", claims.getSubject());
                return false;
            }

            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("JWT malformed: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT unsupported: {}", ex.getMessage());
        } catch (SignatureException ex) {
            log.warn("JWT signature invalid: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Extracts all claims from a valid JWT token.
     *
     * @param token the raw JWT string
     * @return the parsed {@link Claims} object
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the subject (user ID) from the token.
     *
     * @param token the raw JWT string
     * @return the user UUID string
     */
    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts the user's email from the token.
     *
     * @param token the raw JWT string
     * @return the email address
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    /**
     * Extracts the user's roles from the token.
     *
     * @param token the raw JWT string
     * @return list of role strings (e.g., ["BUYER", "ADMIN"])
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractAllClaims(token).get("roles", List.class);
    }

    /**
     * Extracts the organization ID from the token.
     *
     * @param token the raw JWT string
     * @return the organization UUID string
     */
    public String extractOrgId(String token) {
        return extractAllClaims(token).get("orgId", String.class);
    }
}
