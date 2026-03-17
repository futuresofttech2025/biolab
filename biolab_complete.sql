-- ════════════════════════════════════════════════════════════════════════════
-- BioLab Complete Database Setup — Run this ONCE on a FRESH database
-- Replaces all Flyway migrations V1–V13
-- Database: postgres
--
-- After running:
--   Disable Flyway in application.yml: flyway.enabled=false
--   Login: ndate1976@gmail.com / nitin@1976
-- ════════════════════════════════════════════════════════════════════════════

-- Drop and recreate schemas cleanly
DROP SCHEMA IF EXISTS sec_schema CASCADE;
DROP SCHEMA IF EXISTS app_schema CASCADE;
DROP TABLE IF EXISTS public.flyway_schema_history CASCADE;

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
                                  first_name          VARCHAR(255)    NOT NULL,
                                  last_name           VARCHAR(255)    NOT NULL,
                                  phone               VARCHAR(255),
                                  avatar_url          text,
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
                                  name            VARCHAR(255)     NOT NULL,
                                  display_name    VARCHAR(255)    NOT NULL,
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
                                        name        VARCHAR(255)    NOT NULL,
                                        module      VARCHAR(100)     NOT NULL,
                                        action      VARCHAR(100)     NOT NULL,
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
                                           revoked_reason  VARCHAR(255),
                                           ip_address      VARCHAR(255),
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
                                                token_type      VARCHAR(255)     NOT NULL CHECK (token_type IN ('ACCESS', 'REFRESH')),
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
                                          ip_address          VARCHAR(255)     NOT NULL,
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
                                         mfa_type    VARCHAR(255)     NOT NULL CHECK (mfa_type IN ('TOTP', 'EMAIL')),
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
                                            ip_address      VARCHAR(255)     NOT NULL,
                                            user_agent      VARCHAR(500),
                                            action          VARCHAR(255)     NOT NULL CHECK (action IN ('LOGIN','LOGOUT','FAILED_LOGIN','TOKEN_REFRESH','PASSWORD_RESET','MFA_CHALLENGE','TOKEN_ROTATION','REUSE_DETECTED')),
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
                                            consent_type    VARCHAR(255)     NOT NULL CHECK (consent_type IN ('GDPR','HIPAA','TOS','MARKETING')),
                                            granted_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                            revoked_at      TIMESTAMP WITH TIME ZONE,
                                            ip_address      VARCHAR(255)     NOT NULL,
                                            version         VARCHAR(255)     NOT NULL DEFAULT '1.0',
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
                                            resource_type   VARCHAR(255)     NOT NULL,
                                            resource_id     UUID            NOT NULL,
                                            action          VARCHAR(255)     NOT NULL CHECK (action IN ('VIEW','DOWNLOAD','EXPORT','PRINT','CREATE','UPDATE','DELETE')),
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
-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs — Application Schema (app_schema)
-- Version: V2 | Organizations and user-organization memberships.
-- ════════════════════════════════════════════════════════════════════════════

CREATE SCHEMA IF NOT EXISTS app_schema;

