package com.biolab.config.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 (Swagger) configuration for the Config Server.
 *
 * <p>Documentation available at /swagger-ui.html</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI configServerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BioLab Config Server API")
                        .description(
                            "Centralized configuration server for BioLabs Services Hub. "
                            + "Provides environment-specific properties, encrypted secrets, "
                            + "and hot-reload support for all microservices."
                        )
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("BioLab Engineering Team")
                                .email("engineering@biolab.com"))
                        .license(new License()
                                .name("Proprietary")))
                .externalDocs(new ExternalDocumentation()
                        .description("BioLab Config Server Guide")
                        .url("https://docs.biolab.com/config-server"));
    }
}
