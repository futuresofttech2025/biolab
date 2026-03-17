package com.biolab.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Backend-for-Frontend (BFF) filter — Sprint 1, GAP-04.
 *
 * <p>Intercepts four BFF paths and transforms the request/response to
 * implement the httpOnly cookie pattern for refresh tokens:</p>
 *
 * <ol>
 *   <li><b>/api/auth/bff/login</b>
 *       <ul>
 *         <li>Rewrites path to {@code /api/auth/login} and forwards to auth service</li>
 *         <li>On 200: extracts {@code refreshToken} from JSON response body,
 *             sets httpOnly cookie, returns response WITHOUT {@code refreshToken} field</li>
 *       </ul>
 *   </li>
 *   <li><b>/api/auth/bff/refresh-token</b>
 *       <ul>
 *         <li>Reads {@code biolab_rt} cookie from the request</li>
 *         <li>Rewrites path to {@code /api/auth/refresh-token} and injects
 *             {@code {"refreshToken":"..."}} body</li>
 *         <li>On 200: rotates the cookie with the new refresh token</li>
 *       </ul>
 *   </li>
 *   <li><b>/api/auth/bff/logout</b>
 *       <ul>
 *         <li>Reads cookie, injects it as request body, forwards to {@code /api/auth/logout}</li>
 *         <li>Always clears the cookie regardless of upstream response</li>
 *       </ul>
 *   </li>
 *   <li><b>/api/auth/bff/mfa/verify</b>
 *       <ul>
 *         <li>Forwards to {@code /api/auth/mfa/verify}</li>
 *         <li>On 200: sets cookie from {@code refreshToken} in response</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BffCookieFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper;

    @Value("${app.jwt.refresh-token-cookie-name:biolab_rt}")
    private String cookieName;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    private static final String BFF_LOGIN          = "/api/auth/bff/login";
    private static final String BFF_REFRESH        = "/api/auth/bff/refresh-token";
    private static final String BFF_LOGOUT         = "/api/auth/bff/logout";
    private static final String BFF_MFA_VERIFY     = "/api/auth/bff/mfa/verify";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (path.equals(BFF_LOGIN)) {
            return handleBffLogin(exchange, chain);
        }
        if (path.equals(BFF_REFRESH)) {
            return handleBffRefresh(exchange, chain);
        }
        if (path.equals(BFF_LOGOUT)) {
            return handleBffLogout(exchange, chain);
        }
        if (path.equals(BFF_MFA_VERIFY)) {
            return handleBffMfaVerify(exchange, chain);
        }

        return chain.filter(exchange);
    }

    // ── BFF Login ─────────────────────────────────────────────────────────

    private Mono<Void> handleBffLogin(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Rewrite path to the real auth endpoint
        ServerHttpRequest rewritten = exchange.getRequest().mutate()
                .path("/api/auth/login").build();

        return chain.filter(exchange.mutate()
                .request(rewritten)
                .response(new CookieSettingDecorator(exchange, exchange.getResponse(), true))
                .build());
    }

    // ── BFF Refresh ───────────────────────────────────────────────────────

    private Mono<Void> handleBffRefresh(ServerWebExchange exchange, GatewayFilterChain chain) {
        String rt = JwtAuthenticationFilter.getRefreshFromCookie(
                exchange.getRequest(), cookieName);

        if (rt == null || rt.isBlank()) {
            exchange.getResponse().setStatusCode(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Inject refresh token as request body
        String body;
        try {
            body = objectMapper.writeValueAsString(
                    java.util.Map.of("refreshToken", rt));
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

        ServerHttpRequest withBody = exchange.getRequest().mutate()
                .path("/api/auth/refresh-token")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                .build();

        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(new BodyReplacingRequest(withBody, Flux.just(buffer)))
                .response(new CookieSettingDecorator(exchange, exchange.getResponse(), true))
                .build();

        return chain.filter(modifiedExchange);
    }

    // ── BFF Logout ────────────────────────────────────────────────────────

    private Mono<Void> handleBffLogout(ServerWebExchange exchange, GatewayFilterChain chain) {
        String rt = JwtAuthenticationFilter.getRefreshFromCookie(
                exchange.getRequest(), cookieName);

        // Always clear the cookie, even if the upstream call fails
        JwtAuthenticationFilter.clearRefreshCookie(exchange.getResponse(), cookieName);

        if (rt == null || rt.isBlank()) {
            // No cookie — just return 204
            exchange.getResponse().setStatusCode(
                    org.springframework.http.HttpStatus.NO_CONTENT);
            return exchange.getResponse().setComplete();
        }

        String body;
        try {
            body = objectMapper.writeValueAsString(
                    java.util.Map.of("refreshToken", rt));
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(
                    org.springframework.http.HttpStatus.NO_CONTENT);
            return exchange.getResponse().setComplete();
        }

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

        ServerHttpRequest withBody = exchange.getRequest().mutate()
                .path("/api/auth/logout")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                .build();

        return chain.filter(exchange.mutate()
                .request(new BodyReplacingRequest(withBody, Flux.just(buffer)))
                .build());
    }

    // ── BFF MFA Verify ────────────────────────────────────────────────────

    private Mono<Void> handleBffMfaVerify(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest rewritten = exchange.getRequest().mutate()
                .path("/api/auth/mfa/verify").build();

        return chain.filter(exchange.mutate()
                .request(rewritten)
                .response(new CookieSettingDecorator(exchange, exchange.getResponse(), true))
                .build());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Response decorator: intercepts body to extract and set cookie
    // ─────────────────────────────────────────────────────────────────────

    private class CookieSettingDecorator extends ServerHttpResponseDecorator {
        private final ServerWebExchange exchange;
        private final boolean setCookie;

        CookieSettingDecorator(ServerWebExchange exchange,
                               ServerHttpResponse delegate,
                               boolean setCookie) {
            super(delegate);
            this.exchange  = exchange;
            this.setCookie = setCookie;
        }

        @Override
        public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
            if (!setCookie || !getStatusCode().is2xxSuccessful()) {
                return super.writeWith(body);
            }

            return DataBufferUtils.join(Flux.from(body)).flatMap(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);

                String responseBody = new String(bytes, StandardCharsets.UTF_8);

                try {
                    JsonNode json = objectMapper.readTree(responseBody);
                    JsonNode rtNode = json.get("refreshToken");

                    if (rtNode != null && !rtNode.isNull()) {
                        String refreshToken = rtNode.asText();
                        // Set httpOnly cookie on the response
                        JwtAuthenticationFilter.setRefreshCookie(
                                getDelegate(), refreshToken,
                                cookieName, refreshTokenExpirationMs);

                        // Remove refreshToken from the JSON body sent to the browser
                        com.fasterxml.jackson.databind.node.ObjectNode cleaned =
                                ((com.fasterxml.jackson.databind.node.ObjectNode) json)
                                        .deepCopy();
                        cleaned.remove("refreshToken");
                        responseBody = objectMapper.writeValueAsString(cleaned);
                        bytes = responseBody.getBytes(StandardCharsets.UTF_8);

                        // Update Content-Length
                        getHeaders().setContentLength(bytes.length);
                        log.debug("BFF: refresh token moved to httpOnly cookie");
                    }
                } catch (IOException e) {
                    log.warn("BFF: could not parse auth response body: {}", e.getMessage());
                }

                DataBuffer newBuffer = exchange.getResponse().bufferFactory().wrap(bytes);
                return super.writeWith(Mono.just(newBuffer));
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Request wrapper: replaces the request body
    // ─────────────────────────────────────────────────────────────────────

    private static class BodyReplacingRequest
            extends org.springframework.http.server.reactive.ServerHttpRequestDecorator {

        private final Flux<DataBuffer> bodyFlux;

        BodyReplacingRequest(ServerHttpRequest delegate, Flux<DataBuffer> bodyFlux) {
            super(delegate);
            this.bodyFlux = bodyFlux;
        }

        @Override
        public Flux<DataBuffer> getBody() { return bodyFlux; }
    }

    /** Runs before JWT filter (order -100) so BFF routes are handled first. */
    @Override
    public int getOrder() { return -200; }
}
