-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs Services Hub — Security Schema (sec_schema)
-- Version: V1
-- Description: Core security tables — authentication, authorization, JWT
--              token rotation, MFA, audit logging, HIPAA/GDPR compliance.
-- ════════════════════════════════════════════════════════════════════════════

CREATE SCHEMA IF NOT EXISTS sec_schema;
SET search_path TO sec_schema;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── users ───────────────────────────────────────────────────────────────
-- Core user identity table for authentication credentials and profile.
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE sec_schema.users (
    id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    email               VARCHAR(255)    NOT NULL,
    password_hash       VARCHAR(255)    NOT NULL,
    first_name          VARCHAR(100)    NOT NULL,
    last_name           VARCHAR(100)    NOT NULL,
    phone               VARCHAR(20),
    avatar_url          VARCHAR(512),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    is_email_verified   BOOLEAN         NOT NULL DEFAULT FALSE,
    is_locked           BOOLEAN         NOT NULL DEFAULT FALSE,
    failed_login_count  INTEGER         NOT NULL DEFAULT 0,
    locked_until        TIMESTAMP WITH TIME ZONE,
    last_login_at       TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON sec_schema.users(email);
CREATE INDEX idx_users_active ON sec_schema.users(is_active);

COMMENT ON TABLE sec_schema.users IS 'Core identity table — credentials, lockout, verification status';

-- ─── roles ───────────────────────────────────────────────────────────────
-- RBAC role definitions: SUPER_ADMIN, ADMIN, SUPPLIER, BUYER.
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE sec_schema.roles (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(50)     NOT NULL,
    display_name    VARCHAR(100)    NOT NULL,
    description     VARCHAR(500),
    is_system_role  BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

COMMENT ON TABLE sec_schema.roles IS 'Role definitions for RBAC — system and custom roles';

-- ─── permissions ─────────────────────────────────────────────────────────
-- Granular action-level permissions mapped to modules.
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE sec_schema.permissions (
    id          UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100)    NOT NULL,
    module      VARCHAR(50)     NOT NULL,
    action      VARCHAR(50)     NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_permissions_name UNIQUE (name)
);

CREATE INDEX idx_permissions_module ON sec_schema.permissions(module);

COMMENT ON TABLE sec_schema.permissions IS 'Granular permissions — e.g. SERVICE_CREATE, DOCUMENT_UPLOAD';

-- ─── user_roles ──────────────────────────────────────────────────────────
-- M:N junction: users ↔ roles with assignment metadata.
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE sec_schema.user_roles (
    id          UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID            NOT NULL REFERENCES sec_schema.users(id) ON DELETE CASCADE,
    role_id     UUID            NOT NULL REFERENCES sec_schema.roles(id) ON DELETE CASCADE,
    assigned_by UUID            REFERENCES sec_schema.users(id),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON sec_schema.user_roles(user_id);
CREATE INDEX idx_user_roles_role ON sec_schema.user_roles(role_id);

COMMENT ON TABLE sec_schema.user_roles IS 'User-role assignments with expiry support and audit trail';

-- ─── role_permissions ────────────────────────────────────────────────────
-- M:N junction: roles ↔ permissions.
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE sec_schema.role_permissions (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_id         UUID            NOT NULL REFERENCES sec_schema.roles(id) ON DELETE CASCADE,
    permission_id   UUID            NOT NULL REFERENCES sec_schema.permissions(id) ON DELETE CASCADE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_role_permission UNIQUE (role_id, permission_id)
);

CREATE INDEX idx_role_perms_role ON sec_schema.role_permissions(role_id);

COMMENT ON TABLE sec_schema.role_permissions IS 'Role-permission mapping — defines what each role can do';

-- ─── refresh_tokens ──────────────────────────────────────────────────────
-- Refresh Token Rotation: tracks token families for reuse detection.
--
-- TOKEN ROTATION STRATEGY:
--   1. Each login creates a new "token_family" (UUID).
--   2. On refresh, the old token is revoked and a new token is issued
--      in the SAME family, incrementing "generation".
--   3. If a revoked token is reused (replay attack), the entire family
--      is invalidated immediately — this is "reuse detection".
--   4. Each family tracks: token hash, generation count, revocation.
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE sec_schema.refresh_tokens (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID            NOT NULL REFERENCES sec_schema.users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255)    NOT NULL,
    token_family    UUID            NOT NULL,
    generation      INTEGER         NOT NULL DEFAULT 0,
    is_revoked      BOOLEAN         NOT NULL DEFAULT FALSE,
    revoked_reason  VARCHAR(100),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    issued_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user ON sec_schema.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family ON sec_schema.refresh_tokens(token_family);
CREATE INDEX idx_refresh_tokens_hash ON sec_schema.refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires ON sec_schema.refresh_tokens(expires_at);

COMMENT ON TABLE sec_schema.refresh_tokens IS 'Refresh token rotation — family tracking, generation counting, reuse detection';

-- ─── jwt_token_blacklist ─────────────────────────────────────────────────
-- Revoked access tokens (logout, forced invalidation).
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE sec_schema.jwt_token_blacklist (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    jti             VARCHAR(255)    NOT NULL,
    user_id         UUID            NOT NULL REFERENCES sec_schema.users(id) ON DELETE CASCADE,
    token_type      VARCHAR(20)     NOT NULL CHECK (token_type IN ('ACCESS', 'REFRESH')),
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    blacklisted_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    reason          VARCHAR(255),
    CONSTRAINT uq_blacklist_jti UNIQUE (jti)
);

CREATE INDEX idx_blacklist_jti ON sec_schema.jwt_token_blacklist(jti);
CREATE INDEX idx_blacklist_expires ON sec_schema.jwt_token_blacklist(expires_at);

COMMENT ON TABLE sec_schema.jwt_token_blacklist IS 'Revoked JWT access tokens — checked on every authenticated request';

-- ─── password_history ────────────────────────────────────────────────────
-- Prevents password reuse (HIPAA/NIST 800-63B requirement).
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE sec_schema.password_history (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID            NOT NULL REFERENCES sec_schema.users(id) ON DELETE CASCADE,
    password_hash   VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pwd_history_user ON sec_schema.password_history(user_id);

COMMENT ON TABLE sec_schema.password_history IS 'Password history — prevents reuse of last N passwords (default: 5)';

-- ─── user_sessions ───────────────────────────────────────────────────────
-- Active session tracking for concurrent login management.
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE sec_schema.user_sessions (
    id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID            NOT NULL REFERENCES sec_schema.users(id) ON DELETE CASCADE,
    refresh_token_id    UUID            REFERENCES sec_schema.refresh_tokens(id),
    session_token       VARCHAR(500)    NOT NULL,
    ip_address          VARCHAR(45)     NOT NULL,
    user_agent          VARCHAR(500),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    last_accessed_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_session_token UNIQUE (session_token)
);

CREATE INDEX idx_sessions_user ON sec_schema.user_sessions(user_id);
CREATE INDEX idx_sessions_active ON sec_schema.user_sessions(is_active);

COMMENT ON TABLE sec_schema.user_sessions IS 'Active sessions — concurrent session limiting and forced logout';

-- ─── mfa_settings ────────────────────────────────────────────────────────
-- Multi-Factor Authentication configuration per user.
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE sec_schema.mfa_settings (
    id          UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID            NOT NULL REFERENCES sec_schema.users(id) ON DELETE CASCADE,
    mfa_type    VARCHAR(20)     NOT NULL CHECK (mfa_type IN ('TOTP', 'EMAIL')),
    secret_key  VARCHAR(255),
    is_enabled  BOOLEAN         NOT NULL DEFAULT FALSE,
    backup_codes TEXT[],
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_mfa_type UNIQUE (user_id, mfa_type)
);

COMMENT ON TABLE sec_schema.mfa_settings IS 'MFA config — TOTP (Google Authenticator) and email OTP';

-- ─── login_audit_log ─────────────────────────────────────────────────────
-- Immutable authentication event log (HIPAA requirement).
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE sec_schema.login_audit_log (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID            REFERENCES sec_schema.users(id),
    email           VARCHAR(255)    NOT NULL,
    ip_address      VARCHAR(45)     NOT NULL,
    user_agent      VARCHAR(500),
    action          VARCHAR(30)     NOT NULL CHECK (action IN ('LOGIN','LOGOUT','FAILED_LOGIN','TOKEN_REFRESH','PASSWORD_RESET','MFA_CHALLENGE','TOKEN_ROTATION','REUSE_DETECTED')),
    status          VARCHAR(20)     NOT NULL CHECK (status IN ('SUCCESS', 'FAILURE')),
    mfa_used        BOOLEAN         NOT NULL DEFAULT FALSE,
    failure_reason  VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user ON sec_schema.login_audit_log(user_id);
CREATE INDEX idx_audit_email ON sec_schema.login_audit_log(email);
CREATE INDEX idx_audit_created ON sec_schema.login_audit_log(created_at);

COMMENT ON TABLE sec_schema.login_audit_log IS 'Immutable audit log — all auth events including token rotation (HIPAA/SOC2)';

-- ─── consent_records ─────────────────────────────────────────────────────
-- GDPR/HIPAA consent tracking.
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE sec_schema.consent_records (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID            NOT NULL REFERENCES sec_schema.users(id) ON DELETE CASCADE,
    consent_type    VARCHAR(20)     NOT NULL CHECK (consent_type IN ('GDPR','HIPAA','TOS','MARKETING')),
    granted_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMP WITH TIME ZONE,
    ip_address      VARCHAR(45)     NOT NULL,
    version         VARCHAR(20)     NOT NULL DEFAULT '1.0',
    CONSTRAINT uq_user_consent UNIQUE (user_id, consent_type)
);

CREATE INDEX idx_consent_user ON sec_schema.consent_records(user_id);

COMMENT ON TABLE sec_schema.consent_records IS 'GDPR/HIPAA consent with grant/revoke timestamps and IP';

-- ─── data_access_log ─────────────────────────────────────────────────────
-- PHI/PII access tracking (HIPAA requirement).
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE sec_schema.data_access_log (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID            NOT NULL REFERENCES sec_schema.users(id),
    resource_type   VARCHAR(50)     NOT NULL,
    resource_id     UUID            NOT NULL,
    action          VARCHAR(20)     NOT NULL CHECK (action IN ('VIEW','DOWNLOAD','EXPORT','PRINT','CREATE','UPDATE','DELETE')),
    ip_address      VARCHAR(45)     NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_data_access_user ON sec_schema.data_access_log(user_id);
CREATE INDEX idx_data_access_resource ON sec_schema.data_access_log(resource_type, resource_id);
CREATE INDEX idx_data_access_created ON sec_schema.data_access_log(created_at);

COMMENT ON TABLE sec_schema.data_access_log IS 'PHI/PII access audit trail — every data view/download logged (HIPAA)';

-- ─── Triggers ────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION sec_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON sec_schema.users
    FOR EACH ROW EXECUTE FUNCTION sec_schema.update_updated_at_column();
CREATE TRIGGER trg_roles_updated_at BEFORE UPDATE ON sec_schema.roles
    FOR EACH ROW EXECUTE FUNCTION sec_schema.update_updated_at_column();
CREATE TRIGGER trg_permissions_updated_at BEFORE UPDATE ON sec_schema.permissions
    FOR EACH ROW EXECUTE FUNCTION sec_schema.update_updated_at_column();
CREATE TRIGGER trg_mfa_updated_at BEFORE UPDATE ON sec_schema.mfa_settings
    FOR EACH ROW EXECUTE FUNCTION sec_schema.update_updated_at_column();
