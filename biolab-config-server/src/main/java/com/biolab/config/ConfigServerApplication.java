package com.biolab.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * BioLab Config Server — Centralized Configuration Management.
 *
 * <p>Provides externalized configuration for all BioLabs microservices via
 * a native filesystem backend (with Git backend support for production).
 * Each service fetches its configuration on startup by querying:</p>
 *
 * <pre>
 *   GET http://config-server:8888/{application}/{profile}
 *   Example: GET /biolab-auth-service/dev
 * </pre>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Environment-specific configs (dev, staging, prod)</li>
 *   <li>Encrypted secret values via /encrypt and /decrypt endpoints</li>
 *   <li>Hot reload with Spring Cloud Bus + RabbitMQ (Phase 2)</li>
 *   <li>Registered with Eureka for discovery by client services</li>
 * </ul>
 *
 * <h3>Port:</h3> 8888
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 * @since 2026-02-15
 */
@SpringBootApplication
@EnableConfigServer
@EnableDiscoveryClient
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
