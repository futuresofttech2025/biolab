-- ════════════════════════════════════════════════════════════════════════════
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
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;