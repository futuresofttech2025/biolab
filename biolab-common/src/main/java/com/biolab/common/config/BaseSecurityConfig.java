package com.biolab.common.config;

import com.biolab.common.audit.AuditInterceptor;
import com.biolab.common.security.JwtAuthenticationFilter;
import com.biolab.common.security.SecurityHeadersFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Base WebMVC configuration that registers security interceptors
 * and filters. Extend in each microservice to activate.
 *
 * <p>Registers:</p>
 * <ul>
 *   <li>{@link AuditInterceptor} — HIPAA audit logging</li>
 * </ul>
 *
 * <p>Note: {@link JwtAuthenticationFilter} and {@link SecurityHeadersFilter}
 * are registered via the SecurityFilterChain, not here.</p>
 *
 * @author BioLab Engineering Team
 */
public abstract class BaseSecurityConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuditInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**");
    }
}
