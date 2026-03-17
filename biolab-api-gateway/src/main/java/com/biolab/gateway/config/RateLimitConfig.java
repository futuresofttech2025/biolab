package com.biolab.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Redis-backed rate limiting configuration for the API Gateway.
 *
 * <p>Only activated when {@code app.rate-limit.enabled=true} (default: true in prod).
 * Set {@code app.rate-limit.enabled=false} in local dev to run without Redis.</p>
 */
@Configuration
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitConfig {

    @Bean
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(2, 120, 1);
    }

    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (ip != null && !ip.isBlank()) {
                return Mono.just("ip:" + ip.split(",")[0].trim());
            }
            return Mono.just("ip:" + Objects.requireNonNull(exchange.getRequest()
                    .getRemoteAddress()).getAddress().getHostAddress());
        };
    }
}