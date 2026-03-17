-- ════════════════════════════════════════════════════════════════════════════
-- BioLab — Complete Clean & Re-Seed Script
-- ════════════════════════════════════════════════════════════════════════════
-- Cleans ALL existing data from sec_schema and app_schema,
-- then inserts comprehensive demo data for all features.
--
-- Demo Credentials (BCrypt cost=12):
--   admin@biolab.com      / Admin@BioLab2026!     → ADMIN + SUPER_ADMIN
--   supplier@biolab.com   / Supplier@BioLab2026!   → SUPPLIER
--   buyer@pharmaco.com    / Buyer@BioLab2026!      → BUYER
--   ndate1976@gmail.com   / nitin@1976              → SUPER_ADMIN
-- ════════════════════════════════════════════════════════════════════════════

BEGIN;

-- ════════════════════════════════════════════════════════════════════════════
-- PHASE 1: CLEAN ALL DATA
-- ════════════════════════════════════════════════════════════════════════════

TRUNCATE app_schema.messages                   CASCADE;
TRUNCATE app_schema.conversation_participants  CASCADE;
TRUNCATE app_schema.conversations              CASCADE;
TRUNCATE app_schema.documents                  CASCADE;
TRUNCATE app_schema.invoice_items              CASCADE;
TRUNCATE app_schema.invoices                   CASCADE;
TRUNCATE app_schema.project_milestones         CASCADE;
TRUNCATE app_schema.project_members            CASCADE;
TRUNCATE app_schema.projects                   CASCADE;
TRUNCATE app_schema.service_requests           CASCADE;
TRUNCATE app_schema.services                   CASCADE;
TRUNCATE app_schema.service_categories         CASCADE;
TRUNCATE app_schema.notifications              CASCADE;
TRUNCATE app_schema.notification_preferences   CASCADE;
TRUNCATE app_schema.audit_events               CASCADE;
TRUNCATE app_schema.compliance_audits          CASCADE;
TRUNCATE app_schema.policy_documents           CASCADE;
TRUNCATE app_schema.platform_settings          CASCADE;
TRUNCATE app_schema.user_organizations         CASCADE;
TRUNCATE app_schema.organizations              CASCADE;

TRUNCATE sec_schema.data_access_log            CASCADE;
TRUNCATE sec_schema.consent_records            CASCADE;
TRUNCATE sec_schema.login_audit_log            CASCADE;
TRUNCATE sec_schema.jwt_token_blacklist        CASCADE;
TRUNCATE sec_schema.refresh_tokens             CASCADE;
TRUNCATE sec_schema.user_sessions              CASCADE;
TRUNCATE sec_schema.mfa_settings               CASCADE;
TRUNCATE sec_schema.password_reset_tokens      CASCADE;
TRUNCATE sec_schema.email_verification_tokens  CASCADE;
TRUNCATE sec_schema.password_history           CASCADE;
TRUNCATE sec_schema.role_permissions           CASCADE;
TRUNCATE sec_schema.user_roles                 CASCADE;
TRUNCATE sec_schema.users                      CASCADE;


-- ════════════════════════════════════════════════════════════════════════════
-- PHASE 2: sec_schema
-- ════════════════════════════════════════════════════════════════════════════

-- ── 2.1 Users (BCrypt cost=12) ───────────────────────────────────────────

INSERT INTO sec_schema.users (id, email, password_hash, first_name, last_name, phone, is_active, is_email_verified, password_changed_at) VALUES
                                                                                                                                             ('d0000000-0000-0000-0000-000000000001', 'admin@biolab.com',
                                                                                                                                              '$2b$12$IpEBnQ5Kic7/dlJYbcg.ue7S97aQKM26HUBGvv23IeMH3dH6cZDyO',
                                                                                                                                              'System', 'Administrator', '+1-800-BIO-LABS', TRUE, TRUE, NOW()),
                                                                                                                                             ('d0000000-0000-0000-0000-000000000002', 'supplier@biolab.com',
                                                                                                                                              '$2b$12$JO8qZvfxHwc45H0g9Z.1vO9LvI5OZxLaqAjIy.wGXEG1cD2Ylj5wm',
                                                                                                                                              'Sarah', 'Chen', '+91-9000000002', TRUE, TRUE, NOW()),
                                                                                                                                             ('d0000000-0000-0000-0000-000000000003', 'buyer@pharmaco.com',
                                                                                                                                              '$2b$12$SDnzzvK0/IMGUZ77mdGEzO5rZ0pYT5AHXHmdCfxgr3GjK0ahyxDcS',
                                                                                                                                              'James', 'Wilson', '+1-800-PHA-RMCO', TRUE, TRUE, NOW()),
                                                                                                                                             ('d0000000-0000-0000-0000-000000000004', 'ndate1976@gmail.com',
                                                                                                                                              '$2b$12$gm6WhmEIpBDZ8STh9B0vt.6cZuDma.HsivFWg/nK61.M1p1NJotf.',
                                                                                                                                              'Nitin', 'Date', '+91-9000000001', TRUE, TRUE, NOW()),
                                                                                                                                             ('d0000000-0000-0000-0000-000000000005', 'lab.tech@biolab.com',
                                                                                                                                              '$2b$12$IpEBnQ5Kic7/dlJYbcg.ue7S97aQKM26HUBGvv23IeMH3dH6cZDyO',
                                                                                                                                              'Emily', 'Rodriguez', '+1-555-0105', TRUE, TRUE, NOW()),
                                                                                                                                             ('d0000000-0000-0000-0000-000000000006', 'research@genecorp.com',
                                                                                                                                              '$2b$12$SDnzzvK0/IMGUZ77mdGEzO5rZ0pYT5AHXHmdCfxgr3GjK0ahyxDcS',
                                                                                                                                              'Michael', 'Park', '+1-555-0106', TRUE, TRUE, NOW());

