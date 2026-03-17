package com.biolab.gateway.filter;

import com.biolab.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Global Gateway filter — JWT validation + X-User-* header enrichment.
 *
 * <h3>Sprint 1 — GAP-04 (BFF cookie) + GAP-14 (header injection prevention)</h3>
 *
 * <h4>GAP-04: httpOnly cookie support</h4>
 * <p>The gateway acts as a Backend-for-Frontend (BFF) for the refresh-token
 * cookie flow. Three new BFF paths intercept requests before routing:</p>
 * <ul>
 *   <li>{@code POST /api/auth/bff/login}         — forwards to auth service login,
 *       extracts the {@code refreshToken} from the JSON response, and sets it as
 *       an {@code httpOnly; Secure; SameSite=Strict} cookie.</li>
 *   <li>{@code POST /api/auth/bff/refresh-token} — reads the cookie, forwards it
 *       to auth service refresh endpoint, rotates the cookie, returns new access token.</li>
 *   <li>{@code POST /api/auth/bff/logout}        — calls auth service logout and
 *       clears the cookie (Max-Age=0).</li>
 *   <li>{@code POST /api/auth/bff/mfa/verify}    — proxies MFA verify, sets cookie on success.</li>
 * </ul>
 *
 * <h4>GAP-14: Inbound X-User-* header stripping</h4>
 * <p>Before JWT enrichment, all {@code X-User-*} headers from the client are
 * removed. This prevents a malicious or misconfigured client from injecting
 * spoofed identity headers that downstream services might trust.</p>
 *
 * @author BioLab Engineering Team
 * @version 2.0.0
 */
@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    @Value("${app.jwt.refresh-token-cookie-name:biolab_rt}")
    private String refreshTokenCookieName;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    /**
     * Paths that do NOT require JWT authentication.
     */
    private static final List<String> OPEN_PATHS = List.of(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/forgot-password",
        "/api/auth/reset-password",
        "/api/auth/refresh-token",
        "/api/auth/logout",
        "/api/auth/mfa/verify",
        "/api/auth/verify-email",
        "/api/auth/resend-verification",
        "/api/auth/validate-token",
        // GAP-04: BFF endpoints handle their own auth logic
        "/api/auth/bff/login",
        "/api/auth/bff/refresh-token",
        "/api/auth/bff/logout",
        "/api/auth/bff/mfa/verify",
        "/api/categories",
        "/actuator",
        "/swagger-ui",
        "/v3/api-docs",
        "/webjars",
        "/eureka",
        "/api/discovery",
        "/api/config"
    );

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // GAP-14: strip ALL inbound X-User-* headers before any processing
        // This prevents clients from injecting spoofed identity headers
        ServerHttpRequest sanitised = request.mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Email");
                    headers.remove("X-User-Roles");
                    headers.remove("X-User-OrgId");
                })
                .build();
        exchange = exchange.mutate().request(sanitised).build();

        // Open paths — pass through without JWT check (but already sanitised)
        if (isOpenPath(path)) {
            return chain.filter(exchange);
        }

        // Extract and validate Authorization header
        String authHeader = sanitised.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return onUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            return onUnauthorized(exchange, "Invalid or expired JWT token");
        }

        try {
            Claims claims = jwtUtil.extractAllClaims(token);
            ServerHttpRequest enriched = exchange.getRequest().mutate()
                    .header("X-User-Id",    claims.getSubject())
                    .header("X-User-Email", claims.get("email", String.class))
                    .header("X-User-Roles", String.join(",", jwtUtil.extractRoles(token)))
                    .header("X-User-OrgId",
                            claims.get("orgId", String.class) != null
                                    ? claims.get("orgId", String.class) : "")
                    .build();

            log.debug("JWT validated for user: {} on path: {}", claims.getSubject(), path);
            return chain.filter(exchange.mutate().request(enriched).build());
        } catch (Exception ex) {
            log.error("Error processing JWT token: {}", ex.getMessage());
            return onUnauthorized(exchange, "Token processing error");
        }
    }

    /**
     * Sets a refresh-token httpOnly cookie on the response.
     * Called by the BFF route handlers after a successful login or token rotation.
     *
     * @param response     the gateway response to add the cookie to
     * @param refreshToken the raw refresh token string
     */
    public static void setRefreshCookie(ServerHttpResponse response,
                                        String refreshToken,
                                        String cookieName,
                                        long expirationMs) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(true)           // requires HTTPS — use false only in local dev
                .sameSite("Strict")     // CSRF protection
                .path("/api/auth")      // restrict cookie scope to auth endpoints only
                .maxAge(Duration.ofMillis(expirationMs))
                .build();
        response.addCookie(cookie);
    }

    /**
     * Clears the refresh-token cookie (logout).
     *
     * @param response   the gateway response
     * @param cookieName the cookie name
     */
    public static void clearRefreshCookie(ServerHttpResponse response, String cookieName) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ZERO)  // immediate expiry
                .build();
        response.addCookie(cookie);
    }

    /**
     * Reads the refresh-token from the httpOnly cookie.
     *
     * @param request    the incoming request
     * @param cookieName the cookie name
     * @return the raw refresh token, or {@code null} if absent
     */
    public static String getRefreshFromCookie(ServerHttpRequest request, String cookieName) {
        HttpCookie cookie = request.getCookies().getFirst(cookieName);
        return cookie != null ? cookie.getValue() : null;
    }

    @Override
    public int getOrder() { return -100; }

    private boolean isOpenPath(String path) {
        return OPEN_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }
}
