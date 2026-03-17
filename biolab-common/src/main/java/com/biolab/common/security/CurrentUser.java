package com.biolab.common.security;

import java.util.List;
import java.util.UUID;

/**
 * Immutable value object representing the authenticated user extracted
 * from the API Gateway's X-User-* headers.
 *
 * <h3>Sprint 3 — GAP-19 additions</h3>
 * <p>{@link #hasRole(String)} and {@link #hasAnyRole(String...)} are
 * already present; this version ensures {@link #roles()} is never null
 * (returns an empty list for anonymous/system contexts) so
 * {@link com.biolab.common.rls.RlsContextAspect} can call
 * {@code user.roles().isEmpty()} safely.</p>
 *
 * @param userId  UUID of the authenticated user
 * @param email   user's email address (from JWT claim)
 * @param roles   list of role names (e.g. ["BUYER", "ADMIN"]); never null
 * @param orgId   primary organisation UUID; may be null
 *
 * @author BioLab Engineering Team
 * @version 1.1.0
 */
public record CurrentUser(
        UUID         userId,
        String       email,
        List<String> roles,
        String       orgId
) {
    /** Compact constructor — guarantees roles is never null. */
    public CurrentUser {
        roles = (roles != null) ? List.copyOf(roles) : List.of();
    }

    /** Returns true if the user holds the given role. */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /** Returns true if the user holds any of the given roles. */
    public boolean hasAnyRole(String... roleNames) {
        for (String r : roleNames) {
            if (roles.contains(r)) return true;
        }
        return false;
    }

    /** Returns true if the user is an ADMIN or SUPER_ADMIN. */
    public boolean isAdmin() {
        return hasRole(SecurityConstants.ROLE_ADMIN) ||
               hasRole(SecurityConstants.ROLE_SUPER_ADMIN);
    }
}
