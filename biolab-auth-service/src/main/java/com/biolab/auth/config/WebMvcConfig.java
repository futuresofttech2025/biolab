package com.biolab.auth.config;

import com.biolab.common.config.BaseSecurityConfig;
import org.springframework.context.annotation.Configuration;

/**
 * WebMVC configuration for Auth Service.
 * Inherits audit interceptor registration from {@link BaseSecurityConfig}.
 *
 * @author BioLab Engineering Team
 */
@Configuration
public class WebMvcConfig extends BaseSecurityConfig {
    // Inherits AuditInterceptor registration from BaseSecurityConfig
}
