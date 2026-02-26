package com.biolab.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * BioLab Discovery Server — Netflix Eureka Service Registry.
 *
 * <p>Central hub for microservice registration and discovery within the
 * BioLabs Services Hub ecosystem. Every microservice registers its network
 * location on startup and discovers peer services through this registry.</p>
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Service instance registration and heartbeat monitoring (30s default)</li>
 *   <li>Unhealthy instance eviction (90s without heartbeat)</li>
 *   <li>Client-side load-balanced lookups via Eureka REST API</li>
 *   <li>Web dashboard for operational visibility at /</li>
 * </ul>
 *
 * <h3>Port:</h3> 8761 (default Eureka port)
 * <h3>Dashboard:</h3> http://localhost:8761 (secured via HTTP Basic)
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 * @since 2026-02-15
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
