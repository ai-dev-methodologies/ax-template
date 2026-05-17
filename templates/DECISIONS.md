# templates/DECISIONS.md — Decision Provenance Trail

This file records architectural decisions for the ax-template frontend templatization system.
Each ADR uses the TD-2026-05-17-NNN numbering scheme. All entries are machine-validated by
`evidence_guard.sh` (via §4.10 templates/ walk extension).

Do not edit frontmatter by hand — all `provenance_class`, `evidence`, and `spec_ref` fields
are validated by the guard system on every PR.

---

## TD-2026-05-17-001 — Next.js 16 App Router as the singular frontend stack

```yaml
---
adr_id: TD-2026-05-17-001
title: "Next.js 16 App Router as the singular frontend stack"
provenance_class: locked_constraint
evidence:
  source_type: internal
  source_ref: docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter4.md
  rationale: |
    Section A of the user brief locks Next.js 16 App Router as THE frontend
    stack (singular). Vite coexistence is explicitly CONSTRAINT-BLOCKED. This
    decision is pre-aligned and not subject to reversal within this PRD cycle.
    Lock is inherited by every fork; fork-receivers may override with a new ADR.
spec_ref: N/A
status: ACCEPTED
date: 2026-05-17
---
```

### Decision

Adopt Next.js 16 App Router as the one and only frontend runtime for the ax-template
composition kit. The Vite 6 + react-router-dom v7 baseline is replaced in one transition
(SP1), with no coexistence period.

### Rationale

This is a locked constraint from the user brief (Section A). The rollback safety concern
(steelman Option D — Vite coexistence) is addressed by the `pre-nextjs-migration` git tag
in SP1, not by stack coexistence.

### Consequences

