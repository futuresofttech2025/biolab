package com.biolab.auth.security;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying that RBAC {@code @PreAuthorize} annotations
 * correctly enforce role-based access control on protected endpoints.
 *
 * <h3>Sprint 3 — GAP-19 / Defence-in-depth verification</h3>
 * <p>These tests simulate the X-User-* headers that the API Gateway injects
 * after JWT validation. They verify that the server-side enforcement works
 * independently of the gateway — so even if the gateway were bypassed, the
 * service itself would reject unauthorized requests.</p>
 *
 * @author BioLab Engineering Team
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("RBAC Endpoint Enforcement Tests")
class RbacEndpointEnforcementTest {

    @Autowired
    private MockMvc mvc;

    // ─────────────────────────────────────────────────────────────────────
    // Admin-only endpoints — non-admin roles must get 403
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BUYER cannot access admin user list — returns 403")
    void buyerCannotListUsers() throws Exception {
        mvc.perform(get("/api/auth/users")
                        .header("X-User-Id",    "d0000000-0000-0000-0000-000000000003")
                        .header("X-User-Email", "buyer@test.com")
                        .header("X-User-Roles", "BUYER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUPPLIER cannot access admin user list — returns 403")
    void supplierCannotListUsers() throws Exception {
        mvc.perform(get("/api/auth/users")
                        .header("X-User-Id",    "d0000000-0000-0000-0000-000000000002")
                        .header("X-User-Email", "supplier@test.com")
                        .header("X-User-Roles", "SUPPLIER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN can access user list — returns 200 or 404, not 403")
    void adminCanListUsers() throws Exception {
        mvc.perform(get("/api/auth/users")
                        .header("X-User-Id",    "d0000000-0000-0000-0000-000000000001")
                        .header("X-User-Email", "admin@test.com")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().is(not(403)));
    }

    @Test
    @DisplayName("BUYER cannot delete another user — returns 403")
    void buyerCannotDeleteOtherUser() throws Exception {
        mvc.perform(delete("/api/auth/users/d0000000-0000-0000-0000-000000000002")
                        .header("X-User-Id",    "d0000000-0000-0000-0000-000000000003")
                        .header("X-User-Email", "buyer@test.com")
                        .header("X-User-Roles", "BUYER"))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Self-access — users can access their own resources
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BUYER can access own profile — returns 200 or 404, not 403")
    void buyerCanAccessOwnProfile() throws Exception {
        mvc.perform(get("/api/auth/users/d0000000-0000-0000-0000-000000000003")
                        .header("X-User-Id",    "d0000000-0000-0000-0000-000000000003")
                        .header("X-User-Email", "buyer@test.com")
                        .header("X-User-Roles", "BUYER"))
                .andExpect(status().is(not(403)));
    }

    @Test
    @DisplayName("BUYER cannot access another user's profile — returns 403")
    void buyerCannotAccessOtherProfile() throws Exception {
        mvc.perform(get("/api/auth/users/d0000000-0000-0000-0000-000000000002")
                        .header("X-User-Id",    "d0000000-0000-0000-0000-000000000003")
                        .header("X-User-Email", "buyer@test.com")
                        .header("X-User-Roles", "BUYER"))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Unauthenticated requests — must get 401, not 403 or 200
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401")
    void unauthenticatedRequestReturns401() throws Exception {
        mvc.perform(get("/api/auth/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Request with no X-User-* headers returns 401")
    void missingUserHeadersReturns401() throws Exception {
        mvc.perform(get("/api/auth/users/d0000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Actuator — health probe is public, other actuator endpoints are secured
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Actuator health is public — returns 200")
    void actuatorHealthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Actuator metrics requires ADMIN — BUYER gets 403")
    void actuatorMetricsRequiresAdmin() throws Exception {
        mvc.perform(get("/actuator/metrics")
                        .header("X-User-Id",    "d0000000-0000-0000-0000-000000000003")
                        .header("X-User-Email", "buyer@test.com")
                        .header("X-User-Roles", "BUYER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Actuator metrics accessible to ADMIN")
    void actuatorMetricsAccessibleToAdmin() throws Exception {
        mvc.perform(get("/actuator/metrics")
                        .header("X-User-Id",    "d0000000-0000-0000-0000-000000000001")
                        .header("X-User-Email", "admin@test.com")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().is(not(403)));
    }
}