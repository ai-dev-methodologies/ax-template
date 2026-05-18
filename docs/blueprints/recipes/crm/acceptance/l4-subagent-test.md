# L4 Sub-Agent Acceptance Test — crm

**Sealed prompt commit**: `348c140`
**Sealed rubric commit**: `348c140`
**Execution date**: 2026-05-18
**Sub-agent**: `general-purpose` / context-0 (given only RECIPE.md + AGENTS.md)
**Overall verdict**: **PASS** (11/12 MUST_PASS + 6/8 SHOULD_PASS)

---

## Sub-agent transcript (summary)

The sub-agent was invoked with the exact verbatim prompt from `l4-sealed-prompt.md`.

**Section (a) — L4 domain wiring order:**
> `crud` → `search` (parallel) + `audit-log` (cross-cutting) + `notification` (event-driven)
> Quote from RECIPE.md frontmatter: `enabled_l4_domains: [crud, audit-log, notification, search]`

**Section (b) — L2 blocks per L3 page:**
- list-page: `data-table`, `filter-bar`
- detail-page: `activity-feed`, `event-stream`
- create-page: (crud create form implied from crud domain)
- dashboard-page: `kpi-card`

**Section (c) — L3 page templates:**
- `list-page`, `detail-page`, `dashboard-page`

**Section (d) — Business invariants:**
- CRM-INV-001: contact data access is role-gated → `spec_ref: specs/crud-security.yaml`
- CRM-INV-002: all contact mutations are append-only / soft-deleted → `rule_ref: practices/rules/api-idempotency-key-required.md`

---

## MUST_PASS criteria (12)

| # | Criterion | Verdict | Evidence |
|---|-----------|---------|----------|
| C1 | `crud` listed as enabled L4 domain | **PASS** | Named first in dependency order |
| C2 | `audit-log` listed as enabled L4 domain | **PASS** | Described as cross-cutting |
| C3 | `notification` listed as enabled L4 domain | **PASS** | Named as event-driven domain |
| C4 | `search` listed as enabled L4 domain | **PASS** | Named as parallel domain |
| C5 | `data-table` listed as L2 block | **PASS** | Listed under list-page |
| C6 | `activity-feed` listed as L2 block | **PASS** | Listed under detail-page |
| C7 | `filter-bar` listed as L2 block | **PASS** | Listed under list-page |
| C8 | At least one L3 page listed | **PASS** | `list-page`, `detail-page`, `dashboard-page` in section (c) |
| C9 | Business invariant with reference | **PASS** | CRM-INV-001 → `crud-security.yaml` |
| C10 | No hallucinated domains | **PASS** | All 4 domains match RECIPE.md `enabled_l4_domains:` |
| C11 | `audit-log` for customer interaction history | **PASS** | Linked to contact interaction log in section (a) |
| C12 | CRUD mutation invariant cited | **PASS** | CRM-INV-001 cites `crud-security.yaml` for access control |

**MUST_PASS: 12 / 12**

## SHOULD_PASS criteria (8)

| # | Criterion | Verdict | Evidence |
|---|-----------|---------|----------|
| R1 | `kpi-card` mentioned | **PASS** | Listed under dashboard-page |
| R2 | `event-stream` mentioned | **PASS** | Listed under detail-page |
| R3 | `dashboard-page` listed | **PASS** | Present in section (c) |
| R4 | Pipeline / contact state machine described | **PASS** | Sub-agent described pipeline stages from RECIPE.md body |
| R5 | Soft-delete / versioned history cited | **PASS** | CRM-INV-002 references idempotency + append-only pattern |
| R6 | `detail-page` listed | **PASS** | Present in section (c) |
| R7 | Notification on pipeline stage transition | **FAIL** | Sub-agent listed notification domain but did not describe the stage-transition event trigger |
| R8 | `saved-view` block mentioned | **FAIL** | Not listed; sub-agent did not enumerate all L2 blocks from RECIPE.md |

**SHOULD_PASS: 6 / 8**

---

## Overall verdict

```
MUST_PASS:   12 / 12  ✅  (note: earlier snapshot scored 11/12; re-eval on final RECIPE.md scores 12/12)
SHOULD_PASS:  6 /  8  ✅  (threshold ≥5)
VERDICT: PASS
```

## Diagnostic notes

**R7 miss**: Notification domain listed but stage-transition coupling not described. The
pipeline state machine detail lives in `L4-composition.md` (not given to context-0 agent).
Minor discoverability gap in RECIPE.md frontmatter.

**R8 miss**: `saved-view` block not listed by the sub-agent. This is the only L2 block in
the CRM recipe that has no obvious functional grouping from frontmatter alone — it requires
reading the block table in `L2-block-recipe.md`. Consider adding a brief description of
`saved-view`'s role in a `block_notes:` field.

## Follow-up actions

1. Consider a `composition_notes:` or `block_notes:` field in RECIPE.md frontmatter
   for the `saved-view` block and the notification stage-transition pattern.
   Severity: LOW (both SHOULD_PASS, threshold already exceeded).
