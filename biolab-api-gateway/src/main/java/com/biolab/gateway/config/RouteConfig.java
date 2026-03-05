package com.biolab.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

/**
 * DISABLED — All routes are defined in {@link GatewayRouteConfig}.
 *
 * <p>This class was incorrectly applying {@code stripPrefix(1)} to ALL routes,
 * which stripped the {@code /api} segment before forwarding to downstream services.
 * Since every microservice controller is mapped at {@code /api/**}, stripping
 * the prefix caused requests to miss the Spring Security {@code permitAll} rules
 * and controller mappings — resulting in 403 or 405 responses.</p>
 *
 * <p>{@code @Configuration} is intentionally removed. Do not re-add it.</p>
 *
 * @deprecated Replaced by {@link GatewayRouteConfig}
 */
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        // This bean is never loaded — @Configuration is removed from class.
        return builder.routes().build();
    }
}