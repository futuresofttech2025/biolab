package com.biolab.discovery.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 (Swagger) configuration for the Discovery Server.
 *
 * <p>Provides interactive API documentation accessible at:</p>
 * <ul>
 *   <li><b>Swagger UI:</b> /swagger-ui.html</li>
 *   <li><b>OpenAPI JSON:</b> /v3/api-docs</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * Builds the OpenAPI specification with BioLab-specific metadata.
     *
     * @return the configured {@link OpenAPI} specification
     */
    @Bean
    public OpenAPI discoveryServerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BioLab Discovery Server API")
                        .description(
                            "Netflix Eureka Service Registry for BioLabs Services Hub. "
                            + "Manages service registration, heartbeat monitoring, and "
                            + "instance discovery across the microservices ecosystem."
                        )
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("BioLab Engineering Team")
                                .email("engineering@biolab.com")
                                .url("https://biolab.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://biolab.com/license")))
                .externalDocs(new ExternalDocumentation()
                        .description("BioLab Architecture Documentation")
                        .url("https://docs.biolab.com/architecture"));
    }
}
