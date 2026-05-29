CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    subject       VARCHAR(256) NOT NULL UNIQUE,
    email         VARCHAR(256),
    display_name  VARCHAR(256),
    role          VARCHAR(16)  NOT NULL,
    password_hash VARCHAR(256),
    provider      VARCHAR(16)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_subject ON users (subject);
