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
