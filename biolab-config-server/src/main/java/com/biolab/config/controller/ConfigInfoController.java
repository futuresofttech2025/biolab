package com.biolab.config.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
 * REST controller exposing Config Server operational metadata.
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/config")
@Tag(name = "Config Server", description = "Configuration server status and metadata")
public class ConfigInfoController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port}")
    private int serverPort;

    @Value("${spring.cloud.config.server.native.search-locations:classpath:/config-repo}")
    private String configLocation;

    private final Instant startupTime = Instant.now();

    /**
     * Returns Config Server metadata including backend location and uptime.
     *
     * @return 200 OK with server metadata
     */
    @GetMapping("/info")
    @Operation(
        summary = "Get Config Server info",
        description = "Returns metadata about the Config Server instance and backend",
        responses = @ApiResponse(responseCode = "200", description = "Metadata returned")
    )
    public ResponseEntity<Map<String, Object>> getServerInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", applicationName);
        info.put("description", "BioLab Centralized Config Server");
        info.put("port", serverPort);
        info.put("configBackend", configLocation);
        info.put("startedAt", startupTime.toString());
        info.put("uptime", Duration.between(startupTime, Instant.now()).toString());
        info.put("timestamp", Instant.now().toString());
        info.put("status", "UP");
        return ResponseEntity.ok(info);
    }

    /**
     * Liveness probe for container orchestration.
     *
     * @return 200 OK
     */
    @GetMapping("/status")
    @Operation(summary = "Liveness check")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "biolab-config-server"
        ));
    }
}
