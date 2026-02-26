package com.biolab.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Centralized CORS configuration for the API Gateway.
 *
 * <p>Handles cross-origin requests from the React SPA frontend and
 * mobile clients. All CORS decisions are made at the gateway level —
 * downstream services do not need their own CORS configuration.</p>
 *
 * <h3>Configuration is externalized via properties:</h3>
 * <ul>
 *   <li>{@code app.cors.allowed-origins} — comma-separated origin URLs</li>
 *   <li>{@code app.cors.allowed-methods} — HTTP methods</li>
 *   <li>{@code app.cors.max-age} — preflight cache duration (seconds)</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    /**
     * Creates a reactive CORS filter applied to all gateway routes.
     *
     * @return the configured {@link CorsWebFilter}
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        config.setAllowedHeaders(List.of(allowedHeaders));
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(maxAge);

        // Expose custom headers to the browser
        config.setExposedHeaders(List.of(
            "X-Correlation-Id",
            "X-Auth-Error",
            "X-RateLimit-Remaining"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
