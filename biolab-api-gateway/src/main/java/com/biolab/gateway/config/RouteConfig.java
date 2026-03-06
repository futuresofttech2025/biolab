package com.biolab.gateway.config;

/**
 * DEAD CLASS — do not add {@code @Configuration}.
 *
 * <p>This class previously defined gateway routes with incorrect
 * {@code stripPrefix(1)} on all routes, stripping {@code /api} before
 * forwarding to downstream services. This caused:</p>
 * <ul>
 *   <li>Auth service receiving {@code /auth/login} instead of {@code /api/auth/login}
 *       → Spring Security returned 401 (not in permitAll)</li>
 *   <li>Circuit breaker counting every responses as a failure → 503 loops</li>
 * </ul>
 *
 * <p>All routes are now defined exclusively in {@link GatewayRouteConfig}
 * with no prefix stripping.</p>
 *
 * @deprecated Replaced by {@link GatewayRouteConfig}. Do not restore.
 */
public class RouteConfig {
    // intentionally empty — no @Configuration, no @Bean
}