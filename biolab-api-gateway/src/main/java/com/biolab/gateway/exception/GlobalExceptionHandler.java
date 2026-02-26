package com.biolab.gateway.exception;

import com.biolab.gateway.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Global exception handler for the reactive API Gateway.
 *
 * <p>Catches all unhandled exceptions and returns a standardized
 * {@link ErrorResponse} JSON body instead of default Spring error pages.
 * This ensures consistent error format across all gateway responses.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Component
@Order(-1)  // Higher priority than default error handler
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handles all unhandled exceptions from the reactive pipeline.
     *
     * @param exchange the current server exchange
     * @param ex       the caught exception
     * @return a Mono completing the error response
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "An unexpected error occurred";

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
        }

        log.error("Gateway error on {} {}: {} ({})",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(),
                message, ex.getClass().getSimpleName());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(exchange.getRequest().getURI().getPath())
                .build();

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException jpe) {
            log.error("Failed to serialize error response", jpe);
            return exchange.getResponse().setComplete();
        }
    }
}
