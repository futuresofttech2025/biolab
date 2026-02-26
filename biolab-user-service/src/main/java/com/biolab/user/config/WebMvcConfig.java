package com.biolab.user.config;

import com.biolab.common.config.BaseSecurityConfig;
import org.springframework.context.annotation.Configuration;

/**
 * WebMVC configuration for User Service.
 * Inherits audit interceptor registration from BaseSecurityConfig.
 *
 * @author BioLab Engineering Team
 */
@Configuration
public class WebMvcConfig extends BaseSecurityConfig {
    // Inherits AuditInterceptor registration
}
