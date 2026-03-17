-- ════════════════════════════════════════════════════════════════════════
-- V14__encrypt_user_pii_columns.sql
--
-- PURPOSE
--   Widens PII columns on sec_schema.users to accommodate AES-256-GCM
--   ciphertext, then adds email_otp_expires_at to mfa_settings.
--
-- BACKGROUND (Encryption at Rest)
--   User.firstName, lastName, phone are now encrypted by the JPA
--   EncryptedStringConverter (AES-256-GCM, IV prepended, Base64-encoded).
--   Output size: ceil(plainLen/16)*16 + 28 bytes, ~1.33× as Base64.
--   A 100-char name → ~192 chars ciphertext → VARCHAR(512) is safe.
--
--   email is intentionally NOT encrypted — it is a lookup key.
--   passwordHash is already one-way BCrypt — no further encryption needed.
--
-- EXECUTION NOTES
--   This migration is SAFE to run on a live database:
--   - ALTER COLUMN … VARCHAR increase is non-destructive in PostgreSQL.
--   - Existing plaintext values remain readable until the application
--     re-encrypts them via a data migration job.
--
-- DATA MIGRATION (post-deploy, run separately)
--   After deploying the new application, run a one-off job that reads
--   every user record (which will auto-decrypt as empty / identity because
--   EncryptedStringConverter gracefully handles non-encrypted legacy values)
--   and re-saves it. The JPA converter will then encrypt on write.
-- ════════════════════════════════════════════════════════════════════════

-- ─── 1. Widen PII columns on users ───────────────────────────────────

ALTER TABLE sec_schema.users
    ALTER COLUMN first_name TYPE VARCHAR(512),
    ALTER COLUMN last_name  TYPE VARCHAR(512),
    ALTER COLUMN phone      TYPE VARCHAR(512);

COMMENT ON COLUMN sec_schema.users.first_name IS
    'AES-256-GCM encrypted first name (Base64, IV-prepended) — GDPR PII';
COMMENT ON COLUMN sec_schema.users.last_name IS
    'AES-256-GCM encrypted last name (Base64, IV-prepended) — GDPR PII';
COMMENT ON COLUMN sec_schema.users.phone IS
    'AES-256-GCM encrypted phone number (Base64, IV-prepended) — HIPAA PHI';

-- ─── 2. Add email_otp_expires_at to mfa_settings (FIX-14) ────────────

ALTER TABLE sec_schema.mfa_settings
    ADD COLUMN IF NOT EXISTS email_otp_expires_at TIMESTAMPTZ;

COMMENT ON COLUMN sec_schema.mfa_settings.email_otp_expires_at IS
    'Expiry timestamp for EMAIL OTP codes — enforces 10-minute validity window. '
    'NULL for TOTP records. Cleared after successful verification.';

-- ─── 3. Add token_hash unique index to password_reset_tokens (FIX-2) ─
-- Index already exists as uq_prt_token_hash from V12 — this is a no-op guard.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = 'sec_schema'
          AND tablename  = 'password_reset_tokens'
          AND indexname  = 'uq_prt_token_hash'
    ) THEN
        CREATE UNIQUE INDEX uq_prt_token_hash
            ON sec_schema.password_reset_tokens (token_hash);
    END IF;
END
$$;
