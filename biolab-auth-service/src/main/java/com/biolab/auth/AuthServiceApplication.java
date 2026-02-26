package com.biolab.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * BioLab Auth Service — Authentication & Authorization Microservice.
 *
 * <h3>Core Features:</h3>
 * <ul>
 *   <li>User registration with email verification</li>
 *   <li>Login with BCrypt password verification and account lockout</li>
 *   <li>JWT access token + refresh token with <b>Token Rotation</b></li>
 *   <li>Token Rotation: family-based tracking with reuse detection</li>
 *   <li>Multi-Factor Authentication (TOTP + Email OTP)</li>
 *   <li>Password reset, change, and history enforcement (NIST 800-63B)</li>
 *   <li>Session management and concurrent login control</li>
 *   <li>Immutable HIPAA-compliant login audit logging</li>
 *   <li>Full CRUD for all security entities</li>
 * </ul>
 *
 * <h3>Token Rotation Strategy:</h3>
 * <ol>
 *   <li>Login creates a token family (UUID) with generation=0</li>
 *   <li>On refresh, old token is revoked, new token issued in same family (gen++)</li>
 *   <li>If revoked token is reused → entire family invalidated (theft detection)</li>
 * </ol>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
@EnableScheduling
public class AuthServiceApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(AuthServiceApplication.class, args);

    }
}