-- ── 2.2 Role Assignments ─────────────────────────────────────────────────

INSERT INTO sec_schema.user_roles (user_id, role_id, assigned_at) VALUES
                                                                      ('d0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', NOW()),
                                                                      ('d0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', NOW()),
                                                                      ('d0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000003', NOW()),
                                                                      ('d0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000004', NOW()),
                                                                      ('d0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', NOW()),
                                                                      ('d0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000003', NOW()),
                                                                      ('d0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000004', NOW());

-- ── 2.3 Role Permissions ─────────────────────────────────────────────────

INSERT INTO sec_schema.role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000001', id FROM sec_schema.permissions;

INSERT INTO sec_schema.role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000002', id FROM sec_schema.permissions WHERE name != 'SETTINGS_PLATFORM';

INSERT INTO sec_schema.role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000003', id FROM sec_schema.permissions
WHERE name IN ('SERVICE_CREATE','SERVICE_VIEW','SERVICE_EDIT','PROJECT_CREATE','PROJECT_VIEW_OWN','PROJECT_UPDATE',
               'DOCUMENT_UPLOAD','DOCUMENT_DOWNLOAD','DOCUMENT_DELETE','MESSAGE_SEND','MESSAGE_VIEW',
               'INVOICE_CREATE','INVOICE_VIEW_OWN','SETTINGS_OWN_PROFILE');

INSERT INTO sec_schema.role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000004', id FROM sec_schema.permissions
WHERE name IN ('SERVICE_VIEW','PROJECT_CREATE','PROJECT_VIEW_OWN','DOCUMENT_UPLOAD','DOCUMENT_DOWNLOAD',
               'MESSAGE_SEND','MESSAGE_VIEW','INVOICE_VIEW_OWN','SETTINGS_OWN_PROFILE');

-- ── 2.4 Password History ─────────────────────────────────────────────────

INSERT INTO sec_schema.password_history (user_id, password_hash)
SELECT id, password_hash FROM sec_schema.users;

-- ── 2.5 Consent Records ─────────────────────────────────────────────────

INSERT INTO sec_schema.consent_records (user_id, consent_type, ip_address, version) VALUES
                                                                                        ('d0000000-0000-0000-0000-000000000001', 'GDPR',  '10.0.0.1', '2.0'),
                                                                                        ('d0000000-0000-0000-0000-000000000001', 'HIPAA', '10.0.0.1', '1.0'),
                                                                                        ('d0000000-0000-0000-0000-000000000001', 'TOS',   '10.0.0.1', '3.0'),
                                                                                        ('d0000000-0000-0000-0000-000000000002', 'GDPR',  '10.0.0.2', '2.0'),
                                                                                        ('d0000000-0000-0000-0000-000000000002', 'HIPAA', '10.0.0.2', '1.0'),
                                                                                        ('d0000000-0000-0000-0000-000000000002', 'TOS',   '10.0.0.2', '3.0'),
                                                                                        ('d0000000-0000-0000-0000-000000000003', 'GDPR',  '10.0.0.3', '2.0'),
                                                                                        ('d0000000-0000-0000-0000-000000000003', 'HIPAA', '10.0.0.3', '1.0'),
                                                                                        ('d0000000-0000-0000-0000-000000000003', 'TOS',   '10.0.0.3', '3.0');

-- ── 2.6 Login Audit ──────────────────────────────────────────────────────

