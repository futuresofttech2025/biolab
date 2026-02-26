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
    ON app_schema.user_organizations (org_id);