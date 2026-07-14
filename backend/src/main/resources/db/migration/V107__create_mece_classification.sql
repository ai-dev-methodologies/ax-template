-- mece-classification reference workload — realizes specs/mece-classification-l0.yaml
-- (Lane G capital-markets wave: an item is assigned to EXACTLY one category of a scheme; every
-- scheme MUST declare a residual bucket at config time so classification never fails open;
-- reclassification is an append-only move record, current category derive-on-read from the latest).

CREATE TABLE classification_schemes (
    id                UUID          NOT NULL PRIMARY KEY,
    scheme_key        VARCHAR(200)  NOT NULL,
    residual_category VARCHAR(200)  NOT NULL,
    created_at        TIMESTAMP     NOT NULL,
    CONSTRAINT chk_mece_scheme CHECK (LENGTH(residual_category) > 0)
);

CREATE UNIQUE INDEX uq_mece_scheme_key ON classification_schemes (scheme_key);

CREATE TABLE classification_rules (
    id          UUID          NOT NULL PRIMARY KEY,
    scheme_key  VARCHAR(200)  NOT NULL,
    match_value VARCHAR(200)  NOT NULL,
    category    VARCHAR(200)  NOT NULL,
    created_at  TIMESTAMP     NOT NULL
);

CREATE UNIQUE INDEX uq_mece_rule_match ON classification_rules (scheme_key, match_value);

CREATE TABLE item_classifications (
    id         UUID          NOT NULL PRIMARY KEY,
    scheme_key VARCHAR(200)  NOT NULL,
    item_ref   VARCHAR(200)  NOT NULL,
    created_at TIMESTAMP     NOT NULL
);

CREATE UNIQUE INDEX uq_mece_scheme_item ON item_classifications (scheme_key, item_ref);

CREATE TABLE classification_moves (
    id                UUID          NOT NULL PRIMARY KEY,
    classification_id UUID          NOT NULL,
    from_category     VARCHAR(200),
    to_category       VARCHAR(200)  NOT NULL,
    actor             VARCHAR(200)  NOT NULL,
    reason            VARCHAR(500)  NOT NULL,
    moved_at          TIMESTAMP     NOT NULL
);

CREATE INDEX idx_classification_moves_classification ON classification_moves (classification_id);