-- ─── organizations ───────────────────────────────────────────────────────
CREATE TABLE app_schema.organizations (
                                          id          UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
                                          name        VARCHAR(255)    NOT NULL,
                                          type        VARCHAR(255)     NOT NULL CHECK (type IN ('BUYER', 'SUPPLIER')),
                                          address     VARCHAR(500),
                                          phone       VARCHAR(255),
                                          website     VARCHAR(255),
                                          logo_url    VARCHAR(512),
                                          is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
                                          created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                          updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_org_type ON app_schema.organizations(type);
CREATE INDEX idx_org_active ON app_schema.organizations(is_active);

COMMENT ON TABLE app_schema.organizations IS 'Buyer and Supplier organizations on the BioLabs platform';

-- ─── user_organizations ──────────────────────────────────────────────────
CREATE TABLE app_schema.user_organizations (
                                               id          UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
                                               user_id     UUID            NOT NULL,
                                               org_id      UUID            NOT NULL REFERENCES app_schema.organizations(id) ON DELETE CASCADE,
                                               role_in_org VARCHAR(255)     NOT NULL DEFAULT 'MEMBER',
                                               is_primary  BOOLEAN         NOT NULL DEFAULT FALSE,
                                               joined_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                               CONSTRAINT uq_user_org UNIQUE (user_id, org_id)
);

CREATE INDEX idx_user_org_user ON app_schema.user_organizations(user_id);
CREATE INDEX idx_user_org_org ON app_schema.user_organizations(org_id);

COMMENT ON TABLE app_schema.user_organizations IS 'User-Organization membership with role and primary flag';

CREATE OR REPLACE FUNCTION app_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_org_updated_at BEFORE UPDATE ON app_schema.organizations
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs — Seed: Roles & Permissions
-- Version: V3 | System roles + granular RBAC permissions + assignments.
-- ════════════════════════════════════════════════════════════════════════════
SET search_path TO sec_schema;

-- ─── System Roles ────────────────────────────────────────────────────────
INSERT INTO sec_schema.roles (id, name, display_name, description, is_system_role) VALUES
                                                                                       ('a0000000-0000-0000-0000-000000000001','SUPER_ADMIN','Super Administrator','Full platform access — all tenants, compliance, settings',TRUE),
                                                                                       ('a0000000-0000-0000-0000-000000000002','ADMIN','Administrator','Organization admin — manages users, services, compliance within org',TRUE),
                                                                                       ('a0000000-0000-0000-0000-000000000003','SUPPLIER','Supplier','Service provider — catalog, experiments, results',TRUE),
                                                                                       ('a0000000-0000-0000-0000-000000000004','BUYER','Buyer','Client — browse services, request experiments, review results',TRUE);

-- ─── Permissions ─────────────────────────────────────────────────────────
INSERT INTO sec_schema.permissions (name, module, action, description) VALUES
                                                                           ('USER_CREATE','USER','CREATE','Create new user accounts'),
                                                                           ('USER_VIEW','USER','VIEW','View user profiles'),
                                                                           ('USER_VIEW_ALL','USER','VIEW_ALL','View all user profiles'),
                                                                           ('USER_UPDATE','USER','UPDATE','Update user profile'),
                                                                           ('USER_DELETE','USER','DELETE','Deactivate user accounts'),
                                                                           ('USER_MANAGE','USER','MANAGE','Full user lifecycle management'),
                                                                           ('ROLE_ASSIGN','ROLE','ASSIGN','Assign roles to users'),
                                                                           ('ROLE_MANAGE','ROLE','MANAGE','Create and manage custom roles'),
                                                                           ('SERVICE_CREATE','SERVICE','CREATE','Create catalog services'),
                                                                           ('SERVICE_VIEW','SERVICE','VIEW','View service listings'),
                                                                           ('SERVICE_EDIT','SERVICE','EDIT','Edit service details'),
                                                                           ('SERVICE_DELETE','SERVICE','DELETE','Remove services from catalog'),
                                                                           ('PROJECT_CREATE','PROJECT','CREATE','Create new projects'),
                                                                           ('PROJECT_VIEW_ALL','PROJECT','VIEW_ALL','View all projects'),
                                                                           ('PROJECT_VIEW_OWN','PROJECT','VIEW_OWN','View own projects'),
                                                                           ('PROJECT_UPDATE','PROJECT','UPDATE','Update project details'),
                                                                           ('PROJECT_MANAGE','PROJECT','MANAGE','Full project management'),
                                                                           ('DOCUMENT_UPLOAD','DOCUMENT','UPLOAD','Upload documents'),
                                                                           ('DOCUMENT_DOWNLOAD','DOCUMENT','DOWNLOAD','Download documents'),
                                                                           ('DOCUMENT_DELETE','DOCUMENT','DELETE','Delete documents'),
                                                                           ('MESSAGE_SEND','MESSAGE','SEND','Send messages'),
                                                                           ('MESSAGE_VIEW','MESSAGE','VIEW','View messages'),
                                                                           ('INVOICE_CREATE','INVOICE','CREATE','Create invoices'),
                                                                           ('INVOICE_VIEW_ALL','INVOICE','VIEW_ALL','View all invoices'),
                                                                           ('INVOICE_VIEW_OWN','INVOICE','VIEW_OWN','View own invoices'),
                                                                           ('COMPLIANCE_VIEW','COMPLIANCE','VIEW','View compliance dashboards'),
                                                                           ('COMPLIANCE_MANAGE','COMPLIANCE','MANAGE','Manage compliance settings'),
                                                                           ('AUDIT_LOG_VIEW','AUDIT','VIEW','View audit logs'),
                                                                           ('SETTINGS_PLATFORM','SETTINGS','PLATFORM','Platform-wide settings'),
                                                                           ('SETTINGS_OWN_PROFILE','SETTINGS','PROFILE','Update own profile');

-- SUPER_ADMIN → ALL permissions
INSERT INTO sec_schema.role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000001', id FROM sec_schema.permissions;

-- ADMIN → all except SETTINGS_PLATFORM
INSERT INTO sec_schema.role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000002', id FROM sec_schema.permissions WHERE name != 'SETTINGS_PLATFORM';

-- SUPPLIER
INSERT INTO sec_schema.role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000003', id FROM sec_schema.permissions
WHERE name IN ('SERVICE_CREATE','SERVICE_VIEW','SERVICE_EDIT','PROJECT_CREATE','PROJECT_VIEW_OWN','PROJECT_UPDATE',
               'DOCUMENT_UPLOAD','DOCUMENT_DOWNLOAD','DOCUMENT_DELETE','MESSAGE_SEND','MESSAGE_VIEW',
               'INVOICE_CREATE','INVOICE_VIEW_OWN','SETTINGS_OWN_PROFILE');

-- BUYER
INSERT INTO sec_schema.role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000004', id FROM sec_schema.permissions
WHERE name IN ('SERVICE_VIEW','PROJECT_CREATE','PROJECT_VIEW_OWN','DOCUMENT_UPLOAD','DOCUMENT_DOWNLOAD',
               'MESSAGE_SEND','MESSAGE_VIEW','INVOICE_VIEW_OWN','SETTINGS_OWN_PROFILE');
-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs — Row-Level Security (RLS) Policies
-- Version: V4 | Enforces organization-scoped data isolation at database level.
-- Compliance: HIPAA §164.312(a) — Access Control | GDPR Art.25 — Privacy by Design
-- ════════════════════════════════════════════════════════════════════════════

-- ─── Enable RLS on tables with organization-scoped data ─────────────────

-- Organizations: users can only see their own org (unless admin)
ALTER TABLE app_schema.organizations ENABLE ROW LEVEL SECURITY;

CREATE POLICY org_isolation_policy ON app_schema.organizations
    USING (
        -- Admins see all organizations
        current_setting('app.current_user_role', true) IN ('SUPER_ADMIN', 'ADMIN')
        OR
        -- Non-admins see only their own organization
        id::text = current_setting('app.current_user_org_id', true)
    );

-- User-Organizations: users see memberships for their own org
ALTER TABLE app_schema.user_organizations ENABLE ROW LEVEL SECURITY;

CREATE POLICY user_org_isolation_policy ON app_schema.user_organizations
    USING (
        current_setting('app.current_user_role', true) IN ('SUPER_ADMIN', 'ADMIN')
        OR
        org_id::text = current_setting('app.current_user_org_id', true)
        OR
        user_id::text = current_setting('app.current_user_id', true)
    );

-- ─── RLS on Security Schema tables ─────────────────────────────────────

-- Login Audit Logs: users see only their own login history
ALTER TABLE sec_schema.login_audit_log ENABLE ROW LEVEL SECURITY;

CREATE POLICY audit_log_isolation ON sec_schema.login_audit_log
    USING (
        current_setting('app.current_user_role', true) IN ('SUPER_ADMIN', 'ADMIN')
        OR
        user_id::text = current_setting('app.current_user_id', true)
    );

-- Consent Records: users see only their own consents
ALTER TABLE sec_schema.consent_records ENABLE ROW LEVEL SECURITY;

CREATE POLICY consent_isolation ON sec_schema.consent_records
    USING (
        current_setting('app.current_user_role', true) IN ('SUPER_ADMIN', 'ADMIN')
        OR
        user_id::text = current_setting('app.current_user_id', true)
    );

-- Data Access Logs: admin-only visibility (HIPAA audit trail)
ALTER TABLE sec_schema.data_access_log ENABLE ROW LEVEL SECURITY;

CREATE POLICY data_access_admin_only ON sec_schema.data_access_log
    USING (
        current_setting('app.current_user_role', true) IN ('SUPER_ADMIN', 'ADMIN')
    );

-- User Sessions: users see only their own sessions
ALTER TABLE sec_schema.user_sessions ENABLE ROW LEVEL SECURITY;

CREATE POLICY session_isolation ON sec_schema.user_sessions
    USING (
        current_setting('app.current_user_role', true) IN ('SUPER_ADMIN', 'ADMIN')
        OR
        user_id::text = current_setting('app.current_user_id', true)
    );

-- ─── Service Account bypass (for microservice internal operations) ──────

-- Create application role that bypasses RLS for service-to-service calls
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'biolab_service') THEN
CREATE ROLE biolab_service NOLOGIN;
END IF;
END $$;

