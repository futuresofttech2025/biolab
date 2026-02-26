package com.biolab.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * SpEL helper bean for fine-grained permission checks in {@code @PreAuthorize}.
 *
 * <p>Used in controllers to enforce the RBAC matrix from Slide 6:</p>
 * <pre>
 *   &#64;PreAuthorize("@perm.isAdmin()")
 *   &#64;PreAuthorize("@perm.hasPermission('SERVICE_CREATE')")
 *   &#64;PreAuthorize("@perm.isOwnerOrAdmin(#userId)")
 * </pre>
 *
 * <p>The bean is registered as {@code @perm} via the component name.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Component("perm")
public class PermissionChecker {

    private static final Logger log = LoggerFactory.getLogger(PermissionChecker.class);

    /** Returns true if the current user has SUPER_ADMIN or ADMIN role. */
    public boolean isAdmin() {
        return CurrentUserContext.get().map(CurrentUser::isAdmin).orElse(false);
    }

    /** Returns true if the current user has the SUPER_ADMIN role. */
    public boolean isSuperAdmin() {
        return CurrentUserContext.get()
                .map(u -> u.hasRole(SecurityConstants.ROLE_SUPER_ADMIN))
                .orElse(false);
    }

    /**
     * Checks if the current user has a specific role.
     *
     * @param roleName role to check (e.g., "SUPPLIER")
     * @return true if user holds the role
     */
    public boolean hasRole(String roleName) {
        return CurrentUserContext.get().map(u -> u.hasRole(roleName)).orElse(false);
    }

    /**
     * Checks if the current user has any of the specified roles.
     *
     * @param roles comma-separated role names
     * @return true if user holds at least one
     */
    public boolean hasAnyRole(String... roles) {
        return CurrentUserContext.get().map(u -> u.hasAnyRole(roles)).orElse(false);
    }

    /**
     * Checks if the current user is the resource owner or an admin.
     * Used for endpoints like "view own profile" / "update own profile".
     *
     * @param resourceOwnerId UUID of the resource owner
     * @return true if current user is the owner or an admin
     */
    public boolean isOwnerOrAdmin(UUID resourceOwnerId) {
        return CurrentUserContext.get().map(u ->
                u.isAdmin() || u.userId().equals(resourceOwnerId)
        ).orElse(false);
    }

    /**
     * Checks if the current user belongs to the specified organization.
     *
     * @param orgId organization UUID
     * @return true if user's orgId matches
     */
    public boolean belongsToOrg(String orgId) {
        return CurrentUserContext.get().map(u ->
                orgId != null && orgId.equals(u.orgId())
        ).orElse(false);
    }

    /**
     * Combined check: user is admin OR belongs to the specified org.
     * Used for organization-scoped data isolation.
     *
     * @param orgId organization UUID
     * @return true if admin or org member
     */
    public boolean isAdminOrOrgMember(String orgId) {
        return CurrentUserContext.get().map(u ->
                u.isAdmin() || (orgId != null && orgId.equals(u.orgId()))
        ).orElse(false);
    }
}
