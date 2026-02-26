package com.biolab.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gateway operational metadata endpoint.
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/gateway")
@Tag(name = "API Gateway", description = "Gateway status and metadata")
public class GatewayInfoController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port:8080}")
    private int serverPort;

    private final Instant startupTime = Instant.now();

    @GetMapping("/info")
    @Operation(summary = "Get API Gateway info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", applicationName);
        info.put("description", "BioLab API Gateway — Spring Cloud Gateway");
        info.put("port", serverPort);
        info.put("startedAt", startupTime.toString());
        info.put("uptime", Duration.between(startupTime, Instant.now()).toString());
        info.put("features", Map.of(
            "jwtValidation", true,
            "rateLimiting", true,
            "circuitBreaker", true,
            "cors", true,
            "requestLogging", true
        ));
        info.put("status", "UP");
        return ResponseEntity.ok(info);
    }

    @GetMapping("/status")
    @Operation(summary = "Liveness check")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "biolab-api-gateway"
        ));
    }
}
