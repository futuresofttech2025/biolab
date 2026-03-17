package com.biolab.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.TimeZone;

/**
 * BioLab User Service — User Profile, Organization & RBAC Management.
 *
 * <h3>Component Scan</h3>
 * <p>Explicitly scans {@code com.biolab.common} sub-packages to register
 * shared beans from biolab-common (AesEncryptionService, converters, filters).
 * The full {@code com.biolab.common} package is NOT scanned to avoid pulling
 * in {@code GlobalExceptionHandler} which conflicts with this service's own
 * exception handler. Instead only the sub-packages actually needed are listed.</p>
 *
 * @author BioLab Engineering Team
 * @version 2.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
@ComponentScan(basePackages = {
        "com.biolab.user",                // this service
        "com.biolab.common.encryption",   // AesEncryptionService, EncryptedStringConverter, DeterministicStringConverter
        "com.biolab.common.security",     // JwtAuthenticationFilter, SecurityHeadersFilter, PermissionChecker
        "com.biolab.common.logging",      // MdcLoggingFilter, LoggingInterceptor
        "com.biolab.common.rls",          // RlsContextAspect
        "com.biolab.common.audit"         // AuditInterceptor
})
public class UserServiceApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
