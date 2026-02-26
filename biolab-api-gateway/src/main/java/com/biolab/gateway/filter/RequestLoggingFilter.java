package com.biolab.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Global filter that logs every request passing through the API Gateway.
 *
 * <p>Generates a unique correlation ID for each request and attaches it
 * to the downstream request headers. This enables end-to-end request
 * tracing across all microservices — critical for HIPAA audit trails.</p>
 *
 * <h3>Headers Added:</h3>
 * <ul>
 *   <li><b>X-Correlation-Id:</b> Unique UUID for distributed tracing</li>
 *   <li><b>X-Request-Timestamp:</b> ISO-8601 timestamp of gateway receipt</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    /**
     * Logs the request method, path, and client IP, then generates
     * a correlation ID and forwards it to the backend service.
     *
     * @param exchange the current server exchange
     * @param chain    the gateway filter chain
     * @return a Mono signaling completion
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = UUID.randomUUID().toString();
        Instant requestTime = Instant.now();

        // Log the incoming request
        log.info("Gateway Request  | {} {} | IP: {} | Correlation: {}",
                request.getMethod(),
                request.getURI().getPath(),
                request.getRemoteAddress() != null
                        ? request.getRemoteAddress().getAddress().getHostAddress()
                        : "unknown",
                correlationId
        );

        // Enrich request with tracing headers
        ServerHttpRequest enrichedRequest = request.mutate()
                .header("X-Correlation-Id", correlationId)
                .header("X-Request-Timestamp", requestTime.toString())
                .build();

        return chain.filter(exchange.mutate().request(enrichedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    // Log the response after processing
                    log.info("Gateway Response | {} {} | Status: {} | Correlation: {}",
                            request.getMethod(),
                            request.getURI().getPath(),
                            exchange.getResponse().getStatusCode(),
                            correlationId
                    );
                }));
    }

    /**
     * Runs before the JWT filter to ensure correlation ID is available
     * even for unauthenticated requests.
     *
     * @return the filter order
     */
    @Override
    public int getOrder() {
        return -200;
    }
}
