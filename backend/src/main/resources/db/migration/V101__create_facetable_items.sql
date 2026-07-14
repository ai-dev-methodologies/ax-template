-- facet-count-l0 (specs/facet-count-l0.yaml)
-- FACET-COUNT-001: bucket counts scoped to owner_id, identical to the list's caller scope.
-- FACET-ALLOWLIST-002: category/status are the only facetable columns; owner_id is not.

CREATE TABLE facetable_items (
    id UUID PRIMARY KEY,
    owner_id VARCHAR(200) NOT NULL,
    category VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_facetable_items_owner ON facetable_items(owner_id);
