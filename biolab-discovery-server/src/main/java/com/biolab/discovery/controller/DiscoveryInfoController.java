package com.biolab.discovery.controller;

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
 * REST controller exposing Discovery Server operational metadata.
 *
 * <p>Provides lightweight endpoints for monitoring tools, CI/CD health gates,
 * and the BioLab admin dashboard to verify registry health.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/discovery")
@Tag(name = "Discovery Server", description = "Eureka registry status and metadata")
public class DiscoveryInfoController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port}")
    private int serverPort;

    /** Captures server startup time for uptime calculation. */
    private final Instant startupTime = Instant.now();

    /**
     * Returns Discovery Server metadata: name, port, uptime, and timestamp.
     *
     * <p>Used by monitoring dashboards and CI/CD pipeline health gates.</p>
     *
     * @return 200 OK with server metadata JSON
     */
    @GetMapping("/info")
    @Operation(
        summary = "Get Discovery Server info",
        description = "Returns runtime metadata about this Eureka server instance",
        responses = @ApiResponse(responseCode = "200", description = "Server metadata returned")
    )
    public ResponseEntity<Map<String, Object>> getServerInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", applicationName);
        info.put("description", "BioLab Eureka Discovery Server");
        info.put("port", serverPort);
        info.put("startedAt", startupTime.toString());
        info.put("uptime", Duration.between(startupTime, Instant.now()).toString());
        info.put("timestamp", Instant.now().toString());
        info.put("status", "UP");
        return ResponseEntity.ok(info);
    }

    /**
     * Simple liveness probe for Kubernetes and load balancers.
     *
     * @return 200 OK with status "UP"
     */
    @GetMapping("/status")
    @Operation(
        summary = "Liveness check",
        description = "Returns 200 if the server is alive — used by K8s liveness probes"
    )
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "biolab-discovery-server"
        ));
    }
}
