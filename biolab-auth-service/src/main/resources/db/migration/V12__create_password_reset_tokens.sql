-- ═══════════════════════════════════════════════════════════════════════
-- V12 — Password Reset Tokens
-- Stores short-lived, single-use tokens for the forgot-password flow.
-- ═══════════════════════════════════════════════════════════════════════

CREATE TABLE sec_schema.password_reset_tokens (
                                                  id          UUID        NOT NULL DEFAULT gen_random_uuid(),
                                                  user_id     UUID        NOT NULL,
                                                  token_hash  VARCHAR(255) NOT NULL,          -- BCrypt hash of the raw token
                                                  expires_at  TIMESTAMPTZ NOT NULL,
                                                  used        BOOLEAN     NOT NULL DEFAULT FALSE,
                                                  used_at     TIMESTAMPTZ,
                                                  ip_address  VARCHAR(45),
                                                  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                                  CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
                                                  CONSTRAINT fk_prt_user FOREIGN KEY (user_id)
                                                      REFERENCES sec_schema.users (id) ON DELETE CASCADE,
                                                  CONSTRAINT uq_prt_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_prt_user_id   ON sec_schema.password_reset_tokens (user_id);
CREATE INDEX idx_prt_expires   ON sec_schema.password_reset_tokens (expires_at);

COMMENT ON TABLE  sec_schema.password_reset_tokens              IS 'Single-use password reset tokens — 15-minute TTL';
COMMENT ON COLUMN sec_schema.password_reset_tokens.token_hash  IS 'BCrypt hash of the raw token sent via email — raw token is never stored';
COMMENT ON COLUMN sec_schema.password_reset_tokens.used        IS 'True once the token has been consumed — cannot be reused';