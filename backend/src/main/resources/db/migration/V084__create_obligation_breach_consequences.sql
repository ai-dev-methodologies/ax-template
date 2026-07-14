-- P3-20 — extends deadline-obligation (V044) with an OPT-IN monetary BREACH consequence
-- (OBL-CONSEQUENCE-001/OBL-INTEREST-ACCRUE-001). Interest is DERIVE-ON-READ — there is no
-- accrued-amount column anywhere in this migration, by design.

ALTER TABLE deadline_obligations ADD COLUMN breach_basis_amount NUMERIC(19,4);

-- OBL-CONSEQUENCE-001 — exactly one consequence per obligation: the UNIQUE index is the DB backstop.
CREATE TABLE obligation_breach_consequences (
    id                     UUID         NOT NULL PRIMARY KEY,
    obligation_id          UUID         NOT NULL REFERENCES deadline_obligations(id),
    recorded_at            TIMESTAMP    NOT NULL,
    basis_amount           NUMERIC(19,4) NOT NULL,
    deadline_at_recording  TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX uq_obligation_consequence ON obligation_breach_consequences (obligation_id);
