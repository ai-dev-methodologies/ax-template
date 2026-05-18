# L4 Sub-Agent Acceptance Test — e-commerce

**Sealed prompt commit**: `348c140`
**Sealed rubric commit**: `348c140`
**Execution date**: 2026-05-18
**Sub-agent**: `general-purpose` / context-0 (given only RECIPE.md + AGENTS.md)
**Overall verdict**: **PASS** (12/12 MUST_PASS + 7/8 SHOULD_PASS)

---

## Sub-agent transcript (summary)

The sub-agent was invoked with the exact verbatim prompt from `l4-sealed-prompt.md`.

**Section (a) — L4 domain wiring order:**
> `crud` → `payment` → `notification` + `audit-log` (cross-cutting) + `search` (parallel)
> Quote from RECIPE.md frontmatter: `enabled_l4_domains: [crud, payment, notification, audit-log, search]`

**Section (b) — L2 blocks per L3 page:**
- list-page: `crud-list-adapter`, `data-table`, `filter-bar`, `faceted-filter`
- detail-page: `event-stream`, `kpi-card`
- create-page: `crud-create-form`
- edit-page: `crud-create-form` (edit mode)
- search-results-page: `data-table`, `filter-bar`, `faceted-filter`

**Section (c) — L3 page templates:**
- `list-page`, `detail-page`, `create-page`, `edit-page`, `search-results-page`

**Section (d) — Business invariants:**
- ECOM-INV-001: payment must be idempotent → `spec_ref: specs/payment-l0.yaml`
- ECOM-INV-002: order status transitions are append-only → `spec_ref: specs/audit-log-l0.yaml`

---

## MUST_PASS criteria (12)

| # | Criterion | Verdict | Evidence |
|---|-----------|---------|----------|
| C1 | `crud` listed as enabled L4 domain | **PASS** | Named first in dependency order |
| C2 | `payment` listed as enabled L4 domain | **PASS** | Named after crud in wiring chain |
| C3 | `notification` listed as enabled L4 domain | **PASS** | Named in section (a) |
| C4 | `audit-log` listed as enabled L4 domain | **PASS** | Described as cross-cutting |
| C5 | `search` listed as enabled L4 domain | **PASS** | Named as parallel domain |
| C6 | `data-table` listed as L2 block | **PASS** | Listed under list-page |
| C7 | `filter-bar` listed as L2 block | **PASS** | Listed under list-page |
| C8 | `crud-list-adapter` or `crud-create-form` listed | **PASS** | Both listed |
| C9 | At least one L3 page listed | **PASS** | All 5 pages in section (c) |
| C10 | Business invariant with reference | **PASS** | ECOM-INV-001 → `payment-l0.yaml` |
| C11 | No hallucinated domains | **PASS** | All 5 domains match RECIPE.md `enabled_l4_domains:` |
| C12 | Payment idempotency requirement | **PASS** | ECOM-INV-001 cites idempotency via `payment-l0.yaml` |

**MUST_PASS: 12 / 12**

## SHOULD_PASS criteria (8)

| # | Criterion | Verdict | Evidence |
|---|-----------|---------|----------|
| R1 | `faceted-filter` mentioned | **PASS** | Listed under list-page and search-results-page |
| R2 | `event-stream` mentioned | **PASS** | Listed under detail-page |
| R3 | `kpi-card` mentioned | **PASS** | Listed under detail-page |
| R4 | `crud` before `payment` in dependency order | **PASS** | crud → payment shown |
| R5 | `edit-page` and `search-results-page` both listed | **PASS** | Both in section (c) |
| R6 | `audit-log` as append-only / cross-cutting | **PASS** | Described as cross-cutting |
| R7 | Notification coupling to payment events | **FAIL** | Sub-agent listed notification in domain list but did not explicitly link it to payment state transitions |
| R8 | `detail-page` and `create-page` both listed | **PASS** | Both in section (c) |

**SHOULD_PASS: 7 / 8**

---

## Overall verdict

```
MUST_PASS:   12 / 12  ✅
SHOULD_PASS:  7 /  8  ✅  (threshold ≥5)
VERDICT: PASS
```

## Diagnostic notes

R7 miss: The sub-agent listed `notification` as an enabled domain but did not explicitly
describe the payment → notification event coupling in section (b). The RECIPE.md
`L4-composition.md` sibling document (not given to the agent) covers this coupling detail.
This is a minor discoverability gap — the RECIPE.md frontmatter alone is sufficient for
domain inventory but the event coupling narrative lives in the composition doc.

## Follow-up actions

Consider adding a brief `composition_notes:` field to RECIPE.md frontmatter for
the payment → notification coupling, so it is discoverable without opening
`L4-composition.md`. Severity: LOW (threshold already exceeded).
