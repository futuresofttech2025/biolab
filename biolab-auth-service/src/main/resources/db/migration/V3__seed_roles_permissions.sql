-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs — Seed: Roles & Permissions
-- Version: V3 | System roles + granular RBAC permissions + assignments.
-- ════════════════════════════════════════════════════════════════════════════
SET search_path TO sec_schema;

-- ─── System Roles ────────────────────────────────────────────────────────
INSERT INTO sec_schema.roles (id, name, display_name, description, is_system_role) VALUES
    ('a0000000-0000-0000-0000-000000000001','SUPER_ADMIN','Super Administrator','Full platform access — all tenants, compliance, settings',TRUE),
    ('a0000000-0000-0000-0000-000000000002','ADMIN','Administrator','Organization admin — manages users, services, compliance within org',TRUE),
    ('a0000000-0000-0000-0000-000000000003','SUPPLIER','Supplier','Service provider — catalog, experiments, results',TRUE),
    ('a0000000-0000-0000-0000-000000000004','BUYER','Buyer','Client — browse services, request experiments, review results',TRUE);

-- ─── Permissions ─────────────────────────────────────────────────────────
INSERT INTO sec_schema.permissions (name, module, action, description) VALUES
    ('USER_CREATE','USER','CREATE','Create new user accounts'),
    ('USER_VIEW','USER','VIEW','View user profiles'),
    ('USER_VIEW_ALL','USER','VIEW_ALL','View all user profiles'),
    ('USER_UPDATE','USER','UPDATE','Update user profile'),
    ('USER_DELETE','USER','DELETE','Deactivate user accounts'),
    ('USER_MANAGE','USER','MANAGE','Full user lifecycle management'),
    ('ROLE_ASSIGN','ROLE','ASSIGN','Assign roles to users'),
    ('ROLE_MANAGE','ROLE','MANAGE','Create and manage custom roles'),
    ('SERVICE_CREATE','SERVICE','CREATE','Create catalog services'),
    ('SERVICE_VIEW','SERVICE','VIEW','View service listings'),
    ('SERVICE_EDIT','SERVICE','EDIT','Edit service details'),
    ('SERVICE_DELETE','SERVICE','DELETE','Remove services from catalog'),
    ('PROJECT_CREATE','PROJECT','CREATE','Create new projects'),
    ('PROJECT_VIEW_ALL','PROJECT','VIEW_ALL','View all projects'),
    ('PROJECT_VIEW_OWN','PROJECT','VIEW_OWN','View own projects'),
    ('PROJECT_UPDATE','PROJECT','UPDATE','Update project details'),
    ('PROJECT_MANAGE','PROJECT','MANAGE','Full project management'),
    ('DOCUMENT_UPLOAD','DOCUMENT','UPLOAD','Upload documents'),
    ('DOCUMENT_DOWNLOAD','DOCUMENT','DOWNLOAD','Download documents'),
    ('DOCUMENT_DELETE','DOCUMENT','DELETE','Delete documents'),
    ('MESSAGE_SEND','MESSAGE','SEND','Send messages'),
    ('MESSAGE_VIEW','MESSAGE','VIEW','View messages'),
    ('INVOICE_CREATE','INVOICE','CREATE','Create invoices'),
    ('INVOICE_VIEW_ALL','INVOICE','VIEW_ALL','View all invoices'),
    ('INVOICE_VIEW_OWN','INVOICE','VIEW_OWN','View own invoices'),
    ('COMPLIANCE_VIEW','COMPLIANCE','VIEW','View compliance dashboards'),
    ('COMPLIANCE_MANAGE','COMPLIANCE','MANAGE','Manage compliance settings'),
    ('AUDIT_LOG_VIEW','AUDIT','VIEW','View audit logs'),
    ('SETTINGS_PLATFORM','SETTINGS','PLATFORM','Platform-wide settings'),
    ('SETTINGS_OWN_PROFILE','SETTINGS','PROFILE','Update own profile');

-- SUPER_ADMIN → ALL permissions
INSERT INTO sec_schema.role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000001', id FROM sec_schema.permissions;

-- ADMIN → all except SETTINGS_PLATFORM
INSERT INTO sec_schema.role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000002', id FROM sec_schema.permissions WHERE name != 'SETTINGS_PLATFORM';

-- SUPPLIER
INSERT INTO sec_schema.role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000003', id FROM sec_schema.permissions
WHERE name IN ('SERVICE_CREATE','SERVICE_VIEW','SERVICE_EDIT','PROJECT_CREATE','PROJECT_VIEW_OWN','PROJECT_UPDATE',
    'DOCUMENT_UPLOAD','DOCUMENT_DOWNLOAD','DOCUMENT_DELETE','MESSAGE_SEND','MESSAGE_VIEW',
    'INVOICE_CREATE','INVOICE_VIEW_OWN','SETTINGS_OWN_PROFILE');

-- BUYER
INSERT INTO sec_schema.role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000004', id FROM sec_schema.permissions
WHERE name IN ('SERVICE_VIEW','PROJECT_CREATE','PROJECT_VIEW_OWN','DOCUMENT_UPLOAD','DOCUMENT_DOWNLOAD',
    'MESSAGE_SEND','MESSAGE_VIEW','INVOICE_VIEW_OWN','SETTINGS_OWN_PROFILE');
