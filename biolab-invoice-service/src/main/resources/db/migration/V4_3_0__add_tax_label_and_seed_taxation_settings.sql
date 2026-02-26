-- V4.3.0: Add tax_label to invoices + seed taxation platform settings
-- Part of: Admin Settings → Taxation tab feature

-- Add tax_label column to invoices
ALTER TABLE app_schema.invoices
    ADD COLUMN IF NOT EXISTS tax_label VARCHAR(50) DEFAULT 'Tax';

-- Seed default taxation settings (Admin → Settings → Taxation)
INSERT INTO app_schema.platform_settings (id, key, value, category, updated_at)
VALUES
    (gen_random_uuid(), 'taxation.default_tax_rate', '18', 'TAXATION', NOW()),
    (gen_random_uuid(), 'taxation.tax_label', 'GST', 'TAXATION', NOW()),
    (gen_random_uuid(), 'taxation.tax_enabled', 'true', 'TAXATION', NOW())
ON CONFLICT (key) DO NOTHING;
