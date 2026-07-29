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
  note: "no upstream_id, no source_type — an unrecognised shape must not pass silently"
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

---

## TD-2026-07-29-002 — Clean same-shape sibling ADR

```yaml
---
adr_id: TD-2026-07-29-002
title: "Clean sibling ADR"
provenance_class: internal_design
evidence:
  source_type: internal
  source_ref: practices/evals/fixtures/template-evidence/fail_adr_unknown_shape
  rationale: |
    Present so this fixture isolates exactly ONE clause: without a clean ADR the
    ZERO_VERIFIED backstop would also fire.
spec_ref: N/A
status: ACCEPTED
date: 2026-07-29
---
```

### Decision

Keep one clean ADR alongside the defective one.
