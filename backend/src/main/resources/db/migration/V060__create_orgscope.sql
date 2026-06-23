-- containment-scope-authz reference workload — realizes specs/containment-scope-authz-l0.yaml
-- (P1-47 / IDW11-G7: hierarchical containment-scope authorization — an org-unit TREE where a role
-- granted at a NODE cascades to the entire SUBTREE under it; the cascade is DERIVED from the tree
-- path (a prefix test at arbitrary depth), never a denormalized per-node ACL).

CREATE TABLE org_units (
    id          UUID          NOT NULL PRIMARY KEY,
    parent_id   UUID          REFERENCES org_units(id),   -- NULL only for a tree root
    name        VARCHAR(200)  NOT NULL,
    -- ORGSCOPE-TREE-001 — materialized ancestor path: /ancestor/.../self/ ; a node N is in the
    -- subtree of node A iff N.path starts with A.path (the containment cascade is this prefix test).
    path        VARCHAR(2000) NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMP     NOT NULL,
    -- a node is never its own parent (no self-loop in the tree)
    CONSTRAINT chk_org_unit_no_self_parent CHECK (parent_id IS NULL OR parent_id <> id)
);

-- ORGSCOPE-GRANT-001 — an immutable grant: a principal holds a role AT a node; the cascade to the
-- node's subtree is derived at decision time, never stored. One grant per (node, principal, role) —
-- the uq makes a re-grant idempotent and makes a concurrent same-key grant a deterministic loser.
CREATE TABLE scope_grants (
    id          UUID         NOT NULL PRIMARY KEY,
    org_unit_id UUID         NOT NULL REFERENCES org_units(id),
    principal   VARCHAR(320) NOT NULL,
    role        VARCHAR(20)  NOT NULL,                    -- VIEWER | EDITOR | MANAGER
    granted_by  VARCHAR(320) NOT NULL,
    granted_at  TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX uq_scope_grant ON scope_grants (org_unit_id, principal, role);
