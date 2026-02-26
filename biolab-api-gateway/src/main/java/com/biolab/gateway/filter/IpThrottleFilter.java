package com.biolab.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory IP throttle filter for brute-force protection on auth endpoints.
 *
 * <h3>Protection (Slide 10 — Network Security):</h3>
 * <ul>
 *   <li>Limits failed auth requests to 20 per IP per 5-minute window</li>
 *   <li>Returns 429 Too Many Requests when threshold exceeded</li>
 *   <li>Production: Replace with Redis-backed implementation</li>
 * </ul>
 *
 * <p>This provides an additional layer beyond the general rate limiter,
 * specifically targeting authentication endpoints to prevent credential
 * stuffing and brute-force attacks.</p>
 *
 * @author BioLab Engineering Team
 */
@Component
@Slf4j
public class IpThrottleFilter implements GlobalFilter, Ordered {

    private static final int MAX_AUTH_ATTEMPTS = 20;
    private static final long WINDOW_MS = 300_000; // 5 minutes

    /** In-memory store — replace with Redis in production. */
    private final Map<String, WindowCounter> authAttempts = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Only throttle authentication endpoints
        if (!path.startsWith("/api/auth/login") && !path.startsWith("/api/auth/register")) {
            return chain.filter(exchange);
        }

        String clientIp = getClientIp(exchange);
        WindowCounter counter = authAttempts.computeIfAbsent(clientIp, k -> new WindowCounter());

        if (counter.isExceeded()) {
            log.warn("IP throttle: {} exceeded {} auth attempts in 5min window", clientIp, MAX_AUTH_ATTEMPTS);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().set("Retry-After", "300");
            return exchange.getResponse().setComplete();
        }

        counter.increment();
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() { return -5; } // Before JWT filter

    private String getClientIp(ServerWebExchange exchange) {
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        var addr = exchange.getRequest().getRemoteAddress();
        return addr != null ? addr.getAddress().getHostAddress() : "unknown";
    }

    /** Simple sliding window counter. */
    private static class WindowCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        void increment() {
            resetIfExpired();
            count.incrementAndGet();
        }

        boolean isExceeded() {
            resetIfExpired();
            return count.get() >= MAX_AUTH_ATTEMPTS;
        }

        private void resetIfExpired() {
            if (System.currentTimeMillis() - windowStart > WINDOW_MS) {
                count.set(0);
                windowStart = System.currentTimeMillis();
            }
        }
    }
}
