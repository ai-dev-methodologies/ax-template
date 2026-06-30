-- HOLDER-AUTHZ seam — ownership registry (specs/tokenized-securities-l0.yaml: HOLDER-AUTHZ-001/002)
-- A holder is controlled by exactly one principal (first-claim-wins, unique on holder_id).
-- Absence of a row means the holder is uncontrolled (fail-closed default for HolderAuthorization).

CREATE TABLE holder_ownership (
    id              UUID         NOT NULL PRIMARY KEY,
    holder_id       VARCHAR(200) NOT NULL,
    owner_principal VARCHAR(200) NOT NULL,
    claimed_at      TIMESTAMP    NOT NULL
);
CREATE UNIQUE INDEX uq_holder_ownership_holder ON holder_ownership (holder_id);
