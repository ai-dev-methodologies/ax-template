-- authorization-parity reference workload — realizes specs/authorization-parity-l0.yaml
-- (P1-5 executed-matches-authorized: execution re-hashes the actual parameters and MUST match
--  the authorized envelope hash, else blocked; P1-6 four-eyes-signoff: a high-value action needs
--  TWO distinct approvers separated from the requester; P1-36 positive-gates: execution is
--  refused unless every declared mandatory companion gate is recorded present).

CREATE TABLE authorized_actions (
    id                 UUID          NOT NULL PRIMARY KEY,
    action_type        VARCHAR(100)  NOT NULL,
    authorized_params  VARCHAR(2000) NOT NULL,        -- canonical sorted key=value map (immutable)
    parity_hash        VARCHAR(64)   NOT NULL,         -- canonical SHA-256 over the authorized params
    high_value         BOOLEAN       NOT NULL,         -- four-eyes-required path
    requester_user_id  VARCHAR(200)  NOT NULL,
    status             VARCHAR(20)   NOT NULL,         -- AUTHORIZED | EXECUTED (no delete path)
    executed_at        TIMESTAMP,
    version            BIGINT        NOT NULL DEFAULT 0,
    created_at         TIMESTAMP     NOT NULL,
    -- AUTHZPARITY-EXEC-001 — an EXECUTED action records when; an AUTHORIZED one has not.
    CONSTRAINT chk_authz_action_executed CHECK (status = 'AUTHORIZED' OR executed_at IS NOT NULL)
);

-- AUTHZPARITY-GATES-001 — the declared mandatory companion gate keys (immutable after authorize).
CREATE TABLE authorized_action_required_gates (
    action_id UUID         NOT NULL REFERENCES authorized_actions(id),
    gate_key  VARCHAR(100) NOT NULL,
    PRIMARY KEY (action_id, gate_key)
);

-- AUTHZPARITY-FOUREYES-001 — one immutable signoff per (action, approver); signer <> requester.
CREATE TABLE action_signoffs (
    id                UUID         NOT NULL PRIMARY KEY,
    action_id         UUID         NOT NULL REFERENCES authorized_actions(id),
    approver_user_id  VARCHAR(200) NOT NULL,
    requester_user_id VARCHAR(200) NOT NULL,
    signed_at         TIMESTAMP    NOT NULL,
    CONSTRAINT chk_signoff_not_requester CHECK (approver_user_id <> requester_user_id)
);

CREATE UNIQUE INDEX uq_signoff_action_approver ON action_signoffs (action_id, approver_user_id);

-- AUTHZPARITY-GATES-001 — one immutable satisfaction per (action, gate_key).
CREATE TABLE gate_satisfactions (
    id           UUID         NOT NULL PRIMARY KEY,
    action_id    UUID         NOT NULL REFERENCES authorized_actions(id),
    gate_key     VARCHAR(100) NOT NULL,
    satisfied_by VARCHAR(200) NOT NULL,
    satisfied_at TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX uq_gate_action_key ON gate_satisfactions (action_id, gate_key);

-- AUTHZPARITY-EXEC-001 — one immutable record per refused (parity-mismatch) execution attempt.
CREATE TABLE blocked_attempts (
    id              UUID         NOT NULL PRIMARY KEY,
    action_id       UUID         NOT NULL REFERENCES authorized_actions(id),
    offered_hash    VARCHAR(64)  NOT NULL,
    authorized_hash VARCHAR(64)  NOT NULL,
    attempted_by    VARCHAR(200) NOT NULL,
    attempted_at    TIMESTAMP    NOT NULL,
    -- a blocked attempt is recorded ONLY for a genuine mismatch.
    CONSTRAINT chk_blocked_is_mismatch CHECK (offered_hash <> authorized_hash)
);

CREATE INDEX ix_blocked_attempts_action ON blocked_attempts (action_id);
