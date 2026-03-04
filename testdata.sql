-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs — V12: Seed Default Platform Users
-- ════════════════════════════════════════════════════════════════════════════
-- Creates three default users for Phase 1 testing:
--
--   SUPER_ADMIN : admin@biolab.com      / Admin@BioLab2026!
--   BUYER       : buyer@pharmaco.com    / Buyer@BioLab2026!
--   SUPPLIER    : supplier@biolab.com   / Supplier@BioLab2026!
--
-- Passwords are hashed with BCrypt cost-factor 12 using pgcrypto.
-- Each user is assigned roles via sec_schema.user_roles.
-- GDPR CONSENT is seeded for each user.
-- MFA is set up but NOT enabled (opt-in).
-- ════════════════════════════════════════════════════════════════════════════

SET search_path TO sec_schema;

-- ─── Helper: idempotent insert guard ─────────────────────────────────────
-- All inserts use ON CONFLICT DO NOTHING so re-running is safe.

-- ─── 1. Insert users ─────────────────────────────────────────────────────
INSERT INTO sec_schema.users (
    id, email, password_hash,
    first_name, last_name, phone,
    is_active, is_email_verified,
    password_changed_at, created_at, updated_at
) VALUES
      -- SUPER_ADMIN / ADMIN user
      (
          'd0000000-0000-0000-0000-000000000001',
          'admin@biolab.com',
          crypt('Admin@BioLab2026!', gen_salt('bf', 12)),
          'System', 'Administrator', '+1-800-BIO-LABS',
          TRUE, TRUE,
          NOW(), NOW(), NOW()
      ),
      -- BUYER user (PharmaCorp)
      (
          'd0000000-0000-0000-0000-000000000002',
          'buyer@pharmaco.com',
          crypt('Buyer@BioLab2026!', gen_salt('bf', 12)),
          'Alice', 'Buyer', '+1-555-2000',
          TRUE, TRUE,
          NOW(), NOW(), NOW()
      ),
      -- SUPPLIER user (BioLab Alpha)
      (
          'd0000000-0000-0000-0000-000000000003',
          'supplier@biolab.com',
          crypt('Supplier@BioLab2026!', gen_salt('bf', 12)),
          'Jane', 'Supplier', '+1-555-1000',
          TRUE, TRUE,
          NOW(), NOW(), NOW()
      )
    ON CONFLICT (email) DO NOTHING;

-- ─── 2. Assign roles ─────────────────────────────────────────────────────
-- Role IDs match V3 seed: SUPER_ADMIN=..001, ADMIN=..002, SUPPLIER=..003, BUYER=..004

-- admin@biolab.com → SUPER_ADMIN + ADMIN
INSERT INTO sec_schema.user_roles (user_id, role_id)
VALUES
    ('d0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001'),
    ('d0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002')
    ON CONFLICT DO NOTHING;

-- buyer@pharmaco.com → BUYER
INSERT INTO sec_schema.user_roles (user_id, role_id)
VALUES
    ('d0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000004')
    ON CONFLICT DO NOTHING;

-- supplier@biolab.com → SUPPLIER
INSERT INTO sec_schema.user_roles (user_id, role_id)
VALUES
    ('d0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003')
    ON CONFLICT DO NOTHING;

-- ─── 3. Password history (required by PasswordHistoryService) ────────────
INSERT INTO sec_schema.password_history (user_id, password_hash, created_at)
SELECT id, password_hash, NOW()
FROM sec_schema.users
WHERE id IN (
             'd0000000-0000-0000-0000-000000000001',
             'd0000000-0000-0000-0000-000000000002',
             'd0000000-0000-0000-0000-000000000003'
    )
    ON CONFLICT DO NOTHING;

