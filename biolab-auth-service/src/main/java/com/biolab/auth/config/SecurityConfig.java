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
 * <p>All public auth endpoints are permitted via BOTH their full path
 * ({@code /api/auth/**}) and their stripped path ({@code /auth/**}).
 * This makes the service resilient to whether the API Gateway applies
 * {@code stripPrefix(1)} or forwards the full path as-is.</p>
 *
 * <h3>Architecture (Slide 5 &amp; 10):</h3>
 * <ul>
 *   <li>Auth endpoints ({@code /api/auth/**}) are publicly accessible</li>
 *   <li>Admin endpoints require JWT via gateway X-User-* headers</li>
 *   <li>BCrypt strength 12 for HIPAA-compliant password hashing</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 * @version 1.1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@org.springframework.scheduling.annotation.EnableAsync
public class SecurityConfig {

    @Value("${app.security.bcrypt-strength:12}")
    private int bcryptStrength;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Public auth endpoints ─────────────────────────────────────
                        // Both /api/auth/** (no stripPrefix) and /auth/** (with stripPrefix)
                        // are permitted so the service works regardless of gateway config.
                        .requestMatchers(
                                "/api/auth/login",          "/auth/login",
                                "/api/auth/register",       "/auth/register",
                                "/api/auth/forgot-password","/auth/forgot-password",
                                "/api/auth/reset-password", "/auth/reset-password",
                                "/api/auth/refresh-token",  "/auth/refresh-token",
                                "/api/auth/logout",         "/auth/logout",
                                "/api/auth/mfa/verify",         "/auth/mfa/verify",
                                "/api/auth/verify-email",       "/auth/verify-email",
                                "/api/auth/resend-verification","/auth/resend-verification"
                        ).permitAll()

                        // ── Monitoring & documentation ────────────────────────────────
                        .requestMatchers(
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // ── Everything else requires authentication ────────────────────
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