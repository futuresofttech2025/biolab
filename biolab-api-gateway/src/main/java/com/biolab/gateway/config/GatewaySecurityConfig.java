package com.biolab.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

/**
 * Reactive Spring Security configuration for the API Gateway.
 *
 * <p>CORS is handled here via Spring Security's built-in cors() support,
 * which ensures the CORS filter runs before authentication — critical for
 * preflight OPTIONS requests (which carry no credentials).</p>
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public GatewaySecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                // Enable CORS using our CorsConfig bean — handles OPTIONS preflight correctly
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                // Permit everything — JWT auth enforced by JwtAuthenticationFilter (GlobalFilter)
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                )
                .build();
    }
}