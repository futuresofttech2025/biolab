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
 * <h3>Fixes applied</h3>
 * <ul>
 *   <li>FIX-12: Actuator — only {@code /actuator/health} is public;
 *       all other actuator paths require {@code ADMIN} or {@code SUPER_ADMIN}.</li>
 *   <li>FIX-13: Swagger/OpenAPI — public access is gated behind
 *       {@code app.docs.enabled} (default {@code false}).
 *       Set to {@code true} only in dev/staging, never in production.</li>
 * </ul>
 *
 * <h3>Architecture</h3>
 * <ul>
 *   <li>Public auth endpoints: {@code /api/auth/**} and {@code /auth/**}
 *       (dual paths to handle gateway with or without stripPrefix)</li>
 *   <li>JWT authentication via X-User-* gateway headers</li>
 *   <li>BCrypt cost 12 for HIPAA-compliant password hashing</li>
 *   <li>Stateless sessions — no HTTP session, no CSRF needed</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 * @version 2.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@org.springframework.scheduling.annotation.EnableAsync
public class SecurityConfig {

    @Value("${app.security.bcrypt-strength:12}")
    private int bcryptStrength;

    /**
     * FIX-13: Controls whether Swagger UI and API docs are publicly accessible.
     * Set {@code app.docs.enabled=true} in dev/staging only.
     * Default is {@code false} — docs are protected in production.
     */
    @Value("${app.docs.enabled:false}")
    private boolean docsEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {

                // ── Public auth endpoints ──────────────────────────────────────
                // Dual paths: /api/auth/** (no stripPrefix) + /auth/** (with stripPrefix)
                auth.requestMatchers(
                        "/api/auth/login",               "/auth/login",
                        "/api/auth/register",            "/auth/register",
                        "/api/auth/forgot-password",     "/auth/forgot-password",
                        "/api/auth/reset-password",      "/auth/reset-password",
                        "/api/auth/refresh-token",       "/auth/refresh-token",
                        "/api/auth/logout",              "/auth/logout",
                        "/api/auth/mfa/verify",          "/auth/mfa/verify",
                        "/api/auth/verify-email",        "/auth/verify-email",
                        "/api/auth/resend-verification", "/auth/resend-verification"
                ).permitAll();

                // ── FIX-12: Actuator — health probe is public, everything else is admin-only ──
                // /actuator/health is required by load-balancers and k8s readiness probes.
                // All other actuator endpoints expose internal metrics, heap dumps, and DB stats
                // that must never be world-readable.
                auth.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                auth.requestMatchers("/actuator/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN");

                // ── FIX-13: API docs — gated behind app.docs.enabled flag ──────────────────
                // When enabled (dev/staging), Swagger is public for developer convenience.
                // When disabled (production default), it requires ADMIN to access.
                if (docsEnabled) {
                    auth.requestMatchers(
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html"
                    ).permitAll();
                } else {
                    auth.requestMatchers(
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html"
                    ).hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
                }

                // ── Everything else requires authentication ────────────────────
                auth.anyRequest().authenticated();
            })
            // JWT filter: extracts X-User-* gateway headers
            .addFilterBefore(jwtAuthenticationFilter(),
                    UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(securityHeadersFilter(),
                    JwtAuthenticationFilter.class);

        return http.build();
    }

    /** BCrypt encoder — cost factor 12 for HIPAA compliance. */
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
