package com.biolab.gateway.controller;

import com.biolab.gateway.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

/**
 * Circuit breaker fallback controller.
 *
 * <p>When a downstream microservice is unavailable or the circuit breaker
 * is open, the gateway routes the request here instead. Returns a
 * standardized error response with HTTP 503 Service Unavailable.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
@Tag(name = "Fallback", description = "Circuit breaker fallback endpoints")
public class FallbackController {

    /**
     * Fallback for the Auth Service.
     *
     * @param exchange the current server exchange
     * @return 503 Service Unavailable with error details
     */
    @GetMapping("/auth")
    @Operation(summary = "Auth Service fallback", description = "Returned when Auth Service is unavailable")
    public ResponseEntity<ErrorResponse> authFallback(ServerWebExchange exchange) {
        log.warn("Auth Service unavailable — circuit breaker open for path: {}",
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
     * Generic fallback for all other services.
     *
     * @param exchange the current server exchange
     * @return 503 Service Unavailable with error details
     */
    @GetMapping("/service")
    @Operation(summary = "Generic Service fallback", description = "Returned when a backend service is unavailable")
    public ResponseEntity<ErrorResponse> serviceFallback(ServerWebExchange exchange) {
        log.warn("Backend service unavailable — circuit breaker open for path: {}",
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
