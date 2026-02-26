package com.biolab.common.security;

import java.util.List;
import java.util.UUID;

/**
 * Immutable record representing the authenticated user extracted from
 * gateway-forwarded headers. Thread-safe and suitable for use with
 * {@link CurrentUserContext}.
 *
 * @param userId  user UUID from {@code X-User-Id}
 * @param email   user email from {@code X-User-Email}
 * @param roles   role names from {@code X-User-Roles}
 * @param orgId   primary organization UUID from {@code X-User-OrgId}
 *
 * @author BioLab Engineering Team
 */
public record CurrentUser(
        UUID userId,
        String email,
        List<String> roles,
        String orgId
) {
    /** Checks if the current user has the specified role. */
    public boolean hasRole(String roleName) {
        return roles != null && roles.contains(roleName);
    }

    /** Checks if the user is SUPER_ADMIN or ADMIN. */
    public boolean isAdmin() {
        return hasRole(SecurityConstants.ROLE_SUPER_ADMIN)
            || hasRole(SecurityConstants.ROLE_ADMIN);
    }

    /** Checks if the user has any of the specified roles. */
    public boolean hasAnyRole(String... roleNames) {
        if (roles == null) return false;
        for (String r : roleNames) { if (roles.contains(r)) return true; }
        return false;
    }
}
