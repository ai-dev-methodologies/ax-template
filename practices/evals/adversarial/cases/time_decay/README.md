# Adversarial case: time_decay (stub)

Purpose: prove the drift axis rejects rules anchored to upstream snapshots older than 90d.

Status: **stub** — runner not implemented. Sketch:

1. Read each rule's `upstream:` list.
2. For each upstream item, look it up in `practices/upstream/_MANIFEST.yaml`.
3. Compute age from `fetched_at`.
4. If any upstream age > 90 days, return BLOCK.

Not implemented in P1; deferred to P2-A.

See `practices/evals/drift/.gitkeep` and `.github/workflows/practices-drift.yml` for the
companion drift automation.
