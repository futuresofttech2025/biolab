package com.biolab.common.logging;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the logging interceptor for all MVC endpoints.
 */
@Configuration
public class WebMvcLoggingConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;

    public WebMvcLoggingConfig(LoggingInterceptor interceptor) {
        this.loggingInterceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**");
    }
}
