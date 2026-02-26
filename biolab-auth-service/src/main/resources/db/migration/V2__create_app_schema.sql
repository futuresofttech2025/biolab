-- ════════════════════════════════════════════════════════════════════════════
-- BioLabs — Application Schema (app_schema)
-- Version: V2 | Organizations and user-organization memberships.
-- ════════════════════════════════════════════════════════════════════════════

CREATE SCHEMA IF NOT EXISTS app_schema;

-- ─── organizations ───────────────────────────────────────────────────────
CREATE TABLE app_schema.organizations (
    id          UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(255)    NOT NULL,
    type        VARCHAR(20)     NOT NULL CHECK (type IN ('BUYER', 'SUPPLIER')),
    address     VARCHAR(500),
    phone       VARCHAR(20),
    website     VARCHAR(255),
    logo_url    VARCHAR(512),
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_org_type ON app_schema.organizations(type);
CREATE INDEX idx_org_active ON app_schema.organizations(is_active);

COMMENT ON TABLE app_schema.organizations IS 'Buyer and Supplier organizations on the BioLabs platform';

-- ─── user_organizations ──────────────────────────────────────────────────
CREATE TABLE app_schema.user_organizations (
    id          UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID            NOT NULL,
    org_id      UUID            NOT NULL REFERENCES app_schema.organizations(id) ON DELETE CASCADE,
    role_in_org VARCHAR(50)     NOT NULL DEFAULT 'MEMBER',
    is_primary  BOOLEAN         NOT NULL DEFAULT FALSE,
    joined_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_org UNIQUE (user_id, org_id)
);

CREATE INDEX idx_user_org_user ON app_schema.user_organizations(user_id);
CREATE INDEX idx_user_org_org ON app_schema.user_organizations(org_id);

COMMENT ON TABLE app_schema.user_organizations IS 'User-Organization membership with role and primary flag';

CREATE OR REPLACE FUNCTION app_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_org_updated_at BEFORE UPDATE ON app_schema.organizations
    FOR EACH ROW EXECUTE FUNCTION app_schema.update_updated_at_column();
