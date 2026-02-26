package com.biolab.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables method-level security annotations across the Auth Service.
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>{@code @PreAuthorize("@perm.isAdmin()")} — SpEL-based RBAC checks</li>
 *   <li>{@code @PreAuthorize("hasRole('SUPER_ADMIN')")} — Spring role checks</li>
 *   <li>{@code @Secured("ROLE_ADMIN")} — simple role declarations</li>
 * </ul>
 *
 * <p>The {@code @perm} bean is provided by {@code PermissionChecker}
 * from the biolab-common module.</p>
 *
 * @author BioLab Engineering Team
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class MethodSecurityConfig {
    // Enables @PreAuthorize, @PostAuthorize, @Secured
}
