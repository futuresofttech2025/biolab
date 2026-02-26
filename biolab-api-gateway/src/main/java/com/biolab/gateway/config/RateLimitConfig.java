package com.biolab.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Redis-backed rate limiting configuration for the API Gateway.
 *
 * <h3>Configuration (Slide 10 — Network Security):</h3>
 * <ul>
 *   <li>Default: 100 requests/minute per IP (token bucket algorithm)</li>
 *   <li>Burst capacity: 120 requests (accommodates short spikes)</li>
 *   <li>Key resolution: by IP address (X-Forwarded-For aware)</li>
 *   <li>Authenticated users: keyed by user ID for fairer distribution</li>
 * </ul>
 *
 * <p>Rate limit state is stored in Redis for consistency across
 * multiple gateway instances in production.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Configuration
public class RateLimitConfig {

    /**
     * Default rate limiter: 100 requests/minute with burst to 120.
     * Uses Redis token bucket algorithm.
     *
     * @return configured {@link RedisRateLimiter}
     */
    @Bean
    public RedisRateLimiter defaultRateLimiter() {
        // replenishRate=100/min ÷ 60 ≈ 2/sec, burstCapacity=120
        return new RedisRateLimiter(2, 120, 1);
    }

    /**
     * Resolves rate limit key from the authenticated user ID or client IP.
     *
     * <p>Prefers {@code X-User-Id} header (set after JWT validation) for
     * authenticated requests. Falls back to client IP for unauthenticated
     * requests (login, register, public catalog).</p>
     *
     * @return the key resolver
     */
    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            // Fall back to client IP
            String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (ip != null && !ip.isBlank()) {
                return Mono.just("ip:" + ip.split(",")[0].trim());
            }
            return Mono.just("ip:" + exchange.getRequest()
                    .getRemoteAddress().getAddress().getHostAddress());
        };
    }
}
