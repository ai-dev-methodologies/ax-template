-- R20 feature-flags domain (specs/feature-flags-l0.yaml, blueprints/feature-flags-manifest.yaml)
--
-- FF-CRUD-001..004 — admin CRUD persistence
-- FF-EVAL-001/002  — name is the natural PK; lookup by name powers the eval endpoint
-- FF-VALID-001     — name pattern (^[a-z][a-z0-9-]{1,62}$) is enforced at the request layer
--                    with @Pattern; the column is sized for the regex upper bound (63 chars).
-- FF-VALID-002     — description max 500 chars enforced by both column length and @Size
--
-- Hard delete (blueprints/feature-flags-manifest.yaml#crud.delete_strategy: hard_delete)
-- so unknown names fail-closed on subsequent eval (FF-EVAL-002).

CREATE TABLE feature_flags (
    name         VARCHAR(63)  PRIMARY KEY,
    enabled      BOOLEAN      NOT NULL DEFAULT FALSE,
    description  VARCHAR(500),
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);
