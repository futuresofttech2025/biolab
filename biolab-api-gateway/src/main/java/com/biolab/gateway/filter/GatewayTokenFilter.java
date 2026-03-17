package com.biolab.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that adds a shared gateway token to every forwarded request.
 *
 * <h3>Sprint 3 — GAP-14: Service-to-gateway trust verification</h3>
 * <p>Adds {@code X-Gateway-Token} to every downstream request so services can
 * verify the call arrived through the gateway and not via a direct bypass.</p>
 *
 * <h3>Configuration</h3>
 * <pre>
 *   # Production — set a strong random value:
 *   GATEWAY_INTERNAL_TOKEN=$(openssl rand -base64 32)
 *
 *   # Local dev — omit the variable entirely; the filter sends an empty
 *   # header and services must have gateway-token-required=false (default).
 * </pre>
 *
 * @author BioLab Engineering Team
 * @version 1.1.0
 */
@Component
@Slf4j
public class GatewayTokenFilter implements GlobalFilter, Ordered {

    /**
     * Shared secret injected into every forwarded request.
     * Defaults to an empty string so the gateway starts cleanly in local dev
     * without GATEWAY_INTERNAL_TOKEN being set.
     * In production this MUST be a strong random value — the downstream
     * GatewayTokenVerificationFilter rejects requests with a blank token
     * when app.security.gateway-token-required=true.
     */
    @Value("${app.gateway.internal-token:}")
    private String gatewayInternalToken;

    public static final String HEADER_GATEWAY_TOKEN = "X-Gateway-Token";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Only add the header when a token is configured.
        // An empty token in local dev simply means the header is omitted —
        // services with gateway-token-required=false (dev default) are unaffected.
        if (gatewayInternalToken == null || gatewayInternalToken.isBlank()) {
            log.debug("GATEWAY_INTERNAL_TOKEN not configured — skipping X-Gateway-Token header (dev mode)");
            return chain.filter(exchange);
        }

        ServerHttpRequest enriched = exchange.getRequest().mutate()
                .header(HEADER_GATEWAY_TOKEN, gatewayInternalToken)
                .build();

        return chain.filter(exchange.mutate().request(enriched).build());
    }

    @Override
    public int getOrder() { return -50; }
}