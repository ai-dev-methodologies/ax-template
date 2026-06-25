-- identityclaim domain (identity-claim-on-auth-l0.yaml)
-- IDCLAIM-CLAIM-001: atomic transfer of all unclaimed rows for a claim_key
-- IDCLAIM-IDEMPOTENT-001: CAS WHERE owner_user_id IS NULL → replay returns 0 rows
-- IDCLAIM-GUARD-001: rows already owned (owner_user_id IS NOT NULL) are never touched

CREATE TABLE claimable_records (
    id              UUID         NOT NULL,
    claim_key       VARCHAR(255) NOT NULL,
    owner_user_id   VARCHAR(255),             -- NULL = unclaimed/anonymous
    label           VARCHAR(255) NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_claimable_records PRIMARY KEY (id)
);

CREATE INDEX idx_claimable_records_claim_key ON claimable_records (claim_key);