INSERT INTO sec_schema.login_audit_log (user_id, email, ip_address, user_agent, action, status, mfa_used) VALUES
                                                                                                              ('d0000000-0000-0000-0000-000000000001', 'admin@biolab.com',    '10.0.0.1', 'Mozilla/5.0 Chrome/120', 'LOGIN', 'SUCCESS', FALSE),
                                                                                                              ('d0000000-0000-0000-0000-000000000002', 'supplier@biolab.com', '10.0.0.2', 'Mozilla/5.0 Chrome/120', 'LOGIN', 'SUCCESS', FALSE),
                                                                                                              ('d0000000-0000-0000-0000-000000000003', 'buyer@pharmaco.com',  '10.0.0.3', 'Mozilla/5.0 Chrome/120', 'LOGIN', 'SUCCESS', FALSE);


-- ════════════════════════════════════════════════════════════════════════════
-- PHASE 3: app_schema
-- ════════════════════════════════════════════════════════════════════════════

-- ── 3.1 Organizations ────────────────────────────────────────────────────

INSERT INTO app_schema.organizations (id, name, type, address, phone, website, is_active) VALUES
                                                                                              ('b0000000-0000-0000-0000-000000000001', 'BioLabs Inc.', 'SUPPLIER',
                                                                                               '100 Lab Drive, San Francisco, CA 94107', '+1-800-246-5227', 'https://www.biolabs.com', TRUE),
                                                                                              ('b0000000-0000-0000-0000-000000000002', 'PharmaCo Research', 'BUYER',
                                                                                               '200 Pharma Blvd, Boston, MA 02110', '+1-800-742-7626', 'https://www.pharmaco-research.com', TRUE),
                                                                                              ('b0000000-0000-0000-0000-000000000003', 'GeneCorp Diagnostics', 'BUYER',
                                                                                               '500 Genome Way, San Diego, CA 92121', '+1-555-436-3267', 'https://www.genecorp.com', TRUE);

-- ── 3.2 User-Organization Mappings ───────────────────────────────────────

