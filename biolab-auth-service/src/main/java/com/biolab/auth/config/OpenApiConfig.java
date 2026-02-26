package com.biolab.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 / Swagger configuration for Auth Service.
 * Swagger UI: /swagger-ui.html | API Docs: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI authServiceOpenApi() {
        return new OpenAPI()
            .info(new Info().title("BioLab Auth Service API")
                .description("Authentication & authorization — JWT with refresh token rotation, MFA, HIPAA-compliant audit logging")
                .version("v1.0.0").contact(new Contact().name("BioLab Engineering").email("engineering@biolab.com"))
                .license(new License().name("Proprietary")))
            .components(new Components().addSecuritySchemes("Bearer",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
