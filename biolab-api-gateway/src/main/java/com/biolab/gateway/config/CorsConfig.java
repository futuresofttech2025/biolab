package com.biolab.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Centralized CORS configuration for the API Gateway.
 *
 * <h3>Why both CorsConfigurationSource and CorsWebFilter?</h3>
 * <p>{@code CorsConfigurationSource} is injected into Spring Security's
 * {@code ServerHttpSecurity.cors()} so that CORS headers are added during
 * the security filter phase — before any authentication check. This is
 * essential for preflight OPTIONS requests, which browsers send without
 * credentials and which must receive a 200 + CORS headers, never a 401.</p>
 *
 * <p>{@code CorsWebFilter} additionally handles CORS for routes that bypass
 * the security filter chain entirely.</p>
 *
 * <h3>allowedHeaders: no wildcard with credentials</h3>
 * <p>When {@code allowCredentials=true}, the CORS spec prohibits
 * {@code Access-Control-Allow-Headers: *} — browsers treat it as a literal
 * asterisk and reject any custom header. All headers must be listed explicitly.</p>
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    /**
     * Shared CORS configuration source — used by both Spring Security and CorsWebFilter.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Origins — split comma-separated list
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));

        // Methods — split comma-separated list
        config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));

        // Explicit headers — wildcard forbidden when credentials=true
        config.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "X-Request-ID",
                "Accept",
                "Cache-Control",
                "Origin",
                "Cookie"
        ));

        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(maxAge);

        // Headers the browser JS can read from the response
        config.setExposedHeaders(List.of(
                "X-Correlation-Id",
                "X-Auth-Error",
                "X-RateLimit-Remaining",
                "Set-Cookie"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * CorsWebFilter applies CORS for requests that bypass the security chain.
     */
    @Bean
    public CorsWebFilter corsWebFilter(CorsConfigurationSource corsConfigurationSource) {
        return new CorsWebFilter(corsConfigurationSource);
    }
}