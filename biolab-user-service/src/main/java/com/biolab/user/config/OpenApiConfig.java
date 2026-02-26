package com.biolab.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 / Swagger UI configuration for User Service.
 * Swagger UI: http://localhost:8082/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI userServiceOpenApi() {
        return new OpenAPI()
            .info(new Info().title("BioLab User Service API")
                .description("User profiles, organizations, RBAC role/permission management")
                .version("v1.0.0")
                .contact(new Contact().name("BioLab Engineering").email("engineering@biolab.com"))
                .license(new License().name("Proprietary")))
            .components(new Components().addSecuritySchemes("Bearer",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
