package com.biolab.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Reactive Spring Security configuration for the API Gateway.
 *
 * <h3>Why this exists:</h3>
 * <p>{@code spring-cloud-starter-gateway-server-webflux} pulls in
 * {@code spring-security-web} as a transitive dependency. Without an explicit
 * {@link SecurityWebFilterChain} bean, Spring Boot's reactive security
 * auto-configuration creates a default chain that requires HTTP Basic
 * authentication for every request — blocking all traffic with 401 before
 * our custom {@link com.biolab.gateway.filter.JwtAuthenticationFilter}
 * (a {@code GlobalFilter}) even runs.</p>
 *
 * <h3>Security model:</h3>
 * <p>The gateway does <strong>not</strong> use Spring Security for JWT
 * authentication. Instead, {@link com.biolab.gateway.filter.JwtAuthenticationFilter}
 * is a {@code GlobalFilter} that:</p>
 * <ol>
 *   <li>Passes all {@code /api/auth/**} requests through unauthenticated</li>
 *   <li>Validates JWT Bearer tokens on all other paths</li>
 *   <li>Enriches forwarded requests with {@code X-User-*} headers for downstream services</li>
 * </ol>
 * <p>This class simply disables the default Spring Security behaviour so the
 * GlobalFilter can do its job.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    /**
     * Configures a permissive {@link SecurityWebFilterChain} that disables
     * all Spring Security authentication/authorization at the WebFlux layer.
     *
     * <p>JWT validation is handled downstream by
     * {@link com.biolab.gateway.filter.JwtAuthenticationFilter}.</p>
     *
     * @param http the reactive HTTP security builder
     * @return a fully permissive security filter chain
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                // Permit everything — JWT auth is enforced by JwtAuthenticationFilter (GlobalFilter)
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                )
                .build();
    }
}