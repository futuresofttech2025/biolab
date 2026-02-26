-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs — Add Missing Audit Columns to consent_records
-- Version: V6
-- Fix: Hibernate schema-validation failed — missing created_at / updated_at
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE sec_schema.consent_records
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE sec_schema.consent_records
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;