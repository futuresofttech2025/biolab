package com.biolab.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global gateway filter that adds OWASP security headers to all responses.
 *
 * <h3>Headers Applied (Slide 10 — Network Security &amp; Input Validation):</h3>
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff}</li>
 *   <li>{@code X-Frame-Options: DENY}</li>
 *   <li>{@code Strict-Transport-Security: max-age=31536000}</li>
 *   <li>{@code Content-Security-Policy: default-src 'self'}</li>
 *   <li>{@code Referrer-Policy: strict-origin-when-cross-origin}</li>
 *   <li>{@code Permissions-Policy: camera=(), microphone=()}</li>
 *   <li>{@code Cache-Control: no-store} (for API responses)</li>
 * </ul>
 *
 * <p>Runs after JWT authentication (order = -2) to ensure headers
 * are applied to all responses, including error responses.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();

            headers.set("X-Content-Type-Options", "nosniff");
            headers.set("X-Frame-Options", "DENY");
            headers.set("X-XSS-Protection", "0");
            headers.set("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains; preload");
            headers.set("Content-Security-Policy",
                    "default-src 'self'; frame-ancestors 'none'");
            headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
            headers.set("Permissions-Policy",
                    "camera=(), microphone=(), geolocation=(), payment=()");

            // Prevent caching of API responses
            String path = exchange.getRequest().getPath().value();
            if (path.startsWith("/api/")) {
                headers.set("Cache-Control", "no-store, no-cache, must-revalidate");
                headers.set("Pragma", "no-cache");
            }
        }));
    }

    /** Runs after JWT filter but before response is sent. */
    @Override
    public int getOrder() { return -2; }
}
