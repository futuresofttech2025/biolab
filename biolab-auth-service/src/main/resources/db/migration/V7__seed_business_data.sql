-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs — Seed Data for Business Tables
-- Version: V7 | Categories, sample services, compliance audits, policies
-- ════════════════════════════════════════════════════════════════════════════

-- ─── Service Categories ──────────────────────────────────────────────────
INSERT INTO app_schema.service_categories (id, name, slug, description, icon, sort_order) VALUES
  ('a1000000-0000-0000-0000-000000000001', 'Biochemical',      'biochemical',      'Enzyme kinetics, HPLC, mass spec, binding assays',       'FlaskConical', 1),
  ('a1000000-0000-0000-0000-000000000002', 'Protein Sciences',  'protein-sciences',  'Purification, characterization, antibody development',    'Database',     2),
  ('a1000000-0000-0000-0000-000000000003', 'Cell Biology',      'cell-biology',      'Cell-based assays, flow cytometry, cell line development', 'Microscope',   3),
  ('a1000000-0000-0000-0000-000000000004', 'Bioprocess',        'bioprocess',        'CHO optimization, bioprocess scale-up',                   'Zap',          4),
  ('a1000000-0000-0000-0000-000000000005', 'Genomics',          'genomics',          'PCR, gene expression, microbiome analysis',               'Dna',          5),
  ('a1000000-0000-0000-0000-000000000006', 'Compliance',        'compliance',        'Stability, endotoxin, dissolution, toxicology',           'ShieldCheck',  6);

-- ─── Sample Organizations (Suppliers) ────────────────────────────────────
INSERT INTO app_schema.organizations (id, name, type, is_active) VALUES
  ('b1000000-0000-0000-0000-000000000001', 'BioLabs Alpha',       'SUPPLIER', true),
  ('b1000000-0000-0000-0000-000000000002', 'CoreGen Labs',        'SUPPLIER', true),
  ('b1000000-0000-0000-0000-000000000003', 'Pacific BioLabs',     'SUPPLIER', true),
  ('b1000000-0000-0000-0000-000000000004', 'SynBio Solutions',    'SUPPLIER', true),
  ('b1000000-0000-0000-0000-000000000005', 'Helix Dynamics',      'SUPPLIER', true),
  ('b1000000-0000-0000-0000-000000000006', 'MolecuLab',           'SUPPLIER', true)
ON CONFLICT (id) DO NOTHING;

-- ─── Sample Organizations (Buyers) ──────────────────────────────────────
INSERT INTO app_schema.organizations (id, name, type, is_active) VALUES
  ('c1000000-0000-0000-0000-000000000001', 'PharmaCorp Inc.',        'BUYER', true),
  ('c1000000-0000-0000-0000-000000000002', 'GeneTech Labs',          'BUYER', true),
  ('c1000000-0000-0000-0000-000000000003', 'BioVista Research',      'BUYER', true),
  ('c1000000-0000-0000-0000-000000000004', 'MediSync Pharma',        'BUYER', true),
  ('c1000000-0000-0000-0000-000000000005', 'NovaBio Therapeutics',   'BUYER', true),
  ('c1000000-0000-0000-0000-000000000006', 'CureLogic',              'BUYER', true),
  ('c1000000-0000-0000-0000-000000000007', 'Vertex Bio',             'BUYER', true),
  ('c1000000-0000-0000-0000-000000000008', 'OmniCell Research',      'BUYER', true),
  ('c1000000-0000-0000-0000-000000000009', 'Elara Therapeutics',     'BUYER', true),
  ('c1000000-0000-0000-0000-000000000010', 'Nexus Pharma',           'BUYER', true)
ON CONFLICT (id) DO NOTHING;

