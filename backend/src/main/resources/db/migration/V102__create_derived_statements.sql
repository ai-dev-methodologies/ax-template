-- derived-statement-l0 (specs/derived-statement-l0.yaml)
-- STMT-DERIVE-001: identity = (subject, period, basis_hash) — a content hash, never a client token.
-- STMT-IMMUTABLE-003: every column is append-only; there is no update path.

CREATE TABLE derived_statements (
    id UUID PRIMARY KEY,
    subject VARCHAR(200) NOT NULL,
    period VARCHAR(50) NOT NULL,
    version_no INT NOT NULL,
    basis_hash VARCHAR(64) NOT NULL,
    basis_json VARCHAR(4000) NOT NULL,
    total_amount DECIMAL(15,4) NOT NULL,
    generated_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX uq_statement_basis ON derived_statements(subject, period, basis_hash);
