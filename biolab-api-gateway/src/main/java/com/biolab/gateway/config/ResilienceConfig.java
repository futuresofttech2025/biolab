package com.biolab.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit breaker configuration using Resilience4j.
 *
 * <p>Protects the gateway from cascading failures when downstream
 * microservices become unresponsive. Uses a sliding window to monitor
 * failure rates and automatically opens the circuit when thresholds
 * are exceeded.</p>
 *
 * <h3>Defaults:</h3>
 * <ul>
 *   <li>Failure rate threshold: 50%</li>
 *   <li>Sliding window size: 10 calls</li>
 *   <li>Wait in open state: 10 seconds</li>
 *   <li>Time limiter: 4 seconds per call</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Configuration
public class ResilienceConfig {

    /**
     * Configures the default circuit breaker factory for all gateway routes.
     *
     * @return the customizer for the reactive circuit breaker factory
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id ->
                new Resilience4JConfigBuilder(id)
                        .circuitBreakerConfig(CircuitBreakerConfig.custom()
                                // Open circuit when 50% of calls fail
                                .failureRateThreshold(50)
                                // Monitor last 10 calls in sliding window
                                .slidingWindowSize(10)
                                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                                // Wait 10 seconds before half-opening
                                .waitDurationInOpenState(Duration.ofSeconds(10))
                                // Allow 5 test calls in half-open state
                                .permittedNumberOfCallsInHalfOpenState(5)
                                // Minimum 5 calls before calculating failure rate
                                .minimumNumberOfCalls(5)
                                .build())
                        .timeLimiterConfig(TimeLimiterConfig.custom()
                                // 4-second timeout per downstream call
                                .timeoutDuration(Duration.ofSeconds(4))
                                .build())
                        .build());
    }
}
