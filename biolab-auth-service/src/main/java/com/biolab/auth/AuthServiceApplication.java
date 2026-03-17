package com.biolab.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * BioLab Auth Service — Authentication & Authorization Microservice.
 *
 * <h3>Component Scan</h3>
 * <p>Scans specific {@code com.biolab.common} sub-packages rather than the
 * whole common tree, to avoid picking up {@code GlobalExceptionHandler} from
 * common which conflicts with the auth-service's own handler.</p>
 *
 * <p>Sub-packages explicitly included:</p>
 * <ul>
 *   <li>{@code com.biolab.common.encryption} — {@code AesEncryptionService},
 *       {@code EncryptedStringConverter} (needed for PII column encryption)</li>
 *   <li>{@code com.biolab.common.security} — {@code JwtAuthenticationFilter},
 *       {@code SecurityHeadersFilter}, {@code PermissionChecker}</li>
 *   <li>{@code com.biolab.common.logging} — {@code MdcLoggingFilter},
 *       {@code LoggingInterceptor}</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 * @version 2.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
@EnableScheduling
@ComponentScan(basePackages = {
        "com.biolab.auth",               // this service
        "com.biolab.common.encryption",  // AesEncryptionService, EncryptedStringConverter, DeterministicStringConverter
        "com.biolab.common.security",    // JwtAuthenticationFilter, SecurityHeadersFilter, PermissionChecker
        "com.biolab.common.logging",     // MdcLoggingFilter, LoggingInterceptor
        "com.biolab.common.rls",         // RlsContextAspect
        "com.biolab.common.audit"        // AuditInterceptor
})
public class AuthServiceApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