INSERT INTO app_schema.user_organizations (user_id, org_id, role_in_org, is_primary) VALUES
                                                                                         ('d0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'ADMIN',  TRUE),
                                                                                         ('d0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'MEMBER', TRUE),
                                                                                         ('d0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000002', 'ADMIN',  TRUE),
                                                                                         ('d0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001', 'ADMIN',  TRUE),
                                                                                         ('d0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001', 'MEMBER', TRUE),
                                                                                         ('d0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000003', 'MEMBER', TRUE);

-- ── 3.3 Service Categories ───────────────────────────────────────────────

INSERT INTO app_schema.service_categories (id, name, slug, description, icon, sort_order) VALUES
                                                                                              ('c0000000-0000-0000-0000-000000000001', 'Genomic Sequencing', 'genomic-sequencing', 'Next-gen and Sanger sequencing for DNA/RNA analysis', 'dna', 1),
                                                                                              ('c0000000-0000-0000-0000-000000000002', 'Protein Analysis', 'protein-analysis', 'Mass spectrometry, ELISA, and Western blot services', 'microscope', 2),
                                                                                              ('c0000000-0000-0000-0000-000000000003', 'Cell Culture', 'cell-culture', 'Primary and immortalized cell line maintenance', 'flask', 3),
                                                                                              ('c0000000-0000-0000-0000-000000000004', 'Drug Screening', 'drug-screening', 'High-throughput compound screening and IC50 assays', 'pill', 4),
                                                                                              ('c0000000-0000-0000-0000-000000000005', 'Biomarker Discovery', 'biomarker-discovery', 'Multi-omics biomarker identification and validation', 'target', 5);

-- ── 3.4 Services ─────────────────────────────────────────────────────────

INSERT INTO app_schema.services (id, name, slug, category_id, supplier_org_id, description, methodology, price_from, turnaround, rating, review_count) VALUES
                                                                                                                                                           ('e0000000-0000-0000-0000-000000000001', 'Whole Genome Sequencing (WGS)', 'wgs-30x',
                                                                                                                                                            'c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001',
                                                                                                                                                            'Complete human genome at 30x coverage using Illumina NovaSeq 6000.',
                                                                                                                                                            'Library prep → Cluster generation → Paired-end 150bp sequencing → DRAGEN analysis', 2500.00, '10-14 days', 4.8, 42),
                                                                                                                                                           ('e0000000-0000-0000-0000-000000000002', 'RNA-Seq Expression Profiling', 'rnaseq-expression',
                                                                                                                                                            'c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001',
                                                                                                                                                            'Bulk RNA-Seq for differential gene expression analysis.',
                                                                                                                                                            'Poly-A selection → cDNA synthesis → NovaSeq sequencing → DESeq2 analysis', 1800.00, '7-10 days', 4.6, 28),
                                                                                                                                                           ('e0000000-0000-0000-0000-000000000003', 'LC-MS/MS Proteomics', 'lcms-proteomics',
                                                                                                                                                            'c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001',
                                                                                                                                                            'Quantitative proteomics using liquid chromatography-tandem mass spectrometry.',
                                                                                                                                                            'Trypsin digestion → TMT labeling → nanoLC-MS/MS → MaxQuant analysis', 3200.00, '14-21 days', 4.9, 35),
                                                                                                                                                           ('e0000000-0000-0000-0000-000000000004', 'CRISPR Cell Line Engineering', 'crispr-cell-line',
                                                                                                                                                            'c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001',
                                                                                                                                                            'Custom CRISPR-Cas9 knockout/knock-in cell line generation.',
                                                                                                                                                            'Guide RNA design → Electroporation → Single-cell cloning → Sanger validation', 5500.00, '6-8 weeks', 4.7, 19),
                                                                                                                                                           ('e0000000-0000-0000-0000-000000000005', 'High-Throughput Drug Screening', 'hts-drug-screen',
                                                                                                                                                            'c0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001',
                                                                                                                                                            '384-well plate automated compound screening with dose-response curves.',
                                                                                                                                                            'Compound plating → Cell seeding → 72h incubation → CellTiter-Glo readout → IC50 fitting', 8000.00, '3-4 weeks', 4.5, 12),
                                                                                                                                                           ('e0000000-0000-0000-0000-000000000006', 'Plasma Biomarker Panel', 'plasma-biomarker-panel',
                                                                                                                                                            'c0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001',
                                                                                                                                                            'Multiplex biomarker panel from plasma samples (up to 96 analytes).',
                                                                                                                                                            'Sample processing → Luminex MAGPIX → Standard curve fitting → QC report', 1200.00, '5-7 days', 4.4, 56);

-- ── 3.5 Service Requests ─────────────────────────────────────────────────

INSERT INTO app_schema.service_requests (id, service_id, buyer_id, buyer_org_id, sample_type, timeline, requirements, priority, status) VALUES
                                                                                                                                            ('f0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001',
                                                                                                                                             'd0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000002',
                                                                                                                                             'Blood (EDTA)', 'STANDARD', '30x WGS on 12 patient samples for pharmacogenomics study.', 'HIGH', 'ACCEPTED'),
                                                                                                                                            ('f0000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000003',
                                                                                                                                             'd0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000002',
                                                                                                                                             'Cell lysate', 'STANDARD', 'Quantitative proteomics comparing treated vs control. 3 bio replicates.', 'MEDIUM', 'ACCEPTED'),
                                                                                                                                            ('f0000000-0000-0000-0000-000000000003', 'e0000000-0000-0000-0000-000000000005',
                                                                                                                                             'd0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000003',
                                                                                                                                             'Compound library', 'STANDARD', 'Screen 2000 compounds against HEK293. Need IC50 for top hits.', 'HIGH', 'PENDING'),
                                                                                                                                            ('f0000000-0000-0000-0000-000000000004', 'e0000000-0000-0000-0000-000000000002',
                                                                                                                                             'd0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000003',
                                                                                                                                             'Total RNA', 'STANDARD', 'RNA-Seq on 8 tumor vs normal tissue pairs.', 'URGENT', 'ACCEPTED');

-- ── 3.6 Projects ─────────────────────────────────────────────────────────

INSERT INTO app_schema.projects (id, title, service_request_id, buyer_org_id, supplier_org_id, status, progress_pct, budget, start_date, deadline) VALUES
                                                                                                                                                       ('10000000-0000-0000-0000-000000000001', 'PharmaCo WGS Study - Batch 1',
                                                                                                                                                        'f0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001',
                                                                                                                                                        'IN_PROGRESS', 65, 30000.00, CURRENT_DATE - 20, CURRENT_DATE + 10),
                                                                                                                                                       ('10000000-0000-0000-0000-000000000002', 'PharmaCo Proteomics - Drug Target Validation',
                                                                                                                                                        'f0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001',
                                                                                                                                                        'ACTIVE', 25, 19200.00, CURRENT_DATE - 5, CURRENT_DATE + 25),
                                                                                                                                                       ('10000000-0000-0000-0000-000000000003', 'GeneCorp RNA-Seq Tumor Profiling',
                                                                                                                                                        'f0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001',
                                                                                                                                                        'PENDING', 0, 14400.00, CURRENT_DATE, CURRENT_DATE + 14),
                                                                                                                                                       ('10000000-0000-0000-0000-000000000004', 'PharmaCo Biomarker Panel - Cohort A',
                                                                                                                                                        NULL, 'b0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001',
                                                                                                                                                        'COMPLETED', 100, 12000.00, CURRENT_DATE - 45, CURRENT_DATE - 10);

UPDATE app_schema.projects SET completed_at = NOW() - INTERVAL '10 days' WHERE id = '10000000-0000-0000-0000-000000000004';

-- ── 3.7 Project Members ──────────────────────────────────────────────────

INSERT INTO app_schema.project_members (project_id, user_id, role) VALUES
                                                                       ('10000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000003', 'OWNER'),
                                                                       ('10000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002', 'MEMBER'),
                                                                       ('10000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000005', 'MEMBER'),
                                                                       ('10000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000003', 'OWNER'),
                                                                       ('10000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'MEMBER'),
                                                                       ('10000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000006', 'OWNER'),
                                                                       ('10000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000002', 'MEMBER'),
                                                                       ('10000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000003', 'OWNER'),
                                                                       ('10000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000005', 'MEMBER');

-- ── 3.8 Milestones ───────────────────────────────────────────────────────

INSERT INTO app_schema.project_milestones (project_id, title, description, milestone_date, is_completed, completed_at, sort_order) VALUES
                                                                                                                                       ('10000000-0000-0000-0000-000000000001', 'Sample Receipt & QC', 'Receive blood samples, extract DNA, run QC', CURRENT_DATE - 15, TRUE, NOW() - INTERVAL '15 days', 1),
                                                                                                                                       ('10000000-0000-0000-0000-000000000001', 'Library Preparation', 'Prepare sequencing libraries using Illumina DNA Prep', CURRENT_DATE - 10, TRUE, NOW() - INTERVAL '10 days', 2),
                                                                                                                                       ('10000000-0000-0000-0000-000000000001', 'Sequencing Run', 'Run on NovaSeq 6000 S4 flow cell, 2x150bp', CURRENT_DATE - 3, TRUE, NOW() - INTERVAL '3 days', 3),
                                                                                                                                       ('10000000-0000-0000-0000-000000000001', 'Bioinformatics Analysis', 'DRAGEN pipeline: alignment, variant calling', CURRENT_DATE + 5, FALSE, NULL, 4),
                                                                                                                                       ('10000000-0000-0000-0000-000000000001', 'Final Report Delivery', 'QC metrics, VCF files, interpretation report', CURRENT_DATE + 10, FALSE, NULL, 5),
                                                                                                                                       ('10000000-0000-0000-0000-000000000002', 'Sample Processing', 'Cell lysis, trypsin digestion, TMT labeling', CURRENT_DATE + 5, FALSE, NULL, 1),
                                                                                                                                       ('10000000-0000-0000-0000-000000000002', 'Mass Spectrometry', 'nanoLC-MS/MS acquisition on Orbitrap', CURRENT_DATE + 15, FALSE, NULL, 2),
                                                                                                                                       ('10000000-0000-0000-0000-000000000002', 'Data Analysis & Report', 'MaxQuant, stats, pathway enrichment', CURRENT_DATE + 25, FALSE, NULL, 3);

-- ── 3.9 Documents ────────────────────────────────────────────────────────

INSERT INTO app_schema.documents (id, project_id, uploaded_by, file_name, file_type, file_size, storage_key, mime_type, version) VALUES
                                                                                                                                     ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002',
                                                                                                                                      'WGS_QC_Report_Batch1.pdf', 'PDF', 245760, 's3://biolab-docs/proj1/WGS_QC_Report.pdf', 'application/pdf', 1),
                                                                                                                                     ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000003',
                                                                                                                                      'Sample_Manifest_PharmaCo.xlsx', 'XLSX', 52480, 's3://biolab-docs/proj1/Sample_Manifest.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 1),
                                                                                                                                     ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000005',
                                                                                                                                      'Biomarker_Final_Report.pdf', 'PDF', 1048576, 's3://biolab-docs/proj4/Biomarker_Final_Report.pdf', 'application/pdf', 2);

-- ── 3.10 Invoices ────────────────────────────────────────────────────────

INSERT INTO app_schema.invoices (id, invoice_number, project_id, supplier_org_id, buyer_org_id, status, subtotal, tax_rate, tax_amount, total, issue_date, due_date, paid_date, notes) VALUES
                                                                                                                                                                                           ('30000000-0000-0000-0000-000000000001', 'INV-2026-0001', '10000000-0000-0000-0000-000000000001',
                                                                                                                                                                                            'b0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002',
                                                                                                                                                                                            'SENT', 15000.00, 8.50, 1275.00, 16275.00, CURRENT_DATE - 5, CURRENT_DATE + 25, NULL, 'WGS Study - 50% milestone payment'),
                                                                                                                                                                                           ('30000000-0000-0000-0000-000000000002', 'INV-2026-0002', '10000000-0000-0000-0000-000000000004',
                                                                                                                                                                                            'b0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002',
                                                                                                                                                                                            'PAID', 12000.00, 8.50, 1020.00, 13020.00, CURRENT_DATE - 30, CURRENT_DATE - 5, CURRENT_DATE - 8, 'Biomarker Panel - Final payment'),
                                                                                                                                                                                           ('30000000-0000-0000-0000-000000000003', 'INV-2026-0003', '10000000-0000-0000-0000-000000000002',
                                                                                                                                                                                            'b0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002',
                                                                                                                                                                                            'DRAFT', 4800.00, 8.50, 408.00, 5208.00, CURRENT_DATE, CURRENT_DATE + 30, NULL, 'Proteomics - 25% upfront deposit');

-- ── 3.11 Invoice Items ───────────────────────────────────────────────────

INSERT INTO app_schema.invoice_items (invoice_id, description, quantity, unit_price, amount, sort_order) VALUES
                                                                                                             ('30000000-0000-0000-0000-000000000001', 'WGS - DNA Extraction & QC (12 samples)', 12, 150.00, 1800.00, 1),
                                                                                                             ('30000000-0000-0000-0000-000000000001', 'WGS - Library Preparation (12 samples)', 12, 350.00, 4200.00, 2),
                                                                                                             ('30000000-0000-0000-0000-000000000001', 'WGS - NovaSeq Sequencing 30x (12 samples)', 12, 750.00, 9000.00, 3),
                                                                                                             ('30000000-0000-0000-0000-000000000002', 'Plasma Biomarker Panel 96 analytes (100 samples)', 100, 120.00, 12000.00, 1),
                                                                                                             ('30000000-0000-0000-0000-000000000003', 'LC-MS/MS - Sample Processing (6 samples)', 6, 400.00, 2400.00, 1),
                                                                                                             ('30000000-0000-0000-0000-000000000003', 'LC-MS/MS - Mass Spec Run (6 samples)', 6, 400.00, 2400.00, 2);

-- ── 3.12 Conversations & Messages ────────────────────────────────────────

INSERT INTO app_schema.conversations (id, project_id, title) VALUES
                                                                 ('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'WGS Project - General Discussion'),
                                                                 ('40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'Proteomics - Sample Prep Questions');

INSERT INTO app_schema.conversation_participants (conversation_id, user_id, org_id) VALUES
                                                                                        ('40000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000002'),
                                                                                        ('40000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001'),
                                                                                        ('40000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000002'),
                                                                                        ('40000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001');

INSERT INTO app_schema.messages (conversation_id, sender_id, content, created_at) VALUES
                                                                                      ('40000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000003', 'Hi Sarah, 12 EDTA blood samples shipped via FedEx Priority. Tracking: FX-2026-789456.', NOW() - INTERVAL '18 days'),
                                                                                      ('40000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002', 'Received! All 12 passed QC. DNA 25-80 ng/uL. Proceeding to library prep.', NOW() - INTERVAL '15 days'),
                                                                                      ('40000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000003', 'Great news! Can you share the QC report when ready?', NOW() - INTERVAL '14 days'),
                                                                                      ('40000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002', 'QC report uploaded. Sequencing run started on NovaSeq S4 flow cell.', NOW() - INTERVAL '10 days'),
                                                                                      ('40000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002', 'Sequencing complete! Average 32x coverage. Starting DRAGEN analysis now.', NOW() - INTERVAL '3 days'),
                                                                                      ('40000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000003', 'For proteomics, should we use RIPA or NP-40 for HeLa cell lysis?', NOW() - INTERVAL '4 days'),
                                                                                      ('40000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'For HeLa with TMT labeling, I recommend 8M urea lysis buffer. Better peptide yield.', NOW() - INTERVAL '3 days');

-- ── 3.13 Notifications ───────────────────────────────────────────────────

INSERT INTO app_schema.notifications (user_id, type, title, message, link, is_read) VALUES
                                                                                        ('d0000000-0000-0000-0000-000000000003', 'PROJECT_UPDATE', 'Sequencing Complete', 'WGS sequencing for Batch 1 is complete.', '/buyer/workspace/10000000-0000-0000-0000-000000000001', FALSE),
                                                                                        ('d0000000-0000-0000-0000-000000000003', 'INVOICE', 'New Invoice: INV-2026-0001', 'BioLabs sent invoice for $16,275.00.', '/buyer/invoices/30000000-0000-0000-0000-000000000001', FALSE),
                                                                                        ('d0000000-0000-0000-0000-000000000003', 'MESSAGE', 'New message from Sarah Chen', 'Sequencing complete! Average 32x coverage...', '/buyer/messages', FALSE),
                                                                                        ('d0000000-0000-0000-0000-000000000002', 'SERVICE_REQUEST', 'New Service Request', 'GeneCorp requested High-Throughput Drug Screening.', '/supplier/requests', FALSE),
                                                                                        ('d0000000-0000-0000-0000-000000000002', 'PROJECT_UPDATE', 'New Project Assigned', 'Added to GeneCorp RNA-Seq Tumor Profiling.', '/supplier/projects', TRUE),
                                                                                        ('d0000000-0000-0000-0000-000000000001', 'COMPLIANCE', 'Quarterly Audit Due', 'HIPAA Q1 2026 audit due in 7 days.', '/admin/compliance', FALSE);

-- ── 3.14 Notification Preferences ────────────────────────────────────────

INSERT INTO app_schema.notification_preferences (user_id, email_enabled, project_updates, new_messages, invoice_reminders, security_alerts, marketing) VALUES
                                                                                                                                                           ('d0000000-0000-0000-0000-000000000001', TRUE, TRUE, TRUE, TRUE, TRUE, FALSE),
                                                                                                                                                           ('d0000000-0000-0000-0000-000000000002', TRUE, TRUE, TRUE, TRUE, TRUE, FALSE),
                                                                                                                                                           ('d0000000-0000-0000-0000-000000000003', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE),
                                                                                                                                                           ('d0000000-0000-0000-0000-000000000004', TRUE, TRUE, FALSE, FALSE, TRUE, FALSE),
                                                                                                                                                           ('d0000000-0000-0000-0000-000000000005', TRUE, TRUE, TRUE, FALSE, TRUE, FALSE),
                                                                                                                                                           ('d0000000-0000-0000-0000-000000000006', TRUE, TRUE, TRUE, TRUE, TRUE, FALSE);

-- ── 3.15 Compliance Audits ───────────────────────────────────────────────

INSERT INTO app_schema.compliance_audits (audit_date, audit_type, result, findings, auditor, notes) VALUES
                                                                                                        (CURRENT_DATE - 90, 'HIPAA Annual Assessment', 'PASSED', 0, 'MedCompliance Corp', 'Full HIPAA compliance verified.'),
                                                                                                        (CURRENT_DATE - 60, 'GDPR Data Processing Review', 'PASSED', 1, 'EU Privacy Partners', 'Minor: update data retention schedule.'),
                                                                                                        (CURRENT_DATE - 30, 'FDA 21 CFR Part 11 Audit', 'PASSED', 0, 'RegTech Solutions', 'Electronic signatures compliant.'),
                                                                                                        (CURRENT_DATE - 15, 'SOC 2 Type II', 'PASSED', 2, 'Deloitte', 'Two low-risk observations.'),
                                                                                                        (CURRENT_DATE + 7,  'HIPAA Quarterly Review Q1', 'PENDING', 0, 'MedCompliance Corp', 'Scheduled for next week.');

-- ── 3.16 Policy Documents ────────────────────────────────────────────────

INSERT INTO app_schema.policy_documents (name, version, status, content_url) VALUES
                                                                                 ('HIPAA Privacy Policy',           '3.1', 'CURRENT', '/policies/hipaa-privacy-v3.1.pdf'),
                                                                                 ('HIPAA Security Policy',          '2.4', 'CURRENT', '/policies/hipaa-security-v2.4.pdf'),
                                                                                 ('GDPR Data Processing Agreement', '2.0', 'CURRENT', '/policies/gdpr-dpa-v2.0.pdf'),
                                                                                 ('Acceptable Use Policy',          '1.5', 'CURRENT', '/policies/aup-v1.5.pdf'),
                                                                                 ('Incident Response Plan',         '4.0', 'CURRENT', '/policies/irp-v4.0.pdf'),
                                                                                 ('Data Retention Policy',          '1.2', 'REVIEW',  '/policies/data-retention-v1.2.pdf'),
                                                                                 ('Information Security Policy',    '2.1', 'CURRENT', '/policies/infosec-v2.1.pdf');

-- ── 3.17 Platform Settings ───────────────────────────────────────────────

INSERT INTO app_schema.platform_settings (key, value, category, updated_by) VALUES
                                                                                ('platform.name',             'BioLabs Services Hub', 'GENERAL',    'd0000000-0000-0000-0000-000000000001'),
                                                                                ('platform.version',          '1.0.0',                'GENERAL',    'd0000000-0000-0000-0000-000000000001'),
                                                                                ('auth.session_timeout_min',  '30',                   'SECURITY',   'd0000000-0000-0000-0000-000000000001'),
                                                                                ('auth.max_failed_attempts',  '5',                    'SECURITY',   'd0000000-0000-0000-0000-000000000001'),
                                                                                ('auth.lockout_duration_min', '30',                   'SECURITY',   'd0000000-0000-0000-0000-000000000001'),
                                                                                ('auth.password_min_length',  '12',                   'SECURITY',   'd0000000-0000-0000-0000-000000000001'),
                                                                                ('auth.mfa_enabled',          'true',                 'SECURITY',   'd0000000-0000-0000-0000-000000000001'),
                                                                                ('email.smtp_host',           'smtp.biolab.com',      'EMAIL',      'd0000000-0000-0000-0000-000000000001'),
                                                                                ('email.from_address',        'noreply@biolab.com',   'EMAIL',      'd0000000-0000-0000-0000-000000000001'),
                                                                                ('compliance.hipaa_enabled',  'true',                 'COMPLIANCE', 'd0000000-0000-0000-0000-000000000001'),
                                                                                ('compliance.gdpr_enabled',   'true',                 'COMPLIANCE', 'd0000000-0000-0000-0000-000000000001'),
                                                                                ('compliance.fda_cfr11',      'true',                 'COMPLIANCE', 'd0000000-0000-0000-0000-000000000001');

-- ── 3.18 Audit Events ────────────────────────────────────────────────────

INSERT INTO app_schema.audit_events (user_id, action, entity_type, entity_id, details, ip_address) VALUES
                                                                                                       ('d0000000-0000-0000-0000-000000000001', 'CREATE', 'USER', 'd0000000-0000-0000-0000-000000000002', '{"email":"supplier@biolab.com"}'::jsonb, '10.0.0.1'),
                                                                                                       ('d0000000-0000-0000-0000-000000000001', 'CREATE', 'USER', 'd0000000-0000-0000-0000-000000000003', '{"email":"buyer@pharmaco.com"}'::jsonb, '10.0.0.1'),
                                                                                                       ('d0000000-0000-0000-0000-000000000003', 'CREATE', 'SERVICE_REQUEST', 'f0000000-0000-0000-0000-000000000001', '{"service":"WGS","priority":"HIGH"}'::jsonb, '10.0.0.3'),
                                                                                                       ('d0000000-0000-0000-0000-000000000002', 'UPDATE', 'PROJECT', '10000000-0000-0000-0000-000000000001', '{"status":"IN_PROGRESS","progress":65}'::jsonb, '10.0.0.2'),
                                                                                                       ('d0000000-0000-0000-0000-000000000002', 'UPLOAD', 'DOCUMENT', '20000000-0000-0000-0000-000000000001', '{"fileName":"WGS_QC_Report.pdf"}'::jsonb, '10.0.0.2'),
                                                                                                       ('d0000000-0000-0000-0000-000000000002', 'CREATE', 'INVOICE', '30000000-0000-0000-0000-000000000001', '{"invoiceNumber":"INV-2026-0001","total":16275.00}'::jsonb, '10.0.0.2');


-- ════════════════════════════════════════════════════════════════════════════
-- VERIFY
-- ════════════════════════════════════════════════════════════════════════════

SELECT u.email, u.first_name || ' ' || u.last_name AS name, u.is_active,
       ARRAY_AGG(r.name ORDER BY r.name) AS roles
FROM sec_schema.users u
         LEFT JOIN sec_schema.user_roles ur ON ur.user_id = u.id
         LEFT JOIN sec_schema.roles r ON r.id = ur.role_id
GROUP BY u.email, u.first_name, u.last_name, u.is_active ORDER BY u.email;

SELECT 'users' AS t, COUNT(*) FROM sec_schema.users
UNION ALL SELECT 'user_roles', COUNT(*) FROM sec_schema.user_roles
UNION ALL SELECT 'organizations', COUNT(*) FROM app_schema.organizations
UNION ALL SELECT 'services', COUNT(*) FROM app_schema.services
UNION ALL SELECT 'projects', COUNT(*) FROM app_schema.projects
UNION ALL SELECT 'invoices', COUNT(*) FROM app_schema.invoices
UNION ALL SELECT 'messages', COUNT(*) FROM app_schema.messages
UNION ALL SELECT 'notifications', COUNT(*) FROM app_schema.notifications
ORDER BY t;

COMMIT;