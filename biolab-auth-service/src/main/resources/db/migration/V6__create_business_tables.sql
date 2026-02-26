-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs — Business Domain Tables (app_schema)
-- Version: V6 | Catalog, Projects, Documents, Invoices, Messaging, Notifications, Audit
-- ════════════════════════════════════════════════════════════════════════════

-- ─── SERVICE CATALOG ─────────────────────────────────────────────────────

CREATE TABLE app_schema.service_categories (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100)    NOT NULL UNIQUE,
    slug        VARCHAR(120)    NOT NULL UNIQUE,
    description TEXT,
    icon        VARCHAR(50),
    sort_order  INTEGER         NOT NULL DEFAULT 0,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE app_schema.services (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name              VARCHAR(255)    NOT NULL,
    slug              VARCHAR(280)    NOT NULL UNIQUE,
    category_id       UUID            NOT NULL REFERENCES app_schema.service_categories(id),
    supplier_org_id   UUID            NOT NULL REFERENCES app_schema.organizations(id),
    description       TEXT,
    methodology       TEXT,
    price_from        DECIMAL(12,2),
    turnaround        VARCHAR(50),
    rating            DECIMAL(2,1)    NOT NULL DEFAULT 0.0,
    review_count      INTEGER         NOT NULL DEFAULT 0,
    is_active         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_svc_category   ON app_schema.services(category_id);
CREATE INDEX idx_svc_supplier   ON app_schema.services(supplier_org_id);
CREATE INDEX idx_svc_active     ON app_schema.services(is_active);

CREATE TABLE app_schema.service_requests (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_id      UUID            NOT NULL REFERENCES app_schema.services(id),
    buyer_id        UUID            NOT NULL,
    buyer_org_id    UUID            REFERENCES app_schema.organizations(id),
    sample_type     VARCHAR(255),
    timeline        VARCHAR(20)     NOT NULL DEFAULT 'STANDARD',
    requirements    TEXT,
    priority        VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM'
        CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','ACCEPTED','DECLINED','CANCELLED')),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sr_service ON app_schema.service_requests(service_id);
CREATE INDEX idx_sr_buyer   ON app_schema.service_requests(buyer_id);
CREATE INDEX idx_sr_status  ON app_schema.service_requests(status);

-- ─── PROJECTS ────────────────────────────────────────────────────────────

CREATE TABLE app_schema.projects (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title               VARCHAR(255)    NOT NULL,
    service_request_id  UUID            REFERENCES app_schema.service_requests(id),
    buyer_org_id        UUID            NOT NULL REFERENCES app_schema.organizations(id),
    supplier_org_id     UUID            NOT NULL REFERENCES app_schema.organizations(id),
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','ACTIVE','IN_PROGRESS','IN_REVIEW','COMPLETED','CANCELLED','OVERDUE')),
    progress_pct        INTEGER         NOT NULL DEFAULT 0 CHECK (progress_pct BETWEEN 0 AND 100),
    budget              DECIMAL(12,2),
    start_date          DATE,
    deadline            DATE,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_proj_buyer    ON app_schema.projects(buyer_org_id);
CREATE INDEX idx_proj_supplier ON app_schema.projects(supplier_org_id);
CREATE INDEX idx_proj_status   ON app_schema.projects(status);

CREATE TABLE app_schema.project_members (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id  UUID            NOT NULL REFERENCES app_schema.projects(id) ON DELETE CASCADE,
    user_id     UUID            NOT NULL,
    role        VARCHAR(20)     NOT NULL DEFAULT 'MEMBER'
        CHECK (role IN ('OWNER','MEMBER','VIEWER')),
    added_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_proj_member UNIQUE (project_id, user_id)
);

CREATE TABLE app_schema.project_milestones (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id      UUID            NOT NULL REFERENCES app_schema.projects(id) ON DELETE CASCADE,
    title           VARCHAR(255)    NOT NULL,
    description     TEXT,
    milestone_date  DATE,
    is_completed    BOOLEAN         NOT NULL DEFAULT FALSE,
    completed_at    TIMESTAMPTZ,
    sort_order      INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pm_project ON app_schema.project_milestones(project_id);

-- ─── DOCUMENTS ───────────────────────────────────────────────────────────

CREATE TABLE app_schema.documents (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id      UUID            NOT NULL REFERENCES app_schema.projects(id) ON DELETE CASCADE,
    uploaded_by     UUID            NOT NULL,
    file_name       VARCHAR(500)    NOT NULL,
    file_type       VARCHAR(20),
    file_size       BIGINT          NOT NULL DEFAULT 0,
    storage_key     VARCHAR(1024)   NOT NULL,
    mime_type       VARCHAR(100),
    version         INTEGER         NOT NULL DEFAULT 1,
    checksum        VARCHAR(64),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_doc_project ON app_schema.documents(project_id);
CREATE INDEX idx_doc_uploader ON app_schema.documents(uploaded_by);

-- ─── INVOICES ────────────────────────────────────────────────────────────

CREATE TABLE app_schema.invoices (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_number  VARCHAR(20)     NOT NULL UNIQUE,
    project_id      UUID            REFERENCES app_schema.projects(id),
    supplier_org_id UUID            NOT NULL REFERENCES app_schema.organizations(id),
    buyer_org_id    UUID            NOT NULL REFERENCES app_schema.organizations(id),
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT','SENT','VIEWED','PAID','OVERDUE','CANCELLED')),
    subtotal        DECIMAL(12,2)   NOT NULL DEFAULT 0,
    tax_rate        DECIMAL(5,2)    NOT NULL DEFAULT 0,
    tax_amount      DECIMAL(12,2)   NOT NULL DEFAULT 0,
    total           DECIMAL(12,2)   NOT NULL DEFAULT 0,
    issue_date      DATE            NOT NULL DEFAULT CURRENT_DATE,
    due_date        DATE,
    paid_date       DATE,
    notes           TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inv_supplier ON app_schema.invoices(supplier_org_id);
CREATE INDEX idx_inv_buyer    ON app_schema.invoices(buyer_org_id);
CREATE INDEX idx_inv_status   ON app_schema.invoices(status);
CREATE INDEX idx_inv_number   ON app_schema.invoices(invoice_number);

CREATE TABLE app_schema.invoice_items (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id  UUID            NOT NULL REFERENCES app_schema.invoices(id) ON DELETE CASCADE,
    description VARCHAR(500)    NOT NULL,
    quantity    INTEGER         NOT NULL DEFAULT 1,
    unit_price  DECIMAL(12,2)   NOT NULL,
    amount      DECIMAL(12,2)   NOT NULL,
    sort_order  INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_ii_invoice ON app_schema.invoice_items(invoice_id);

-- ─── MESSAGING ───────────────────────────────────────────────────────────

CREATE TABLE app_schema.conversations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id      UUID            REFERENCES app_schema.projects(id),
    title           VARCHAR(255),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE app_schema.conversation_participants (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID            NOT NULL REFERENCES app_schema.conversations(id) ON DELETE CASCADE,
    user_id         UUID            NOT NULL,
    org_id          UUID            REFERENCES app_schema.organizations(id),
    joined_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_read_at    TIMESTAMPTZ,
    CONSTRAINT uq_conv_part UNIQUE (conversation_id, user_id)
);

CREATE TABLE app_schema.messages (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID            NOT NULL REFERENCES app_schema.conversations(id) ON DELETE CASCADE,
    sender_id       UUID            NOT NULL,
    content         TEXT            NOT NULL,
    attachment_id   UUID            REFERENCES app_schema.documents(id),
    is_read         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_msg_conv    ON app_schema.messages(conversation_id);
CREATE INDEX idx_msg_sender  ON app_schema.messages(sender_id);
CREATE INDEX idx_msg_created ON app_schema.messages(created_at DESC);

-- ─── NOTIFICATIONS ───────────────────────────────────────────────────────

CREATE TABLE app_schema.notifications (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID            NOT NULL,
    type        VARCHAR(50)     NOT NULL,
    title       VARCHAR(255)    NOT NULL,
    message     TEXT,
    link        VARCHAR(500),
    is_read     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_user     ON app_schema.notifications(user_id);
CREATE INDEX idx_notif_unread   ON app_schema.notifications(user_id, is_read) WHERE NOT is_read;

CREATE TABLE app_schema.notification_preferences (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID            NOT NULL UNIQUE,
    email_enabled   BOOLEAN         NOT NULL DEFAULT TRUE,
    project_updates BOOLEAN         NOT NULL DEFAULT TRUE,
    new_messages    BOOLEAN         NOT NULL DEFAULT TRUE,
    invoice_reminders BOOLEAN       NOT NULL DEFAULT TRUE,
    security_alerts BOOLEAN         NOT NULL DEFAULT TRUE,
    marketing       BOOLEAN         NOT NULL DEFAULT FALSE,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ─── AUDIT / COMPLIANCE ──────────────────────────────────────────────────

CREATE TABLE app_schema.audit_events (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID,
    action      VARCHAR(100)    NOT NULL,
    entity_type VARCHAR(50)     NOT NULL,
    entity_id   UUID,
    details     JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user     ON app_schema.audit_events(user_id);
CREATE INDEX idx_audit_action   ON app_schema.audit_events(action);
CREATE INDEX idx_audit_created  ON app_schema.audit_events(created_at DESC);

CREATE TABLE app_schema.compliance_audits (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    audit_date  DATE            NOT NULL,
    audit_type  VARCHAR(100)    NOT NULL,
    result      VARCHAR(20)     NOT NULL DEFAULT 'PASSED'
        CHECK (result IN ('PASSED','FAILED','PENDING')),
    findings    INTEGER         NOT NULL DEFAULT 0,
    auditor     VARCHAR(255),
    report_url  VARCHAR(512),
    notes       TEXT,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE app_schema.policy_documents (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(255)    NOT NULL,
    version     VARCHAR(20)     NOT NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'CURRENT'
        CHECK (status IN ('CURRENT','REVIEW','ARCHIVED')),
    content_url VARCHAR(512),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE app_schema.platform_settings (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key         VARCHAR(100)    NOT NULL UNIQUE,
    value       TEXT            NOT NULL,
    category    VARCHAR(50)     NOT NULL DEFAULT 'GENERAL',
    updated_by  UUID,
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ─── TRIGGERS ────────────────────────────────────────────────────────────

CREATE TRIGGER trg_svc_cat_updated BEFORE UPDATE ON app_schema.service_categories
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
CREATE TRIGGER trg_svc_updated BEFORE UPDATE ON app_schema.services
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
CREATE TRIGGER trg_sr_updated BEFORE UPDATE ON app_schema.service_requests
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
CREATE TRIGGER trg_proj_updated BEFORE UPDATE ON app_schema.projects
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
CREATE TRIGGER trg_inv_updated BEFORE UPDATE ON app_schema.invoices
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
CREATE TRIGGER trg_conv_updated BEFORE UPDATE ON app_schema.conversations
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
CREATE TRIGGER trg_notif_pref_updated BEFORE UPDATE ON app_schema.notification_preferences
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