- `frontend/package.json` is rewritten in SP1 (drop vite/react-router; add next@16).
- All frontend tests target Next.js App Router conventions.
- SP3 guard extensions target `templates/**` (not `frontend/` — that is SP1's territory).

---

## TD-2026-05-17-002 — shadcn/ui as the L1 component primitive layer

```yaml
---
adr_id: TD-2026-05-17-002
title: "shadcn/ui as the L1 component primitive layer"
provenance_class: external_canonical
evidence:
  source_type: upstream
  upstream_id: shadcn-ui-2026-05
  section: "Overview and component list"
  quote: "shadcn/ui provides accessible, unstyled components you copy into your project."
spec_ref: N/A
status: ACCEPTED
date: 2026-05-17
---
```

### Decision

Use shadcn/ui (2026-05 freeze, 32 blessed components) as the L1 primitive layer in
`templates/L1/`. Components are copied-in (not imported from a package) per shadcn's
design. A drift-detection script (`templates/L1/_check-shadcn-drift.sh`) compares
installed files against the frozen `shadcn-registry-2026-05.snapshot.md`.

### Rationale

shadcn/ui is accessible-by-default, copy-in design avoids upstream breaking changes,
and the 32-component set covers all L2 block composition needs identified in §4.2.

### Consequences

- 32 component directories under `templates/L1/components/`.
- `time_decay_guard.sh` walks `practices-react/upstream/shadcn-registry-2026-05.snapshot.md`.
- Drift > 90 days from the frozen snapshot flags FAIL.

---

## TD-2026-05-17-003 — TanStack Query v5 for server-state management

```yaml
---
adr_id: TD-2026-05-17-003
title: "TanStack Query v5 for server-state management"
provenance_class: external_canonical
evidence:
  source_type: upstream
  upstream_id: tanstack-query-v5
  section: "Overview — stale-while-revalidate"
  quote: "TanStack Query makes fetching, caching, synchronizing and updating async state trivial."
spec_ref: N/A
status: ACCEPTED
date: 2026-05-17
---
```

### Decision

Adopt TanStack Query v5 (`@tanstack/react-query`) for all server-state management in
`templates/L2/` and `templates/L4/`. Client-only state uses Zustand (TD-004).

### Rationale

v5 introduces a streamlined API (single `useQuery` signature), first-class support for
React 19 Suspense, and removes the separate `QueryClientProvider` ceremony. SWR and
manual `useEffect` fetching are forbidden in L2+ layers.

### Consequences

- `frontend/package.json` adds `@tanstack/react-query@^5`.
- L2 feature blocks that fetch data import `useQuery` / `useMutation` from this package.
- No hand-rolled fetch wrappers in templates.

---

## TD-2026-05-17-004 — Zustand for client-state management

```yaml
---
adr_id: TD-2026-05-17-004
title: "Zustand for client-state management"
provenance_class: external_canonical
evidence:
  source_type: external
  citation: "Zustand documentation — Guides: Updating state. pmndrs/zustand on GitHub."
  url: "https://docs.pmnd.rs/zustand/guides/updating-state"
spec_ref: N/A
status: ACCEPTED
date: 2026-05-17
---
```

### Decision

Adopt Zustand for client-side UI state (auth token, theme, notification flags). Server
state (remote data) is owned by TanStack Query (TD-003). Redux and Context-as-store
patterns are forbidden in templates.

### Rationale

Zustand is framework-agnostic, minimal boilerplate, and supports selective subscriptions
without Provider wrapping. It is already present in the frontend baseline and is
compatible with React 19 concurrent features.

### Consequences

- Zustand stores live in `templates/L2/stores/` or `templates/L4/<domain>/stores/`.
- `practices-react/rules/` may gain a rule capping store complexity (SP7, if empirically needed).

---

## TD-2026-05-17-005 — Zod for runtime schema validation

```yaml
---
adr_id: TD-2026-05-17-005
title: "Zod for runtime schema validation"
provenance_class: external_canonical
evidence:
  source_type: external
  citation: "Zod documentation — Basic usage. colinhacks/zod on GitHub."
  url: "https://zod.dev/?id=basic-usage"
spec_ref: N/A
status: ACCEPTED
date: 2026-05-17
---
```

### Decision

Adopt Zod for all runtime schema validation in `templates/L2/` and `templates/L4/`:
form schemas, API response parsing, and environment variable validation.
`yup` and manual `typeof` guards are forbidden in templates.

### Rationale

Zod provides TypeScript-first schemas that double as runtime validators, eliminating
duplication between TypeScript types and validation logic. React Hook Form v7 integrates
with Zod via `@hookform/resolvers/zod` without ceremony.

### Consequences

- `zod` added to `frontend/package.json`.
- All form schemas in templates use `z.object({...})` shape.
- API response parsing in L2/L4 uses `schema.parse()` with explicit error propagation.

---

## TD-2026-05-17-006 — Playwright + Vitest + MSW as the testing triad

```yaml
---
adr_id: TD-2026-05-17-006
title: "Playwright + Vitest + MSW as the frontend testing triad"
provenance_class: external_canonical
evidence:
  source_type: external
  citation: "Playwright documentation — Getting started. playwright.dev."
  url: "https://playwright.dev/docs/intro"
spec_ref: N/A
status: ACCEPTED
date: 2026-05-17
---
```

### Decision

The frontend testing triad is:
- **Playwright** — E2E tests that hit a real backend (RANDOM_PORT spring context).
- **Vitest** — Unit and component tests; isolated from network.
- **MSW (Mock Service Worker)** — Network interception for Vitest isolation only; never
  used in Playwright tests which hit the real backend.

MockMvc is forbidden for backend-adjacent frontend tests (CLAUDE.md anti-pattern).

### Rationale

This triad mirrors the backend's RestAssured (black-box HTTP) philosophy. Playwright
provides the black-box assertion; Vitest provides fast unit coverage; MSW keeps unit
tests hermetic without mocking at the module level.

### Consequences

- `@playwright/test`, `vitest`, `msw` in `frontend/package.json`.
- SP1 acceptance gate: `playwright test` must pass on the migrated auth flow.
- SP3 fixtures use bash (not Playwright); the guard tests are shell-level, not E2E.

---

## TD-2026-05-17-007 — Frontend Spec Trio schema (page-compliance + ui-contract + ui-manifest)

```yaml
---
adr_id: TD-2026-05-17-007
title: "Frontend Spec Trio schema as the frontend analog of the backend Spec Trio"
provenance_class: internal_design
evidence:
  source_type: internal
  source_ref: docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter4.md
  rationale: |
    The Frontend Spec Trio (page-compliance spec + UI contract + UI manifest) is an
    internal design decision with no external standard governing composition-kit
    spec topology. The design mirrors the backend Spec Trio
    (specs/auth-asvs-l1.yaml + contracts/auth-openapi.yaml + blueprints/auth-manifest.yaml)
    symmetrically. Empirical anchor: the Payment domain L4 sealed sub-agent PASS
    (11/11 MUST + 6/6 SHOULD) validated the backend Spec Trio approach; the frontend
    mirror applies the same pattern.
spec_ref: docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter4.md#4.8
status: ACCEPTED
date: 2026-05-17
---
```

### Decision

Introduce three schema artifacts per frontend domain:
1. `specs/<domain>-frontend-l0.yaml` — page compliance items (§4.8.1).
2. `contracts/<domain>-ui.yaml` — route-level UI contract (§4.8.2).
3. `blueprints/<domain>-ui-manifest.yaml` — a11y + CWV + motion policy (§4.8.3).

These are validated by `trio_integrity_guard.sh` (§4.8.4).

### Rationale

The backend Spec Trio + RestAssured verification loop was empirically validated by the
Payment domain. The Frontend Spec Trio applies the same "spec-before-code" discipline
to frontend domains, enabling `trio_integrity_guard.sh` to binary-verify coverage.

### Consequences

- `trio_integrity_guard.sh` and `cross_trio_guard.sh` implemented in SP3.
- SP2 ships the first concrete instances for `auth` domain.
- `frontend_only` mode (§4.8.4) allows domains like `practices` that have no backend API.

---

## TD-2026-05-17-008 — 3-tier skill topology (Tier-1 exposed / Tier-2 axes / Tier-3 leaf guards)

```yaml
---
adr_id: TD-2026-05-17-008
title: "3-tier skill topology: 3 Tier-1 user commands + 8 Tier-2 axes + 6 Tier-3 leaf guards"
provenance_class: internal_design
evidence:
  source_type: internal
  source_ref: docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter4.md
  rationale: |
    The 3-tier topology is an internal design decision. No external standard governs
    Claude Code skill hierarchy topology. The design follows Principle 3 from the PRD:
    "Few exposed surfaces, dense feedback loops underneath." Tier-1 (3 commands) keeps
    the user-facing surface minimal; Tier-2 (8 axes) maps 1:1 to pathPatterns per §4.14;
    Tier-3 (6 guards) are not pathPattern-triggered, eliminating pathPattern overlap risk
    identified in the Architect review.
spec_ref: docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter4.md#4.14
status: ACCEPTED
date: 2026-05-17
---
```

### Decision

17 SKILL.md files arranged in 3 tiers:
- **Tier-1 (3, exposed):** `/ax-transform`, `/ax-verify`, `/ax-scaffold`
- **Tier-2 (8, axes):** `/ax-verify-{java,react,shared,L1,L2,L3,L4,domain}`
- **Tier-3 (6, leaf guards):** `/ax-guard-{evidence,substance,time-decay,spec-ref,trio-integrity,cross-trio}`

Tier-3 guards are NOT pathPattern-triggered; they are invoked by Tier-2 skills only.

### Rationale

pathPattern overlap risk (a context-0 agent editing `templates/L2/blocks/LoginForm.tsx`
would trigger multiple skills if guards had pathPatterns) is eliminated by this design.
Tier-2 disambiguation table in §4.14 is the single source of truth.

### Consequences

- SP4a ships Tier-1 SKILL.md files; SP4b ships Tier-2 + Tier-3 SKILL.md wrappers.
- SP3 (this SP) ships the underlying `.sh` guard implementations that Tier-3 wraps.

---

## TD-2026-05-17-009 — templates/ directory shape (L1/L2/L3/L4/backend top-level)

```yaml
---
adr_id: TD-2026-05-17-009
title: "templates/ top-level directory shape with L1/L2/L3/L4/backend peer dirs"
provenance_class: internal_design
evidence:
  source_type: internal
  source_ref: docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter4.md
  rationale: |
    The templates/ directory shape is an internal design decision. No external
    standard governs template layer naming conventions. The L1/L2/L3/L4 naming
    mirrors the 4-layer model from §4.1-§4.4 of the PRD. The backend/ peer dir
    (§4.5) is a new addition for the 10 backend cross-cutting templates identified
    in the Architect review (revision #3). The separation of layer directories
    enables Tier-2 skill pathPatterns to map cleanly to verification scope.
spec_ref: docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter4.md#4.5
status: ACCEPTED
date: 2026-05-17
---
```

### Decision

`templates/` has 5 peer directories:
- `L1/` — shadcn/ui primitive wrappers + design tokens manifest.
- `L2/` — ax-template feature blocks (reusable across domains).
- `L3/` — Next.js 16 page template families (7 families).
- `L4/` — domain workloads (1:1 with backend Spec Trio domains with frontend).
- `backend/` — Java cross-cutting templates (controllers, services, repositories, DTOs, error, security, config).

Plus `DECISIONS.md` (this file) and `AGENTS.md` (sentinel).

### Rationale

The L-numbering encodes the abstraction level: L1 is the most primitive, L4 is the most
domain-specific. This naming is consistent with the `practices/` rule tier convention.
`backend/` is a peer (not under L1-L4) because it is not a frontend layer.

### Consequences

- SP3 creates the 5 directories as empty placeholders.
- SP5 populates `L1/`; SP6 populates `L2/`; SP7 populates `L3/`; SP8-SP11 populate `L4/`.
- SP4b's `ax-verify-java` skill covers `templates/backend/**`.

---

## TD-2026-05-17-010 — Design-tokens manifest format (WCAG 2.2 + CWV-anchored)

```yaml
---
adr_id: TD-2026-05-17-010
title: "Design-tokens manifest format anchored to WCAG 2.2 contrast + CWV thresholds"
provenance_class: external_canonical
evidence:
  source_type: upstream
  upstream_id: wcag-2-2
  section: "Success Criterion 1.4.3 Contrast (Minimum)"
  quote: "The visual presentation of text and images of text has a contrast ratio of at least 4.5:1."
spec_ref: N/A
status: ACCEPTED
date: 2026-05-17
---
```

### Decision

The design-tokens manifest (`templates/L1/tokens/tokens.css`) uses CSS custom properties
(`--color-*`, `--text-*`, `--space-*`, `--duration-*`) anchored to:
- **WCAG 2.2 SC 1.4.3** — minimum contrast ratio 4.5:1 for normal text.
- **CWV 2026 targets** — LCP ≤ 2500ms, INP ≤ 200ms, CLS ≤ 0.1 (per `cwv-2026` snapshot).

The `blueprints/<domain>-ui-manifest.yaml` schema (`contrast_min: 4.5`) enforces the
WCAG threshold at the domain level; `trio_integrity_guard.sh` validates the field.

### Rationale

External anchoring (WCAG + CWV) prevents arbitrary token choices from violating
accessibility and performance contracts. The guard enforces this at merge time, not
code review time.

### Consequences

- `blueprints/<domain>-ui-manifest.yaml` must carry `a11y.contrast_min: 4.5` (≥).
- `cwv.lcp_ms` ≤ 2500, `cwv.inp_ms` ≤ 200, `cwv.cls` ≤ 0.1.
- SP5 ships the concrete token manifest; `trio_integrity_guard.sh` validates thresholds.
