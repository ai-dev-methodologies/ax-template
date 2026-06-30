-- ANCHOR-001 — add immutable anchor_ref to transfer_entries
-- Every applied transfer stores the OnChainAnchor tx-ref set at apply time.
-- DEFAULT '' keeps H2/Postgres happy for existing rows on schema init.
ALTER TABLE transfer_entries ADD COLUMN anchor_ref VARCHAR(200) NOT NULL DEFAULT '';
