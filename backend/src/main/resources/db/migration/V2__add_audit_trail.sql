CREATE TABLE audit_entries (
    id          UUID         PRIMARY KEY,
    flag_id     UUID         NOT NULL,
    flag_key    VARCHAR(128) NOT NULL,
    action      VARCHAR(16)  NOT NULL,
    actor       VARCHAR(256) NOT NULL,
    detail      TEXT,
    occurred_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entries_flag_id ON audit_entries (flag_id);
CREATE INDEX idx_audit_entries_occurred_at ON audit_entries (occurred_at DESC);
