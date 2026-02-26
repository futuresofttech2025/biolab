package com.biolab.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a revoked refresh token is reused — indicates a potential
 * token theft. Triggers immediate invalidation of the entire token family.
 *
 * <p>This is the core security mechanism of Token Rotation: if someone
 * replays a previously-used refresh token, ALL tokens in that family
 * are revoked to protect the legitimate user.</p>
 */
public class TokenReusedException extends AuthException {
    public TokenReusedException(String tokenFamily) {
        super("Refresh token reuse detected for family: " + tokenFamily +
              " — all tokens in this family have been invalidated", HttpStatus.UNAUTHORIZED);
    }
}
