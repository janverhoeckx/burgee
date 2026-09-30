-- Targeting Rule: the Conditions of a flag, in the order the admin gave them.
-- Spring Data JDBC maps this as an ordered one-to-many (List) on the feature_flags aggregate:
-- flag_id is the back reference, position the list index.
CREATE TABLE flag_condition (
    flag_id   UUID         NOT NULL REFERENCES feature_flags (id) ON DELETE CASCADE,
    position  INTEGER      NOT NULL,
    attribute VARCHAR(64)  NOT NULL,
    operator  VARCHAR(16)  NOT NULL,
    "values"  VARCHAR(256) ARRAY NOT NULL,
    PRIMARY KEY (flag_id, position),
    UNIQUE (flag_id, attribute)
);
