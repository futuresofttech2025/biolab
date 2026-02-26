ALTER TABLE sec_schema.refresh_tokens
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ;
