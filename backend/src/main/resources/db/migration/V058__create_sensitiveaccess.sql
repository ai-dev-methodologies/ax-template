-- sensitive-read-audit reference workload — realizes specs/sensitive-read-audit-l0.yaml
-- (P1-43: the GENERIC sensitive-data read-audit primitive, lifted off the clinical @Phi tag onto
-- the domain-agnostic @SensitiveField marker). Reading the raw value of a @SensitiveField through
-- the service is itself an audited event: an immutable SensitiveAccessLog row (who/when/what/why per
-- NIST SP 800-53 AU-3) is appended in the SAME transaction, BEFORE the value is returned. The default
-- projection masks the value (AC-6 least privilege); the raw value is reached only via the audited,
-- purpose-stated reveal path. NO delete path exists — the access trail is append-only.

CREATE TABLE sensitive_records (
    id          UUID         NOT NULL PRIMARY KEY,
    record_ref  VARCHAR(200) NOT NULL,
    field_name  VARCHAR(100) NOT NULL,
    raw_value   VARCHAR(500) NOT NULL,        -- the @SensitiveField datum; never in the default projection
    owner       VARCHAR(200) NOT NULL,        -- the custodian (Authentication principal)
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL
);

-- SENSITIVE-READ-001 — one IMMUTABLE access record per reveal: the AU-3 content-of-audit-records
-- (what / when / where / source / outcome / identity). Append-only; no UNIQUE backstop (a record may
-- legitimately be revealed many times) — every reveal is a distinct audited event.
CREATE TABLE sensitive_access_logs (
    id          UUID         NOT NULL PRIMARY KEY,
    record_id   UUID         NOT NULL REFERENCES sensitive_records(id),
    record_ref  VARCHAR(200) NOT NULL,        -- WHAT (external reference)
    field_name  VARCHAR(100) NOT NULL,        -- WHAT (which sensitive field)
    accessor    VARCHAR(200) NOT NULL,        -- WHO (Authentication principal)
    purpose     VARCHAR(500) NOT NULL,        -- WHY (the stated non-blank purpose; AU-2 rationale)
    occurred_at TIMESTAMP    NOT NULL          -- WHEN (recorded from the injected Clock)
);

-- SENSITIVE-QUERY-001 — the append-only trail is queried per record in occurredAt order.
CREATE INDEX idx_sensitive_access_record ON sensitive_access_logs (record_id, occurred_at);
