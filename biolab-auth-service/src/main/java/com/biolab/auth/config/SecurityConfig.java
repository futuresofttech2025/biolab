package com.biolab.auth.config;

import com.biolab.common.security.JwtAuthenticationFilter;
import com.biolab.common.security.SecurityHeadersFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for Auth Service.
 *
 * <h3>Architecture (Slide 5 &amp; 10):</h3>
 * <ul>
 *   <li>Auth endpoints ({@code /api/auth/**}) are publicly accessible</li>
 *   <li>Admin endpoints (user CRUD, roles, audit) require JWT via gateway headers</li>
 *   <li>BCrypt strength 12 for HIPAA-compliant password hashing</li>
 *   <li>{@link JwtAuthenticationFilter} extracts gateway headers for admin operations</li>
 *   <li>{@link SecurityHeadersFilter} adds OWASP security headers</li>
 *   <li>Method-level security via {@code @PreAuthorize} on controllers</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    @Value("${app.security.bcrypt-strength:12}")
    private int bcryptStrength;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Auth endpoints are public (login, register, password reset)
                .requestMatchers("/api/auth/login", "/api/auth/register",
                        "/api/auth/forgot-password", "/api/auth/reset-password",
                        "/api/auth/mfa/verify").permitAll()
                // Monitoring & documentation
                .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                // All other endpoints require authentication (admin operations)
                .anyRequest().authenticated()
            )
            // JWT filter: extracts X-User-* headers from gateway for admin endpoints
            .addFilterBefore(jwtAuthenticationFilter(),
                    UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(securityHeadersFilter(),
                    JwtAuthenticationFilter.class);

        return http.build();
    }

    /** BCrypt encoder — cost factor 12 for HIPAA compliance (Slide 10). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
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