-- Grant the service role ability to bypass RLS
ALTER TABLE app_schema.organizations FORCE ROW LEVEL SECURITY;
ALTER TABLE app_schema.user_organizations FORCE ROW LEVEL SECURITY;

COMMENT ON POLICY org_isolation_policy ON app_schema.organizations IS
    'HIPAA §164.312(a): Organization-scoped data isolation. Admins see all; others see own org only.';
COMMENT ON POLICY user_org_isolation_policy ON app_schema.user_organizations IS
    'GDPR Art.25: Privacy by design. Users see memberships for their own org or themselves.';
-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs — Security Functions, Audit Triggers & PII Protection
-- Version: V5
-- Compliance: HIPAA — PHI encryption, audit trails | FDA 21 CFR Part 11 — timestamps
-- ════════════════════════════════════════════════════════════════════════════

-- ─── PII Masking Function (for query results in logs) ───────────────────

CREATE OR REPLACE FUNCTION sec_schema.mask_email(email TEXT)
RETURNS TEXT AS $$
BEGIN
    IF email IS NULL OR length(email) < 5 THEN RETURN '***@***'; END IF;
RETURN substring(email, 1, 2) || '***@***' || substring(email FROM position('@' IN email) + 1);
END;
$$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION sec_schema.mask_phone(phone TEXT)
RETURNS TEXT AS $$
BEGIN
    IF phone IS NULL OR length(phone) < 4 THEN RETURN '****'; END IF;
RETURN '***-***-' || right(phone, 4);
END;
$$ LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION sec_schema.mask_email IS 'Masks email for audit log display — HIPAA minimum necessary principle';
COMMENT ON FUNCTION sec_schema.mask_phone IS 'Masks phone for audit log display — HIPAA minimum necessary principle';

-- ─── Immutable Audit Log Protection (append-only) ───────────────────────

-- Prevent UPDATE and DELETE on login_audit_log (HIPAA tamper-proof requirement)
CREATE OR REPLACE FUNCTION sec_schema.prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit logs are immutable — UPDATE and DELETE operations are prohibited (HIPAA §164.312(b))';
RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_login_audit_immutable
    BEFORE UPDATE OR DELETE ON sec_schema.login_audit_log
    FOR EACH ROW EXECUTE FUNCTION sec_schema.prevent_audit_modification();

CREATE TRIGGER trg_data_access_immutable
    BEFORE UPDATE OR DELETE ON sec_schema.data_access_log
    FOR EACH ROW EXECUTE FUNCTION sec_schema.prevent_audit_modification();

-- ─── Password Rotation Tracking ─────────────────────────────────────────