-- ─── Sample Services ─────────────────────────────────────────────────────
INSERT INTO app_schema.services (name, slug, category_id, supplier_org_id, description, price_from, turnaround, rating, review_count) VALUES
  ('Enzyme Kinetics Analysis', 'enzyme-kinetics-analysis', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 'Full Michaelis-Menten kinetic profiling with inhibition constants', 2800, '3-7 days', 4.9, 47),
  ('Protein Characterization', 'protein-characterization', 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000002', 'SEC-MALS, DSC, DLS for biophysical characterization', 3500, '5-10 days', 4.8, 32),
  ('Cell-Based Assays', 'cell-based-assays', 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000001', 'Viability, proliferation, and cytotoxicity panels', 3000, '5-10 days', 4.9, 56),
  ('Stability Studies', 'stability-studies', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000003', 'ICH Q1A/Q1B compliant stability programs', 4500, '6-12 months', 4.9, 24),
  ('Bioprocess Optimization', 'bioprocess-optimization', 'a1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000005', 'CHO cell culture and media optimization', 5000, '6-12 weeks', 4.7, 18),
  ('Flow Cytometry Panel', 'flow-cytometry-panel', 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000001', 'Multi-color flow cytometry with immune profiling', 1800, '3-5 days', 4.8, 38),
  ('Mass Spectrometry', 'mass-spectrometry', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000006', 'LC-MS/MS for proteomics and metabolomics', 3100, '4-8 days', 4.8, 29),
  ('Western Blot Analysis', 'western-blot-analysis', 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000006', 'Quantitative western blot with phospho-panels', 1500, '2-4 days', 4.7, 64),
  ('PCR & qPCR Services', 'pcr-qpcr-services', 'a1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000002', 'Real-time PCR with gene expression analysis', 2200, '2-5 days', 4.9, 78),
  ('HPLC Analysis', 'hplc-analysis', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 'Method development and validation for small molecules', 2600, '3-7 days', 4.8, 41),
  ('Gene Expression Profiling', 'gene-expression-profiling', 'a1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000002', 'RNA-Seq and microarray analysis', 5400, '7-14 days', 4.9, 19),
  ('Binding Assay Suite', 'binding-assay-suite', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000004', 'SPR and BLI for affinity measurements', 3300, '5-10 days', 4.7, 27),
  ('Protein Purification', 'protein-purification', 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000002', 'Affinity, ion exchange, and SEC purification', 3900, '5-10 days', 4.8, 35),
  ('Endotoxin Testing', 'endotoxin-testing', 'a1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000003', 'LAL and rFC assays for endotoxin quantification', 1500, '1-3 days', 4.9, 88),
  ('Dissolution Studies', 'dissolution-studies', 'a1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000003', 'USP apparatus I/II with full profiles', 3600, '3-6 weeks', 4.7, 15),
  ('Metabolomics Panel', 'metabolomics-panel', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000006', 'Untargeted metabolomics with pathway analysis', 5700, '7-14 days', 4.8, 12),
  ('Cell Line Development', 'cell-line-development', 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000001', 'Stable cell line generation and characterization', 8500, '8-16 weeks', 4.9, 9),
  ('Microbiome Analysis', 'microbiome-analysis', 'a1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000005', '16S rRNA and shotgun metagenomics', 4800, '7-14 days', 4.6, 16),
  ('Toxicology Assessment', 'toxicology-assessment', 'a1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000003', 'GLP-compliant acute and chronic toxicology', 8200, '4-8 weeks', 4.8, 11),
  ('Antibody Development', 'antibody-development', 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000004', 'Custom monoclonal and polyclonal antibody generation', 12000, '12-20 weeks', 4.9, 8),
  ('Immunohistochemistry', 'immunohistochemistry', 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000006', 'IHC staining and quantitative image analysis', 3400, '5-8 days', 4.6, 22),
  ('Drug Metabolism Panel', 'drug-metabolism-panel', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000002', 'CYP inhibition and induction studies', 6800, '5-10 days', 4.7, 14),
  ('Bioavailability Study', 'bioavailability-study', 'a1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000003', 'PK/PD modeling with bioavailability determination', 11500, '8-16 weeks', 4.8, 6),
  ('CHO Cell Optimization', 'cho-cell-optimization', 'a1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000005', 'DoE-based CHO process development', 7200, '6-12 weeks', 4.7, 10);

-- ─── Compliance Audits ───────────────────────────────────────────────────
INSERT INTO app_schema.compliance_audits (audit_date, audit_type, result, findings) VALUES
  ('2025-12-15', 'HIPAA Assessment', 'PASSED', 0),
  ('2025-11-20', 'GDPR Compliance Review', 'PASSED', 1),
  ('2025-10-10', 'SOC 2 Type II Audit', 'PASSED', 0),
  ('2025-09-01', 'FDA 21 CFR Part 11', 'PASSED', 2),
  ('2025-08-15', 'Penetration Testing', 'PASSED', 3),
  ('2025-07-05', 'ISO 27001 Surveillance', 'PASSED', 0),
  ('2025-06-20', 'Data Privacy Impact Assessment', 'PASSED', 1),
  ('2025-05-10', 'Business Continuity Test', 'PASSED', 0),
  ('2025-04-01', 'Vendor Security Review', 'PASSED', 2),
  ('2025-03-15', 'Internal Vulnerability Scan', 'PASSED', 4),
  ('2025-02-10', 'HIPAA Risk Assessment', 'PASSED', 1),
  ('2025-01-05', 'SOC 2 Type I Readiness', 'PASSED', 0);

-- ─── Policy Documents ────────────────────────────────────────────────────
INSERT INTO app_schema.policy_documents (name, version, status) VALUES
  ('Data Protection Policy', 'v4.2', 'CURRENT'),
  ('Incident Response Plan', 'v3.1', 'CURRENT'),
  ('Access Control Policy', 'v5.0', 'CURRENT'),
  ('Encryption Standards', 'v2.8', 'CURRENT'),
  ('Vendor Security Policy', 'v1.5', 'REVIEW'),
  ('Acceptable Use Policy', 'v3.4', 'CURRENT'),
  ('Password Management Policy', 'v2.1', 'CURRENT'),
  ('Data Retention Policy', 'v1.9', 'CURRENT'),
  ('Change Management Procedure', 'v4.0', 'CURRENT'),
  ('Disaster Recovery Plan', 'v2.5', 'REVIEW'),
  ('Network Security Policy', 'v3.7', 'CURRENT'),
  ('Physical Security Policy', 'v1.3', 'CURRENT'),
  ('Third-Party Risk Framework', 'v2.0', 'CURRENT'),
  ('Data Classification Guide', 'v1.6', 'REVIEW');

-- ─── Platform Settings ───────────────────────────────────────────────────
INSERT INTO app_schema.platform_settings (key, value, category) VALUES
  ('session.timeout', '30', 'SECURITY'),
  ('password.min_length', '8', 'SECURITY'),
  ('password.rotation_days', '90', 'SECURITY'),
  ('mfa.required', 'true', 'SECURITY'),
  ('rate_limit.requests_per_minute', '100', 'SECURITY'),
  ('data.retention_years', '7', 'COMPLIANCE'),
  ('data.encryption_algorithm', 'AES-256-GCM', 'COMPLIANCE'),
  ('email.notifications_enabled', 'true', 'NOTIFICATIONS'),
  ('platform.maintenance_mode', 'false', 'GENERAL'),
  ('platform.max_file_size_mb', '100', 'GENERAL');
