package com.biolab.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * BioLab API Gateway — Central Entry Point for All Client Requests.
 *
 * <p>Built on Spring Cloud Gateway (reactive/WebFlux stack), this service
 * acts as the single ingress point for the BioLabs Services Hub platform.
 * All client traffic flows through this gateway before reaching backend
 * microservices.</p>
 *
 * <h3>Key Responsibilities:</h3>
 * <ul>
 *   <li><b>JWT Validation:</b> Verifies access tokens on every request</li>
 *   <li><b>Route Management:</b> Maps URI paths to backend services via Eureka</li>
 *   <li><b>Rate Limiting:</b> Token-bucket algorithm backed by Redis</li>
 *   <li><b>CORS Handling:</b> Centralized cross-origin configuration</li>
 *   <li><b>Circuit Breaking:</b> Resilience4j fallbacks for degraded services</li>
 *   <li><b>Request Logging:</b> Audit-grade request/response tracing</li>
 * </ul>
 *
 * <h3>Port:</h3> 8080
 * <h3>Tech:</h3> WebFlux (Netty), non-blocking reactive pipeline
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 * @since 2026-02-15
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
