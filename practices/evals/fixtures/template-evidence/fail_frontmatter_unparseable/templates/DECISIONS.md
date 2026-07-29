# templates/DECISIONS.md (fixture) — Decision Provenance Trail

Shape C: one fenced yaml block per ADR, `evidence` as a single mapping (not a list).

---

## TD-2026-07-29-001 — Fixture ADR with an internal evidence mapping

```yaml
---
adr_id: TD-2026-07-29-001
title: "Fixture ADR"
provenance_class: internal_design
evidence:
  source_type: internal
  source_ref: practices/evals/fixtures/template-evidence/pass_clean
  rationale: |
    Positive control for shape C — an ADR whose evidence mapping carries a
    non-empty rationale, so the gate is proven to PASS an honest ADR rather
    than only to FAIL a dishonest one.
spec_ref: N/A
status: ACCEPTED
date: 2026-07-29
---
```

### Decision

Keep the fixture minimal: one ADR per shape-C defect class.
