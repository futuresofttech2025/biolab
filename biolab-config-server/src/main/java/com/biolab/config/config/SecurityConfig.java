package com.biolab.config.config;

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
 * Security configuration for the Config Server.
 *
 * <p>All configuration endpoints are protected with HTTP Basic authentication.
 * Microservices must supply credentials in their bootstrap configuration
 * to fetch their properties.</p>
 *
 * <h3>Access Matrix:</h3>
 * <pre>
 *   /actuator/health, /actuator/info    → Public  (probes)
 *   /v3/api-docs/**, /swagger-ui/**     → Public  (docs)
 *   /{application}/{profile}            → Authenticated (config fetch)
 *   /encrypt, /decrypt                  → Authenticated (secret management)
 * </pre>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.config.username:configuser}")
    private String configUsername;

    @Value("${app.config.password:configpass}")
    private String configPassword;

    /**
     * Configures HTTP security with Basic auth for config endpoints.
     *
     * <p>CSRF is disabled since all consumers are microservice REST clients.</p>
     *
     * @param http the {@link HttpSecurity} builder
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                // Public: health probes and monitoring
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/prometheus"
                ).permitAll()

                // Public: Swagger / OpenAPI
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // All config endpoints require authentication
                .anyRequest().authenticated()
            )

            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    /**
     * In-memory user for config server access.
     *
     * @return the configured {@link UserDetailsService}
     */
    @Bean
    public UserDetailsService userDetailsService() {
        var configUser = User.builder()
                .username(configUsername)
                .password(passwordEncoder().encode(configPassword))
                .roles("CONFIG")
                .build();
        return new InMemoryUserDetailsManager(configUser);
    }

    /** BCrypt encoder. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
