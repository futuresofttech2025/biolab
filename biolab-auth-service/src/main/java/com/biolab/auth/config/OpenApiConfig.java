package com.biolab.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * OpenAPI 3.0 / Swagger configuration.
 *
 * <h3>FIX-13 — Production guard</h3>
 * <p>This bean is restricted to the {@code !prod} Spring profile.
 * When the application runs with {@code spring.profiles.active=prod},
 * Spring will not instantiate this bean, which means the Swagger UI
 * and {@code /v3/api-docs} endpoint return 404.</p>
 *
 * <p>In {@link SecurityConfig}, the {@code app.docs.enabled} flag provides
 * a complementary runtime guard — both layers must be bypassed for docs
 * to be accessible in production, making accidental exposure extremely unlikely.</p>
 *
 * <h3>Profiles</h3>
 * <ul>
 *   <li>{@code dev}     — Swagger enabled (default)</li>
 *   <li>{@code staging} — Swagger enabled (for QA)</li>
 *   <li>{@code prod}    — Swagger disabled (this bean not created)</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 * @version 2.0.0
 */
@Configuration
@Profile("!prod")   // FIX-13: never load in production
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("BioLab Auth Service API")
                .description("Authentication & authorization — JWT with refresh token rotation, " +
                             "MFA, HIPAA-compliant audit logging")
                .version("v2.0.0")
                .contact(new Contact()
                    .name("BioLab Engineering")
                    .email("engineering@biolab.com"))
                .license(new License().name("Proprietary")))
            .components(new Components()
                .addSecuritySchemes("Bearer",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT access token — obtain from POST /api/auth/login")));
    }
}
