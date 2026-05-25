-- R54 — identity-verification residual closure (IDV-CALLBACK-002 persistence).
-- specs/identity-verification-l0.yaml#IDV-CALLBACK-002, IDV-CALLBACK-003, IDV-ADMIN-001.

CREATE TABLE verified_identity (
    id              UUID         PRIMARY KEY,
    ci              VARCHAR(128) NOT NULL,
    di              VARCHAR(128) NOT NULL,
    name            VARCHAR(128) NOT NULL,
    -- DOB stored as VARCHAR not DATE — providers occasionally return partial
    -- DOBs (yyyy-MM with missing day); the catalog refuses to silently
    -- normalise that for fork-receivers.
    dob             VARCHAR(16)  NOT NULL,
    verified_at     TIMESTAMP    NOT NULL,
    provider_name   VARCHAR(32)  NOT NULL
);

-- IDV-CALLBACK-002 query path: admin list filtered by provider, sorted by recency.
CREATE INDEX idx_verified_identity_provider_verified_at
    ON verified_identity (provider_name, verified_at DESC);

-- IDV-CALLBACK-002 dedup support: cross-service unique correlation by CI.
CREATE INDEX idx_verified_identity_ci ON verified_identity (ci);

-- Provider-specific extras (carrier code, request_id, response_seq, etc) live
-- in a side table indexed by parent FK. The catalog promises no schema on
-- these keys — fork-receivers query them as they like.
CREATE TABLE verified_identity_metadata (
    verified_identity_id UUID         NOT NULL,
    meta_key             VARCHAR(64)  NOT NULL,
    meta_value           VARCHAR(1024),
    PRIMARY KEY (verified_identity_id, meta_key),
    FOREIGN KEY (verified_identity_id)
        REFERENCES verified_identity (id)
        ON DELETE CASCADE
);
