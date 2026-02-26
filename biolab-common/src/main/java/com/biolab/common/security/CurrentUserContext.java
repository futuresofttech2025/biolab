package com.biolab.common.security;

import java.util.Optional;

/**
 * Thread-local holder for the authenticated user context.
 *
 * <p>Populated by {@link JwtAuthenticationFilter} on each request.
 * Allows any service layer code to access the current user without
 * passing it through method parameters.</p>
 *
 * <pre>
 *   CurrentUser user = CurrentUserContext.require();
 *   UUID userId = user.userId();
 * </pre>
 *
 * @author BioLab Engineering Team
 */
public final class CurrentUserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private CurrentUserContext() { }

    /** Sets the current user for the current thread. */
    public static void set(CurrentUser user) { HOLDER.set(user); }

    /** Returns the current user, or empty if unauthenticated. */
    public static Optional<CurrentUser> get() { return Optional.ofNullable(HOLDER.get()); }

    /** Returns the current user, throws if unauthenticated. */
    public static CurrentUser require() {
        return get().orElseThrow(() ->
                new SecurityException("No authenticated user in context"));
    }

    /** Clears the context (must be called in filter finally block). */
    public static void clear() { HOLDER.remove(); }
}
