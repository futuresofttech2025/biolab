-- ════════════════════════════════════════════════════════════════════════════
-- V13__seed_admin_and_test_users.sql
-- Seeds platform admin + test accounts for development/testing.
--
-- Admin password : Nitin@1976
-- BCrypt cost=12 : $2b$12$WeoCR17i5fv21u7FXRC44ezuwWQhmSELUJ8WTeHUwhXP7Mm8fPRbe
--
-- Accounts:
--   ndate1976@gmail.com  → SUPER_ADMIN  (password: Nitin@1976)
--   supplier@biolab.com  → SUPPLIER     (password: Nitin@1976)
--   buyer@biolab.com     → BUYER        (password: Nitin@1976)
--
-- ⚠️  DEVELOPMENT ONLY — do not run in production.
-- ════════════════════════════════════════════════════════════════════════════

-- ─── Users ────────────────────────────────────────────────────────────────
INSERT INTO sec_schema.users (
    id,
    email,
    password_hash,
    first_name,
    last_name,
    phone,
    is_active,
    is_email_verified,
    password_changed_at
) VALUES
      (
          'd0000000-0000-0000-0000-000000000001'::uuid,
          'ndate1976@gmail.com',
          '$2b$12$WeoCR17i5fv21u7FXRC44ezuwWQhmSELUJ8WTeHUwhXP7Mm8fPRbe',
          'Nitin',
          'Date',
          '+91-9000000001',
          TRUE, TRUE, NOW()
      ),
      (
          'd0000000-0000-0000-0000-000000000002'::uuid,
          'supplier@biolab.com',
          '$2b$12$WeoCR17i5fv21u7FXRC44ezuwWQhmSELUJ8WTeHUwhXP7Mm8fPRbe',
          'Supplier',
          'Test',
          '+91-9000000002',
          TRUE, TRUE, NOW()
      ),
      (
          'd0000000-0000-0000-0000-000000000003'::uuid,
          'buyer@biolab.com',
          '$2b$12$WeoCR17i5fv21u7FXRC44ezuwWQhmSELUJ8WTeHUwhXP7Mm8fPRbe',
          'Buyer',
          'Test',
          '+91-9000000003',
          TRUE, TRUE, NOW()
      )
    ON CONFLICT (email) DO UPDATE SET
    password_hash       = EXCLUDED.password_hash,
                               first_name          = EXCLUDED.first_name,
                               last_name           = EXCLUDED.last_name,
                               is_active           = TRUE,
                               is_email_verified   = TRUE,
                               updated_at          = NOW();

-- ─── Role Assignments ─────────────────────────────────────────────────────
INSERT INTO sec_schema.user_roles (user_id, role_id)
VALUES
    ('d0000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid),
    ('d0000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000003'::uuid),
    ('d0000000-0000-0000-0000-000000000003'::uuid, 'a0000000-0000-0000-0000-000000000004'::uuid)
    ON CONFLICT ON CONSTRAINT uq_user_role DO NOTHING;

-- ─── Initial Password History ─────────────────────────────────────────────
INSERT INTO sec_schema.password_history (user_id, password_hash)
VALUES
    ('d0000000-0000-0000-0000-000000000001'::uuid, '$2b$12$WeoCR17i5fv21u7FXRC44ezuwWQhmSELUJ8WTeHUwhXP7Mm8fPRbe'),
    ('d0000000-0000-0000-0000-000000000002'::uuid, '$2b$12$WeoCR17i5fv21u7FXRC44ezuwWQhmSELUJ8WTeHUwhXP7Mm8fPRbe'),
    ('d0000000-0000-0000-0000-000000000003'::uuid, '$2b$12$WeoCR17i5fv21u7FXRC44ezuwWQhmSELUJ8WTeHUwhXP7Mm8fPRbe')
    ON CONFLICT DO NOTHING;