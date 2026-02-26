package com.biolab.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 (Swagger) configuration for the API Gateway.
 *
 * <p>Configures JWT Bearer token authentication scheme in the Swagger UI,
 * allowing developers to test authenticated endpoints directly.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI gatewayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BioLab API Gateway")
                        .description(
                            "Central entry point for the BioLabs Services Hub platform. "
                            + "Routes requests to backend microservices with JWT authentication, "
                            + "rate limiting, CORS handling, and circuit breaking."
                        )
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("BioLab Engineering Team")
                                .email("engineering@biolab.com"))
                        .license(new License()
                                .name("Proprietary")))
                .externalDocs(new ExternalDocumentation()
                        .description("BioLab API Documentation")
                        .url("https://docs.biolab.com/api"))
                // Global JWT security requirement
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT access token")));
    }
}
