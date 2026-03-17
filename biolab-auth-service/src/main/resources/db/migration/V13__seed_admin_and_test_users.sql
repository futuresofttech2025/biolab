-- ════════════════════════════════════════════════════════════════════════════
-- V13__seed_admin_and_test_users.sql
-- Seeds platform admin + test accounts for development/testing ONLY.
--
-- SPRINT 0 — GAP-03 FIX:
--   ❌ REMOVED: real admin email (ndate1976@gmail.com)
--   ❌ REMOVED: plain-text password comment (Nitin@1976)
--   ❌ REMOVED: real BCrypt hash computed from a known password
--   ✅ REPLACED: generic placeholder emails, placeholder BCrypt hash
--
-- HOW TO USE IN DEV:
--   1. Generate a new BCrypt hash for your chosen dev password:
--        htpasswd -bnBC 12 "" "YourDevPassword" | tr -d ':\n'
--      Or use: https://bcrypt-generator.com (cost 12)
--   2. Replace PLACEHOLDER_BCRYPT_HASH_CHANGE_ME below with that hash.
--   3. Set ADMIN_EMAIL, SUPPLIER_EMAIL, BUYER_EMAIL in your .env.
--
-- ⚠️  THIS FILE MUST NEVER CONTAIN:
--   - Real email addresses
--   - Plain-text passwords (even in comments)
--   - BCrypt hashes derived from known/reused passwords
--   - Production credentials of any kind
--
-- ⚠️  DO NOT RUN IN PRODUCTION.
-- ════════════════════════════════════════════════════════════════════════════

-- ─── Users ────────────────────────────────────────────────────────────────
-- Replace PLACEHOLDER_BCRYPT_HASH_CHANGE_ME with a real bcrypt hash before use.
-- Replace admin@yourdomain.com etc. with actual dev/test email addresses.
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
        'admin@yourdomain.com',                          -- Replace with your dev admin email
        'PLACEHOLDER_BCRYPT_HASH_CHANGE_ME',             -- Replace with bcrypt hash (cost 12)
        'Platform',
        'Admin',
        '+1-000-000-0001',
        TRUE, TRUE, NOW()
    ),
    (
        'd0000000-0000-0000-0000-000000000002'::uuid,
        'supplier-test@yourdomain.com',                  -- Replace with your dev supplier email
        'PLACEHOLDER_BCRYPT_HASH_CHANGE_ME',             -- Replace with bcrypt hash (cost 12)
        'Supplier',
        'Test',
        '+1-000-000-0002',
        TRUE, TRUE, NOW()
    ),
    (
        'd0000000-0000-0000-0000-000000000003'::uuid,
        'buyer-test@yourdomain.com',                     -- Replace with your dev buyer email
        'PLACEHOLDER_BCRYPT_HASH_CHANGE_ME',             -- Replace with bcrypt hash (cost 12)
        'Buyer',
        'Test',
        '+1-000-000-0003',
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
    ('d0000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid),  -- SUPER_ADMIN
    ('d0000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000003'::uuid),  -- SUPPLIER
    ('d0000000-0000-0000-0000-000000000003'::uuid, 'a0000000-0000-0000-0000-000000000004'::uuid)   -- BUYER
ON CONFLICT ON CONSTRAINT uq_user_role DO NOTHING;

-- ─── Initial Password History ─────────────────────────────────────────────
-- NOTE: This will fail with a foreign-key error if the PLACEHOLDER hash above
-- has not been replaced. That is intentional — it forces you to set a real hash.
INSERT INTO sec_schema.password_history (user_id, password_hash)
VALUES
    ('d0000000-0000-0000-0000-000000000001'::uuid, 'PLACEHOLDER_BCRYPT_HASH_CHANGE_ME'),
    ('d0000000-0000-0000-0000-000000000002'::uuid, 'PLACEHOLDER_BCRYPT_HASH_CHANGE_ME'),
    ('d0000000-0000-0000-0000-000000000003'::uuid, 'PLACEHOLDER_BCRYPT_HASH_CHANGE_ME')
ON CONFLICT DO NOTHING;
