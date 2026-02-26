package com.biolab.discovery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Eureka Discovery Server.
 *
 * <p>Protects the Eureka dashboard and registration REST API with HTTP Basic
 * authentication. All microservices must supply credentials when registering.</p>
 *
 * <h3>Access Matrix:</h3>
 * <pre>
 *   /actuator/health, /actuator/info    → Public  (K8s probes, ALB health checks)
 *   /actuator/prometheus                → Public  (Prometheus scraping)
 *   /v3/api-docs/**, /swagger-ui/**     → Public  (Developer documentation)
 *   /eureka/**                          → Authenticated (service registration)
 *   / (dashboard)                       → Authenticated (admin visibility)
 * </pre>
 *
 * <p><b>Production Note:</b> Replace in-memory credentials with Vault-backed
 * secrets or environment variables via {@code EUREKA_USERNAME} / {@code EUREKA_PASSWORD}.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.eureka.username:admin}")
    private String eurekaUsername;

    @Value("${app.eureka.password:admin}")
    private String eurekaPassword;

    /**
     * Configures the HTTP security filter chain for the Eureka server.
     *
     * <p>CSRF is disabled because Eureka clients use REST calls (not browser forms)
     * for service registration. HTTP Basic is used for compatibility with the
     * standard Eureka client library configuration.</p>
     *
     * @param http the {@link HttpSecurity} builder
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — Eureka clients register via REST, not browser forms
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                // Public: health probes and monitoring
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/prometheus"
                ).permitAll()

                // Public: Swagger / OpenAPI documentation
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // HTTP Basic auth for Eureka dashboard and client registration
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    /**
     * In-memory user details for Eureka authentication.
     *
     * <p>Credentials are sourced from application properties and can be
     * overridden with environment variables in production environments.</p>
     *
     * @return the configured {@link UserDetailsService}
     */
    @Bean
    public UserDetailsService userDetailsService() {
        var admin = User.builder()
                .username(eurekaUsername)
                .password(passwordEncoder().encode(eurekaPassword))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    /**
     * BCrypt password encoder with default strength factor (10 rounds).
     *
     * @return the {@link PasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
