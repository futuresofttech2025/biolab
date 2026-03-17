package com.biolab.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that verifies the {@code X-Gateway-Token} header on every
 * inbound request, rejecting direct (non-gateway) calls.
 *
 * <h3>Sprint 3 — GAP-14: Service bypass prevention</h3>
 * <p>The API Gateway adds {@code X-Gateway-Token: <shared-secret>} to every
 * forwarded request (via {@code GatewayTokenFilter}). This filter verifies
 * that the token is present and matches the configured value, ensuring no
 * service receives requests that did not pass through the gateway.</p>
 *
 * <h3>Activation</h3>
 * <p>Enabled by setting {@code app.security.gateway-token-required=true} in
 * the service's application.yml (or via Config Server). Disabled by default so
 * that local development without the gateway still works.</p>
 *
 * <h3>Open paths</h3>
 * <p>Actuator health probes ({@code /actuator/health}) are excluded so
 * Kubernetes readiness/liveness probes continue to work without the gateway.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Component
@ConditionalOnProperty(name = "app.security.gateway-token-required", havingValue = "true")
public class GatewayTokenVerificationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayTokenVerificationFilter.class);

    /** Header name — must match GatewayTokenFilter.HEADER_GATEWAY_TOKEN. */
    private static final String HEADER_GATEWAY_TOKEN = "X-Gateway-Token";

    @Value("${app.gateway.internal-token}")
    private String expectedToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = request.getHeader(HEADER_GATEWAY_TOKEN);

        if (token == null || token.isBlank()) {
            log.warn("GATEWAY-TOKEN MISSING: {} {} from {}",
                    request.getMethod(), request.getRequestURI(),
                    request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Direct service access not permitted. Route through API Gateway.\"}");
            return;
        }

        // Constant-time comparison to prevent timing attacks
        if (!constantTimeEquals(token, expectedToken)) {
            log.warn("GATEWAY-TOKEN INVALID: {} {} from {} — possible direct access attempt",
                    request.getMethod(), request.getRequestURI(),
                    request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Invalid gateway token.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    /** Paths that bypass the gateway token check (health probes). */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") || path.startsWith("/actuator/info");
    }

    /**
     * Constant-time string comparison — prevents timing oracle attacks where
     * an attacker could infer the correct token by measuring response time.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) return false;
        int diff = 0;
        for (int i = 0; i < aBytes.length; i++) {
            diff |= aBytes[i] ^ bBytes[i];
        }
        return diff == 0;
    }
}
