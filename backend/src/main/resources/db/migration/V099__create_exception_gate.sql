-- orthogonal-exception-gate-l0 (specs/orthogonal-exception-gate-l0.yaml)
-- Generalized from dsr.DsrRestrictionGate (GDPR Art 18). EXC-DIM-INDEPENDENT:
-- raised/lifted is orthogonal to primary_state — neither mutates the other.
-- EXC-DIM-LIFT: every raise/lift is audited append-only in exception_audit_entries.

CREATE TABLE exception_gates (
    id UUID PRIMARY KEY,
    subject_type VARCHAR(100) NOT NULL,
    subject_id VARCHAR(200) NOT NULL,
    raised BOOLEAN NOT NULL,
    reason VARCHAR(500),
    primary_state VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX uq_exception_gate_subject ON exception_gates(subject_type, subject_id);

CREATE TABLE exception_audit_entries (
    id UUID PRIMARY KEY,
    gate_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    actor VARCHAR(200) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_exception_audit_gate FOREIGN KEY (gate_id) REFERENCES exception_gates(id)
);
