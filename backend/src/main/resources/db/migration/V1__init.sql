CREATE TABLE feature_flags (
    id          UUID         PRIMARY KEY,
    key         VARCHAR(128) NOT NULL UNIQUE,
    name        VARCHAR(256) NOT NULL,
    description TEXT,
    enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_feature_flags_key ON feature_flags (key);
