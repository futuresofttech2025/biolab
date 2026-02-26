package com.biolab.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit gateway routes for all BioLab microservices.
 * Each route strips the /api prefix and forwards to the service via Eureka.
 */
@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Auth Service — /api/auth/**
            .route("auth-service", r -> r
                .path("/api/auth/**")
                .filters(f -> f.stripPrefix(1)
                    .circuitBreaker(cb -> cb.setName("authServiceCB")))
                .uri("lb://biolab-auth-service"))

            // User Service — /api/users/**
            .route("user-service", r -> r
                .path("/api/users/**", "/api/organizations/**")
                .filters(f -> f.stripPrefix(1)
                    .circuitBreaker(cb -> cb.setName("userServiceCB")))
                .uri("lb://biolab-user-service"))

            // Catalog Service — /api/catalog/**
            .route("catalog-service", r -> r
                .path("/api/catalog/**")
                .filters(f -> f.stripPrefix(1)
                    .circuitBreaker(cb -> cb.setName("catalogServiceCB")))
                .uri("lb://biolab-catalog-service"))

            // Project Service — /api/projects/**
            .route("project-service", r -> r
                .path("/api/projects/**")
                .filters(f -> f.stripPrefix(1)
                    .circuitBreaker(cb -> cb.setName("projectServiceCB")))
                .uri("lb://biolab-project-service"))

            // Document Service — /api/documents/**
            .route("document-service", r -> r
                .path("/api/documents/**")
                .filters(f -> f.stripPrefix(1)
                    .circuitBreaker(cb -> cb.setName("documentServiceCB")))
                .uri("lb://biolab-document-service"))

            // Invoice Service — /api/invoices/**
            .route("invoice-service", r -> r
                .path("/api/invoices/**")
                .filters(f -> f.stripPrefix(1)
                    .circuitBreaker(cb -> cb.setName("invoiceServiceCB")))
                .uri("lb://biolab-invoice-service"))

            // Messaging Service — /api/messages/**
            .route("messaging-service", r -> r
                .path("/api/messages/**")
                .filters(f -> f.stripPrefix(1)
                    .circuitBreaker(cb -> cb.setName("messagingServiceCB")))
                .uri("lb://biolab-messaging-service"))

            // Notification Service — /api/notifications/**
            .route("notification-service", r -> r
                .path("/api/notifications/**")
                .filters(f -> f.stripPrefix(1)
                    .circuitBreaker(cb -> cb.setName("notificationServiceCB")))
                .uri("lb://biolab-notification-service"))

            // Audit Service — /api/audit/**
            .route("audit-service", r -> r
                .path("/api/audit/**")
                .filters(f -> f.stripPrefix(1)
                    .circuitBreaker(cb -> cb.setName("auditServiceCB")))
                .uri("lb://biolab-audit-service"))

            .build();
    }
}
