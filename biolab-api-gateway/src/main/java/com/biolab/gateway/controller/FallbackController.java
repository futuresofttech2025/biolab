package com.biolab.gateway.controller;

import com.biolab.gateway.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

/**
 * Circuit breaker fallback controller.
 *
 * <p>When a downstream microservice is unavailable or the circuit breaker
 * is open, the gateway forwards the request here and returns a standardised
 * 503 Service Unavailable response.</p>
 *
 * <p><strong>Important:</strong> {@code @RequestMapping} (not {@code @GetMapping})
 * is used on both endpoints so that POST, PUT, DELETE, and PATCH requests
 * forwarded by the circuit breaker are handled correctly. Using {@code @GetMapping}
 * would cause Spring to return 405 Method Not Allowed for any non-GET fallback
 * (e.g. {@code POST /api/auth/reset-password} triggering the auth circuit breaker).</p>
 *
 * @author BioLab Engineering Team
 * @version 1.1.0
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
@Tag(name = "Fallback", description = "Circuit breaker fallback endpoints")
public class FallbackController {

    /**
     * Fallback for the Auth Service circuit breaker.
     * Handles ALL HTTP methods — GET, POST, PUT, PATCH, DELETE.
     *
     * @param exchange the current server exchange
     * @return 503 Service Unavailable
     */
    @RequestMapping("/auth")
    @Operation(summary = "Auth Service fallback — circuit breaker open")
    public ResponseEntity<ErrorResponse> authFallback(ServerWebExchange exchange) {
        log.warn("Auth Service unavailable — circuit breaker open for: {} {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.builder()
                        .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .error("SERVICE_UNAVAILABLE")
                        .message("Authentication service is temporarily unavailable. Please try again shortly.")
                        .path(exchange.getRequest().getURI().getPath())
                        .build());
    }

    /**
     * Generic fallback for all other service circuit breakers.
     * Handles ALL HTTP methods — GET, POST, PUT, PATCH, DELETE.
     *
     * @param exchange the current server exchange
     * @return 503 Service Unavailable
     */
    @RequestMapping("/service")
    @Operation(summary = "Generic service fallback — circuit breaker open")
    public ResponseEntity<ErrorResponse> serviceFallback(ServerWebExchange exchange) {
        log.warn("Backend service unavailable — circuit breaker open for: {} {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.builder()
                        .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .error("SERVICE_UNAVAILABLE")
                        .message("The requested service is temporarily unavailable. Please try again shortly.")
                        .path(exchange.getRequest().getURI().getPath())
                        .build());
    }
}