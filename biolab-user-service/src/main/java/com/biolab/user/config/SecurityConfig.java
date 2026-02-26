package com.biolab.user.config;

import com.biolab.common.security.JwtAuthenticationFilter;
import com.biolab.common.security.SecurityHeadersFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for User Service with JWT authentication
 * and method-level authorization.
 *
 * <h3>Security Architecture (Slide 5 &amp; 10):</h3>
 * <ol>
 *   <li>API Gateway validates JWT and forwards X-User-* headers</li>
 *   <li>{@link JwtAuthenticationFilter} extracts headers into Spring Security context</li>
 *   <li>{@code @PreAuthorize} on controllers enforces RBAC permissions</li>
 *   <li>{@link SecurityHeadersFilter} adds OWASP security headers</li>
 * </ol>
 *
 * <p>Actuator and Swagger endpoints remain open for monitoring and docs.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless — no HTTP session (JWT-based)
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Open endpoints
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // All API endpoints require authentication (from gateway headers)
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )

            // Register JWT authentication filter (extracts X-User-* headers)
            .addFilterBefore(jwtAuthenticationFilter(),
                    UsernamePasswordAuthenticationFilter.class)

            // Register security headers filter
            .addFilterAfter(securityHeadersFilter(),
                    JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }
}
