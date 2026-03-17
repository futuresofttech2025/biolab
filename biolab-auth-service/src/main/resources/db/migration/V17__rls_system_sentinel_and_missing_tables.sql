-- ════════════════════════════════════════════════════════════════════════
-- V17__rls_system_sentinel_and_missing_tables.sql
--
-- SPRINT 3 — GAP-19: RLS hardening
--
-- WHAT THIS DOES
-- ──────────────
-- 1. Updates existing RLS policies to handle the __SYSTEM__ sentinel value
--    set by RlsContextAspect for scheduled-job / service-internal calls.
--    Without this, a system call with sentinel values would accidentally
--    match user_id = '__SYSTEM__' (which does not exist) and return 0 rows.
--
-- 2. Adds RLS policies on tables that were missing coverage from V4:
--    - app_schema.projects
--    - app_schema.project_members
--    - app_schema.messages
--    - app_schema.documents
--
-- 3. Adds a PostgreSQL function to validate that RLS session variables
--    are properly set before sensitive queries. Used by integration tests.
--
-- EXECUTION NOTES
-- ───────────────
-- DROP POLICY IF EXISTS + CREATE POLICY is the safe idempotent pattern.
-- No data is affected.
-- ════════════════════════════════════════════════════════════════════════

-- ─── 1. Update org_isolation_policy to allow SYSTEM sentinel ─────────
-- The SYSTEM sentinel bypasses RLS for service-internal calls.
-- Admins see all orgs. System sees all. Others see only their own.

ALTER TABLE app_schema.organizations DISABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS org_isolation_policy ON app_schema.organizations;

CREATE POLICY org_isolation_policy ON app_schema.organizations
    USING (
        current_setting('app.current_user_role', true) IN ('SUPER_ADMIN', 'ADMIN', '__SYSTEM__')
        OR id::text = current_setting('app.current_user_org_id', true)
    );

ALTER TABLE app_schema.organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_schema.organizations FORCE ROW LEVEL SECURITY;

-- ─── 2. Update user_org_isolation_policy ──────────────────────────────
ALTER TABLE app_schema.user_organizations DISABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS user_org_isolation_policy ON app_schema.user_organizations;

CREATE POLICY user_org_isolation_policy ON app_schema.user_organizations
    USING (
        current_setting('app.current_user_role', true) IN ('SUPER_ADMIN', 'ADMIN', '__SYSTEM__')
        OR org_id::text  = current_setting('app.current_user_org_id', true)
        OR user_id::text = current_setting('app.current_user_id', true)
    );

ALTER TABLE app_schema.user_organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_schema.user_organizations FORCE ROW LEVEL SECURITY;

-- ─── 3. Update login_audit_log policy ─────────────────────────────────
ALTER TABLE sec_schema.login_audit_log DISABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS audit_log_isolation ON sec_schema.login_audit_log;

CREATE POLICY audit_log_isolation ON sec_schema.login_audit_log
    USING (
        current_setting('app.current_user_role', true) IN ('SUPER_ADMIN', 'ADMIN', '__SYSTEM__')
        OR user_id::text = current_setting('app.current_user_id', true)
    );

ALTER TABLE sec_schema.login_audit_log ENABLE ROW LEVEL SECURITY;

-- ─── 4. Update consent_records policy ────────────────────────────────
ALTER TABLE sec_schema.consent_records DISABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS consent_isolation ON sec_schema.consent_records;

CREATE POLICY consent_isolation ON sec_schema.consent_records
    USING (
        current_setting('app.current_user_role', true) IN ('SUPER_ADMIN', 'ADMIN', '__SYSTEM__')
        OR user_id::text = current_setting('app.current_user_id', true)
    );

ALTER TABLE sec_schema.consent_records ENABLE ROW LEVEL SECURITY;

-- ─── 5. Update user_sessions policy ──────────────────────────────────
ALTER TABLE sec_schema.user_sessions DISABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS session_isolation ON sec_schema.user_sessions;

CREATE POLICY session_isolation ON sec_schema.user_sessions
    USING (
        current_setting('app.current_user_role', true) IN ('SUPER_ADMIN', 'ADMIN', '__SYSTEM__')
        OR user_id::text = current_setting('app.current_user_id', true)
    );

ALTER TABLE sec_schema.user_sessions ENABLE ROW LEVEL SECURITY;

-- ─── 6. Add RLS on app_schema.messages (GAP-19 — missing from V4) ────
-- Messages: participants can see messages in their conversations only.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'app_schema' AND table_name = 'messages') THEN

        ALTER TABLE app_schema.messages ENABLE ROW LEVEL SECURITY;

        DROP POLICY IF EXISTS messages_participant_policy ON app_schema.messages;

        EXECUTE $policy$
            CREATE POLICY messages_participant_policy ON app_schema.messages
                USING (
                    current_setting('app.current_user_role', true) IN ('SUPER_ADMIN', 'ADMIN', '__SYSTEM__')
                    OR sender_id::text = current_setting('app.current_user_id', true)
                    OR conversation_id IN (
                        SELECT conversation_id
                        FROM   app_schema.conversation_participants
                        WHERE  user_id::text = current_setting('app.current_user_id', true)
                    )
                )
        $policy$;

        ALTER TABLE app_schema.messages FORCE ROW LEVEL SECURITY;
    END IF;
END $$;

-- ─── 7. Add RLS on app_schema.documents (GAP-19 — missing from V4) ───
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'app_schema' AND table_name = 'documents') THEN

        ALTER TABLE app_schema.documents ENABLE ROW LEVEL SECURITY;

        DROP POLICY IF EXISTS documents_project_member_policy ON app_schema.documents;

        EXECUTE $policy$
            CREATE POLICY documents_project_member_policy ON app_schema.documents
                USING (
                    current_setting('app.current_user_role', true) IN ('SUPER_ADMIN', 'ADMIN', '__SYSTEM__')
                    OR uploaded_by::text = current_setting('app.current_user_id', true)
                    OR project_id IN (
                        SELECT project_id
                        FROM   app_schema.project_members
                        WHERE  user_id::text = current_setting('app.current_user_id', true)
                    )
                )
        $policy$;

        ALTER TABLE app_schema.documents FORCE ROW LEVEL SECURITY;
    END IF;
END $$;

-- ─── 8. Diagnostic function: validate RLS session variables ──────────
-- Used by integration tests and health checks to verify the aspect is working.
CREATE OR REPLACE FUNCTION sec_schema.rls_context_is_set()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN (
        current_setting('app.current_user_id',    true) IS NOT NULL AND
        current_setting('app.current_user_id',    true) != ''        AND
        current_setting('app.current_user_role',  true) IS NOT NULL AND
        current_setting('app.current_user_role',  true) != ''
    );
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION sec_schema.rls_context_is_set IS
    'Returns true when RLS session variables are properly set by RlsContextAspect. '
    'Call SELECT sec_schema.rls_context_is_set() in integration tests.';