-- ─── 4. MFA settings (disabled by default — user must opt in) ────────────
INSERT INTO sec_schema.mfa_settings (user_id, mfa_type, is_enabled, created_at, updated_at)
VALUES
    ('d0000000-0000-0000-0000-000000000001', 'TOTP', FALSE, NOW(), NOW()),
    ('d0000000-0000-0000-0000-000000000002', 'TOTP', FALSE, NOW(), NOW()),
    ('d0000000-0000-0000-0000-000000000003', 'TOTP', FALSE, NOW(), NOW())
    ON CONFLICT DO NOTHING;

-- ─── 5. GDPR Consent records ─────────────────────────────────────────────
select * from sec_schema.consent_records

    INSERT INTO sec_schema.consent_records
(id, user_id, consent_type, version, granted_at, revoked_at, ip_address, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'd0000000-0000-0000-0000-000000000001', 'GDPR',      '2.0', NOW(), NULL, '127.0.0.1', NOW(), NOW()),
    (gen_random_uuid(), 'd0000000-0000-0000-0000-000000000001', 'TOS',       '3.1', NOW(), NULL, '127.0.0.1', NOW(), NOW()),
    (gen_random_uuid(), 'd0000000-0000-0000-0000-000000000002', 'GDPR',      '2.0', NOW(), NULL, '127.0.0.1', NOW(), NOW()),
    (gen_random_uuid(), 'd0000000-0000-0000-0000-000000000002', 'TOS',       '3.1', NOW(), NULL, '127.0.0.1', NOW(), NOW()),
    (gen_random_uuid(), 'd0000000-0000-0000-0000-000000000002', 'MARKETING', '1.0', NOW(), NULL, '127.0.0.1', NOW(), NOW()),
    (gen_random_uuid(), 'd0000000-0000-0000-0000-000000000003', 'GDPR',      '2.0', NOW(), NULL, '127.0.0.1', NOW(), NOW()),
    (gen_random_uuid(), 'd0000000-0000-0000-0000-000000000003', 'TOS',       '3.1', NOW(), NULL, '127.0.0.1', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- ─── 6. Seed app_schema user copies (User Service reads from app_schema) ──
-- NOTE: The auth-service writes to sec_schema.users. The user-service
-- reads from app_schema.users (synced via event or shared view).
-- If your app_schema.users is a separate table, uncomment and adapt:
/*
INSERT INTO app_schema.users (
    id, email, first_name, last_name, phone, is_active, created_at, updated_at
) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'admin@biolab.com',    'System', 'Administrator', '+1-800-BIO-LABS', TRUE, NOW(), NOW()),
    ('d0000000-0000-0000-0000-000000000002', 'buyer@pharmaco.com',  'Alice',  'Buyer',         '+1-555-2000',    TRUE, NOW(), NOW()),
    ('d0000000-0000-0000-0000-000000000003', 'supplier@biolab.com', 'Jane',   'Supplier',      '+1-555-1000',    TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
*/

-- ─── 7. Org membership for seeded users ──────────────────────────────────
-- Admin → no org (platform-level)
-- Buyer → PharmaCorp Inc. (c1000000-...-001 from V7)
-- Supplier → BioLabs Alpha (b1000000-...-001 from V7)
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'app_schema'
  AND table_name = 'user_organizations'
ORDER BY ordinal_position;

INSERT INTO app_schema.user_organizations
(id, user_id, org_id, role_in_org, is_primary, joined_at, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'd0000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000001', 'MEMBER', TRUE, NOW(), NOW(), NOW()),
    (gen_random_uuid(), 'd0000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000001', 'MEMBER', TRUE, NOW(), NOW(), NOW())
    ON CONFLICT DO NOTHING;

-- ─── Verification queries (run these to confirm seeding) ─────────────────
-- SELECT u.email, r.name AS role
-- FROM sec_schema.users u
-- JOIN sec_schema.user_roles ur ON ur.user_id = u.id
-- JOIN sec_schema.roles r ON r.id = ur.role_id
-- WHERE u.id IN (
--   'd0000000-0000-0000-0000-000000000001',
--   'd0000000-0000-0000-0000-000000000002',
--   'd0000000-0000-0000-0000-000000000003'
-- );