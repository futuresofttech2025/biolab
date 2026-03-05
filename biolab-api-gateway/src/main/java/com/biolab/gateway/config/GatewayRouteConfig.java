package com.biolab.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic route configuration for the API Gateway.
 *
 * <p><strong>No {@code stripPrefix} is used on any route.</strong>
 * Every downstream microservice controller is mapped at {@code /api/**},
 * so the full path must be forwarded as-is. Stripping {@code /api} would
 * cause requests to bypass Spring Security {@code permitAll} rules and
 * miss controller mappings — producing 403 or 405 responses.</p>
 *
 * <h3>Route Table:</h3>
 * <pre>
 *   /api/auth/**          → BIOLAB-AUTH-SERVICE    (login, register, password reset)
 *   /api/admin/tokens/**  → BIOLAB-AUTH-SERVICE    (admin token management)
 *   /api/users/**         → BIOLAB-USER-SERVICE
 *   /api/organizations/** → BIOLAB-USER-SERVICE
 *   /api/roles/**         → BIOLAB-USER-SERVICE
 *   /api/services/**      → BIOLAB-CATALOG-SERVICE
 *   /api/categories/**    → BIOLAB-CATALOG-SERVICE
 *   /api/service-requests/** → BIOLAB-CATALOG-SERVICE
 *   /api/projects/**      → BIOLAB-PROJECT-SERVICE
 *   /api/documents/**     → BIOLAB-DOCUMENT-SERVICE
 *   /api/invoices/**      → BIOLAB-INVOICE-SERVICE
 *   /api/conversations/** → BIOLAB-MESSAGING-SERVICE
 *   /api/notifications/** → BIOLAB-NOTIFICATION-SERVICE
 *   /api/audit/**         → BIOLAB-AUDIT-SERVICE
 *   /api/compliance/**    → BIOLAB-AUDIT-SERVICE
 * </pre>
 *
 * @author BioLab Engineering Team
 * @version 1.1.0
 */
@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator biolabRoutes(RouteLocatorBuilder builder) {
        return builder.routes()

                // ─── Auth Service ─────────────────────────────────────────────
                // No stripPrefix — AuthController is mapped at /api/auth/**
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("authServiceCB")
                                        .setFallbackUri("forward:/fallback/auth")))
                        .uri("lb://BIOLAB-AUTH-SERVICE"))

                // ─── Admin Token Management (Auth Service) ────────────────────
                .route("admin-token-service", r -> r
                        .path("/api/admin/tokens/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("authServiceCB")
                                        .setFallbackUri("forward:/fallback/auth")))
                        .uri("lb://BIOLAB-AUTH-SERVICE"))

                // ─── User Service ─────────────────────────────────────────────
                .route("user-service", r -> r
                        .path("/api/users/**", "/api/organizations/**", "/api/roles/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("userServiceCB")
                                        .setFallbackUri("forward:/fallback/service")))
                        .uri("lb://BIOLAB-USER-SERVICE"))

                // ─── Catalog Service ──────────────────────────────────────────
                .route("catalog-service", r -> r
                        .path("/api/services/**", "/api/categories/**", "/api/service-requests/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("catalogServiceCB")
                                        .setFallbackUri("forward:/fallback/service")))
                        .uri("lb://BIOLAB-CATALOG-SERVICE"))

                // ─── Project Service ──────────────────────────────────────────
                .route("project-service", r -> r
                        .path("/api/projects/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("projectServiceCB")
                                        .setFallbackUri("forward:/fallback/service")))
                        .uri("lb://BIOLAB-PROJECT-SERVICE"))

                // ─── Document Service ─────────────────────────────────────────
                .route("document-service", r -> r
                        .path("/api/documents/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("documentServiceCB")
                                        .setFallbackUri("forward:/fallback/service")))
                        .uri("lb://BIOLAB-DOCUMENT-SERVICE"))

                // ─── Invoice Service ──────────────────────────────────────────
                .route("invoice-service", r -> r
                        .path("/api/invoices/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("invoiceServiceCB")
                                        .setFallbackUri("forward:/fallback/service")))
                        .uri("lb://BIOLAB-INVOICE-SERVICE"))

                // ─── Messaging Service ────────────────────────────────────────
                .route("messaging-service", r -> r
                        .path("/api/conversations/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("messagingServiceCB")
                                        .setFallbackUri("forward:/fallback/service")))
                        .uri("lb://BIOLAB-MESSAGING-SERVICE"))

                // ─── Notification Service ─────────────────────────────────────
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("notificationServiceCB")
                                        .setFallbackUri("forward:/fallback/service")))
                        .uri("lb://BIOLAB-NOTIFICATION-SERVICE"))

                // ─── Audit Service ────────────────────────────────────────────
                .route("audit-service", r -> r
                        .path("/api/audit/**", "/api/compliance/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("auditServiceCB")
                                        .setFallbackUri("forward:/fallback/service")))
                        .uri("lb://BIOLAB-AUDIT-SERVICE"))

                .build();
    }
}