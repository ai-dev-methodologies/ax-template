-- time-bounded-access-grant reference workload — realizes specs/time-bounded-access-grant-l0.yaml
-- (P1-44 IDW9-G13 time-bounded relationship grant + P1-45 IDW9-G16 multi-credential eligibility gate):
-- a (subject, resource, relation) grant carrying a half-open validity window [valid_from, valid_until)
-- whose 'expired' verdict is RECOMPUTED over the injected Clock (NEVER a stored boolean), append-only +
-- revocable (who/when recorded, no delete); plus credentials each carrying their own window, gated as a
-- SET (an operation passes only when EVERY required class is held and non-expired at now).

CREATE TABLE access_grants (
    id            UUID         NOT NULL PRIMARY KEY,
    subject_id    VARCHAR(200) NOT NULL,
    resource_ref  VARCHAR(200) NOT NULL,
    relation      VARCHAR(100) NOT NULL,
    valid_from    TIMESTAMP    NOT NULL,
    valid_until   TIMESTAMP    NOT NULL,
    status        VARCHAR(20)  NOT NULL,        -- ACTIVE | REVOKED (no EXPIRED — expiry is recomputed)
    revoked_by    VARCHAR(200),                 -- recorded ONCE on revoke (write-once via the revoke UPDATE)
    revoked_at    TIMESTAMP,                    -- recorded ONCE on revoke (write-once via the revoke UPDATE)
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL,
    -- AGRANT-WINDOW/REVOKE-001 — the window is non-empty; status is one of the two legal values;
    -- a REVOKED status iff a revoke instant is recorded; revoked_at and revoked_by are recorded together.
    CONSTRAINT chk_access_grant CHECK (
        valid_until > valid_from
        AND (status = 'ACTIVE' OR status = 'REVOKED')
        AND ((status = 'REVOKED') = (revoked_at IS NOT NULL))
        AND ((revoked_at IS NULL) = (revoked_by IS NULL))
    )
);

-- AGRANT-ELIGIBILITY-001 — a credential a subject holds, with its OWN half-open validity window;
-- non-expired iff now in [valid_from, valid_until). Append-only; no delete path.
CREATE TABLE access_credentials (
    id               UUID         NOT NULL PRIMARY KEY,
    subject_id       VARCHAR(200) NOT NULL,
    credential_class VARCHAR(100) NOT NULL,
    valid_from       TIMESTAMP    NOT NULL,
    valid_until      TIMESTAMP    NOT NULL,
    created_at       TIMESTAMP    NOT NULL,
    CONSTRAINT chk_access_credential CHECK (valid_until > valid_from)
);

-- AGRANT-ELIGIBILITY-001 — the gate reads one subject's credentials by subject_id.
CREATE INDEX idx_access_credentials_subject ON access_credentials (subject_id);
