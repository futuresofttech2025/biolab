package com.biolab.gateway.filter;

import com.biolab.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Global Gateway filter that validates JWT Bearer tokens on every request.
 *
 * <p>Intercepts all incoming requests, extracts the Authorization header,
 * validates the JWT token, and enriches downstream requests with user
 * context headers. Unauthenticated requests to protected routes receive
 * HTTP 401 Unauthorized.</p>
 *
 * <h3>Open (unauthenticated) routes:</h3>
 * <ul>
 *   <li>{@code /api/auth/**} — login, register, password reset</li>
 *   <li>{@code /api/categories} — public catalog browsing</li>
 *   <li>{@code /actuator/**} — health probes</li>
 *   <li>{@code /swagger-ui/**, /v3/api-docs/**} — API documentation</li>
 * </ul>
 *
 * <h3>Enriched Headers (forwarded to downstream services):</h3>
 * <pre>
 *   X-User-Id:    user UUID
 *   X-User-Email: user email
 *   X-User-Roles: comma-separated roles
 *   X-User-OrgId: organization UUID
 * </pre>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    /**
     * Paths that do NOT require JWT authentication.
     * Auth endpoints, public catalog, actuator probes, and docs.
     */
    private static final List<String> OPEN_PATHS = List.of(
        "/api/auth/",
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

    /**
     * Main filter logic: validates JWT and enriches the request.
     *
     * @param exchange the current server exchange
     * @param chain    the gateway filter chain
     * @return a {@link Mono} signaling filter completion
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip authentication for open paths
        if (isOpenPath(path)) {
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return onUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        // Extract and validate the JWT token
        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            return onUnauthorized(exchange, "Invalid or expired JWT token");
        }

        // Extract claims and enrich the request with user context headers
        try {
            Claims claims = jwtUtil.extractAllClaims(token);
            ServerHttpRequest enrichedRequest = request.mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Email", claims.get("email", String.class))
                    .header("X-User-Roles", String.join(",",
                            jwtUtil.extractRoles(token)))
                    .header("X-User-OrgId",
                            claims.get("orgId", String.class) != null
                                    ? claims.get("orgId", String.class)
                                    : "")
                    .build();

            log.debug("JWT validated for user: {} on path: {}",
                    claims.getSubject(), path);

            return chain.filter(exchange.mutate().request(enrichedRequest).build());

        } catch (Exception ex) {
            log.error("Error processing JWT token: {}", ex.getMessage());
            return onUnauthorized(exchange, "Token processing error");
        }
    }

    /**
     * Filter execution order — runs early in the chain (before routing).
     *
     * @return the filter order (lower = earlier)
     */
    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * Checks if the given path is in the open (unauthenticated) path list.
     *
     * @param path the request URI path
     * @return true if the path is open
     */
    private boolean isOpenPath(String path) {
        return OPEN_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Returns an HTTP 401 Unauthorized response.
     *
     * @param exchange the server exchange
     * @param message  the error message (logged, not sent to client)
     * @return a Mono that completes the response
     */
    private Mono<Void> onUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }
}
