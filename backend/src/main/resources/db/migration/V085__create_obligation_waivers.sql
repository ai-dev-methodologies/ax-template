-- P3-40 — extends deadline-obligation (V044, V084) with a dual-axis (time AND usage-cycle)
-- conditional waiver that suspends ONLY the BREACH consequence (OBL-WAIVER-001/002). The
-- escalation ladder is untouched by any waiver.

ALTER TABLE deadline_obligations ADD COLUMN usage_cycle_count BIGINT NOT NULL DEFAULT 0;

-- OBL-WAIVER-002 — fully immutable once granted; there is no UPDATE path in this schema.
CREATE TABLE obligation_waivers (
    id                    UUID          NOT NULL PRIMARY KEY,
    obligation_id         UUID          NOT NULL REFERENCES deadline_obligations(id),
    granted_by            VARCHAR(200)  NOT NULL,
    obligation_owner      VARCHAR(200)  NOT NULL,
    reason                VARCHAR(500)  NOT NULL,
    granted_at            TIMESTAMP     NOT NULL,
    granted_at_cycle      BIGINT        NOT NULL,
    expires_at            TIMESTAMP     NOT NULL,
    expires_after_cycles  BIGINT        NOT NULL
);

CREATE INDEX ix_obligation_waiver ON obligation_waivers (obligation_id);

-- OBL-WAIVER-002 — revoke APPENDS here; the grant row above is never re-mutated. The UNIQUE
-- index makes a double-revoke unrepresentable even if the service check is bypassed.
CREATE TABLE obligation_waiver_revocations (
    id             UUID         NOT NULL PRIMARY KEY,
    waiver_id      UUID         NOT NULL REFERENCES obligation_waivers(id),
    obligation_id  UUID         NOT NULL REFERENCES deadline_obligations(id),
    revoked_by     VARCHAR(200) NOT NULL,
    revoked_at     TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX uq_waiver_revocation ON obligation_waiver_revocations (waiver_id);
