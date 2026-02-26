package com.biolab.common.security;

/**
 * Centralized security constants shared across all microservices.
 *
 * <p>Defines header names, role identifiers, token configuration,
 * and password policy parameters referenced throughout the platform.</p>
 *
 * <h3>Header Contract (set by API Gateway, consumed by services):</h3>
 * <pre>
 *   X-User-Id      → UUID of the authenticated user
 *   X-User-Email   → User email address
 *   X-User-Roles   → Comma-separated role names (e.g., "BUYER,ADMIN")
 *   X-User-OrgId   → Primary organization UUID
 *   X-Correlation-Id → Request trace ID for distributed tracing
 * </pre>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
public final class SecurityConstants {

    private SecurityConstants() { /* utility class */ }

    // ─── Gateway-Injected Headers ──────────────────────────────────
    public static final String HEADER_USER_ID       = "X-User-Id";
    public static final String HEADER_USER_EMAIL    = "X-User-Email";
    public static final String HEADER_USER_ROLES    = "X-User-Roles";
    public static final String HEADER_USER_ORG_ID   = "X-User-OrgId";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    // ─── Role Names (must match sec_schema.roles) ──────────────────
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_ADMIN       = "ADMIN";
    public static final String ROLE_SUPPLIER    = "SUPPLIER";
    public static final String ROLE_BUYER       = "BUYER";

    // ─── Token Configuration ───────────────────────────────────────
    public static final String TOKEN_PREFIX  = "Bearer ";
    public static final String TOKEN_ISSUER  = "biolab-auth-service";
    public static final int    ACCESS_TOKEN_EXPIRY_SECONDS  = 900;    // 15 min
    public static final int    REFRESH_TOKEN_EXPIRY_SECONDS = 604800; // 7 days

    // ─── Password Policy (Slide 10) ────────────────────────────────
    public static final int PASSWORD_MIN_LENGTH          = 8;
    public static final int PASSWORD_RECOMMENDED_LENGTH  = 12;
    public static final int PASSWORD_HISTORY_COUNT       = 5;
    public static final int PASSWORD_ROTATION_DAYS       = 90;

    // ─── Account Lockout ───────────────────────────────────────────
    public static final int MAX_FAILED_LOGIN_ATTEMPTS  = 5;
    public static final int LOCKOUT_DURATION_MINUTES   = 30;

    // ─── Session Limits ────────────────────────────────────────────
    public static final int MAX_CONCURRENT_SESSIONS = 5;

    // ─── Rate Limiting ─────────────────────────────────────────────
    public static final int RATE_LIMIT_REQUESTS_PER_MINUTE = 100;

    // ─── Encryption ────────────────────────────────────────────────
    public static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    public static final int    ENCRYPTION_KEY_SIZE  = 256;
    public static final int    GCM_IV_LENGTH        = 12;
    public static final int    GCM_TAG_LENGTH       = 128;
}
