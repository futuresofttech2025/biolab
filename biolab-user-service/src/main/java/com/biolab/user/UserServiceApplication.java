package com.biolab.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.TimeZone;

/**
 * BioLab User Service — User Profile, Organization & RBAC Management.
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>User profile CRUD (view, update, deactivate, avatar upload)</li>
 *   <li>Organization CRUD (BUYER/SUPPLIER entities on the platform)</li>
 *   <li>User-Organization membership management</li>
 *   <li>Role viewing and assignment (RBAC — reads sec_schema roles)</li>
 *   <li>Permission viewing (granular module-action permissions)</li>
 *   <li>Admin user search with filtering and pagination</li>
 * </ul>
 *
 * <h3>Schemas:</h3>
 * <ul>
 *   <li>{@code sec_schema} — users, roles, permissions, user_roles, role_permissions</li>
 *   <li>{@code app_schema} — organizations, user_organizations</li>
 * </ul>
 *
 * <h3>Security:</h3>
 * <p>All endpoints require JWT authentication via the API Gateway.
 * The gateway sets {@code X-User-Id}, {@code X-User-Email}, {@code X-User-Roles}
 * headers after JWT validation. This service trusts those headers.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 * @since 2026-02-16
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
public class UserServiceApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