-- Function to check if password rotation is due (90-day policy from Slide 10)
CREATE OR REPLACE FUNCTION sec_schema.is_password_rotation_due(p_user_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
last_change TIMESTAMPTZ;
BEGIN
SELECT password_changed_at INTO last_change
FROM sec_schema.users WHERE id = p_user_id;

IF last_change IS NULL THEN RETURN TRUE; END IF;
RETURN (CURRENT_TIMESTAMP - last_change) > INTERVAL '90 days';
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION sec_schema.is_password_rotation_due IS
    'Checks if user password exceeds 90-day rotation policy (Slide 10)';

-- ─── Automatic Timestamp Updates ────────────────────────────────────────

CREATE OR REPLACE FUNCTION sec_schema.update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to users table
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_users_updated_at') THEN
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON sec_schema.users
    FOR EACH ROW EXECUTE FUNCTION sec_schema.update_timestamp();
END IF;
END $$;

-- ─── Session Cleanup (automatic expiry) ─────────────────────────────────

CREATE OR REPLACE FUNCTION sec_schema.cleanup_expired_sessions()
RETURNS INTEGER AS $$
DECLARE
expired_count INTEGER;
BEGIN
UPDATE sec_schema.user_sessions
SET is_active = false
WHERE is_active = true AND expires_at < CURRENT_TIMESTAMP;

GET DIAGNOSTICS expired_count = ROW_COUNT;
RETURN expired_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION sec_schema.cleanup_expired_sessions IS
    'Deactivates expired sessions — run via scheduled job or pg_cron';

-- ─── Security Indexes for Performance ───────────────────────────────────

-- Fast lookups for JWT blacklist validation
-- NOTE: Cannot use CURRENT_TIMESTAMP in partial index (not IMMUTABLE).
-- Expired tokens are filtered at query time and cleaned up by scheduled job.
CREATE INDEX IF NOT EXISTS idx_jwt_blacklist_jti
    ON sec_schema.jwt_token_blacklist (jti);

-- Fast lookups for active sessions
CREATE INDEX IF NOT EXISTS idx_sessions_user_active
    ON sec_schema.user_sessions (user_id) WHERE is_active = true;

-- Fast lookups for login audit by IP (anomaly detection)
CREATE INDEX IF NOT EXISTS idx_audit_ip_created
    ON sec_schema.login_audit_log (ip_address, created_at DESC);

-- Fast lookups for refresh token validation
CREATE INDEX IF NOT EXISTS idx_refresh_token_hash
    ON sec_schema.refresh_tokens (token_hash) WHERE is_revoked = false;

-- Fast lookups for password history
CREATE INDEX IF NOT EXISTS idx_password_history_user
    ON sec_schema.password_history (user_id, created_at DESC);

-- Fast lookups for consent by user and type
CREATE INDEX IF NOT EXISTS idx_consent_user_type
    ON sec_schema.consent_records (user_id, consent_type);

-- App schema indexes for organization isolation
CREATE INDEX IF NOT EXISTS idx_user_org_user
    ON app_schema.user_organizations (user_id);
CREATE INDEX IF NOT EXISTS idx_user_org_org
    ON app_schema.user_organizations (org_id);-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs — Business Domain Tables (app_schema)
-- Version: V6 | Catalog, Projects, Documents, Invoices, Messaging, Notifications, Audit
-- ════════════════════════════════════════════════════════════════════════════

-- ─── SERVICE CATALOG ─────────────────────────────────────────────────────

CREATE TABLE app_schema.service_categories (
                                               id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                               name        VARCHAR(100)    NOT NULL UNIQUE,
                                               slug        VARCHAR(120)    NOT NULL UNIQUE,
                                               description TEXT,
                                               icon        VARCHAR(50),
                                               sort_order  INTEGER         NOT NULL DEFAULT 0,
                                               is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
                                               created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                               updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE app_schema.services (
                                     id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                     name              VARCHAR(255)    NOT NULL,
                                     slug              VARCHAR(280)    NOT NULL UNIQUE,
                                     category_id       UUID            NOT NULL REFERENCES app_schema.service_categories(id),
                                     supplier_org_id   UUID            NOT NULL REFERENCES app_schema.organizations(id),
                                     description       TEXT,
                                     methodology       TEXT,
                                     price_from        DECIMAL(12,2),
                                     turnaround        VARCHAR(50),
                                     rating            DECIMAL(2,1)    NOT NULL DEFAULT 0.0,
                                     review_count      INTEGER         NOT NULL DEFAULT 0,
                                     is_active         BOOLEAN         NOT NULL DEFAULT TRUE,
                                     created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                     updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_svc_category   ON app_schema.services(category_id);
CREATE INDEX idx_svc_supplier   ON app_schema.services(supplier_org_id);
CREATE INDEX idx_svc_active     ON app_schema.services(is_active);

CREATE TABLE app_schema.service_requests (
                                             id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                             service_id      UUID            NOT NULL REFERENCES app_schema.services(id),
                                             buyer_id        UUID            NOT NULL,
                                             buyer_org_id    UUID            REFERENCES app_schema.organizations(id),
                                             sample_type     VARCHAR(255),
                                             timeline        VARCHAR(20)     NOT NULL DEFAULT 'STANDARD',
                                             requirements    TEXT,
                                             priority        VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM'
                                                 CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
                                             status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                                                 CHECK (status IN ('PENDING','ACCEPTED','DECLINED','CANCELLED')),
                                             created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                             updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sr_service ON app_schema.service_requests(service_id);
CREATE INDEX idx_sr_buyer   ON app_schema.service_requests(buyer_id);
CREATE INDEX idx_sr_status  ON app_schema.service_requests(status);

-- ─── PROJECTS ────────────────────────────────────────────────────────────

CREATE TABLE app_schema.projects (
                                     id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                     title               VARCHAR(255)    NOT NULL,
                                     service_request_id  UUID            REFERENCES app_schema.service_requests(id),
                                     buyer_org_id        UUID            NOT NULL REFERENCES app_schema.organizations(id),
                                     supplier_org_id     UUID            NOT NULL REFERENCES app_schema.organizations(id),
                                     status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                                         CHECK (status IN ('PENDING','ACTIVE','IN_PROGRESS','IN_REVIEW','COMPLETED','CANCELLED','OVERDUE')),
                                     progress_pct        INTEGER         NOT NULL DEFAULT 0 CHECK (progress_pct BETWEEN 0 AND 100),
                                     budget              DECIMAL(12,2),
                                     start_date          DATE,
                                     deadline            DATE,
                                     completed_at        TIMESTAMPTZ,
                                     created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                     updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_proj_buyer    ON app_schema.projects(buyer_org_id);
CREATE INDEX idx_proj_supplier ON app_schema.projects(supplier_org_id);
CREATE INDEX idx_proj_status   ON app_schema.projects(status);

CREATE TABLE app_schema.project_members (
                                            id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                            project_id  UUID            NOT NULL REFERENCES app_schema.projects(id) ON DELETE CASCADE,
                                            user_id     UUID            NOT NULL,
                                            role        VARCHAR(20)     NOT NULL DEFAULT 'MEMBER'
                                                CHECK (role IN ('OWNER','MEMBER','VIEWER')),
                                            added_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                            CONSTRAINT uq_proj_member UNIQUE (project_id, user_id)
);

CREATE TABLE app_schema.project_milestones (
                                               id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                               project_id      UUID            NOT NULL REFERENCES app_schema.projects(id) ON DELETE CASCADE,
                                               title           VARCHAR(255)    NOT NULL,
                                               description     TEXT,
                                               milestone_date  DATE,
                                               is_completed    BOOLEAN         NOT NULL DEFAULT FALSE,
                                               completed_at    TIMESTAMPTZ,
                                               sort_order      INTEGER         NOT NULL DEFAULT 0,
                                               created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pm_project ON app_schema.project_milestones(project_id);

-- ─── DOCUMENTS ───────────────────────────────────────────────────────────

CREATE TABLE app_schema.documents (
                                      id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                      project_id      UUID            NOT NULL REFERENCES app_schema.projects(id) ON DELETE CASCADE,
                                      uploaded_by     UUID            NOT NULL,
                                      file_name       VARCHAR(500)    NOT NULL,
                                      file_type       VARCHAR(20),
                                      file_size       BIGINT          NOT NULL DEFAULT 0,
                                      storage_key     VARCHAR(1024)   NOT NULL,
                                      mime_type       VARCHAR(100),
                                      version         INTEGER         NOT NULL DEFAULT 1,
                                      checksum        VARCHAR(64),
                                      created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_doc_project ON app_schema.documents(project_id);
CREATE INDEX idx_doc_uploader ON app_schema.documents(uploaded_by);

-- ─── INVOICES ────────────────────────────────────────────────────────────

CREATE TABLE app_schema.invoices (
                                     id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                     invoice_number  VARCHAR(20)     NOT NULL UNIQUE,
                                     project_id      UUID            REFERENCES app_schema.projects(id),
                                     supplier_org_id UUID            NOT NULL REFERENCES app_schema.organizations(id),
                                     buyer_org_id    UUID            NOT NULL REFERENCES app_schema.organizations(id),
                                     status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT'
                                         CHECK (status IN ('DRAFT','SENT','VIEWED','PAID','OVERDUE','CANCELLED')),
                                     subtotal        DECIMAL(12,2)   NOT NULL DEFAULT 0,
                                     tax_rate        DECIMAL(5,2)    NOT NULL DEFAULT 0,
                                     tax_amount      DECIMAL(12,2)   NOT NULL DEFAULT 0,
                                     total           DECIMAL(12,2)   NOT NULL DEFAULT 0,
                                     issue_date      DATE            NOT NULL DEFAULT CURRENT_DATE,
                                     due_date        DATE,
                                     paid_date       DATE,
                                     notes           TEXT,
                                     created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                     updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inv_supplier ON app_schema.invoices(supplier_org_id);
CREATE INDEX idx_inv_buyer    ON app_schema.invoices(buyer_org_id);
CREATE INDEX idx_inv_status   ON app_schema.invoices(status);
CREATE INDEX idx_inv_number   ON app_schema.invoices(invoice_number);

CREATE TABLE app_schema.invoice_items (
                                          id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                          invoice_id  UUID            NOT NULL REFERENCES app_schema.invoices(id) ON DELETE CASCADE,
                                          description VARCHAR(500)    NOT NULL,
                                          quantity    INTEGER         NOT NULL DEFAULT 1,
                                          unit_price  DECIMAL(12,2)   NOT NULL,
                                          amount      DECIMAL(12,2)   NOT NULL,
                                          sort_order  INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_ii_invoice ON app_schema.invoice_items(invoice_id);

-- ─── MESSAGING ───────────────────────────────────────────────────────────

CREATE TABLE app_schema.conversations (
                                          id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                          project_id      UUID            REFERENCES app_schema.projects(id),
                                          title           VARCHAR(255),
                                          created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                          updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE app_schema.conversation_participants (
                                                      id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                                      conversation_id UUID            NOT NULL REFERENCES app_schema.conversations(id) ON DELETE CASCADE,
                                                      user_id         UUID            NOT NULL,
                                                      org_id          UUID            REFERENCES app_schema.organizations(id),
                                                      joined_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                                      last_read_at    TIMESTAMPTZ,
                                                      CONSTRAINT uq_conv_part UNIQUE (conversation_id, user_id)
);

CREATE TABLE app_schema.messages (
                                     id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                     conversation_id UUID            NOT NULL REFERENCES app_schema.conversations(id) ON DELETE CASCADE,
                                     sender_id       UUID            NOT NULL,
                                     content         TEXT            NOT NULL,
                                     attachment_id   UUID            REFERENCES app_schema.documents(id),
                                     is_read         BOOLEAN         NOT NULL DEFAULT FALSE,
                                     created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_msg_conv    ON app_schema.messages(conversation_id);
CREATE INDEX idx_msg_sender  ON app_schema.messages(sender_id);
CREATE INDEX idx_msg_created ON app_schema.messages(created_at DESC);

-- ─── NOTIFICATIONS ───────────────────────────────────────────────────────

CREATE TABLE app_schema.notifications (
                                          id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                          user_id     UUID            NOT NULL,
                                          type        VARCHAR(50)     NOT NULL,
                                          title       VARCHAR(255)    NOT NULL,
                                          message     TEXT,
                                          link        VARCHAR(500),
                                          is_read     BOOLEAN         NOT NULL DEFAULT FALSE,
                                          created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_user     ON app_schema.notifications(user_id);
CREATE INDEX idx_notif_unread   ON app_schema.notifications(user_id, is_read) WHERE NOT is_read;

CREATE TABLE app_schema.notification_preferences (
                                                     id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                                     user_id         UUID            NOT NULL UNIQUE,
                                                     email_enabled   BOOLEAN         NOT NULL DEFAULT TRUE,
                                                     project_updates BOOLEAN         NOT NULL DEFAULT TRUE,
                                                     new_messages    BOOLEAN         NOT NULL DEFAULT TRUE,
                                                     invoice_reminders BOOLEAN       NOT NULL DEFAULT TRUE,
                                                     security_alerts BOOLEAN         NOT NULL DEFAULT TRUE,
                                                     marketing       BOOLEAN         NOT NULL DEFAULT FALSE,
                                                     updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ─── AUDIT / COMPLIANCE ──────────────────────────────────────────────────

CREATE TABLE app_schema.audit_events (
                                         id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                         user_id     UUID,
                                         action      VARCHAR(100)    NOT NULL,
                                         entity_type VARCHAR(50)     NOT NULL,
                                         entity_id   UUID,
                                         details     JSONB,
                                         ip_address  VARCHAR(45),
                                         created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user     ON app_schema.audit_events(user_id);
CREATE INDEX idx_audit_action   ON app_schema.audit_events(action);
CREATE INDEX idx_audit_created  ON app_schema.audit_events(created_at DESC);

CREATE TABLE app_schema.compliance_audits (
                                              id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                              audit_date  DATE            NOT NULL,
                                              audit_type  VARCHAR(100)    NOT NULL,
                                              result      VARCHAR(20)     NOT NULL DEFAULT 'PASSED'
                                                  CHECK (result IN ('PASSED','FAILED','PENDING')),
                                              findings    INTEGER         NOT NULL DEFAULT 0,
                                              auditor     VARCHAR(255),
                                              report_url  VARCHAR(512),
                                              notes       TEXT,
                                              created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE app_schema.policy_documents (
                                             id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                             name        VARCHAR(255)    NOT NULL,
                                             version     VARCHAR(20)     NOT NULL,
                                             status      VARCHAR(20)     NOT NULL DEFAULT 'CURRENT'
                                                 CHECK (status IN ('CURRENT','REVIEW','ARCHIVED')),
                                             content_url VARCHAR(512),
                                             updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                             created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE app_schema.platform_settings (
                                              id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                              key         VARCHAR(100)    NOT NULL UNIQUE,
                                              value       TEXT            NOT NULL,
                                              category    VARCHAR(50)     NOT NULL DEFAULT 'GENERAL',
                                              updated_by  UUID,
                                              updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ─── TRIGGERS ────────────────────────────────────────────────────────────

CREATE TRIGGER trg_svc_cat_updated BEFORE UPDATE ON app_schema.service_categories
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
CREATE TRIGGER trg_svc_updated BEFORE UPDATE ON app_schema.services
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
CREATE TRIGGER trg_sr_updated BEFORE UPDATE ON app_schema.service_requests
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
CREATE TRIGGER trg_proj_updated BEFORE UPDATE ON app_schema.projects
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
CREATE TRIGGER trg_inv_updated BEFORE UPDATE ON app_schema.invoices
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
CREATE TRIGGER trg_conv_updated BEFORE UPDATE ON app_schema.conversations
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
CREATE TRIGGER trg_notif_pref_updated BEFORE UPDATE ON app_schema.notification_preferences
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs — Seed Data for Business Tables
-- Version: V7 | Categories, sample services, compliance audits, policies
-- ════════════════════════════════════════════════════════════════════════════

-- ─── Service Categories ──────────────────────────────────────────────────
INSERT INTO app_schema.service_categories (id, name, slug, description, icon, sort_order) VALUES
                                                                                              ('a1000000-0000-0000-0000-000000000001', 'Biochemical',      'biochemical',      'Enzyme kinetics, HPLC, mass spec, binding assays',       'FlaskConical', 1),
                                                                                              ('a1000000-0000-0000-0000-000000000002', 'Protein Sciences',  'protein-sciences',  'Purification, characterization, antibody development',    'Database',     2),
                                                                                              ('a1000000-0000-0000-0000-000000000003', 'Cell Biology',      'cell-biology',      'Cell-based assays, flow cytometry, cell line development', 'Microscope',   3),
                                                                                              ('a1000000-0000-0000-0000-000000000004', 'Bioprocess',        'bioprocess',        'CHO optimization, bioprocess scale-up',                   'Zap',          4),
                                                                                              ('a1000000-0000-0000-0000-000000000005', 'Genomics',          'genomics',          'PCR, gene expression, microbiome analysis',               'Dna',          5),
                                                                                              ('a1000000-0000-0000-0000-000000000006', 'Compliance',        'compliance',        'Stability, endotoxin, dissolution, toxicology',           'ShieldCheck',  6);

-- ─── Sample Organizations (Suppliers) ────────────────────────────────────
INSERT INTO app_schema.organizations (id, name, type, is_active) VALUES
                                                                     ('b1000000-0000-0000-0000-000000000001', 'BioLabs Alpha',       'SUPPLIER', true),
                                                                     ('b1000000-0000-0000-0000-000000000002', 'CoreGen Labs',        'SUPPLIER', true),
                                                                     ('b1000000-0000-0000-0000-000000000003', 'Pacific BioLabs',     'SUPPLIER', true),
                                                                     ('b1000000-0000-0000-0000-000000000004', 'SynBio Solutions',    'SUPPLIER', true),
                                                                     ('b1000000-0000-0000-0000-000000000005', 'Helix Dynamics',      'SUPPLIER', true),
                                                                     ('b1000000-0000-0000-0000-000000000006', 'MolecuLab',           'SUPPLIER', true)
    ON CONFLICT (id) DO NOTHING;

-- ─── Sample Organizations (Buyers) ──────────────────────────────────────
INSERT INTO app_schema.organizations (id, name, type, is_active) VALUES
                                                                     ('c1000000-0000-0000-0000-000000000001', 'PharmaCorp Inc.',        'BUYER', true),
                                                                     ('c1000000-0000-0000-0000-000000000002', 'GeneTech Labs',          'BUYER', true),
                                                                     ('c1000000-0000-0000-0000-000000000003', 'BioVista Research',      'BUYER', true),
                                                                     ('c1000000-0000-0000-0000-000000000004', 'MediSync Pharma',        'BUYER', true),
                                                                     ('c1000000-0000-0000-0000-000000000005', 'NovaBio Therapeutics',   'BUYER', true),
                                                                     ('c1000000-0000-0000-0000-000000000006', 'CureLogic',              'BUYER', true),
                                                                     ('c1000000-0000-0000-0000-000000000007', 'Vertex Bio',             'BUYER', true),
                                                                     ('c1000000-0000-0000-0000-000000000008', 'OmniCell Research',      'BUYER', true),
                                                                     ('c1000000-0000-0000-0000-000000000009', 'Elara Therapeutics',     'BUYER', true),
                                                                     ('c1000000-0000-0000-0000-000000000010', 'Nexus Pharma',           'BUYER', true)
    ON CONFLICT (id) DO NOTHING;

-- ─── Sample Services ─────────────────────────────────────────────────────
INSERT INTO app_schema.services (name, slug, category_id, supplier_org_id, description, price_from, turnaround, rating, review_count) VALUES
                                                                                                                                          ('Enzyme Kinetics Analysis', 'enzyme-kinetics-analysis', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 'Full Michaelis-Menten kinetic profiling with inhibition constants', 2800, '3-7 days', 4.9, 47),
                                                                                                                                          ('Protein Characterization', 'protein-characterization', 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000002', 'SEC-MALS, DSC, DLS for biophysical characterization', 3500, '5-10 days', 4.8, 32),
                                                                                                                                          ('Cell-Based Assays', 'cell-based-assays', 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000001', 'Viability, proliferation, and cytotoxicity panels', 3000, '5-10 days', 4.9, 56),
                                                                                                                                          ('Stability Studies', 'stability-studies', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000003', 'ICH Q1A/Q1B compliant stability programs', 4500, '6-12 months', 4.9, 24),
                                                                                                                                          ('Bioprocess Optimization', 'bioprocess-optimization', 'a1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000005', 'CHO cell culture and media optimization', 5000, '6-12 weeks', 4.7, 18),
                                                                                                                                          ('Flow Cytometry Panel', 'flow-cytometry-panel', 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000001', 'Multi-color flow cytometry with immune profiling', 1800, '3-5 days', 4.8, 38),
                                                                                                                                          ('Mass Spectrometry', 'mass-spectrometry', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000006', 'LC-MS/MS for proteomics and metabolomics', 3100, '4-8 days', 4.8, 29),
                                                                                                                                          ('Western Blot Analysis', 'western-blot-analysis', 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000006', 'Quantitative western blot with phospho-panels', 1500, '2-4 days', 4.7, 64),
                                                                                                                                          ('PCR & qPCR Services', 'pcr-qpcr-services', 'a1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000002', 'Real-time PCR with gene expression analysis', 2200, '2-5 days', 4.9, 78),
                                                                                                                                          ('HPLC Analysis', 'hplc-analysis', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 'Method development and validation for small molecules', 2600, '3-7 days', 4.8, 41),
                                                                                                                                          ('Gene Expression Profiling', 'gene-expression-profiling', 'a1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000002', 'RNA-Seq and microarray analysis', 5400, '7-14 days', 4.9, 19),
                                                                                                                                          ('Binding Assay Suite', 'binding-assay-suite', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000004', 'SPR and BLI for affinity measurements', 3300, '5-10 days', 4.7, 27),
                                                                                                                                          ('Protein Purification', 'protein-purification', 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000002', 'Affinity, ion exchange, and SEC purification', 3900, '5-10 days', 4.8, 35),
                                                                                                                                          ('Endotoxin Testing', 'endotoxin-testing', 'a1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000003', 'LAL and rFC assays for endotoxin quantification', 1500, '1-3 days', 4.9, 88),
                                                                                                                                          ('Dissolution Studies', 'dissolution-studies', 'a1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000003', 'USP apparatus I/II with full profiles', 3600, '3-6 weeks', 4.7, 15),
                                                                                                                                          ('Metabolomics Panel', 'metabolomics-panel', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000006', 'Untargeted metabolomics with pathway analysis', 5700, '7-14 days', 4.8, 12),
                                                                                                                                          ('Cell Line Development', 'cell-line-development', 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000001', 'Stable cell line generation and characterization', 8500, '8-16 weeks', 4.9, 9),
                                                                                                                                          ('Microbiome Analysis', 'microbiome-analysis', 'a1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000005', '16S rRNA and shotgun metagenomics', 4800, '7-14 days', 4.6, 16),
                                                                                                                                          ('Toxicology Assessment', 'toxicology-assessment', 'a1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000003', 'GLP-compliant acute and chronic toxicology', 8200, '4-8 weeks', 4.8, 11),
                                                                                                                                          ('Antibody Development', 'antibody-development', 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000004', 'Custom monoclonal and polyclonal antibody generation', 12000, '12-20 weeks', 4.9, 8),
                                                                                                                                          ('Immunohistochemistry', 'immunohistochemistry', 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000006', 'IHC staining and quantitative image analysis', 3400, '5-8 days', 4.6, 22),
                                                                                                                                          ('Drug Metabolism Panel', 'drug-metabolism-panel', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000002', 'CYP inhibition and induction studies', 6800, '5-10 days', 4.7, 14),
                                                                                                                                          ('Bioavailability Study', 'bioavailability-study', 'a1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000003', 'PK/PD modeling with bioavailability determination', 11500, '8-16 weeks', 4.8, 6),
                                                                                                                                          ('CHO Cell Optimization', 'cho-cell-optimization', 'a1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000005', 'DoE-based CHO process development', 7200, '6-12 weeks', 4.7, 10);

-- ─── Compliance Audits ───────────────────────────────────────────────────
INSERT INTO app_schema.compliance_audits (audit_date, audit_type, result, findings) VALUES
                                                                                        ('2025-12-15', 'HIPAA Assessment', 'PASSED', 0),
                                                                                        ('2025-11-20', 'GDPR Compliance Review', 'PASSED', 1),
                                                                                        ('2025-10-10', 'SOC 2 Type II Audit', 'PASSED', 0),
                                                                                        ('2025-09-01', 'FDA 21 CFR Part 11', 'PASSED', 2),
                                                                                        ('2025-08-15', 'Penetration Testing', 'PASSED', 3),
                                                                                        ('2025-07-05', 'ISO 27001 Surveillance', 'PASSED', 0),
                                                                                        ('2025-06-20', 'Data Privacy Impact Assessment', 'PASSED', 1),
                                                                                        ('2025-05-10', 'Business Continuity Test', 'PASSED', 0),
                                                                                        ('2025-04-01', 'Vendor Security Review', 'PASSED', 2),
                                                                                        ('2025-03-15', 'Internal Vulnerability Scan', 'PASSED', 4),
                                                                                        ('2025-02-10', 'HIPAA Risk Assessment', 'PASSED', 1),
                                                                                        ('2025-01-05', 'SOC 2 Type I Readiness', 'PASSED', 0);

-- ─── Policy Documents ────────────────────────────────────────────────────
INSERT INTO app_schema.policy_documents (name, version, status) VALUES
                                                                    ('Data Protection Policy', 'v4.2', 'CURRENT'),
                                                                    ('Incident Response Plan', 'v3.1', 'CURRENT'),
                                                                    ('Access Control Policy', 'v5.0', 'CURRENT'),
                                                                    ('Encryption Standards', 'v2.8', 'CURRENT'),
                                                                    ('Vendor Security Policy', 'v1.5', 'REVIEW'),
                                                                    ('Acceptable Use Policy', 'v3.4', 'CURRENT'),
                                                                    ('Password Management Policy', 'v2.1', 'CURRENT'),
                                                                    ('Data Retention Policy', 'v1.9', 'CURRENT'),
                                                                    ('Change Management Procedure', 'v4.0', 'CURRENT'),
                                                                    ('Disaster Recovery Plan', 'v2.5', 'REVIEW'),
                                                                    ('Network Security Policy', 'v3.7', 'CURRENT'),
                                                                    ('Physical Security Policy', 'v1.3', 'CURRENT'),
                                                                    ('Third-Party Risk Framework', 'v2.0', 'CURRENT'),
                                                                    ('Data Classification Guide', 'v1.6', 'REVIEW');

-- ─── Platform Settings ───────────────────────────────────────────────────
INSERT INTO app_schema.platform_settings (key, value, category) VALUES
                                                                    ('session.timeout', '30', 'SECURITY'),
                                                                    ('password.min_length', '8', 'SECURITY'),
                                                                    ('password.rotation_days', '90', 'SECURITY'),
                                                                    ('mfa.required', 'true', 'SECURITY'),
                                                                    ('rate_limit.requests_per_minute', '100', 'SECURITY'),
                                                                    ('data.retention_years', '7', 'COMPLIANCE'),
                                                                    ('data.encryption_algorithm', 'AES-256-GCM', 'COMPLIANCE'),
                                                                    ('email.notifications_enabled', 'true', 'NOTIFICATIONS'),
                                                                    ('platform.maintenance_mode', 'false', 'GENERAL'),
                                                                    ('platform.max_file_size_mb', '100', 'GENERAL');
-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs — Add Missing Audit Columns to consent_records
-- Version: V6
-- Fix: Hibernate schema-validation failed — missing created_at / updated_at
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE sec_schema.consent_records
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE sec_schema.consent_records
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs — Add Missing BaseEntity Audit Columns to All Tables
-- Version: V9
-- Fix: Hibernate schema-validation fails — tables created without
--      created_at / updated_at columns expected by BaseEntity superclass
-- ════════════════════════════════════════════════════════════════════════════

-- jwt_token_blacklist
ALTER TABLE sec_schema.jwt_token_blacklist
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sec_schema.jwt_token_blacklist
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- login_audit_log
ALTER TABLE sec_schema.login_audit_log
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sec_schema.login_audit_log
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- data_access_log
ALTER TABLE sec_schema.data_access_log
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sec_schema.data_access_log
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- password_history
ALTER TABLE sec_schema.password_history
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sec_schema.password_history
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- refresh_tokens
ALTER TABLE sec_schema.refresh_tokens
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sec_schema.refresh_tokens
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- user_sessions
ALTER TABLE sec_schema.user_sessions
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sec_schema.user_sessions
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- users (may already have these — IF NOT EXISTS prevents errors)
ALTER TABLE sec_schema.users
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sec_schema.users
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- roles
ALTER TABLE sec_schema.roles
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sec_schema.roles
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- permissions
ALTER TABLE sec_schema.permissions
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sec_schema.permissions
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- app_schema tables
ALTER TABLE app_schema.user_organizations
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE app_schema.user_organizations
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs — Add Missing BaseEntity Audit Columns to Remaining Tables
-- Version: V10
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE sec_schema.user_roles
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sec_schema.user_roles
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE sec_schema.role_permissions
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sec_schema.role_permissions
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;ALTER TABLE sec_schema.refresh_tokens
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ;
-- ═══════════════════════════════════════════════════════════════════════
-- V12 — Password Reset Tokens
-- Stores short-lived, single-use tokens for the forgot-password flow.
-- ═══════════════════════════════════════════════════════════════════════

CREATE TABLE sec_schema.password_reset_tokens (
                                                  id          UUID        NOT NULL DEFAULT gen_random_uuid(),
                                                  user_id     UUID        NOT NULL,
                                                  token_hash  VARCHAR(255) NOT NULL,          -- BCrypt hash of the raw token
                                                  expires_at  TIMESTAMPTZ NOT NULL,
                                                  used        BOOLEAN     NOT NULL DEFAULT FALSE,
                                                  used_at     TIMESTAMPTZ,
                                                  ip_address  VARCHAR(45),
                                                  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                                  CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
                                                  CONSTRAINT fk_prt_user FOREIGN KEY (user_id)
                                                      REFERENCES sec_schema.users (id) ON DELETE CASCADE,
                                                  CONSTRAINT uq_prt_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_prt_user_id   ON sec_schema.password_reset_tokens (user_id);
CREATE INDEX idx_prt_expires   ON sec_schema.password_reset_tokens (expires_at);

COMMENT ON TABLE  sec_schema.password_reset_tokens              IS 'Single-use password reset tokens — 15-minute TTL';
COMMENT ON COLUMN sec_schema.password_reset_tokens.token_hash  IS 'BCrypt hash of the raw token sent via email — raw token is never stored';
COMMENT ON COLUMN sec_schema.password_reset_tokens.used        IS 'True once the token has been consumed — cannot be reused';

-- ════════════════════════════════════════════════════════════════════════════
-- SEED USERS  (BCrypt cost=10, password: nitin@1976)
-- ════════════════════════════════════════════════════════════════════════════
INSERT INTO sec_schema.users (
    email, password_hash, first_name, last_name,
    phone, is_active, is_email_verified, password_changed_at
) VALUES
      ('ndate1976@gmail.com',
       '$2a$10$QBdxg2KqYf3zMPJwggno2eCML9U4ZoxmeiNd69iA1cj5LEycVygcq',
       'Nitin', 'Date', '+91-9000000001', TRUE, TRUE, NOW()),
      ('supplier@biolab.com',
       '$2a$10$QBdxg2KqYf3zMPJwggno2eCML9U4ZoxmeiNd69iA1cj5LEycVygcq',
       'Supplier', 'Test', '+91-9000000002', TRUE, TRUE, NOW()),
      ('buyer@biolab.com',
       '$2a$10$QBdxg2KqYf3zMPJwggno2eCML9U4ZoxmeiNd69iA1cj5LEycVygcq',
       'Buyer', 'Test', '+91-9000000003', TRUE, TRUE, NOW());

-- Role assignments (lookup by name — no hardcoded UUIDs)
INSERT INTO sec_schema.user_roles (user_id, role_id)
SELECT u.id, r.id FROM sec_schema.users u, sec_schema.roles r
WHERE u.email = 'ndate1976@gmail.com' AND r.name = 'SUPER_ADMIN';

INSERT INTO sec_schema.user_roles (user_id, role_id)
SELECT u.id, r.id FROM sec_schema.users u, sec_schema.roles r
WHERE u.email = 'supplier@biolab.com' AND r.name = 'SUPPLIER';

INSERT INTO sec_schema.user_roles (user_id, role_id)
SELECT u.id, r.id FROM sec_schema.users u, sec_schema.roles r
WHERE u.email = 'buyer@biolab.com' AND r.name = 'BUYER';

-- Password history
INSERT INTO sec_schema.password_history (user_id, password_hash)
SELECT id, '$2a$10$QBdxg2KqYf3zMPJwggno2eCML9U4ZoxmeiNd69iA1cj5LEycVygcq'
FROM sec_schema.users
WHERE email IN ('ndate1976@gmail.com', 'supplier@biolab.com', 'buyer@biolab.com');

-- ── VERIFY ────────────────────────────────────────────────────────────────
SELECT u.email, LEFT(u.password_hash,7) AS hash_prefix,
    u.is_active, u.is_email_verified, r.name AS role
FROM sec_schema.users u
    JOIN sec_schema.user_roles ur ON ur.user_id = u.id
    JOIN sec_schema.roles r       ON r.id = ur.role_id
ORDER BY u.email;



CREATE TABLE IF NOT EXISTS sec_schema.email_verification_tokens (
                                                                    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES sec_schema.users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,           -- SHA-256 hex of raw token
    expires_at  TIMESTAMPTZ NOT NULL,                  -- 24 hours from creation
    used_at     TIMESTAMPTZ,                           -- NULL = not yet used
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_evt_token_hash ON sec_schema.email_verification_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_evt_user_id    ON sec_schema.email_verification_tokens(user_id);

COMMENT ON TABLE  sec_schema.email_verification_tokens            IS 'One-use email verification tokens. Raw token is never stored — only SHA-256 hash.';
COMMENT ON COLUMN sec_schema.email_verification_tokens.token_hash IS 'SHA-256 hex digest of the raw URL token.';
COMMENT ON COLUMN sec_schema.email_verification_tokens.used_at    IS 'Set when token is consumed. NULL = still valid (if not expired).';

-- Also ensure existing seeded users are marked as verified
-- (they were created directly via SQL so have no verification token)
UPDATE sec_schema.users
SET is_email_verified = true
WHERE email IN ('ndate1976@gmail.com', 'supplier@biolab.com', 'buyer@biolab.com');

SELECT 'email_verification_tokens table created' AS status;