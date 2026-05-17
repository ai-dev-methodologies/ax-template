# Frontend Templatization + Full-Stack Methodology Extension — PRD (CANONICAL, APPROVED)

> **Status:** APPROVED via `/ralplan` consensus loop (Planner → Architect → Codex Critic, 4 iterations).
> **Date:** 2026-05-17. **Repo:** `ax-template`. **Format:** RALPLAN-DR.
> **Sections A–E of the user brief are LOCKED constraints** (do not re-litigate).

## Consensus Loop Provenance

| Iter | Architect | Codex Critic | Notes |
|---|---|---|---|
| 1 | APPROVE WITH MANDATORY REVISIONS (1 FAIL, 4 WEAK, 8 remediations) | **REJECT** — 6 hard blockers, 7 surgical revisions | trio_integrity_guard schema not binary-implementable; 5 SP TDD anchors circular; autonomous /team safety contracts missing |
| 2 | likely-APPROVE (all 9 dimensions PASS) | **ITERATE** — schema rigidity blocks SP11 static `practices` viewer | 6 iter1 hard blockers all CLOSED; new defect: `practices` is static/non-API, schema forces fake-backend |
| 3 | (skipped per Critic's narrow-scope instruction) | **ITERATE** — item-level mutual-exclusion bypass | `frontend_only` route logic strict; page-spec item logic checked only when already null → fake-backend escape |
| **4** | (skipped, ultra-narrow patch) | **APPROVE** — patch verified clean | item algorithm now mirrors route; "Forbidden; guard MUST fail" prose; new `fail_frontend_only_item_non_null_operation/` fixture |

## ADR (final, Codex Critic-authored, iter4 APPROVE)

- **Decision:** Approve PRD iter4 as canonical. Frontend Templatization + Full-Stack Methodology Extension proceeds to autonomous `/team` execution.
- **Drivers:** Eliminate fake-backend pressure in `frontend_only` page-compliance specs; keep route and item validation symmetric; preserve binary guard semantics; binary-implementable verification; composition-kit portability; auth migration safety; avoiding governance loops.
- **Alternatives considered:**
  - Approve iter1/iter2/iter3 as-is — rejected (concrete bypasses identified)
  - Approve iter4 — chosen
  - Iterate to iter5 for broader re-review — rejected (patch is narrow, verified, does not touch previously closed surfaces)
  - Reject the rigorous Spec Trio schema (revert iter1) — rejected (would reopen original FAIL)
  - Add dummy backend OpenAPI operations for `practices` — rejected (encodes false provenance)
- **Why chosen:** iter4 fully closes the remaining concrete bypass with explicit algorithm text and a targeted failing fixture. All four-iteration cumulative findings closed at binary-implementable resolution. No previously closed blocker reopened.
- **Consequences:** Implementation teams (autonomous `/team` agents) proceed against this PRD as canonical. SP1 must NOT start before SP3 (real guards land first). SP9 must complete before SP10/SP11. `practices` domain is `frontend_only` — uses `static_source_ref`, no fake backend operations.
- **Follow-ups:** Commit canonical PRD + all iter artifacts; invoke `/team-builder` to compose execution team; invoke `/team` to dispatch parallel agents. Each SP applies `/tdd-workflow` (RED → GREEN → REFACTOR) + `/verification-loop` against its named Tier-1/2/3 verify skill. Halt thresholds (3 fails / 30 min idle / 5 rebases) trigger ESCAPE valve at `docs/superpowers/escape/`.

## Consensus Loop Artifacts (audit trail)

- `2026-05-17-frontend-templatization-prd.draft.md` — iter 1 (956 lines, REJECTED)
- `2026-05-17-frontend-templatization-architect-review.md` — Architect iter 1
- `2026-05-17-frontend-templatization-critic-codex-iter1.md` — Codex Critic iter 1
- `2026-05-17-frontend-templatization-prd.iter2.md` — iter 2 (1273 lines, ITERATE)
- `2026-05-17-frontend-templatization-architect-review-iter2.md` — Architect iter 2 (likely-APPROVE)
- `2026-05-17-frontend-templatization-critic-codex-iter2.md` — Codex Critic iter 2
- `2026-05-17-frontend-templatization-prd.iter3.md` — iter 3 (1412 lines, ITERATE)
- `2026-05-17-frontend-templatization-critic-codex-iter3.md` — Codex Critic iter 3
- `2026-05-17-frontend-templatization-prd.iter4.md` — iter 4 (1433 lines, APPROVED)
- `2026-05-17-frontend-templatization-critic-codex-iter4.md` — Codex Critic iter 4 (**APPROVE**)
- `2026-05-17-frontend-templatization-prd.md` — this file (canonical, APPROVED)

---

> The body below is the iter 4 content (1433 lines) including all 12+1 SPs, Verification Matrix, Autonomous Execution Safety contracts, pre-mortem, fixtures, and provenance enums.

---

---

## RALPLAN-DR Summary

### Principles (5) — UNCHANGED from iter1

1. **Composition kit, not single product.** Every artifact must be independently
   adoptable by a fork; rejecting one layer must not break the others.
2. **Spec-before-code, evidence-anchored.** Every template artifact (L1…L4 +
   frontend Spec Trio + skills) carries an `evidence:` block; every rule traces
   to a snapshot or external citation; every decision is captured as an ADR.
3. **Few exposed surfaces, dense feedback loops underneath.** 3 Tier-1 user
   commands. Tier-2/Tier-3 are skill files for pathPattern-triggered
   auto-invocation; raw `./gradlew`, `vitest`, `playwright`, guard `.sh` are
   bundled inside skills, not exposed as user surface.
4. **Java/Spring and React/Next.js are equal partners.** No archive, no
   deprecation, no frozen status. Catalog growth on either side is normal.
5. **Binary verification per axis.** Every SP terminates when a Tier-1/2/3
   skill returns `exit 0`. No SP is "done" on prose alone. No placeholder
   `exit 0` guard stubs — a guard either exists fully or does not exist at all.

### Decision Drivers (top 3) — UNCHANGED

1. AI agent self-discoverability (context-0 agent reaches green via AGENTS.md).
2. Migration safety (`./gradlew testAsvs` stays green throughout).
3. Catalog convergence cost (minimize re-cross-check loops).

### Viable Options (sequencing / granularity axis only)

- **Option A — Strict bottom-up.** Build infra → L1 → L2 → L3 → L4 strictly.
  Pros: each layer stable before next consumes it. Cons: late integration risk.
- **Option B — Vertical-slice first.** Ship auth end-to-end, then extract
  catalog. Pros: fast first vertical. Cons: N=1 over-fit.
- **Option C — Hybrid (RECOMMENDED).** Foundation → vertical-slice (auth) →
  horizontalize (crud / payment / practices) → integrate. Pros: matches
  Payment empirical cadence; bounded rework via L2 retro-edit budget. Cons:
  Phase 2 cannot start until Phase 1 lands.
- **Option D — Vite + Next.js coexistence.** *Steelman:* zero migration
  downtime; old Vite app stays bisectable; reduces "big-bang" risk for the
  auth flow. *Real merit:* recoverable rollback path during the migration
  week. **Status: CONSTRAINT-BLOCKED.** Section A of the user brief locks
  Next.js 16 App Router as **the** frontend stack (singular). The steelman's
  benefit is preserved by SP1's `git tag pre-nextjs-migration` rollback
  anchor (Scenario 2 mitigation, §7.2) rather than by coexistence. We
  acknowledge the lost benefit; we do not retain the option.

### Recommended: Option C (hybrid)

Rationale unchanged from iter1.

---

## 1. Context — UNCHANGED from iter1 §1

(See iter1 §1.1 / §1.2 / §1.3. No corrections required by the review loop.)

Key baseline figures restated for traceability:

- 7 backend domains (`auth`, `crud`, `payment`, `practices`, `ratelimit`, `security`, `user`); 4 L4-eligible.
- `practices/` 68 rules, 21 categories.
- `practices-react/` 68 rules, 8 families, 7 ESLint plugin rules.
- Frontend: Vite 6 + React 19 + react-router-dom v7. Only auth wired.
- Skills today: `skills/ax-transform/SKILL.md`, `practices/SKILL.md`, `practices-react/SKILL.md`.

---

## 2. Work Objectives — UNCHANGED (O1–O8)

(See iter1 §2.)

Note on O8: the binary gate is `bash skills/ax-verify/scripts/run-all.sh`
(the new Tier-1 verify skill). Substituted prose for swap-in clarity.

---

## 3. Guardrails — UPDATED

### 3.1 Must Have

- One-stack frontend: Next.js 16 App Router only. No coexistence period.
- Every artifact under `templates/**` has `evidence:` frontmatter.
- Every architectural choice has an ADR in `templates/DECISIONS.md` of the
  form **TD-2026-05-17-NNN**, **and each ADR declares a `provenance_class`**
  (see §4.12 below).
- 3-tier skill topology has ≤ 3 user-facing commands (Tier-1 only).
- Each Tier-2/Tier-3 skill bundles its scripts.
- `practices-react/` and `practices/` catalogs stay green throughout.
- Backend remains untouched except for additive cross-cutting per S7.
- RestAssured for backend HTTP tests; Playwright + Vitest + MSW for frontend.
  No MockMvc.
- TDD anchor per SP: a failing test (RED) must exist before any
  implementation file is touched in that SP.
- **NEW (revision #2): Every SP emits at least one production-style
  observability signal** (event name + assertion) in addition to test exits.

### 3.2 Must NOT

- No governance docs (CLAUDE.md anti-pattern).
- No Vite coexistence.
- No new top-level Tier-1 skill beyond `/ax-transform`, `/ax-verify`, `/ax-scaffold`.
- No domain-specific subdirectory under `practices/rules/` or `practices-react/rules/`.
- No mocking the backend in integration tests.
- No design tokens hardcoded in components.
- No skill wrapping another skill without adding a workflow checklist.
- **NEW (hard blocker fix): No placeholder `exit 0` guard stubs.** A guard
  file either ships its real binary implementation in the SP that creates it
  or is not created in that SP. Iter1's plan to land 2 placeholder `.sh`
  files in SP1 and fill them in SP3 is forbidden.
- **NEW: No quarterly review / governance loop.** Mitigations are binary
  SP4/SP12 checks (revision #7).

---

## 4. Component Inventory (complete enumeration)

### 4.1 Frontend — L1 UI Primitives (shadcn/ui adoption)

UNCHANGED from iter1 §4.1 in terms of blessed list (32 components), token
manifest, and wrapper exports.

**ADDITION (soft-suggestion #2 from Critic):** SP3 ships
`practices-react/upstream/shadcn-registry-2026-05.snapshot.md` (frozen
copy of the 32 components at install date) plus
`templates/L1/_check-shadcn-drift.sh` which diffs current files against the
snapshot. `time_decay_guard.sh` walks this snapshot; drift > 90 days
flags FAIL.

### 4.2 Frontend — L2 Feature Blocks — UNCHANGED counts

Auth (6), CRUD generic (8), Payment (5), Practices viewer (3),
Cross-cutting (4). Total ~26. Membership ambiguities resolved in §4.11
Layer Membership Decision Table.

### 4.3 Frontend — L3 Page Templates — UNCHANGED

7 families. Slot contract README per family.

### 4.4 Frontend — L4 Domain Workloads — UNCHANGED

4 domains: auth, crud, payment, practices. 1:1 with backend Spec Trio
where the backend domain has UI. `ratelimit` / `security` / `user`
are backend-only; they carry `frontend_required: false` markers in their
backend spec (see §4.8).

### 4.5 Backend cross-cutting templates — UNCHANGED count (10)

`templates/backend/controllers/`, `services/`, `repositories/`, `dto/`,
`error/`, `security/` (2 files), `config/` (3 files).

**NEW (revision #3 + Architect (a)):** assigned to verify ownership and
guard coverage — see §4.11.

### 4.6 New backend rules expected — UNCHANGED (cap 4)

### 4.7 New React rules expected — UNCHANGED (cap 10), but capped further
in §5 SP7 to **"implementation-proven needs only"** (soft suggestion #5).
S7 audit may classify any of the 10 as `extend_existing`.

### 4.8 Frontend Spec Trio schema artifacts — REWRITTEN

This section is the single largest revision. Iter1's schemas were too thin
to anchor `trio_integrity_guard`. The schemas below are binary-implementable.

#### 4.8.1 Page Compliance Spec — `specs/<domain>-frontend-l0.yaml`

Required top-level fields:

```yaml
domain: <string>                          # e.g., "auth"
backend_spec_ref: <string|null>           # e.g., "specs/auth-asvs-l1.yaml"; null when frontend_only
frontend_required: true                   # marker; if false, no UI section is generated
items:
  - id: <DOMAIN-FE-NNN>                   # required, unique
    requirement: <string>                 # required, ≥ 20 chars
    backend_operation_id: <string|null>   # nullable. null ONLY when the page is
                                          # non-API-bound (frontend_only domain mode).
                                          # When null, the item MUST declare a non-empty
                                          # static_source_ref list.
    backend_spec_ref: <DOMAIN-NNN|null>   # required key; links to backend item; null when frontend_only
    static_source_ref: array<string>      # required when backend_operation_id is null.
                                          # Each entry MUST resolve to ≥ 1 existing
                                          # file in the repo (literal path OR glob
                                          # using shell-style "*"/"**" expansion).
                                          # Forbidden; guard MUST fail with exit code
                                          # non-zero and a distinct message when
                                          # backend_operation_id is non-null. This is
                                          # BINARY — there is no soft mode.
    test_method: <playwright_test_name>   # required
    verification_type: <e2e_test|unit_test|a11y_test|cwv_test>  # required, enum-constrained
    policy_ref: <blueprints/...-ui-manifest.yaml#item>  # required
    coverage_threshold: <decimal>         # required, 0.0–1.0 — used in 4.8.4
    backend_only_marker: false            # required boolean; if true, item is exempt from cross-trio
```

**Validation note (iter3):** the `backend_operation_id: null` + `static_source_ref`
substitution is the schema-level admission of the `frontend_only` domain mode
introduced in §4.8.4. Schema rigidity is preserved (one of the two fields MUST
carry provenance; both null is BLOCKED).

#### 4.8.2 UI Contract — `contracts/<domain>-ui.yaml`

Required top-level fields:

```yaml
domain: <string>
backend_contract_ref: <string|null>       # e.g., "contracts/auth-openapi.yaml";
                                          # null ONLY when the entire domain is
                                          # frontend_only (no backend OpenAPI binding).
routes:
  - path: <string>                        # e.g., "/login" or "/practices/<ruleId>"
    method: <GET|POST|PUT|DELETE>
    backend_operation_id: <string|null>   # nullable. null ONLY when accompanied by
                                          # a non-empty static_source_ref list
                                          # (frontend_only domain mode). Non-null
                                          # values MUST match an operationId in
                                          # backend_contract_ref.
    static_source_ref: array<string>      # required when backend_operation_id is null.
                                          # Each entry MUST resolve to ≥ 1 existing
                                          # file (literal path OR shell glob).
                                          # Forbidden; guard MUST fail with exit code
                                          # non-zero and a distinct message when
                                          # backend_operation_id is non-null. This is
                                          # BINARY — there is no soft mode.
    params: { ... }
    query: { ... }
    states:
      loading: <slot_ref>
      error: <slot_ref>
      empty: <slot_ref|null>
    redirects:
      on_auth_required: <route_path>
      on_success: <route_path|null>
```

Validation:

- When `backend_operation_id` is non-null: a `swagger-cli`-style resolver walks
  `backend_contract_ref` and asserts the operationId exists in the resolved
  OpenAPI document.
- When `backend_operation_id` is null: `static_source_ref` MUST be non-empty
  AND every entry MUST resolve to ≥ 1 existing file (see §4.8.4 algorithm).
- Schema-level mutual exclusion: a route with BOTH null and empty
  `static_source_ref` FAILS the guard. A route with non-null `backend_operation_id`
  MUST NOT carry any `static_source_ref` entry; if present, guard MUST fail with
  exit code non-zero and a distinct message. This is BINARY — there is no soft mode.

#### 4.8.3 UI Policy Manifest — `blueprints/<domain>-ui-manifest.yaml`

Required top-level fields:

```yaml
domain: <string>
tokens_override: { ... }                  # design-token overrides
a11y:
  axe_rules: [<rule_id>, ...]             # required, ≥ 1 entry
  contrast_min: <decimal>                 # required, ≥ 4.5
cwv:
  lcp_ms: <int>                           # required, ≤ 2500
  inp_ms: <int>                           # required, ≤ 200
  cls: <decimal>                          # required, ≤ 0.1
motion:
  respect_prefers_reduced_motion: true    # required, must be true
  default_duration_ms: <int>
```

#### 4.8.4 `trio_integrity_guard.sh` — binary implementation contract

**Inputs:** `specs/`, `contracts/`, `blueprints/`, backend OpenAPI docs.

**Algorithm (deterministic, domain-mode branching — iter3):**

1. Load `practices/evals/trio_integrity_allowlist.yaml`. For each entry
   `<domain>: <mode>` where `<mode>` ∈ {`full_trio`, `backend_only`,
   `frontend_only`}, resolve `domain_mode` for that domain.
2. **If `domain_mode == backend_only`:**
   - Require backend spec (`specs/<domain>-*.yaml`) to exist; FAIL exit 1
     with `MISSING_BACKEND_SPEC` if absent.
   - Skip the frontend Spec Trio check entirely (no UI is in scope).
   - Continue to next domain.
3. **If `domain_mode == full_trio`:** (existing iter2 algorithm)
   1. Read backend Spec Trio file's `frontend_required` field. If
      `frontend_required: false`, log inconsistency with allowlist and
      FAIL exit 1 `MODE_MISMATCH`.
   2. REQUIRE corresponding files:
      - `specs/<domain>-frontend-l0.yaml`
      - `contracts/<domain>-ui.yaml`
      - `blueprints/<domain>-ui-manifest.yaml`
      FAIL exit 1 if any is missing.
   3. For each route in `contracts/<domain>-ui.yaml`, `backend_operation_id`
      MUST be non-null AND MUST resolve against `backend_contract_ref`
      OpenAPI document. FAIL exit 1 if null or unresolved.
   4. For each item in `specs/<domain>-frontend-l0.yaml`, verify
      `backend_spec_ref` resolves to an item id in the backend spec.
      FAIL exit 1 if unresolved.
   5. **Coverage check:** count backend items where `frontend_required:
      true`. For each, confirm at least one frontend item carries the
      matching `backend_spec_ref`. Required ratio: **100%**. FAIL below.
4. **If `domain_mode == frontend_only` (NEW iter3):**
   1. REQUIRE frontend Spec Trio files:
      - `specs/<domain>-frontend-l0.yaml`
      - `contracts/<domain>-ui.yaml`
      - `blueprints/<domain>-ui-manifest.yaml`
      FAIL exit 1 if any is missing.
   2. **Skip** backend_operation_id resolution entirely (no
      `backend_contract_ref` OpenAPI binding expected).
   3. For each route in `contracts/<domain>-ui.yaml`:
      - Verify `backend_operation_id` is null. If non-null, FAIL exit 1
        with `frontend_only route has non-null backend_operation_id`.
      - Verify `static_source_ref` is non-empty. If empty/missing, FAIL
        exit 1 with `frontend_only route missing static_source_ref`.
      - For each entry in `static_source_ref`, expand as a shell-style
        glob (supports `*` and `**`) rooted at the repo top. If the
        expansion resolves to **zero files**, FAIL exit 1 with
        `static_source_ref resolves to zero files: <entry>`.
   4. For each item in `specs/<domain>-frontend-l0.yaml` (mirror the
      route check exactly — no "applies only to null" exception):
      - Verify `backend_operation_id` is null. If non-null, FAIL exit 1
        with `frontend_only item has non-null backend_operation_id: <id>`.
      - Verify `static_source_ref` is a non-empty array. If empty/missing,
        FAIL exit 1 with `frontend_only item missing static_source_ref`.
      - For each entry in `static_source_ref`, expand as a shell-style
        glob (supports `*` and `**`) rooted at the repo top. If the
        expansion resolves to **zero files**, FAIL exit 1 with
        `static_source_ref resolves to zero files: <entry>`.
      - Otherwise PASS for this item.
5. **Zero-scan guard (all modes):** if the script walked **zero** files
   (no domain matched the allowlist OR every domain block was skipped
   without scanning any file), FAIL exit 1 with message `ZERO_SCAN`.
   This closes iter1 Scenario 1 and remains binding in iter3.

**Domain allowlist (configurable, defaults shipped):**

Three mutually exclusive domain modes are admitted (iter3 adds `frontend_only`):

- `full_trio` — backend Spec Trio AND frontend Spec Trio both required.
  Every UI route MUST carry a non-null `backend_operation_id` resolving
  against `backend_contract_ref`. Applies to `auth`, `crud`, `payment`.
- `backend_only` — backend Spec Trio only (no UI in scope). Frontend
  Spec Trio check is skipped entirely. Applies to `ratelimit`, `security`,
  `user`.
- `frontend_only` (NEW iter3) — frontend Spec Trio only; the domain has
  NO backend OpenAPI to bind to. `backend_contract_ref` MAY be null;
  every UI route MUST set `backend_operation_id: null` AND declare a
  non-empty `static_source_ref` pointing to existing files. Applies to
  `practices` (a static viewer reading `practices/AGENTS.md` and
  `practices-react/AGENTS.md`).

```yaml
# practices/evals/trio_integrity_allowlist.yaml
schema_version: 2
domains:
  auth: full_trio
  crud: full_trio
  payment: full_trio
  practices: frontend_only      # NEW iter3 — static viewer, no backend OpenAPI
  ratelimit: backend_only
  security: backend_only
  user: backend_only
```

New domains added by either (a) updating this file with the appropriate
mode or (b) carrying a `frontend_required: false` marker in the new backend
spec (auto-classifies as `backend_only`). Domains without a backend spec
at all (e.g., pure static viewers) MUST be explicitly enumerated under
`frontend_only` — the guard refuses to infer this mode.

**Required fixtures (live at `practices/evals/fixtures/trio_integrity/`):**

`full_trio` mode (iter2 — unchanged):

- `pass/` — full happy-path: auth Spec Trio complete; operation IDs resolve;
  ratelimit carries `frontend_required: false`. Expected: exit 0.
- `fail_missing_frontend_yaml/` — auth backend spec exists with
  `frontend_required: true` but `specs/auth-frontend-l0.yaml` is absent.
  Expected: exit 1, message `MISSING_FRONTEND_SPEC`.
- `fail_unresolved_operation_id/` — UI Contract references
  `backend_operation_id: bogusOp` not in OpenAPI doc. Expected: exit 1.
- `fail_coverage_shortfall/` — backend spec has 5 items requiring frontend;
  frontend yaml has 3. Expected: exit 1, message `COVERAGE_SHORTFALL: 3/5`.
- `fail_zero_scan/` — allowlist points to non-existent domain. Expected:
  exit 1, message `ZERO_SCAN`.

`frontend_only` mode (iter3 — 3 fixtures; iter4 adds a 4th):

- `pass_frontend_only_practices/` — `practices` domain wired as
  `frontend_only`. `specs/practices-frontend-l0.yaml`,
  `contracts/practices-ui.yaml`, `blueprints/practices-ui-manifest.yaml`
  present. Every UI route has `backend_operation_id: null` AND a
  non-empty `static_source_ref` pointing at real files (e.g.,
  `practices/AGENTS.md`, `practices/rules/**/*.md`,
  `practices-react/AGENTS.md`). Expected: exit 0.
- `fail_frontend_only_missing_source_ref/` — `frontend_only` route has
  `backend_operation_id: null` but `static_source_ref` is empty/missing.
  Expected: exit 1, message
  `frontend_only route missing static_source_ref`.
- `fail_frontend_only_unreachable_route/` — `static_source_ref` lists a
  path/glob (e.g., `practices/rules/does-not-exist-*.md`) whose
  expansion resolves to zero files in the repo. Expected: exit 1,
  message `static_source_ref resolves to zero files`.
- `fail_frontend_only_item_non_null_operation/` (NEW iter4) — a
  `frontend_only` domain's `specs/<domain>-frontend-l0.yaml` has one
  page-compliance item with a non-null `backend_operation_id` (e.g.,
  `fakeReadPracticeRule`, not present in any OpenAPI doc) and no
  `static_source_ref`. Catches the item-level bypass that iter3 left
  open (item check previously only applied when `backend_operation_id`
  was null). Expected: exit 1, message
  `frontend_only item has non-null backend_operation_id: <id>`.

#### 4.8.5 `cross_trio_guard.sh` — binary implementation contract

**Inputs:** `templates/L4/<domain>/`, `templates/L1/`, `templates/L2/`,
`templates/L3/`.

**Algorithm:**

1. For each L4 domain directory under `templates/L4/`:
   1. Static-parse all `.tsx` imports.
   2. For each import resolving to `templates/L{1,2,3}/`, record the
      imported file path.
2. For each imported L1/L2/L3 file, verify it carries an `evidence:` block
   (delegated to `evidence_guard.sh`).
3. FAIL with exit 1 if any imported file lacks evidence.
4. **Domain allowlist:** same as 4.8.4. Backend-only domains are exempt
   (no `templates/L4/<domain>/` expected).
5. **Zero-scan guard:** if no L4 domain directory was walked, FAIL exit 1
   message `ZERO_SCAN`.

**Required fixtures (live at `practices/evals/fixtures/cross_trio/`):**

- `pass/` — minimal L4 auth importing only evidence-anchored L1/L2/L3.
  Expected: exit 0.
- `fail_orphan_l2_import/` — L4 auth imports `templates/L2/blocks/Foo.tsx`
  which carries no `evidence:` block. Expected: exit 1, message
  `ORPHAN_EVIDENCE: templates/L2/blocks/Foo.tsx`.
- `fail_zero_scan/` — `templates/L4/` empty. Expected: exit 1 `ZERO_SCAN`.

### 4.9 DECISIONS.md and ADRs — UNCHANGED count + provenance_class

The 10 ADRs (TD-2026-05-17-001..010) listed in iter1 §4.9 are unchanged in
title. Each ADR now declares a `provenance_class` per §4.12 below.

### 4.10 Guard extensions — REWRITTEN

The 4 existing guards (`evidence_guard.sh`, `spec_ref_guard.sh`,
`substance_guard.sh`, `time_decay_guard.sh`) extend their walk targets to
include:

- `templates/L1/**/*.{md,tsx,ts,yaml}`
- `templates/L2/**/*.{md,tsx,ts,yaml}`
- `templates/L3/**/*.{md,tsx,ts,yaml,md}`
- `templates/L4/**/*.{md,tsx,ts,yaml,md}`
- `templates/backend/**/*.{java,yaml,md}` (NEW — closes Architect (b))
- `templates/DECISIONS.md` (NEW — see §4.12 for ADR `evidence:` schema)

Each guard adds a **zero-scan guard**: if a guard's walk produces zero
files matching its filter, the guard FAILs with exit 1 and message
`ZERO_SCAN`. Iter1's `evidence_guard.sh` could pass with no rule files
scanned; that hole is now closed.

The 2 new guards (`trio_integrity_guard.sh`, `cross_trio_guard.sh`) are
fully specified in §4.8.

### 4.11 Layer Membership Decision Table — NEW (revision #3)

For each ambiguous artifact, this table is authoritative.

| Artifact | Layer | Allowed imports | Forbidden imports | Owning Verify Skill | Example resolution |
|---|---|---|---|---|---|
| `ProtectedRoute` | **L2** (`templates/L2/blocks/ProtectedRoute.tsx`) | L1 components, `lib/auth/session`, `next/navigation` | L4 domain code, other L2 blocks | `/ax-verify-L2` | Reusable across **any** authenticated domain (auth / crud / payment / practices). Component is parametric on redirect target; not auth-specific. Has `.spec.test.tsx` testing the redirect contract in isolation. |
| `PaymentCheckoutForm` | **L2** (`templates/L2/blocks/PaymentCheckoutForm.tsx`) | L1 components, `lib/payment/zod-schemas`, `lib/payment/idempotency` | L4 routing, server actions, direct backend URL strings | `/ax-verify-L2` | Block accepts an `onSubmit: (data) => Promise<Result>` prop. Backend coupling lives in L4 (`templates/L4/payment/app/payment/checkout/page.tsx`) which wires the Server Action. Block is reusable across payment-like flows (subscriptions, refunds). |
| `AppHeader` | **L2** cross-cutting (`templates/L2/blocks/AppHeader.tsx`) | L1 components, `lib/theme`, `next/link` | Domain-specific imports (no `lib/auth/<provider>` etc.) | `/ax-verify-L2` | Iter1 §3.a Architect flagged this as L1/L2 ambiguous. Resolution: L2, because it requires a navigation slot contract (different domains supply different nav entries via props). Pure L1 chrome (icon, brand mark) sits inside the L2 block. |
| `templates/backend/**` (10 Java files) | **Backend-cross-cutting** (not L1–L4; new top-level peer to L1–L4) | Spring Boot package imports per existing `practices/rules/` | Frontend imports | `/ax-verify-java` (NEW: this skill's `pathPatterns` now includes `templates/backend/**`) | A `BaseController.java` template is verified by reading its `evidence:` block (anchored to `practices/rules/api-response-envelope-rfc7807-mandatory.md`) and by `evidence_guard.sh` walking the path per §4.10. No compile required — these are extraction patterns, not compiled units. |
| `templates/DECISIONS.md` | **Decision Provenance Trail** (top-level under `templates/`) | n/a (Markdown) | n/a | `/ax-guard-evidence` (walks the file per §4.10) + `/ax-guard-spec-ref` (walks ADR `spec_ref` fields) | Each ADR is required to carry a frontmatter `evidence:` block (see §4.12 schema). The Markdown body is the ADR text; the YAML frontmatter is the guard target. |

**Consequence for SP7 and SP8 leads:** if an SP author proposes a component
not listed here, they must extend this table in their PR (single-source-of-
truth). The Architect's worked-example principle is preserved without
inventing a 5th layer.

### 4.12 ADR provenance_class — NEW (revision #7)

Every ADR in `templates/DECISIONS.md` declares a `provenance_class` enum
in its frontmatter:

```yaml
---
adr_id: TD-2026-05-17-NNN
provenance_class: external_canonical | internal_design | empirical | locked_constraint
evidence:
  source_type: <upstream|external|internal|empirical>
  source_ref: <path|url|commit|fixture>
  quote: <required for upstream/external; ≥ 20 chars>
spec_ref: <specs/...|N/A>
---
```

**Enum semantics:**

- `external_canonical` — ADR cites an external standard (OWASP, RFC, JEP,
  WCAG, etc.). Requires `quote` field. **Example:** TD-2026-05-17-002
  (shadcn) cites `practices-react/upstream/shadcn-ui-2026-05.snapshot.md`.
- `internal_design` — ADR documents an internal design decision with no
  external anchor available (because the decision is about *our own*
  topology). **Required:** `quote` is replaced by `rationale` (≥ 60 chars)
  explaining why no external evidence applies. **Example:**
  TD-2026-05-17-008 (3-tier skill topology), TD-2026-05-17-009
  (`templates/` directory shape).
- `empirical` — ADR cites the Payment domain L4 sealed sub-agent verdict
  or analogous empirical result inside this repo. Requires `source_ref` to
  a verification log path. **Example (future):** post-SP12 ADR citing the
  fork-receiver smoke test result.
- `locked_constraint` — ADR records a Section A–E pre-aligned decision
  from the user brief. Cites `source_ref: docs/superpowers/specs/<brief>`.
  **Example:** Section A locks Next.js 16 → TD-2026-05-17-001.

**Guard enforcement:** `evidence_guard.sh` (extended per §4.10) reads each
ADR's frontmatter, validates the `provenance_class` value, and requires
either `quote` (for external_canonical/empirical) or `rationale` (for
internal_design/locked_constraint). FAIL exit 1 if missing.

### 4.13 Upstream snapshot additions — UNCHANGED + shadcn drift snapshot

- `nextjs-app-router-16.snapshot.md`
- `nextjs-server-actions-16.snapshot.md`
- `nextjs-use-cache-16.snapshot.md`
- `shadcn-ui-2026-05.snapshot.md` (existing) **+ NEW**
  `shadcn-registry-2026-05.snapshot.md` (frozen 32-component file index for
  drift detection per §4.1)
- `tanstack-query-v5.snapshot.md`
- `wcag-2-2.snapshot.md`
- `cwv-2026.snapshot.md`

All snapshots enter `practices-react/upstream/_MANIFEST.yaml`.

### 4.14 Skills inventory — UNCHANGED count (17), refined pathPatterns

| Tier | Count | Skills |
|---|---|---|
| Tier-1 (exposed) | 3 | `/ax-transform`, `/ax-verify`, `/ax-scaffold` |
| Tier-2 (axes) | 8 | `/ax-verify-{java,react,shared,L1,L2,L3,L4,domain}` |
| Tier-3 (leaf guards) | 6 | `/ax-guard-{evidence,substance,time-decay,spec-ref,trio-integrity,cross-trio}` |

**PathPattern disambiguation (Architect (a) gap):**

| Skill | pathPatterns |
|---|---|
| `/ax-verify-L1` | `templates/L1/**` |
| `/ax-verify-L2` | `templates/L2/**` |
| `/ax-verify-L3` | `templates/L3/**` |
| `/ax-verify-L4` | `templates/L4/**` |
| `/ax-verify-java` | `backend/**`, `templates/backend/**` |
| `/ax-verify-react` | `frontend/**`, `practices-react/rules/**` |
| `/ax-verify-shared` | `specs/**`, `contracts/**`, `blueprints/**`, `templates/DECISIONS.md`, `templates/AGENTS.md` |
| `/ax-verify-domain` | `templates/L4/<domain>/**` (invoked with explicit domain arg) |
| Tier-3 guards | n/a (invoked by Tier-2 only, not pathPattern-triggered) |

Tier-3 guards are NOT pathPattern-triggered; they are invoked by Tier-2.
This eliminates iter1's pathPattern overlap risk (Architect (a) gap).

### 4.15 Methodology document changes — UNCHANGED

(See iter1 §4.13.)

---

## 5. Implementation Plan (Sub-Projects)

### 5.0 SP dependency graph — REVISED

```
SP3 ──┬──▶ SP1 ──▶ SP2 ──▶ SP4a ──▶ SP4b (Phase 1 fan-in)
      │                                  │
      └─────── (guards land first) ──────┘
                                         │
                                         ▼
                                        SP5 ──▶ SP5.5 (fork-receiver smoke) ──▶ SP6 ──▶ SP7 ──▶ SP8 (Phase 2 vertical slice)
                                                                                                  │
                                                                                                  ▼
                                                                                                 SP9 (serial, payment depends on crud patterns)
                                                                                                  │
                                                                          ┌───────────────────────┼─────────────────┐
                                                                          ▼                       ▼                 ▼
                                                                         SP10 (parallel)         SP11 (parallel)   [N/A]
                                                                          │                       │
                                                                          └───────────────────────┘
                                                                                                  │
                                                                                                  ▼
                                                                                                 SP12 (integration)
```

**Critic-driven changes:**

- **SP3 lands first.** Iter1 had SP1 ship placeholder `exit 0` guard stubs
  to be filled by SP3. That is now forbidden. SP3 (Decision Provenance
  Trail + 6 guards including the 2 new ones) ships fully-implemented
  guards before SP1 starts.
- **SP4 split** into SP4a (Tier-1 skills) and SP4b (Tier-2 + Tier-3
  skills) per Architect's recommendation. Different blast radius.
- **SP5.5 NEW (criterion L + soft suggestion #4):** fork-receiver smoke
  test moved from SP12 to immediately after SP5 to catch non-portable L1
  templates before 7 more SPs of in-repo coupling.
- **SP9 serializes before SP10/SP11** per revision #5. Payment depends on
  CRUD-list / data-table patterns that SP9 surfaces.

### Phase 0 — Pre-foundation (NEW: SP3 lands first)

#### SP3 — Decision Provenance Trail infra + guard extensions (formerly Phase 1)

- **Inputs:** repo HEAD; no SP1/SP2 deliverables required.
- **Deliverables:**
  - `templates/` top-level directory created with `L1/`, `L2/`, `L3/`,
    `L4/`, `backend/` subdirs (empty placeholders, no `.tsx` files yet).
  - `templates/DECISIONS.md` with TD-2026-05-17-001..010 ADRs filed, each
    declaring `provenance_class` per §4.12.
  - `templates/generate_agents.sh` — produces `templates/AGENTS.md` with
    sha256 sentinel.
  - 4 existing guards extended to walk `templates/**` per §4.10 (with
    zero-scan guard).
  - 2 new guards (`trio_integrity_guard.sh`, `cross_trio_guard.sh`) fully
    implemented per §4.8.4 and §4.8.5 (binary, with fixtures).
  - `practices/evals/fixtures/trio_integrity/` and
    `practices/evals/fixtures/cross_trio/` populated per §4.8.
  - 7 new upstream snapshots added under `practices-react/upstream/`.
  - `practices/evals/trio_integrity_allowlist.yaml`.
- **Acceptance:** all 6 guards exit 0 on the `pass/` fixtures and exit 1
  on every `fail_*/` fixture; `templates/AGENTS.md` sentinel matches its
  regenerator's output; `time_decay_guard.sh` passes on the 7 new
  snapshots.
- **Verify command:** `bash practices/evals/run-all-guards.sh
  --include-fixtures` exits 0 (a new orchestrator script).
- **Agent count:** 1 lead + 1 worker.
- **TDD anchor:** the failing-fixture files (`fail_zero_scan/`,
  `fail_coverage_shortfall/`, etc.) **predate** the guard implementation;
  each guard is written to make its `pass/` fixture green and its
  `fail_*/` fixtures red. RED-first by construction.
- **Risks + mitigation:**
  - **R:** Zero-scan guard fires on pristine clones where no fixtures
    exist. **M:** fixtures are committed into `practices/evals/fixtures/`
    and walked unconditionally.
  - **R:** sha256 churn. **M (per iter1):** sentinel only regenerated on
    `rules/` or `templates/` changes.

### Phase 1 — Foundation (SP1 → SP4b)

#### SP1 — Vite → Next.js 16 migration

- **Inputs:** SP3 done (guards exist for any new `templates/` content
  SP1 lands; auth pages move to `frontend/app/`, not `templates/`).
- **Deliverables:**
  - `frontend/` migrated to Next.js 16 App Router.
  - `frontend/package.json` updated (drop vite/react-router; add next,
    @tanstack/react-query, zod, shadcn deps; keep zustand, vitest, msw,
    @playwright/test).
  - Existing auth pages ported to `app/(auth)/…/page.tsx`.
- **Removed from iter1 SP1:** no skill stubs landed here (moved to
  SP4a/SP4b); no placeholder `.sh` files (replaced by SP3 real guards).
- **Acceptance:** `cd frontend && npm run build` exits 0; `npm run test`
  exits 0; `./gradlew testAsvs` stays green; Playwright smoke green.
- **Verify command:** `/ax-verify-react` (note: this Tier-2 skill file
  doesn't exist yet — until SP4b, the verification is direct `npm run
  build && npm run test && playwright test`). SP1's acceptance does NOT
  depend on the skill file existing; it depends on the underlying
  commands. The skill file is a wrapper added in SP4b.
- **Agent count:** 1 lead + 2 workers.
- **TDD anchor:** `frontend/tests/auth/login-flow.spec.ts` (Playwright) —
  expects the same login round-trip as the Vite version. The Vite version
  exists; the test asserts the new Next.js shape produces the same
  HTTP-level effects. **Predates SP1 implementation: the test is written
  against the still-running Vite app first (green), then the Vite removal
  turns it red, then the Next.js port turns it green.**
- **Risks + mitigation:** UNCHANGED from iter1.

#### SP2 — Frontend Spec Trio schema + METHODOLOGY.md extension

- **Inputs:** SP3 done; SP1 done.
- **Deliverables:**
  - `specs/templates/page-compliance-spec.schema.yaml` per §4.8.1.
  - `contracts/templates/ui-contract.schema.yaml` per §4.8.2.
  - `blueprints/templates/ui-manifest.schema.yaml` per §4.8.3.
  - `specs/auth-frontend-l0.yaml` (first concrete instance, ≥ 10 items,
    each with `backend_operation_id` + `backend_spec_ref` populated).
  - `contracts/auth-ui.yaml` (all 14 routes carry `backend_operation_id`).
  - `blueprints/auth-ui-manifest.yaml`.
  - `METHODOLOGY.md` Appendix A.3 added; Appendix B amended; Appendix C
    Recipe B promoted.
- **Acceptance:** `bash practices/evals/trio_integrity_guard.sh` exits 0
  on auth (the only domain that has a frontend trio by end of SP2);
  exits 1 on a deliberately-broken fixture in
  `frontend/tests/_fixtures/spec-trio-coverage-fail/` (the TDD anchor).
- **Verify command:** `/ax-verify-shared` (after SP4b) or
  `bash practices/evals/trio_integrity_guard.sh` (direct).
- **Agent count:** 1 lead + 1 worker.
- **TDD anchor:** `frontend/tests/_fixtures/spec-trio-coverage-fail/`
  fixture is created **first**, containing a deliberately-incomplete
  frontend spec (missing backend_operation_id on one route). The test
  runs `trio_integrity_guard.sh` against this fixture; assertion is
  `exit 1 && stderr contains "MISSING_OPERATION_ID"`. RED before SP2
  fixes the real auth-ui.yaml. **The assertion target (the guard) was
  implemented in SP3, so SP2's test has a real script to call.**
- **Risks:** schema bloat — cap meta-schema at ≤ 200 lines per file
  (UNCHANGED).

#### SP4a — Tier-1 skill bodies (3 skills)

- **Inputs:** SP1 + SP3 done.
- **Deliverables:**
  - `skills/ax-transform/SKILL.md` extended (full-stack-aware).
  - `skills/ax-verify/SKILL.md` NEW with `scripts/run-all.sh` that
    chains the 6 guards + `./gradlew test` + `npm run test` +
    `playwright test`.
  - `skills/ax-scaffold/SKILL.md` NEW with `scripts/new-domain.sh
    <domain>` that scaffolds an L4 skeleton per Appendix C.
- **Acceptance:** `/ax-verify` exits 0 on the current repo state
  (post-SP3, post-SP1, post-SP2); `/ax-scaffold dummy-domain --dry-run`
  prints the file plan without writing.
- **Verify command:** `/ax-verify` (recursive).
- **Agent count:** 1 lead + 1 worker.
- **TDD anchor:** `skills/_tests/tier1-topology.test.sh` — asserts
  exactly 3 SKILL.md files exist under `skills/ax-*` whose paths match
  `^skills/ax-(transform|verify|scaffold)/SKILL\.md$`, asserts each
  frontmatter contains a `name:` and `description:` field. This test
  is written **before** the SKILL.md files are populated; pre-SP4a the
  count is 1 (just `/ax-transform`) so the test fails; post-SP4a the
  count is 3.
- **Risks:** workflow checklists drift from actual steps — each
  checklist line references a bundled script or concrete file path.

#### SP4b — Tier-2 / Tier-3 skill bodies (14 skills)

- **Inputs:** SP4a + SP3 done.
- **Deliverables:**
  - 8 Tier-2 SKILL.md files with frontmatter, pathPatterns per §4.14,
    workflow checklists, bundled-scripts tables, inter-skill invocation
    pointers.
  - 6 Tier-3 SKILL.md files (wrappers around the `.sh` guards SP3
    already implemented).
  - Per-skill `scripts/` subdirs populated.
- **Acceptance:** `/ax-verify` end-to-end exits 0; pathPattern
  disambiguation test passes (the test from iter1 §7 Scenario 1
  mitigation, hardened: "I just edited
  `templates/L2/blocks/LoginForm.tsx`, which skill auto-triggers?"
  must resolve to `/ax-verify-L2` and only `/ax-verify-L2`).
- **Verify command:** `/ax-verify` (recursive).
- **Agent count:** 1 lead + 3 workers.
- **TDD anchor:** `skills/_tests/path-pattern-uniqueness.test.sh` —
  for each of 5 sample paths (`templates/L1/components/button.tsx`,
  `templates/L2/blocks/LoginForm.tsx`, `templates/L3/pages/list-page/page.tsx`,
  `templates/L4/auth/app/login/page.tsx`, `backend/src/.../UserService.java`),
  asserts exactly one Tier-2 skill's pathPatterns matches. **Predates
  SP4b implementation: written when only the Tier-2 SKILL.md stubs exist
  with empty pathPatterns; pre-SP4b the test fails (zero matches);
  post-SP4b it passes (exactly 1 match per path).**
- **Risks:** UNCHANGED.

### Phase 2 — Vertical slice (SP5 → SP8) + SP5.5

#### SP5 — L1 shadcn integration + design tokens manifest

- **Inputs:** SP1–SP4b done.
- **Deliverables:** UNCHANGED from iter1 §SP5 + the shadcn drift snapshot
  per §4.1 + `templates/L1/_check-shadcn-drift.sh`.
- **Acceptance:** `/ax-verify-L1` exits 0; `bash
  templates/L1/_check-shadcn-drift.sh` exits 0 against the freshly-frozen
  snapshot.
- **Verify command:** `/ax-verify-L1`.
- **Agent count:** 1 lead + 1 worker.
- **TDD anchor:** `frontend/tests/L1/token-contract.spec.ts` — asserts
  each blessed shadcn component renders with token-driven CSS variables.
  **The test fixture renders each of the 32 blessed components against a
  Vitest snapshot that lists `--color-*`, `--text-*`, `--space-*` CSS
  variables that must be present**. Pre-SP5 the components don't exist;
  the test fails with `ENOENT`. Post-SP5 the test passes.
- **Risks:** UNCHANGED.

#### SP5.5 — Fork-receiver smoke (NEW)

- **Inputs:** SP5 done.
- **Deliverables:**
  - `verify/fork-receiver-smoke.sh` — script that:
    1. Creates a fresh temp directory.
    2. Copies only `templates/L1/`, `templates/L1/_check-shadcn-drift.sh`,
       and a minimal `package.json` listing the L1 deps.
    3. Runs `pnpm install` and `pnpm tsc --noEmit` in the temp dir.
    4. Asserts no compile error; asserts the L1 components do not
       reference any path outside `templates/L1/` or the published deps.
  - The fork-receiver smoke is also a verify skill check (`/ax-verify-L1`
    runs it).
- **Acceptance:** `bash verify/fork-receiver-smoke.sh` exits 0 within
  300s. Any path leakage outside `templates/L1/` exits 1 with message
  `PATH_LEAK: <path>`.
- **Verify command:** `/ax-verify-L1` (extended).
- **Agent count:** 1 lead.
- **TDD anchor:** the smoke script's `PATH_LEAK` assertion is RED before
  SP5 lands (no `templates/L1/` exists), forcing SP5 to ship portable
  artifacts.
- **Risks:**
  - **R:** shadcn requires Tailwind config in the temp dir. **M:** the
    smoke ships a minimal `tailwind.config.js` in the temp scaffolding;
    the smoke validates it can be regenerated by a fork-receiver from
    the L1 README.

#### SP6 — L3 page template catalog

- **Inputs:** SP5 L1 stable.
- **Deliverables:** UNCHANGED from iter1.
- **Acceptance:** UNCHANGED.
- **Verify command:** `/ax-verify-L3`.
- **Agent count:** 1 lead + 2 workers.
- **TDD anchor (REVISED per Critic F):**
  `templates/L3/_fixtures/smoke-app/tests/route-resolution.spec.ts` —
  this fixture and test file are written **first**, in SP5.5 if not in
  SP5. The fixture imports stub `app/<segment>/page.tsx` files from the
  fixture that will be replaced by SP6's actual L3 templates. The
  assertion is: "for each L3 template family (list/detail/create/edit/
  dashboard/auth-callback/error), the fixture's route resolves and
  renders without throwing." Pre-SP6 the fixture's pages are minimal
  placeholders that throw; post-SP6 the L3 templates replace them and
  the test passes.
- **Risks:** UNCHANGED.

#### SP7 — L2 feature block catalog + practices-react rule additions

- **Inputs:** SP5 L1, SP6 L3 contracts.
- **Deliverables:** UNCHANGED except: rule additions capped at
  "implementation-proven needs only" (soft suggestion #5). If a candidate
  rule from §4.7 is not surfaced by SP7 actual block implementation, it
  is deferred to a later SP (subject to S7 audit).
- **Acceptance:** UNCHANGED.
- **Verify command:** `/ax-verify-L2` and `/ax-verify-react`.
- **Agent count:** 1 lead + 4 workers.
- **TDD anchor:** UNCHANGED.

#### SP8 — L4 auth domain workload (vertical proof)

- **Inputs:** SP5/6/7 complete.
- **Deliverables:** UNCHANGED.
- **Acceptance:** UNCHANGED.
- **Verify command:** `/ax-verify-domain auth`.
- **Agent count:** 1 lead + 1 worker.
- **TDD anchor:** UNCHANGED.

### Phase 3 — Horizontalize (SP9 → SP11, partitioned, NOT free parallel)

**Iter1 claimed SP9/SP10/SP11 parallel. Iter2 serializes SP9 first; SP10
and SP11 parallel after.** Justification:

- SP9 (CRUD) surfaces `DataTable` / `FilterBar` / `Pagination` retro-edit
  needs. Payment (SP10) reuses these via the per-page-list contract.
  Running SP10 in parallel with SP9 means SP10 forks on stale L2.
- SP11 (practices viewer) reads only `practices/AGENTS.md` and
  `practices-react/AGENTS.md` statically. It can run in parallel with
  SP10 because the artifacts SP10 modifies (payment-specific L4) are
  disjoint from SP11's reads.

**Shared-artifact partition (revision #5):**

| Artifact | Sole writer | Readers (no writes) |
|---|---|---|
| `practices-react/eslint-plugin-ax/` (3 new rules max) | SP7 only | SP8/9/10/11 may USE rules but not amend the plugin |
| `practices-react/AGENTS.md` sha256 sentinel | SP12 batch regen | SP7..SP11 may modify rule files, but the sentinel regenerates once at SP12 |
| `contracts/templates/ui-contract.schema.yaml` meta-schema | SP2 only | SP9/SP10/SP11 may not amend; if SP9 surfaces a missing field, an SP9.5 sync sub-phase is filed |
| `templates/L2/blocks/*.tsx` | SP7 primary writer; SP9 may amend ≤ 1 file per block with ADR ; SP10/SP11 may not amend (forced to use props/escape hatches) | All |
| `templates/DECISIONS.md` | SP3 establishes; SP9/SP10/SP11 may append ADRs (TD-2026-05-17-011..) in serial order; merge conflicts resolved by sequential rebase at SP12 |

#### SP9 — L4 crud domain workload (serial)

- **Inputs:** SP8 done.
- **Deliverables / Acceptance / Verify command:** UNCHANGED.
- **Agent count:** 1 lead + 1 worker.
- **TDD anchor (REVISED per Critic F):**
  `frontend/tests/crud/list-create-edit-delete.spec.ts` — Playwright
  e2e written **first** against the (not-yet-existing) `templates/L4/crud/`
  routes. Pre-SP9 the routes 404; the test fails with "expected 200, got
  404." Post-SP9 the routes exist and round-trip green.

#### SP10 — L4 payment domain workload (parallel with SP11 after SP9)

- **Inputs:** SP8/SP9 done.
- **Deliverables / Acceptance / Verify command:** UNCHANGED.
- **Agent count:** 1 lead + 1 worker.
- **TDD anchor (REVISED per Critic F):**
  `frontend/tests/payment/idempotency-replay.spec.ts` — Playwright
  e2e written **first**: it sends a checkout POST with idempotency key
  K, then re-sends the same POST with K and asserts the second response
  matches the first (no duplicate charge). Pre-SP10 the endpoint 404s;
  post-SP10 the endpoint replays correctly.

#### SP11 — L4 practices catalog viewer (parallel with SP10 after SP9)

- **Inputs:** SP6 L3, SP7 L2.
- **Deliverables / Acceptance / Verify command:** UNCHANGED.
- **Agent count:** 1 lead + 1 worker.
- **TDD anchor (REVISED per Critic F):**
  `frontend/tests/practices/all-rules-reachable.spec.ts` — Playwright
  e2e reads `practices/AGENTS.md` and `practices-react/AGENTS.md` at
  test-load time, extracts the rule id list, then for each rule id
  navigates to `/practices/<ruleId>` and asserts response 200 + page
  title contains the rule id. Pre-SP11 the viewer doesn't exist (404);
  post-SP11 every rule is reachable.

### Phase 4 — Integration (SP12)

#### SP12 — End-to-end binary green

- **Inputs:** SP1–SP11 done.
- **Deliverables:** UNCHANGED from iter1, except:
  - The fork-receiver smoke (SP5.5) is re-run at SP12 with the full
    `templates/L1+L2+L3+L4+backend/` tree.
  - The skill-topology probe is binary (replaces iter1's "quarterly
    review" mitigation): a single test asserts ≤ 3 Tier-1 skills, ≤ 8
    Tier-2 skills, ≤ 6 Tier-3 skills.
- **Acceptance:** UNCHANGED + fork-receiver smoke exits 0 within 300s
  on the full template tree.
- **Verify command:** `/ax-verify`.
- **Agent count:** 1 lead orchestrator.
- **TDD anchor (REVISED per Critic F):**
  `verify/fork-receiver-cold-start.test.sh` — script written **first**
  that exits 1 because no `templates/` exists yet (very early in the
  cycle, this is true). Post-SP1..SP11 it exits 0. The script's logic
  IS the L3 fork-simulation: cold-clone the repo into a temp dir, run
  `/ax-verify`, assert exit 0 within 300s.

### 5.bonus — Estimated effort (revised)

| Phase | SPs | Estimated days |
|---|---|---|
| Phase 0 (pre-foundation) | SP3 | 2–3 |
| Phase 1 (foundation) | SP1, SP2, SP4a, SP4b | 4–6 |
| Phase 2 (vertical slice + smoke) | SP5, SP5.5, SP6, SP7, SP8 | 6–8 |
| Phase 3 (horizontalize, serial+partial-parallel) | SP9 then SP10‖SP11 | 5–7 (SP9 ≈ 2; SP10‖SP11 ≈ 3–5 parallel wall time) |
| Phase 4 (integration) | SP12 | 1–2 |
| **Total** | 13 SPs (was 12) | **18–26 days** (was 13–19; the increase reflects honest serialization) |

### 5.5 Verification Matrix (NEW — revision #2)

Single authoritative table covering all 13 SPs (SP3 now counted; SP4
split). No "TBD" columns.

| SP | verify_skill | script_path | test_file | assertion | expected_RED_reason | first_green_command | observability_signal |
|---|---|---|---|---|---|---|---|
| SP3 | `/ax-guard-evidence` (via Tier-2 wrapper after SP4b; direct script before) | `practices/evals/run-all-guards.sh` | `practices/evals/fixtures/trio_integrity/pass/` + `practices/evals/fixtures/cross_trio/pass/` (PASS fixtures) AND `fail_*/` (FAIL fixtures) | All 6 guards exit 0 on `pass/`; exit 1 on each `fail_*/` with matching error message | Pre-SP3: `trio_integrity_guard.sh` does not exist; the orchestrator returns "script not found" | `bash practices/evals/run-all-guards.sh --include-fixtures` | `guard.execution.duration` (per-guard wall-time emitted to stderr; assert < 30s each) |
| SP1 | `/ax-verify-react` (Tier-2 wrapper exists only after SP4b; pre-SP4b, direct commands) | `frontend/package.json:scripts.test`, `frontend/package.json:scripts.build` | `frontend/tests/auth/login-flow.spec.ts` (Playwright) | OAuth round-trip: signup → email verify → login → callback → protected route returns 200; same HTTP shape as Vite version | Pre-SP1: Vite removed, Next.js not yet wired; build fails ENOENT on next config | `cd frontend && npm run build && npm run test && npx playwright test tests/auth/` | `frontend.route.rendered` (custom event emitted in `app/layout.tsx`; Playwright probe asserts event fires for each route) |
| SP2 | `/ax-guard-trio-integrity` (direct script pre-SP4b) | `practices/evals/trio_integrity_guard.sh` | `frontend/tests/_fixtures/spec-trio-coverage-fail/` (FAIL fixture) + `specs/auth-frontend-l0.yaml` (PASS data) | Guard exit 0 on real auth trio; exit 1 on fail fixture with `MISSING_OPERATION_ID` | Pre-SP2: `specs/auth-frontend-l0.yaml` does not exist; guard exits 1 with `MISSING_FRONTEND_SPEC` | `bash practices/evals/trio_integrity_guard.sh` | `spec_trio.coverage_ratio` (guard emits ratio per domain; assert ≥ 1.0 for auth) |
| SP4a | `/ax-verify` (recursive after SP4a) | `skills/_tests/tier1-topology.test.sh` | `skills/_tests/tier1-topology.test.sh` (self-test) | `find skills/ax-* -name SKILL.md` returns exactly 3 paths matching the Tier-1 enum | Pre-SP4a: only `skills/ax-transform/SKILL.md` exists (count=1) | `bash skills/_tests/tier1-topology.test.sh` | `skill.invoked` (Tier-1 skills emit this event on invocation; assert each of 3 emits when called) |
| SP4b | `/ax-verify` (recursive after SP4b) | `skills/_tests/path-pattern-uniqueness.test.sh` | `skills/_tests/path-pattern-uniqueness.test.sh` (self-test) | For each of 5 sample paths, exactly one Tier-2 SKILL.md's pathPatterns matches | Pre-SP4b: Tier-2 stubs lack pathPatterns; zero matches per path | `bash skills/_tests/path-pattern-uniqueness.test.sh` | `skill.pathpattern.disambiguation` (test emits per-path match count; assert == 1 for each) |
| SP5 | `/ax-verify-L1` | `templates/L1/_check-shadcn-drift.sh` | `frontend/tests/L1/token-contract.spec.ts` | Each of 32 blessed components renders with token-driven CSS variables; drift script exits 0 | Pre-SP5: `templates/L1/components/` empty; test exits with `ENOENT` | `npx vitest run frontend/tests/L1/ && bash templates/L1/_check-shadcn-drift.sh` | `token.contract.violations` (test emits count of components using hardcoded values; assert == 0) |
| SP5.5 | `/ax-verify-L1` (extended) | `verify/fork-receiver-smoke.sh` | `verify/fork-receiver-smoke.sh` (self-test) | Temp-dir fork build passes; no path leakage outside `templates/L1/` | Pre-SP5.5: script doesn't exist | `bash verify/fork-receiver-smoke.sh` | `fork.receiver.smoke.duration` (script emits wall-time; assert < 300s); `fork.path.leak.count` (== 0) |
| SP6 | `/ax-verify-L3` | `templates/L3/_fixtures/smoke-app/package.json:scripts.test` | `templates/L3/_fixtures/smoke-app/tests/route-resolution.spec.ts` | Each L3 template family resolves and renders without throwing | Pre-SP6: fixture stubs throw `NotImplemented` | `cd templates/L3/_fixtures/smoke-app && npm run test` | `route.render.success_rate` (Playwright probe asserts 7/7 families render) |
| SP7 | `/ax-verify-L2` and `/ax-verify-react` | `practices-react/evals/run.sh`; per-block Vitest | `templates/L2/blocks/<block>.spec.test.tsx` (per block) | Each block renders in isolation; imports only L1 + lib; no cross-block import | Pre-SP7: blocks don't exist; vitest fails with `Cannot find module` | `npx vitest run templates/L2/` | `lint.cross_block_import_violations` (custom ESLint rule emits count; assert == 0) |
| SP8 | `/ax-verify-domain auth` | `frontend/playwright.config.ts` | `frontend/tests/auth/full-flow.spec.ts` (e2e) | signup → email verify → login → OAuth callback → protected route all green; `./gradlew testAsvs` still green | Pre-SP8: `templates/L4/auth/` empty; routes 404 | `cd frontend && npx playwright test tests/auth/ && cd .. && ./gradlew testAsvs` | `traceId_propagated` (Playwright asserts request `X-Trace-Id` header reaches Spring Boot logs; backend log probe confirms match) |
| SP9 | `/ax-verify-domain crud` | `frontend/playwright.config.ts` | `frontend/tests/crud/list-create-edit-delete.spec.ts` | CRUD round-trip exits 0 against real Spring Boot RANDOM_PORT | Pre-SP9: routes 404 | `cd frontend && npx playwright test tests/crud/ && cd .. && ./gradlew testCrud` | `server_action.completed` (Server Action emits count; assert == # of submissions); `traceId_propagated` (same as SP8) |
| SP10 | `/ax-verify-domain payment` | `frontend/playwright.config.ts` | `frontend/tests/payment/idempotency-replay.spec.ts` | Idempotent replay: 2nd POST with same key returns identical response | Pre-SP10: routes 404 | `cd frontend && npx playwright test tests/payment/ && cd .. && ./gradlew testPayment` | `payment.idempotency.replay_match` (boolean per test; assert true for all replay cases); `traceId_propagated` |
| SP11 | `/ax-verify-domain practices` + `/ax-guard-trio-integrity --mode frontend_only` | `frontend/playwright.config.ts`; `practices/evals/trio_integrity_guard.sh --mode frontend_only` | `frontend/tests/practices/all-rules-reachable.spec.ts` | (i) Every rule id from `practices/AGENTS.md` + `practices-react/AGENTS.md` resolves under `/practices/<ruleId>` (Playwright route 200 + title contains id); (ii) `frontend_only` mode passes — every UI route has `backend_operation_id: null` AND `static_source_ref` resolves to ≥ 1 file; (iii) `static_source_ref` signals match the `pass_frontend_only_practices/` fixture exit 0 | Pre-SP11: viewer doesn't exist (404); `specs/practices-frontend-l0.yaml` absent → trio_integrity_guard exits 1 with `MISSING_FRONTEND_SPEC` | `cd frontend && npx playwright test tests/practices/ && bash practices/evals/trio_integrity_guard.sh --mode frontend_only` | `practices.viewer.broken_link.count` (assert == 0); `static_source_ref.unresolved_count` (assert == 0) |
| SP12 | `/ax-verify` | `verify/run-all.sh` (extended) + `verify/fork-receiver-cold-start.test.sh` | `verify/fork-receiver-cold-start.test.sh` | Cold-clone → `/ax-verify` exit 0 within 300s; L4 sealed sub-agent reaches MUST_PASS ≥ 9/11 on frontend rubric | Pre-SP12: cold-clone has no `templates/`; script exits 1 with `NO_TEMPLATES` | `bash verify/run-all.sh && bash verify/fork-receiver-cold-start.test.sh` | `cold_start.duration` (assert < 300s); `l4_sealed.must_pass.count` (assert ≥ 9); `skill.topology.tier_count` (assert tier1=3, tier2=8, tier3=6) |

---

## 6. Verification Plan

### 6.1 Per-SP TDD anchor (RED → GREEN)

Every SP above lists its TDD anchor referencing a target that **predates**
that SP's implementation. The Verification Matrix (§5.5) is the single
authoritative reference.

### 6.2 Per-SP termination via /verification-loop

Each SP terminates when the verify_skill in §5.5 returns exit 0.

### 6.3 Full test plan per SP — see §5.5 Verification Matrix

### 6.4 Autonomous Execution Safety (NEW — revision #4)

This subsection makes autonomous `/team` execution safe. All requirements
are binary.

#### 6.4.1 Rollback boundary per SP

Each SP commits to a single tagged commit. If the SP's verify_skill fails
after 3 consecutive `/ax-verify-*` runs, the lead worker reverts the
working tree to the SP's pre-start tag and writes
`docs/superpowers/halt/<SP_id>-halt-reason.md`.

| SP | Pre-start tag | Rollback boundary commit |
|---|---|---|
| SP3 | `git tag sp3-pre-start` | Any commit not introducing fixture files; revert to tag |
| SP1 | `git tag pre-nextjs-migration` (already in iter1) | Revert frontend/ to tag; reapply skill stubs from SP3 |
| SP2 | `git tag sp2-pre-start` | Revert specs/, contracts/, blueprints/ to tag |
| SP4a | `git tag sp4a-pre-start` | Revert skills/ax-{transform,verify,scaffold}/ to tag |
| SP4b | `git tag sp4b-pre-start` | Revert remaining 14 skill files to tag |
| SP5 | `git tag sp5-pre-start` | Revert templates/L1/ to tag (empty placeholders) |
| SP5.5 | `git tag sp5.5-pre-start` | Revert verify/fork-receiver-smoke.sh to tag (absent) |
| SP6 | `git tag sp6-pre-start` | Revert templates/L3/ to tag |
| SP7 | `git tag sp7-pre-start` | Revert templates/L2/ + practices-react/rules/ to tag |
| SP8 | `git tag sp8-pre-start` | Revert templates/L4/auth/ + frontend/tests/auth/ to tag |
| SP9 | `git tag sp9-pre-start` | Revert templates/L4/crud/ to tag |
| SP10 | `git tag sp10-pre-start` | Revert templates/L4/payment/ to tag |
| SP11 | `git tag sp11-pre-start` | Revert templates/L4/practices/ to tag |
| SP12 | `git tag sp12-pre-start` | Revert verify/, docs/blueprints/frontend-templatization/ to tag |

#### 6.4.2 Shared-artifact ownership matrix

| Artifact | Sole writer SP | Reader SPs (read-only) | Stale-state invalidation rule |
|---|---|---|---|
| `practices-react/eslint-plugin-ax/` (npm package + rules) | SP7 | SP8, SP9, SP10, SP11 | When SP7 bumps the plugin version, downstream SPs must re-run `npm install` and re-run their own lint before resuming |
| `practices-react/AGENTS.md` (sha256 sentinel) | SP12 batches all regenerations | SP3..SP11 modify rule files but DO NOT regenerate the sentinel | Pre-SP12: sentinel is stale by design; SP12 regenerates exactly once |
| `contracts/templates/ui-contract.schema.yaml` (meta-schema) | SP2 | SP9, SP10, SP11 read only | If SP9/10/11 require a schema field that doesn't exist, the SP halts and files an SP2.X amendment ticket |
| `templates/L2/blocks/*.tsx` | SP7 primary; SP9 may amend ≤ 1 per block w/ ADR | SP8, SP10, SP11 read only (or SP9 with ADR) | When SP9 amends an L2 block, SP10/SP11 must rebase against the new L2 before resuming (re-read, not cache) |
| `templates/DECISIONS.md` | SP3 establishes; SP9..SP11 append serial-only | All | Append-only; merge conflicts resolved by sequential rebase at SP12 |
| `templates/AGENTS.md` (sha256 sentinel) | SP12 batches | SP3..SP11 modify templates/ but don't regenerate | Same as practices-react AGENTS.md |
| `frontend/package.json` | SP1 sole writer; SP5 may add shadcn deps; SP7 may add 1 ESLint plugin upgrade | SP6, SP8, SP9, SP10, SP11 read only | Lockfile is committed; readers consume the locked state. Adding a dep requires SP-level coordination |

#### 6.4.3 Stale-state invalidation rule (general)

When SP_k modifies a shared artifact listed in §6.4.2, every downstream
SP_{k+1...} that depends on the artifact must re-read it (not cache it
across worktrees). Concretely:

- Re-run `npm install` if `package.json` or `eslint-plugin-ax` changed.
- Re-run `practices/evals/run-all-guards.sh` if any
  `practices/rules/`, `practices-react/rules/`, or `templates/` file changed.
- Re-run `templates/generate_agents.sh` if any `templates/` content
  changed (read-only verification only; sentinel write deferred to SP12).

#### 6.4.4 Explicit halt thresholds

- **3-fail halt:** if `/ax-verify-*` returns exit 1 three times
  consecutively within the same SP, the lead writes
  `docs/superpowers/halt/<SP_id>-halt-reason.md` and stops the worker
  pool. Format:

  ```yaml
  sp_id: <id>
  halt_reason: <one-line>
  failing_command: <verify_skill invocation>
  exit_code: <int>
  last_3_logs: [<truncated stderr>, ..., ...]
  rollback_tag: <git tag>
  recovery_proposal: <suggested next action>
  ```

- **30-minute idle halt:** if a worker emits no `progress.heartbeat` for
  30 minutes, the lead reassigns the worker's slice OR rolls back.
- **5-rebase halt:** if any SP_k requires more than 5 rebases against a
  shared artifact in a single working session, halt and escalate.

#### 6.4.5 ESCAPE valve

When any halt threshold trips, the lead writes a single file at
`docs/superpowers/escape/<SP_id>-<timestamp>.md` (the user reads this to
take over). Format:

```yaml
sp_id: <id>
escape_trigger: <3_fail_halt | 30min_idle_halt | 5_rebase_halt | manual>
timestamp: <iso-8601>
last_green_commit: <sha>
failing_artifact: <path>
failing_verify_skill: <skill_name>
diagnostic_dump_path: <docs/superpowers/halt/...md>
suggested_recovery_options:
  - <option 1>
  - <option 2>
```

The presence of this file signals the user to take manual control. The
`/team` orchestration MUST NOT auto-retry past 3 consecutive failures.

### 6.5 Self-application proof — UNCHANGED

---

## 7. Pre-mortem (DELIBERATE mode) — REVISED

4 scenarios (was 3). Each mitigation declares owner, command/artifact,
threshold, and rollback-vs-continue criteria.

### Scenario 1 — Guard false-green (REVISED with hard threshold)

**Failure:** `evidence_guard.sh` (or any extended guard) exits 0 with
zero rule files actually scanned. The pre-SP3 guards walk
`practices/rules/`; if an environmental glitch causes the find/grep to
match zero files, the guard exits 0 ceremonially.

**Likelihood:** Medium (only after the SP3 walk-target extension).

**Detection (REVISED):** **HARD THRESHOLD:** every guard now enforces
`ZERO_SCAN` per §4.10. If a guard's walk produces zero files, the
guard exits 1 with message `ZERO_SCAN: <walk_target>`. The pre-existing
guards are amended in SP3 to add this check.

**Mitigation (executable):**

- **Owner:** SP3 lead.
- **Command:** `bash practices/evals/run-all-guards.sh --include-fixtures`
  must return exit 0 on `pass/` fixtures AND exit 1 on
  `fail_zero_scan/` fixtures. **Rollback trigger:** if any guard exits
  0 on `fail_zero_scan/`, revert SP3 to pre-start tag.
- **Threshold:** **0 false-greens tolerated**; the test fails if any
  guard passes a zero-scan input.

### Scenario 2 — Next.js 16 migration breaks the auth flow (REVISED with rollback criteria)

**Failure:** OAuth callback URL changes shape; React 19 Server/Client
boundary breaks `lib/auth/`; Playwright `login-flow.spec.ts` red.

**Likelihood:** High.

**Detection:** SP1 Playwright anchor (`frontend/tests/auth/login-flow.spec.ts`)
fails. `./gradlew testAsvs` may still pass (backend-only).

**Mitigation (executable):**

- **Owner:** SP1 lead.
- **Pre-start tag:** `git tag pre-nextjs-migration` is mandatory before
  SP1 starts.
- **Rollback-vs-continue criteria:**
  - **Rollback** (revert to `pre-nextjs-migration` tag) if:
    - `login-flow.spec.ts` fails after 3 consecutive Next.js fix
      attempts (matches §6.4.4 3-fail halt), OR
    - `./gradlew testAsvs` fails (this would mean the migration broke
      backend contracts, which is out-of-scope and a hard rollback
      trigger).
  - **Continue** (fix forward) if:
    - The failing test is a known App Router quirk listed in
      `docs/superpowers/known-quirks/nextjs-16-migration.md` (e.g.,
      `useSearchParams` requires Suspense wrapper) AND a documented
      fix exists with < 30 min apply time.
- **Threshold:** Continue allowed up to 2 fix attempts per quirk; on
  3rd attempt, rollback.

### Scenario 3 — L2 abstractions over-fit (REVISED, retargeted per Architect)

**Failure:** L2 blocks extracted in SP7 from auth-domain experience
don't survive payment's idempotency state machine or the practices
viewer's Server-Component-reading-static-files pattern.

**Likelihood:** Medium-high.

**Detection:**

- SP9 audit: "list every L2 block consumed; for each, does the prop
  surface match the actual need or were you forced to add an escape
  hatch?" If > 2 escape hatches across SP9 + SP10, halt and run an
  SP9.5 sync sub-phase.
- **NEW (per Architect §5):** auth → payment surface validator: SP10's
  TDD anchor (`idempotency-replay.spec.ts`) directly stresses the
  state machine that auth's `LoginForm` never exercises. If L2's
  `PaymentCheckoutForm` requires a 4th prop (beyond `onSubmit`,
  `defaultValues`, `schema`) to handle idempotency, that is an L2
  defect.
- **NEW:** auth → practices viewer surface validator: SP11 reads
  static files at build time; if L2's `RuleCatalogTable` cannot accept
  pre-fetched data via a server-component-only prop, that is an L2
  defect.

**Mitigation (executable):**

- **Owner:** SP9 + SP10 + SP11 leads collectively (SP10/SP11 wait for
  SP9 audit result before proceeding).
- **Command:** SP9 audit script
  `verify/audit-l2-escape-hatches.sh` exits with the escape-hatch count
  per block.
- **Threshold:** ≤ 2 escape hatches total across SP9 → green; > 2 →
  SP9.5 sync.

### Scenario 4 — Spec Trio drift (NEW per revision #6)

**Failure:** Backend Spec Trio updates without corresponding frontend
Spec Trio updates (or vice versa). Example: a new ASVS item lands in
`specs/auth-asvs-l1.yaml` after SP2 but before SP12; the frontend trio
isn't updated; `trio_integrity_guard.sh` should fail but is not run on
that commit.

**Likelihood:** Medium. Backend catalog growth is normal activity
(CLAUDE.md principle); without enforced re-verification, drift accumulates.

**Detection:**

- `trio_integrity_guard.sh` (§4.8.4) enforces 100% coverage of backend
  items with `frontend_required: true`.
- `/ax-verify-shared` runs `trio_integrity_guard.sh` on every invocation.
- **NEW gate:** `practices/evals/spec_trio_drift_probe.sh` — script that
  walks `specs/<domain>-*.yaml` files modified in the last N commits
  (N=10 by default) and verifies each modification has a matching
  modification in the frontend trio counterpart. Exits 1 with
  `DRIFT_DETECTED: <files>` if not.
- This probe runs in `/ax-verify` (Tier-1) on every invocation.

**Mitigation (executable):**

- **Owner:** SP3 lead (script ships in SP3); ongoing owner is whoever
  invokes `/ax-verify`.
- **Command:** `bash practices/evals/spec_trio_drift_probe.sh
  --window 10` (configurable window).
- **Threshold:** `DRIFT_DETECTED` exit 1 if ≥ 1 backend Spec Trio
  modification in the window lacks a paired frontend modification.
- **Recovery:** the drift_probe's stderr lists the missing frontend
  files; the maintainer adds the matching frontend items in a follow-up
  commit; rerunning the probe exits 0.

### Expanded test plan additions (DELIBERATE mode) — UNCHANGED per iter1

(Lighthouse smoke, a11y axe gate, observability traceId, bundle
budget, cold-start agent test.)

---

## 8. ADR Template (for final commit)

The summary ADR follows this shape (now with `provenance_class`):

```yaml
---
adr_id: TD-2026-05-17-000
title: Frontend Templatization + Full-Stack Methodology Extension
provenance_class: internal_design
evidence:
  source_type: internal
  source_ref: docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md
  rationale: |
    This ADR captures an internal topology decision (4 template layers + 3-tier
    skill graph + Frontend Spec Trio schema). No external standard governs
    composition-kit topology design. The empirical anchor is the Payment domain
    L4 sealed sub-agent PASS (11/11 MUST + 6/6 SHOULD), referenced under empirical
    provenance below.
spec_ref: METHODOLOGY.md#A.3
---

## Decision
Adopt the 4-layer frontend template model (L1 shadcn primitives, L2 ax-template
feature blocks, L3 Next.js 16 App Router page templates, L4 vertical domain
workloads) mirroring the backend Spec Trio. Migrate Vite → Next.js 16 App Router
in one transition. Install a 3-tier skill topology (3 Tier-1 + 8 Tier-2 +
6 Tier-3 = 17 skills); Tier-3 guards are not pathPattern-triggered.

## Drivers
(1) AI agent self-discoverability (context-0 agent must reach green build).
(2) Migration safety (auth domain must stay green; `./gradlew testAsvs`).
(3) Catalog convergence cost (one pass = full coverage).

## Alternatives considered
A. Strict bottom-up — late integration risk.
B. Vertical-slice first — N=1 over-fit risk.
C. Hybrid — **CHOSEN**.
D. Vite + Next.js coexistence — CONSTRAINT-BLOCKED by Section A; steelman benefit
   (rollback-safe migration week) preserved by the SP1 `pre-nextjs-migration`
   tag instead.

## Why chosen
Hybrid (C) matches the Payment empirical cadence (foundation → vertical →
horizontalize → integrate). Bounds rework via the L2 retro-edit budget.

## Consequences
- Vite removed; `frontend/package.json` rewritten.
- `templates/` new top-level directory (L1, L2, L3, L4, backend).
- 4 guard scripts get `templates/**` walking + zero-scan checks; 2 new
  guards (`trio_integrity_guard.sh`, `cross_trio_guard.sh`) land.
- 7 upstream snapshots added (+ shadcn-registry drift snapshot).
- practices-react/ grows by ≤ 10 rules; practices/ by ≤ 4 rules.
- 17 SKILL.md files added.
- METHODOLOGY.md Appendix A gains A.3; Appendix C Recipe B promoted.
- SP3 lands first (guards before placeholders); SP4 split into 4a/4b.
- SP9 serial before SP10/SP11; shared artifacts partitioned.

## Follow-ups
- Post-SP12: run L4 sealed sub-agent on fresh fork (target 9/11 MUST + 4/6 SHOULD).
- 90-day refresh of all 7 new upstream snapshots.
- 180-day: add 5th L4 domain (Notification) to stress-test.

## ADR registry (TD-2026-05-17-001..010)
Each carries its own provenance_class:
- 001 Next.js 16 App Router: locked_constraint
- 002 shadcn/ui as L1: external_canonical
- 003 TanStack Query v5: external_canonical
- 004 Zustand: external_canonical
- 005 Zod: external_canonical
- 006 Playwright + Vitest + MSW: external_canonical
- 007 Frontend Spec Trio schema: internal_design
- 008 3-tier skill topology: internal_design
- 009 templates/ directory shape: internal_design
- 010 Design-tokens manifest format: external_canonical (cites WCAG 2.2 + CWV)
```

---

## 9. Open Questions

Carried from iter1 + revised:

1. **Tailwind dependency.** shadcn 2.x ships with Tailwind v4. Assumed
   transitive. If maintainer wants Tailwind as a first-class concern, add
   TD-2026-05-17-011.
2. **Catalog viewer hosting.** Sub-route of main app or separate static
   export. Default: sub-route.
3. **Storybook.** Out of scope this cycle. Revisit after SP12.
4. **SSR of Markdown evidence.** Start with rehype + DOMPurify; MDX
   only if forced.
5. **i18n.** English-default; i18n harness deferred.
6. **NEW (analyst-extracted): SP5.5 portability target.** The fork-receiver
   smoke runs only on L1 at SP5.5. Should it re-run at SP7 (L2) and SP8
   (L4) increments? Default: re-run at SP12 (full tree) only; per-layer
   smoke deferred to follow-up.
7. **NEW: ESCAPE valve recovery automation.** Once an ESCAPE file is
   written, should the next `/team` invocation auto-resume from the
   `last_green_commit`? Default: NO — human approval required before
   resume. This preserves the §6.4.4 hard halt semantics.
8. **NEW: `provenance_class: locked_constraint` audit cadence.** Section
   A–E constraints are locked for this cycle. Are they audited per fork?
   Default: each fork-receiver inherits the constraints as defaults but
   may override with a new ADR (provenance_class: internal_design)
   citing their rationale.
9. **NEW (iter3): `static_source_ref` glob syntax.** The iter3
   `frontend_only` mode (§4.8.4) requires `static_source_ref` entries to
   resolve to ≥ 1 existing file. **Default chosen by Planner:**
   shell-style glob expansion rooted at the repo top, supporting `*` and
   `**` (POSIX-compatible glob semantics). Literal paths are a degenerate
   glob. Open question: should regex be admitted in addition to glob?
   Default: NO — glob keeps the guard binary and avoids regex injection
   risk in fixtures. Revisit only if a downstream domain needs regex.
10. **NEW (iter3): `static_source_ref` `provenance_class`.** The
    `static_source_ref` field in §4.8.1/§4.8.2 records an internal design
    decision (no external standard exists for "static viewer reads
    catalog Markdown"). Default: any ADR that introduces a new
    `frontend_only` domain SHALL declare
    `provenance_class: internal_design` per §4.12 with a `rationale`
    field explaining why the domain has no backend API. Existing
    `practices` domain inherits this classification by default.

---

## 10. Honored Constraints (cross-check vs CLAUDE.md anti-patterns)

| CLAUDE.md anti-pattern | How this plan honors it |
|---|---|
| 거버넌스 무한루프 (governance infinite loops) | No promotion-gate docs. **Quarterly review removed (was iter1 §7 Scenario 1 mitigation); replaced by binary SP4b + SP12 skill-topology probe.** Every artifact ships with binary verification. |
| MockMvc 전용 테스트 | Backend integration stays RestAssured. Frontend uses Playwright + real Spring Boot RANDOM_PORT, MSW for unit isolation only. |
| Fork받은 팀의 정책을 skill이 강제 | Skill remains advisory. Fork-receiver smoke (SP5.5) tests portability; does not enforce branch/PR/merge policy. |
| 문서 0줄 / 코드 0줄 균형 | Plan ships 80+ code/config artifacts and ≤ 12 new docs. Code-to-doc ratio ≥ 6:1. |
| Catalog 확장 = 정상 활동 | Anticipated 4 backend + 10 React rules (capped to implementation-proven). S7 generalization audit gates each. **Catalog growth welcomed.** |
| React + Spring 둘 다 active equal partner | Both stacks ship in every phase. Backend gains 10 cross-cutting templates + up to 4 rules; frontend gains everything. |
| 단일 npm 패키지 / 단일 product framing 금지 | Composition kit. shadcn external-owned; design tokens internal; wrappers thin; L2 blocks reusable independently. **Fork-receiver smoke at SP5.5 + SP12 validates this.** |
| Adoption probability metric 금지 | Success is binary `exit 0`. No adoption percentages. |

---

## 11. Revision provenance (iter1 → iter2)

This iter2 applies the 7 Codex Critic surgical revisions + all Architect
remediations. Diff summary:

| Critic revision | Where applied |
|---|---|
| 1. §4.8/§4.10 binary-implementable | §4.8.1–4.8.5, §4.10 (zero-scan added) |
| 2. Verification Matrix (SP1–SP12) | §5.5 (covers all 13 SPs including SP3 + SP4a/b + SP5.5) |
| 3. Layer Membership Decision Table | §4.11 |
| 4. Autonomous Execution Safety | §6.4 (rollback, ownership, stale-state, halt, ESCAPE) |
| 5. Phase 3 serialize/partition | §5.0 graph + §6.4.2 ownership matrix |
| 6. §7 pre-mortem patches | §7 (Scenarios 1–2 thresholds, Scenario 4 NEW) |
| 7. Remove quarterly review + provenance_class | §3.2 + §4.12 + §10 cross-check |

| Critic hard blocker | Where applied |
|---|---|
| 1. trio_integrity_guard binary | §4.8.4 |
| 2. Remove placeholder guards | §3.2 + SP3 lands first (§5.0) |
| 3. TDD anchors with RED reason + first green command | §5.5 Verification Matrix |
| 4. Per-SP observability | §5.5 observability_signal column |
| 5. Autonomous /team safety | §6.4 |
| 6. SP9–SP11 serial/partition | §5.0 + §6.4.2 |

| Architect remediation | Where applied |
|---|---|
| (a) Layer Membership Decision Table | §4.11 |
| (b) `templates/backend/**` + DECISIONS.md verify ownership | §4.10 + §4.11 |
| (c) `provenance_class` for internal-design ADRs | §4.12 |
| (d) Remove quarterly review | §10 cross-check |
| (e) TDD anchors concrete | §5.5 |
| (f) Cross-Trio binary | §4.8.4 |
| (g) SP9–11 serialize/partition | §5.0 + §6.4.2 |
| SP4 split into SP4a/SP4b | §5.0 + §5.5 |

| Soft suggestion | Where applied |
|---|---|
| Option D constraint-blocked wording | RALPLAN-DR Summary + §8 |
| shadcn drift probe | §4.1 + §4.13 + SP3 + SP5 |
| ADR `provenance_class` | §4.12 + §8 |
| Fork-receiver smoke earlier | SP5.5 (NEW) |
| Cap React rule additions | SP7 deliverable note |

| Steelman (criterion L) | SP5.5 fork-receiver smoke + SP12 cold-start re-run |

---

## End of PRD iter3.

> **iter3 delta vs iter2 (surgical patch — closes Codex Critic iter2 ITERATE blocker):**
>
> - §4.8.1 — `backend_operation_id` made nullable for page items;
>   `static_source_ref` array added (required when backend_operation_id is null).
> - §4.8.2 — same flexibility at UI route level; `backend_contract_ref`
>   nullable for `frontend_only` domains.
> - §4.8.4 — `trio_integrity_allowlist.yaml` enum gains `frontend_only`
>   third class; algorithm gains domain-mode branching with strict
>   `static_source_ref` resolution check (zero-file expansion → FAIL).
> - §4.8.4 fixtures — 3 new fixtures added: `pass_frontend_only_practices/`,
>   `fail_frontend_only_missing_source_ref/`,
>   `fail_frontend_only_unreachable_route/`.
> - §5.5 SP11 row — assertion and first_green_command extended to include
>   `frontend_only` mode signal + `static_source_ref.unresolved_count`.
> - §9 — 2 new open questions documenting glob syntax + `provenance_class`
>   for `static_source_ref`.
>
> No other section was modified. Six iter1→iter2 hard blockers remain
> CLOSED. SP ordering, observability signals, rollback rules, and
> verification-matrix structure are unchanged.

> **Next step in the ralplan loop:** re-judge by Architect + Critic. If both
> APPROVE, commit final PRD as TD-2026-05-17-000 ADR with Lore trailers
> (Step 6) and hand off SP3 → SP1 → SP2 → SP4a → SP4b → SP5 → SP5.5 → SP6 →
> SP7 → SP8 → SP9 → SP10‖SP11 → SP12 sequence to `/team`.
