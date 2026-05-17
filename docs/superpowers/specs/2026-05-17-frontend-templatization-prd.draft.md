# Frontend Templatization + Full-Stack Methodology Extension — PRD (draft)

> **Status:** Planner draft for `/ralplan` consensus loop (Step 1).
> **Owner:** Planner agent (oh-my-claudecode:planner).
> **Date:** 2026-05-17.
> **Repo:** `ax-template`.
> **Audience:** Architect (Step 3), Critic / Codex (Step 4), maintainer (Step 6).
> **Format contract:** RALPLAN-DR. Pre-aligned decisions (Sections A–E of the user
> brief) are treated as **constraints**, not options. Open options live only in the
> SEQUENCING / GRANULARITY axis (see § RALPLAN-DR Summary below).

---

## RALPLAN-DR Summary

### Principles (5)

1. **Composition kit, not single product.** Every artifact must be independently
   adoptable by a fork; rejecting one layer must not break the others. (Anchors
   to `CLAUDE.md` § Project Vision and `skills/ax-transform/SKILL.md`.)
2. **Spec-before-code, evidence-anchored.** Every template artifact (L1…L4 +
   frontend Spec Trio + skills) carries an `evidence:` block; every rule traces
   to a snapshot in `practices*/upstream/` or an external citation; every
   decision is captured in a DECISIONS.md ADR.
3. **Few exposed surfaces, dense feedback loops underneath.** User-facing skill
   surface stays small (3 Tier-1 commands). Each leaf node is itself an
   invocable skill with bundled scripts and a workflow checklist (Anthropic
   workflows-and-feedback-loops pattern). Raw build tools (`./gradlew`,
   `vitest`, `playwright`, guard `.sh`) are bundled *inside* skills, not
   exposed as surface.
4. **Java/Spring and React/Next.js are equal partners.** No archive, no
   deprecation, no "frozen" status. Catalog growth on either side is normal
   activity and increases system value.
5. **Binary verification per axis.** Every SP terminates when a Tier-1/2/3
   skill returns `exit 0`. No SP is "done" on prose alone.

### Decision Drivers (top 3)

1. **AI agent self-discoverability.** A context-0 agent dropped into a forked
   repo must reach a green build by reading AGENTS.md / SKILL.md and invoking
   the right skill — without human hand-holding. (Payment L4 sealed sub-agent
   PASS established this as the empirical bar; the frontend templatization
   must clear the same bar.)
2. **Migration safety.** The Vite frontend is a working reference workload
   (auth flow lives there). Switching to Next.js 16 App Router cannot drop
   the auth flow on the floor mid-cycle; the migration must keep
   `./gradlew testAsvs` green and not break the existing OAuth callbacks.
3. **Catalog convergence cost.** Adding rules, snapshots, ADRs, skills, and
   guards together is multiplicative work. The plan must minimize the number
   of times a developer / agent has to re-cross-check the same artifact.

### Viable Options (sequencing / granularity axis only — Sections A–E are
constraints, not options)

The pre-confirmed decisions fix WHAT we build. The remaining real options
concern **how to sequence and granulate** the 7–12 sub-projects.

- **Option A — Strict bottom-up (infra → L1 → L2 → L3 → L4).**
  Build Next.js migration + skills + DECISIONS infra first, then L1 (shadcn),
  then L2 feature blocks, then L3 page templates, then L4 domain workloads
  last. Pros: each layer is fully stable before the next consumes it; aligns
  cleanly with the layer dependency graph; matches Payment's P0…P11 cadence.
  Cons: late integration risk — L4 only proves the stack at the very end; if
  L1/L2 design choices are wrong, late discovery is expensive; longer wall
  time before any user-visible result.

- **Option B — Vertical-slice first (one domain end-to-end, then horizontalize).**
  Migrate Next.js + ship full auth domain L1+L2+L3+L4 end-to-end first, then
  extract the L1/L2/L3 abstractions for the remaining 3 domains.
  Pros: fastest path to a working full-stack auth reference; surfaces
  L1↔L2↔L3↔L4 boundary disputes immediately; matches "vibe-driven" template
  evolution. Cons: abstractions extracted from a single domain risk being
  over-fitted to auth and breaking when applied to crud/payment/practices;
  the catalog grows by 1 domain only at first, weakening empirical claim.

- **Option C — Hybrid: infra → vertical-slice → horizontalize (RECOMMENDED).**
  Phase 1: ship Next.js migration + 3-tier skills + frontend Spec Trio
  schema + DECISIONS infra + L1 shadcn integration (foundation). Phase 2:
  ship auth domain as the first vertical L1+L2+L3+L4 reference, extracting
  L2/L3 catalog entries as it goes. Phase 3: horizontalize across crud +
  payment + practices in parallel SPs, each consuming the now-stable L1/L2/L3
  catalog. Phase 4: end-to-end binary gate.
  Pros: foundation is built once, auth proves the abstractions in one
  vertical, then 3 parallel horizontal SPs scale the catalog with high
  empirical validation (4 domains = same bar Payment cleared). Cons:
  3-phase sequencing means Phase 2 cannot start until Phase 1 lands, so
  parallelism is delayed by one SP-chain length; if the L2 abstractions
  extracted from auth turn out wrong, Phase 3 has to re-edit L2.

- **Option D — Big-bang Next.js coexistence (Vite + Next.js side-by-side
  during transition).** Run both frameworks under `frontend-next/` while
  `frontend/` stays Vite, then cut over at the end. Pros: zero migration
  downtime; old Vite app keeps working through development. Cons: two
  parallel dependency trees, two ESLint configs, two test runners; doubles
  CI surface and AGENTS.md sentinel scope; explicitly contradicts the
  "one stack" composition kit framing. **Invalidation rationale:** the
  pre-aligned decision (Section A) locks Next.js 16 App Router as **the**
  frontend stack, not a parallel one. Coexistence violates the equal-partner
  principle by carrying a deprecated runtime indefinitely.

### Recommended: Option C (hybrid)

**Rationale:** matches the empirical Payment domain cadence (P0…P11 with
foundation → vertical → horizontalize → integration), preserves the
"composition kit + few-exposed-surfaces" framing, and keeps `./gradlew test`
+ `npm run test` green throughout. Option A delays integration risk too far;
Option B over-fits abstractions to a single domain; Option D violates a
pre-aligned constraint.

---

## 1. Context

### 1.1 What ax-template is today (baseline at HEAD)

| Asset | Count / state |
|---|---|
| Backend domains (Spring Boot, `backend/src/main/java/com/ax/template/`) | 7: `auth`, `crud`, `payment`, `practices`, `ratelimit`, `security`, `user` (the canonical 4 reference workloads are auth/crud/payment/practices, with ratelimit/security/user as cross-cutting support). |
| Spec Trio coverage | `specs/{auth-asvs-l1,crud-security,payment-l0,ratelimit-l0,react-practices-l0,spring-practices-l0}.yaml` + 4 OpenAPI contracts + 4 policy manifests. |
| `practices/` catalog (Java) | 68 rules across 21 categories. `evidence_guard`, `spec_ref_guard`, `substance_guard`, `time_decay_guard` all PASS at HEAD. |
| `practices-react/` catalog (React/Next.js) | 68 rules across 8 families (`async-`, `bundle-`, `server-`, `client-`, `rerender-`, `rendering-`, `js-`, `advanced-`, `nextjs-`). Vercel-seed → 4-phase curated. ESLint plugin `@ax/eslint-plugin-ax` ships 7 custom rules. |
| Frontend (`frontend/`) | **Vite 6 + React 19 + react-router-dom v7**. Only 1 domain (`auth`) wired: `LoginPage`, `SignupPage`, `OAuthCallbackPage`, `VerifyPage`, `DashboardPage`, `ProtectedRoute`. No Next.js, no shadcn, no TanStack Query, no Zod, no design tokens manifest. |
| Methodology | `METHODOLOGY.md` 5-step + Appendix A (worked examples) + Appendix B (dry-run checklist) + Appendix C (12-step procedure for adding a new domain). **Currently backend-only** in the worked examples; frontend recipe (Appendix C Recipe B) is forward-compatible but not yet exercised. |
| Skills | `skills/ax-transform/` only at top level. `practices/SKILL.md` and `practices-react/SKILL.md` are sub-system skills triggered by pathPatterns. No verification skills, no per-layer skills, no per-concern guard skills. |
| AGENTS.md sentinels | `practices/AGENTS.md` (sha `4f7e804a…`) + `practices-react/AGENTS.md` (sha `017c96a0…`). Both auto-regenerated. |

### 1.2 What this initiative ships

The Frontend Templatization initiative does four things in one cycle:

1. **Migrate** the frontend from Vite → Next.js 16 App Router (one-time
   transition, no parallel coexistence).
2. **Introduce 4 frontend template layers** (L1 UI primitives, L2 feature
   blocks, L3 page templates, L4 domain workloads) with explicit ownership
   and evidence anchoring.
3. **Extend the methodology to full-stack** by adding the frontend Spec Trio
   (`specs/{domain}-frontend-l0.yaml`, `contracts/{domain}-ui.yaml`,
   `blueprints/{domain}-ui-manifest.yaml`) and lifting `METHODOLOGY.md`'s
   Appendix C Recipe B from forward-compatible to exercised-and-validated.
4. **Reshape the skill topology** from "one mono-skill" to a 3-tier graph
   (3 Tier-1 exposed + 8 Tier-2 axes + 6 Tier-3 leaf guards = ~17 skills),
   each with its own workflow checklist and bundled scripts.

### 1.3 Why now

| Driver | Evidence |
|---|---|
| Payment empirical validation passed | 22/22 stories pass, L4 sealed sub-agent 11/11 MUST + 6/6 SHOULD. Catalog discoverable to context-0 agent. Backend side is stable enough to extend. |
| Frontend is the asymmetric weak link | 68 React rules + 7 ESLint rules exist, but only 1 domain (auth) consumes them, and the frontend stack (Vite + react-router-dom v7) diverges from the React-19/Next.js-16 framing of the rules themselves. |
| External validation pilot already pointed downstream | `practices-react/SKILL.md` pathPatterns now include downstream-consumer paths (`app/**`, `pages/**`, `src/app/**`). The catalog is ready to be consumed by a real Next.js fork; the template just needs to *be* one. |
| Skill surface bloat risk | If we kept adding capabilities to `/ax-transform` as one skill, the prompt budget and discoverability of each capability would collapse. The 3-tier topology heads this off before it materializes. |

---

## 2. Work Objectives

| # | Objective | Acceptance signal |
|---|-----------|-------------------|
| O1 | Vite → Next.js 16 App Router migration, lossless for the existing auth domain. | `cd frontend && npm run build` exits 0; `npm run test:auth` exits 0; OAuth callbacks resolve via Next.js App Router segments; `./gradlew testAsvs` stays green. |
| O2 | 4 frontend template layers introduced, each with evidence and DECISIONS entry. | `templates/{L1,L2,L3,L4}/` exists; every artifact under `templates/**` has an `evidence:` block; `evidence_guard.sh templates/` exits 0. |
| O3 | Frontend Spec Trio schema added; METHODOLOGY.md Appendix A extended with frontend worked example. | `specs/auth-frontend-l0.yaml` + `contracts/auth-ui.yaml` + `blueprints/auth-ui-manifest.yaml` exist; `swagger-cli validate contracts/auth-ui.yaml` exits 0; METHODOLOGY.md table updated. |
| O4 | 3-tier skill topology installed; 17 skills total. | `ls skills/ax-{transform,verify,scaffold}/`, `ls skills/ax-verify-{java,react,shared,L1,L2,L3,L4,domain}/`, `ls skills/ax-guard-{evidence,substance,time-decay,spec-ref,trio-integrity,cross-trio}/`. `/ax-verify` invocation traverses Tier-2 and Tier-3. |
| O5 | 4 domain workloads (auth/crud/payment/practices) end-to-end on frontend. | Each domain has L4 directory under `templates/L4/<domain>/`; `npm run test:<domain>` + Playwright e2e + `./gradlew test<Domain>` all exit 0. |
| O6 | Decision Provenance Trail (DECISIONS.md ADRs + guard coverage of `templates/**`). | `templates/DECISIONS.md` exists with ≥ 6 ADRs (Next.js, shadcn, TanStack Query, Zustand, Zod, Playwright); 4 guards extended to walk `templates/**`. |
| O7 | AGENTS.md sentinel for frontend templates auto-regenerated. | `templates/AGENTS.md` exists; sha256 sentinel matches `templates/generate_agents.sh` output. |
| O8 | Single binary gate proves all of the above. | `bash skills/ax-verify/scripts/run-all.sh` exits 0 (the new Tier-1 verify skill). |

---

## 3. Guardrails

### 3.1 Must Have

- One-stack frontend: Next.js 16 App Router only. No coexistence period.
- Every artifact under `templates/**` has `evidence:` frontmatter (upstream or external citation).
- Every architectural choice (Next.js, shadcn, TanStack Query, Zustand, Zod, Playwright, design-token shape, motion policy) has an ADR in `templates/DECISIONS.md` of the form **TD-2026-05-17-NNN**.
- 3-tier skill topology has ≤ 3 user-facing commands (Tier-1 only).
- Each Tier-2/Tier-3 skill bundles its scripts; raw `./gradlew`, `vitest`, `playwright`, `.sh` are not user-facing surface.
- `practices-react/` and `practices/` catalogs stay green throughout (each PR keeps all 4 guards passing for each catalog).
- Backend remains untouched except for additive cross-cutting (e.g., if a new generic rule is extracted from frontend pattern work, it lands via the existing catalog growth protocol — `practices/MAINTAINER.md`).
- Use RestAssured for any backend HTTP test added/touched. Frontend uses Playwright (E2E) + Vitest + MSW (unit). No MockMvc.
- TDD anchor per SP: a failing test (RED) must exist before any implementation file is touched in that SP.

### 3.2 Must NOT

- **No governance docs.** No promotion-gate files, no "evidence bundle for the bundle", no draft→curated→stable workflow papers. (CLAUDE.md anti-pattern.)
- **No Vite coexistence.** Vite goes away in SP1.
- **No new top-level Tier-1 skill beyond `/ax-transform`, `/ax-verify`, `/ax-scaffold`.** (3 max, hard cap.)
- **No domain-specific subdirectory under `practices/rules/` or `practices-react/rules/`.** Catalog stays flat-by-category. (METHODOLOGY.md Appendix C anti-pattern.)
- **No mocking the backend.** Frontend integration tests hit the real Spring Boot `RANDOM_PORT` instance the same way `testAsvs` does. MSW is for unit-test-level isolation only.
- **No design tokens hardcoded in components.** All tokens come from `templates/L1/tokens/` manifest.
- **No skill that wraps another skill without adding a workflow checklist.** A wrapper without a checklist is a sign the wrapped skill should have been exposed directly.

---

## 4. Component Inventory (complete enumeration)

### 4.1 Frontend — L1 UI Primitives (shadcn/ui adoption)

We adopt shadcn/ui (Apache-2.0, Radix-based). The shadcn library itself is
external-owned; ax-template owns:
(a) the **install manifest** (which shadcn components are blessed and at what version),
(b) the **design-tokens** layer (`templates/L1/tokens/`),
(c) the **wrapper exports** (`templates/L1/components/`) that re-export shadcn with our tokens applied.

**Blessed shadcn components (install list, ~32 items):**

| Layer | Components |
|---|---|
| Form primitives (10) | `button`, `input`, `textarea`, `label`, `select`, `checkbox`, `radio-group`, `switch`, `slider`, `form` |
| Display primitives (8) | `card`, `badge`, `avatar`, `separator`, `skeleton`, `progress`, `aspect-ratio`, `scroll-area` |
| Layout primitives (4) | `tabs`, `accordion`, `collapsible`, `resizable` |
| Overlay primitives (6) | `dialog`, `alert-dialog`, `popover`, `tooltip`, `hover-card`, `sheet` |
| Feedback primitives (4) | `toast` (Sonner), `alert`, `command`, `dropdown-menu` |

**Design tokens manifest (`templates/L1/tokens/tokens.yaml`):**
colors (oklch), typography (`--text-base`, `--text-hero`, fluid clamp), spacing
(`--space-section`), motion (`--duration-fast/normal`, `--ease-out-expo`),
shadow tiers, radius tiers. Anchored to `blueprints/ui-tokens-manifest.yaml`.

**Wrapper exports (`templates/L1/components/<component>.tsx`):** thin re-export +
typed prop forward + injection of tokens via CSS variables. No business logic.

### 4.2 Frontend — L2 Feature Blocks (ax-template-owned)

`templates/L2/blocks/` directory. Each block is one .tsx file + one
co-located `.stories.tsx` (optional, for Storybook later) + one
`.spec.test.tsx` (Vitest + Testing Library + MSW).

| Domain axis | Blocks (count) |
|---|---|
| Auth (6) | `LoginForm`, `SignupForm`, `OAuthCallbackPanel`, `EmailVerifyPanel`, `PasswordResetForm`, `ProtectedRoute` |
| CRUD generic (8) | `DataTable`, `FilterBar`, `Pagination`, `EmptyState`, `LoadingState`, `ErrorState`, `CrudCreateForm`, `CrudEditForm`, `CrudDeleteConfirm` *(9 — counting `LoadingState` and `ErrorState` separately, both required to mirror RFC 7807 ProblemDetail rendering)* |
| Payment (5) | `PaymentCheckoutForm`, `PaymentMethodPicker`, `PaymentStatusBadge`, `PaymentReceiptCard`, `IdempotencyKeyHandle` |
| Practices viewer (3) | `RuleCatalogTable`, `RuleDetailPanel`, `EvidenceCitationCard` |
| Cross-cutting (4) | `AppHeader`, `AppSidebar`, `ThemeSwitcher`, `BreadcrumbTrail` |

**Total L2 blocks: ~26.** Each block consumes only L1 + design tokens.
Each block carries `evidence:` frontmatter pointing to the React-19 / Next.js-16
docs section that justifies its pattern (e.g., `LoginForm` cites Next.js Server
Actions docs + React 19 `useActionState`).

### 4.3 Frontend — L3 Page Templates (Next.js App Router skeletons)

`templates/L3/pages/` directory. Each page template is a complete
`app/<segment>/page.tsx` + `layout.tsx` + `loading.tsx` + `error.tsx` set, plus
a `README.md` describing the slot contract (what L2 blocks plug in, what data
fetches are expected, what cache policy applies).

| Template | Files | Cache strategy |
|---|---|---|
| `list-page/` | `page.tsx` (Server Component, paginated fetch), `loading.tsx`, `error.tsx` | `'use cache'` with tag invalidation |
| `detail-page/` | `page.tsx` (params async, Server Component), `loading.tsx`, `error.tsx`, `not-found.tsx` | per-id cache tag |
| `create-page/` | `page.tsx` (Server Action + Client form), `loading.tsx`, `error.tsx` | no-store on POST |
| `edit-page/` | `page.tsx` (Server Action + Client form prefilled), `loading.tsx`, `error.tsx` | per-id revalidate on submit |
| `dashboard-page/` | `page.tsx` (parallel `Promise.all` fetches, Suspense boundaries), `loading.tsx`, `error.tsx` | parallel `'use cache'` |
| `auth-callback-page/` | `page.tsx` (OAuth code exchange, Server Action), `error.tsx` | no-store, dynamic |
| `error-page/` | global `error.tsx`, `global-error.tsx`, `not-found.tsx` | n/a |

**Total L3 templates: 7 families.** Each carries `evidence:` to Next.js 16 App
Router docs + WCAG 2.2 a11y section + Core Web Vitals threshold from
`blueprints/ui-cwv-manifest.yaml`.

### 4.4 Frontend — L4 Domain Workloads (vertical reference workloads)

`templates/L4/<domain>/` — one subdir per domain, 1:1 with backend Spec Trio.
Each L4 directory is a fully composed app slice that wires L1 + L2 + L3 to the
backend's existing OpenAPI contract.

| Domain | App segments | Backend coupling |
|---|---|---|
| `auth` | `/login`, `/signup`, `/verify`, `/oauth/callback/<provider>`, `/dashboard`, `/protected/*` | `contracts/auth-openapi.yaml` (14 endpoints) |
| `crud` | `/items`, `/items/[id]`, `/items/new`, `/items/[id]/edit` | `contracts/crud-openapi.yaml` (5 endpoints) |
| `payment` | `/payment/checkout`, `/payment/status/[id]`, `/payment/history` | `contracts/payment-openapi.yaml` |
| `practices` (catalog viewer) | `/practices`, `/practices/[ruleId]`, `/practices/families/[family]` | reads `practices/AGENTS.md` + `practices-react/AGENTS.md` statically at build time |

**Total L4 workloads: 4.** Each has its own frontend Spec Trio under
`specs/<domain>-frontend-l0.yaml`, `contracts/<domain>-ui.yaml`,
`blueprints/<domain>-ui-manifest.yaml`.

### 4.5 Backend cross-cutting templates

`templates/backend/` (new top-level directory for backend-side templates that
are not domain-specific):

| Template | File pattern | Purpose |
|---|---|---|
| `BaseController.java` | `templates/backend/controllers/` | Standard `ResponseEntity<T>` + RFC 7807 ProblemDetail wiring |
| `BaseService.java` | `templates/backend/services/` | Constructor injection + `@Transactional(readOnly=true)` default |
| `BaseRepository.java` | `templates/backend/repositories/` | `JpaRepository<T, Long>` + `@EntityGraph` examples |
| `RequestDto.java` skeleton | `templates/backend/dto/` | Mass-assignment guard pattern (`validation-mass-assignment-guard`) |
| `GlobalExceptionHandler.java` | `templates/backend/error/` | RFC 7807 `@ControllerAdvice` |
| `JwtFilter.java` | `templates/backend/security/` | Already in `backend/src/main/.../security/`; extract templated version |
| `RateLimitFilter.java` | `templates/backend/security/` | Caffeine-backed pattern from ratelimit domain |
| `AuditingConfig.java` | `templates/backend/config/` | `@EnableJpaAuditing` |
| `CorsConfig.java` | `templates/backend/config/` | SPA-friendly CORS |
| `OpenApiConfig.java` | `templates/backend/config/` | springdoc-openapi config |

**Total backend cross-cutting templates: 10.** No new backend domain is
introduced; these are extractions of existing patterns from the 4 reference
workloads, with `evidence:` blocks pointing to the relevant `practices/rules/`.

### 4.6 New backend rules expected (S7 generalization audit candidates)

The frontend-backend integration work will surface a small number of generic
backend rules worth promoting. Candidates (each will be audited per
`practices/MAINTAINER.md` § S7 before landing):

| Candidate rule | Family | Why |
|---|---|---|
| `web-server-action-csrf` | `web-` | Next.js Server Actions hit Spring endpoints; CSRF strategy for SPA + Server Action needs explicit rule. |
| `api-streaming-response` | `api-` | If any L3 dashboard uses streamed `Suspense`, the backend needs a documented streaming-response pattern. |
| `security-cors-allow-credentials` | `security-` | Required for OAuth callback flow with Next.js App Router. |
| `validation-request-dto-server-action-shape` | `validation-` | Server Action FormData → Spring `@Validated` DTO bridge. |

**Cap: 4 new backend rules max.** Audit may classify some as
`extend_existing` (modify existing rule rather than create new one), reducing
the count.

### 4.7 New React rules expected

Anticipated `practices-react/rules/` additions, each anchored to a
React-19 / Next.js-16 docs section:

| Candidate rule | Family | Why |
|---|---|---|
| `nextjs-server-action-validation` | `nextjs-` | Server Action input must use Zod schema, not raw FormData. |
| `nextjs-cache-tag-strategy` | `nextjs-` | `'use cache'` + tag invalidation contract per L3 list-page / detail-page. |
| `rendering-suspense-boundary-policy` | `rendering-` | Where to place `<Suspense>` in dashboard-page parallel fetches. |
| `client-tanstack-query-defaults` | `client-` | `staleTime`, `gcTime`, retry policy defaults for the catalog. |
| `client-zustand-store-shape` | `client-` | Single-store-per-feature rule (mirrors `core-singleton-no-mutable-state`). |
| `client-zod-schema-colocation` | `client-` | Schemas live next to the form they validate. |
| `rerender-server-component-default` | `rerender-` | Default-to-Server-Component, lift `'use client'` only when needed. |
| `bundle-shadcn-tree-shake` | `bundle-` | shadcn components imported one-at-a-time, never barrel. |
| `advanced-use-action-state-pattern` | `advanced-` | React 19 `useActionState` for form submission. |
| `server-route-handler-shape` | `server-` | Next.js Route Handler error envelope must match RFC 7807 from backend. |

**Cap: 10 new React rules max.** S7 audit may collapse a few via
`extend_existing`. Net catalog growth target: 5–10 new rules
(practices-react/ ≈ 68 → 73–78).

### 4.8 Frontend Spec Trio schema artifacts

| Artifact | Path | Schema shape |
|---|---|---|
| Page Compliance Spec | `specs/<domain>-frontend-l0.yaml` | YAML list of `id: <DOMAIN>-FE-<NNN>` items with `requirement`, `test_method` (Playwright test name), `verification_type` (`e2e_test` / `unit_test` / `a11y_test` / `cwv_test`), `policy_ref` |
| UI Contract | `contracts/<domain>-ui.yaml` | YAML describing routes, params, query strings, loading/error/empty states, redirect destinations |
| UI Policy Manifest | `blueprints/<domain>-ui-manifest.yaml` | design-token overrides, a11y thresholds (axe rules, contrast), Core Web Vitals budgets (LCP/INP/CLS), motion policy (`prefers-reduced-motion` handling) |

**Total schema files added in SP2:** 1 template-schema-only set + 4 per-domain
instances (auth/crud/payment/practices) = 13 files (or 15 if we also add
schema-validation `.schema.json` siblings — recommended).

### 4.9 DECISIONS.md and ADRs

`templates/DECISIONS.md` — single file at the top of `templates/` listing
ADRs in chronological order. Initial entries (filed in SP1/SP2):

| ADR id | Title |
|---|---|
| TD-2026-05-17-001 | Next.js 16 App Router as frontend stack (replaces Vite) |
| TD-2026-05-17-002 | shadcn/ui as L1 UI primitives source |
| TD-2026-05-17-003 | TanStack Query v5 as server-state library |
| TD-2026-05-17-004 | Zustand as client-state library |
| TD-2026-05-17-005 | Zod as validation library |
| TD-2026-05-17-006 | Playwright + Vitest + MSW as test stack |
| TD-2026-05-17-007 | Frontend Spec Trio schema shape (extends backend Spec Trio) |
| TD-2026-05-17-008 | 3-tier skill topology (3 Tier-1 + 8 Tier-2 + 6 Tier-3) |
| TD-2026-05-17-009 | `templates/` top-level directory shape |
| TD-2026-05-17-010 | Design-tokens manifest format (CSS custom properties + oklch + clamp typography) |

Each ADR follows the format from § 8 below.

### 4.10 Guard extensions

The 4 existing guards (`evidence_guard.sh`, `spec_ref_guard.sh`,
`substance_guard.sh`, `time_decay_guard.sh`) currently walk `practices/rules/`
and `practices-react/rules/`. SP3 extends each to also walk
`templates/L{1,2,3,4}/**/*.{md,tsx,ts,yaml}` for `evidence:` blocks. Two
**new** guards:

| Guard | What it checks |
|---|---|
| `trio_integrity_guard.sh` | For every domain with a backend `specs/<domain>-*.yaml`, a frontend `specs/<domain>-frontend-l0.yaml` must also exist (and vice versa). UI Contract route paths must reference backend OpenAPI operation IDs. |
| `cross_trio_guard.sh` | For every L4 `templates/L4/<domain>/`, the consumed L1/L2/L3 artifacts must all carry valid `evidence:` blocks. Walks the component import graph and asserts evidence coverage. |

### 4.11 Upstream snapshot additions

`practices-react/upstream/` gets new snapshots:

- `nextjs-app-router-16.snapshot.md` — App Router routing model, segments, layouts, loading/error UI
- `nextjs-server-actions-16.snapshot.md` — Server Actions API, FormData, revalidatePath/Tag
- `nextjs-use-cache-16.snapshot.md` — `'use cache'` directive semantics
- `shadcn-ui-2026-05.snapshot.md` — shadcn install model, Tailwind requirement, component contract
- `tanstack-query-v5.snapshot.md` — QueryClient defaults, suspense mode
- `wcag-2-2.snapshot.md` — WCAG 2.2 SC list
- `cwv-2026.snapshot.md` — Core Web Vitals thresholds (current LCP < 2.5s, INP < 200ms, CLS < 0.1)

All entered into `practices-react/upstream/_MANIFEST.yaml` with `fetched_at` ≤
the day of SP3 completion. `time_decay_guard.sh` enforces 90-day refresh.

### 4.12 Skills inventory (3-tier topology, 17 skills)

#### Tier-1 (exposed, 3)

| Skill | Path | Purpose | Bundled scripts |
|---|---|---|---|
| `/ax-transform` (extended) | `skills/ax-transform/SKILL.md` | Entry point: bootstrap a fork OR transform an existing repo. Now full-stack-aware. | wraps `/ax-scaffold` + `/ax-verify`; reads CLAUDE.md + METHODOLOGY.md |
| `/ax-verify` (NEW) | `skills/ax-verify/SKILL.md` | Single binary "is this fork green?" gate. Delegates to all Tier-2 axes. | `scripts/run-all.sh` (chains `./gradlew test` + `npm run test` + 6 guards + Tier-2 skills) |
| `/ax-scaffold` (NEW) | `skills/ax-scaffold/SKILL.md` | Scaffold a new L4 domain workload (calls METHODOLOGY.md Appendix C S1–S12). | `scripts/new-domain.sh <domain>` |

#### Tier-2 (mid, 8 — language × layer × domain axes)

| Skill | Path | Purpose | Bundled scripts |
|---|---|---|---|
| `/ax-verify-java` | `skills/ax-verify-java/SKILL.md` | Run all Java/Spring catalog gates. | wraps `./gradlew test{Asvs,Crud,Payment,Practices}` + practices guards |
| `/ax-verify-react` | `skills/ax-verify-react/SKILL.md` | Run all React/Next.js catalog gates + frontend build. | wraps `npm run build` + `npm run test` + `playwright test` + practices-react guards |
| `/ax-verify-shared` | `skills/ax-verify-shared/SKILL.md` | Cross-language integrity: trio_integrity + cross_trio + AGENTS.md sentinel match. | wraps `trio_integrity_guard.sh` + `cross_trio_guard.sh` + sha256 check |
| `/ax-verify-L1` | `skills/ax-verify-L1/SKILL.md` | Design tokens manifest valid, shadcn install list matches blessed components. | `scripts/L1-check.sh` |
| `/ax-verify-L2` | `skills/ax-verify-L2/SKILL.md` | All L2 feature blocks have `.spec.test.tsx`, evidence, and pass Vitest. | `scripts/L2-check.sh` + `vitest run templates/L2/` |
| `/ax-verify-L3` | `skills/ax-verify-L3/SKILL.md` | All L3 page templates compile under Next.js, slot contracts documented. | `scripts/L3-check.sh` + `next build` on a smoke fixture |
| `/ax-verify-L4` | `skills/ax-verify-L4/SKILL.md` | All L4 domain workloads pass their full-stack e2e gate. | wraps domain-specific gates per § 4.12.bonus below |
| `/ax-verify-domain` | `skills/ax-verify-domain/SKILL.md` | Verify a single named domain end-to-end (`/ax-verify-domain payment`). | dispatches per-domain |

#### Tier-3 (leaf guards, 6 — concern axis)

| Skill | Path | Purpose | Bundled scripts |
|---|---|---|---|
| `/ax-guard-evidence` | `skills/ax-guard-evidence/SKILL.md` | Every artifact has `evidence:` block (extended to `templates/**`). | `practices/evals/evidence_guard.sh` (extended) |
| `/ax-guard-substance` | `skills/ax-guard-substance/SKILL.md` | No placeholder evidence; quoted text matches snapshot. | `practices/evals/substance_guard.sh` (extended) |
| `/ax-guard-time-decay` | `skills/ax-guard-time-decay/SKILL.md` | Snapshots are < 90 days old. | `practices/evals/time_decay_guard.sh` (extended) |
| `/ax-guard-spec-ref` | `skills/ax-guard-spec-ref/SKILL.md` | Every rule and template artifact carries `spec_ref`. | `practices/evals/spec_ref_guard.sh` (extended) |
| `/ax-guard-trio-integrity` | `skills/ax-guard-trio-integrity/SKILL.md` | NEW — frontend ↔ backend Spec Trio symmetric. | `practices/evals/trio_integrity_guard.sh` (new) |
| `/ax-guard-cross-trio` | `skills/ax-guard-cross-trio/SKILL.md` | NEW — L4 imports of L1/L2/L3 all evidence-covered. | `practices/evals/cross_trio_guard.sh` (new) |

#### Inter-skill invocation graph

```
/ax-transform  ──(scaffold)──▶  /ax-scaffold
                │
                └──(verify)──▶  /ax-verify
                                  │
                  ┌───────────────┼───────────────┬──────────────┐
                  ▼               ▼               ▼              ▼
           /ax-verify-java  /ax-verify-react  /ax-verify-shared  /ax-verify-domain
                  │               │               │                │
                  ▼               ▼               ▼                ▼
          (per-layer)   /ax-verify-{L1,L2,L3,L4}  (per-concern Tier-3 guards)
                                  │
                                  ▼
                  /ax-guard-{evidence,substance,time-decay,spec-ref,trio-integrity,cross-trio}
```

Each Tier-2 skill is independently invocable; Tier-3 guards are leaves with
no further dispatch. Bundled scripts inside Tier-3 are the actual `.sh` files.

### 4.13 Methodology document changes

- `METHODOLOGY.md` Appendix A — add row "**A.3 Frontend Templatization (full-stack first instance)**" mirroring A.1 / A.2 layout.
- `METHODOLOGY.md` Appendix B — add 5 new dry-run checklist items for the frontend Spec Trio.
- `METHODOLOGY.md` Appendix C — promote Recipe B (React/Next.js) from forward-compatible to exercised; add a worked example pointing at the auth-frontend L4.
- `CLAUDE.md` — update "Architecture" tree to include `templates/` and `skills/ax-{verify,scaffold,…}/`.
- `README.md` — update composition-kit description to mention 4 template layers + 3-tier skill topology.

---

## 5. Implementation Plan (Sub-Projects)

12 SPs grouped into 4 phases. Each SP has a single Tier-2 verify skill it
terminates on.

### Phase 1 — Foundation (SP1 → SP4)

#### SP1 — Vite → Next.js 16 migration + 3-tier skill scaffolding bootstrap

- **Inputs:** current `frontend/`, `practices-react/SKILL.md`, Next.js 16 App
  Router docs (snapshot to be added in SP3).
- **Deliverables:**
  - `frontend/` migrated to Next.js 16 App Router (`app/` directory, server
    components by default, TypeScript strict).
  - `frontend/package.json` updated: drop `vite`, `@vitejs/plugin-react`,
    `react-router-dom`; add `next`, `@tanstack/react-query`, `zod`,
    `shadcn`-related deps; keep `zustand`, `vitest`, `msw`, `@playwright/test`.
  - `skills/ax-{transform,verify,scaffold}/SKILL.md` stubs (Tier-1, 3 files).
  - `skills/ax-verify-{java,react,shared,L1,L2,L3,L4,domain}/SKILL.md` stubs
    (Tier-2, 8 files).
  - `skills/ax-guard-{evidence,substance,time-decay,spec-ref,trio-integrity,cross-trio}/SKILL.md`
    stubs (Tier-3, 6 files). The 4 existing guards keep their original `.sh`
    paths; the 2 new guards have placeholder `.sh` files that `exit 0` until
    SP3 fills them in.
  - Existing auth pages (`LoginPage`, `SignupPage`, `OAuthCallbackPage`,
    `VerifyPage`, `DashboardPage`) ported to `app/(auth)/…/page.tsx`
    segments.
- **Acceptance:** `cd frontend && npm run build` exits 0; `npm run test`
  exits 0; auth pages render under `next start`; `./gradlew testAsvs` stays
  green; `ls skills/` shows 17 SKILL.md files.
- **Verify command:** `/ax-verify-react` (must turn green by end of SP1).
- **Agent count for /team:** 1 lead + 2 workers (lead handles Next.js
  migration; worker A ports auth pages; worker B writes skill stubs).
- **TDD anchor:** `frontend/tests/auth/login-flow.spec.ts` (Playwright) —
  expects the same login round-trip as the Vite version. RED before
  migration, GREEN after.
- **Risks + mitigation:**
  - **R:** OAuth callback URLs change shape under App Router segments.
    **M:** keep route paths identical (`/oauth/callback/:provider`) via the
    `app/oauth/callback/[provider]/page.tsx` dynamic segment.
  - **R:** `react-router-dom`-style links break. **M:** sweep replaces
    `<Link to=>` with Next.js `<Link href=>` in one commit, lint passes.

#### SP2 — Frontend Spec Trio schema + METHODOLOGY.md extension

- **Inputs:** SP1 done; backend Spec Trio files (`specs/auth-asvs-l1.yaml` etc.).
- **Deliverables:**
  - `specs/templates/page-compliance-spec.schema.yaml` — meta-schema for `<domain>-frontend-l0.yaml`.
  - `contracts/templates/ui-contract.schema.yaml` — meta-schema.
  - `blueprints/templates/ui-manifest.schema.yaml` — meta-schema.
  - `specs/auth-frontend-l0.yaml` (first concrete instance, ≥ 10 items).
  - `contracts/auth-ui.yaml`.
  - `blueprints/auth-ui-manifest.yaml`.
  - `METHODOLOGY.md` Appendix A.3 added; Appendix B amended; Appendix C
    Recipe B promoted.
- **Acceptance:** `swagger-cli validate contracts/auth-ui.yaml` exits 0;
  YAML lint of all 3 schemas + 3 instances exits 0; METHODOLOGY.md updated
  diff applied.
- **Verify command:** `/ax-verify-shared` (calls `trio_integrity_guard.sh`,
  which by SP2 only needs to check existence + parseability).
- **Agent count:** 1 lead + 1 worker.
- **TDD anchor:** `frontend/tests/auth/spec-trio-coverage.spec.ts` — asserts
  every `AUTH-FE-NNN` item in `specs/auth-frontend-l0.yaml` has a matching
  Playwright test. RED until tests written.
- **Risks:**
  - **R:** Schema bloat. **M:** cap meta-schema at ≤ 200 lines per file.

#### SP3 — Decision Provenance Trail infra + guard extensions

- **Inputs:** SP1 stubs, SP2 schemas.
- **Deliverables:**
  - `templates/` top-level directory created (`L1/`, `L2/`, `L3/`, `L4/`
    subdirs as placeholders).
  - `templates/DECISIONS.md` with TD-2026-05-17-001..010 ADRs filed.
  - `templates/generate_agents.sh` — produces `templates/AGENTS.md` with
    sha256 sentinel.
  - 4 existing guards (`evidence`, `substance`, `time_decay`, `spec_ref`)
    extended to walk `templates/**`. The extension lives in
    `practices/evals/*.sh` so all existing infra reuses it.
  - 2 new guards (`trio_integrity_guard.sh`, `cross_trio_guard.sh`)
    implemented in `practices/evals/`.
  - 7 new upstream snapshots added under `practices-react/upstream/` with
    `_MANIFEST.yaml` entries.
- **Acceptance:** all 6 guards exit 0 when run from repo root; `templates/AGENTS.md` sentinel matches; `practices-react/evals/run.sh` exits 0 (no time-decay regression).
- **Verify command:** `/ax-verify-shared` (full version).
- **Agent count:** 1 lead + 1 worker.
- **TDD anchor:** add a deliberate VIOLATION fixture (`templates/_fixtures/missing-evidence.md`) — `evidence_guard.sh` must FAIL on it; then move it out of scope.
- **Risks:**
  - **R:** sha256 churn — sentinel changes on every commit, polluting diffs. **M:** sentinel only regenerated when a `rules/` or `templates/L*/` file changes; guard checks staleness, not absolute equality, on intermediate commits.

#### SP4 — Tier-1/2/3 skill set bodies (workflow checklists + bundled scripts)

- **Inputs:** SP1 stubs.
- **Deliverables:**
  - Each of the 17 SKILL.md files filled in with: frontmatter, "when this
    triggers", workflow checklist (numbered steps), bundled-scripts table,
    inter-skill invocation pointers.
  - Per-skill `scripts/` subdirs populated.
  - `/ax-verify/scripts/run-all.sh` — orchestrates Tier-2 → Tier-3.
- **Acceptance:** `/ax-verify` end-to-end exits 0 on a clean checkout;
  `/ax-scaffold dummy-domain` produces a scaffolded skeleton without
  touching real files (dry-run mode).
- **Verify command:** `/ax-verify` (recursive: verifies itself).
- **Agent count:** 1 lead + 3 workers (1 worker per tier).
- **TDD anchor:** `skills/_tests/skill-topology.test.sh` — asserts skill count, file existence, frontmatter shape.
- **Risks:**
  - **R:** workflow checklists drift from actual steps. **M:** each checklist line references a bundled script or a concrete file path — no abstract bullets.

### Phase 2 — Vertical slice (SP5 → SP8)

#### SP5 — L1 shadcn integration + design tokens manifest

- **Inputs:** SP1–SP4 done.
- **Deliverables:**
  - `templates/L1/tokens/tokens.yaml` + corresponding `tokens.css` generated artifact.
  - `templates/L1/tokens/typography.css`, `motion.css`, `surfaces.css`.
  - shadcn installed via `pnpm dlx shadcn@latest add <component>` for the 32 blessed components, written to `templates/L1/components/`.
  - Wrapper files re-exporting each shadcn component with token injection.
  - `templates/L1/AGENTS.md` (sub-sentinel, also rolled into top-level `templates/AGENTS.md`).
  - ADR TD-2026-05-17-002 (shadcn) + TD-2026-05-17-010 (tokens) finalized.
- **Acceptance:** `/ax-verify-L1` exits 0; `npm run build` exits 0; visual regression smoke (Playwright screenshot at 320/768/1440) saved as baseline.
- **Verify command:** `/ax-verify-L1`.
- **Agent count:** 1 lead + 1 worker.
- **TDD anchor:** `frontend/tests/L1/token-contract.spec.ts` — asserts each blessed shadcn component renders with token-driven CSS variables, not hardcoded values.
- **Risks:**
  - **R:** shadcn upgrade breaks tokens. **M:** pin shadcn registry version in `blueprints/pinned-versions.yaml`; `time_decay_guard.sh` flags > 90d stale.

#### SP6 — L3 page template catalog

- **Inputs:** SP5 L1 stable.
- **Deliverables:**
  - 7 L3 template families (list/detail/create/edit/dashboard/auth-callback/error) under `templates/L3/pages/<family>/`.
  - Each family includes `page.tsx`, `loading.tsx`, `error.tsx`, `not-found.tsx` where applicable, plus a `README.md` describing the slot contract.
  - Evidence block per family pointing to Next.js 16 docs + WCAG 2.2 + CWV snapshot.
- **Acceptance:** `/ax-verify-L3` exits 0; smoke fixture (`templates/L3/_fixtures/smoke-app/`) builds with `next build`.
- **Verify command:** `/ax-verify-L3`.
- **Agent count:** 1 lead + 2 workers (split families: form-heavy vs read-heavy).
- **TDD anchor:** `templates/L3/_fixtures/smoke-app/tests/route-resolution.spec.ts` — every L3 template's example route resolves and renders.
- **Risks:**
  - **R:** Slot contract ambiguity. **M:** each `README.md` includes a concrete `LoginPage = create-page<auth>` worked example so L4 consumers have no guessing room.

#### SP7 — L2 feature block catalog + practices-react rule additions

- **Inputs:** SP5 L1, SP6 L3 contracts.
- **Deliverables:**
  - 26 L2 blocks under `templates/L2/blocks/<block>.tsx` with co-located
    `.spec.test.tsx`.
  - Each block carries `evidence:` frontmatter (as a top-of-file JSDoc block
    or co-located `.evidence.md`).
  - 10 new rules in `practices-react/rules/` per § 4.7 (subject to S7 audit
    collapsing some).
  - 7 ESLint plugin rules in `practices-react/eslint-plugin-ax/rules/`
    extended as needed (no more than 3 new ESLint rules — anything else
    becomes an advisory rule).
  - `practices-react/AGENTS.md` regenerated; sha256 sentinel updated.
- **Acceptance:** `/ax-verify-L2` exits 0; `practices-react/evals/run.sh` exits 0; `npm test` in `practices-react/eslint-plugin-ax/` exits 0.
- **Verify command:** `/ax-verify-L2` and `/ax-verify-react` (both).
- **Agent count:** 1 lead + 4 workers in parallel (split by domain-axis blocks: auth-worker, crud-worker, payment-worker, practices-viewer-worker). Cross-cutting blocks handled by lead.
- **TDD anchor:** per block — `templates/L2/blocks/<block>.spec.test.tsx` RED before .tsx exists.
- **Risks:**
  - **R:** Cross-block dependency leak (block A imports block B). **M:** lint rule forbids `templates/L2/blocks/*` from importing each other; only L1 + lib utilities allowed.

#### SP8 — L4 auth domain workload (vertical proof)

- **Inputs:** SP5/6/7 complete.
- **Deliverables:**
  - `templates/L4/auth/` fully wired with `app/(auth)/login/page.tsx`,
    `app/(auth)/signup/page.tsx`, `app/oauth/callback/[provider]/page.tsx`,
    `app/(auth)/verify/page.tsx`, `app/(protected)/dashboard/page.tsx`,
    `app/(protected)/protected/page.tsx`.
  - Frontend Spec Trio for auth (already created in SP2) — items now have
    matching Playwright tests.
  - `frontend/playwright.config.ts` updated to discover
    `frontend/tests/auth/*.spec.ts` for the auth domain.
- **Acceptance:** `/ax-verify-domain auth` exits 0; full Playwright e2e covering signup → email verify → login → OAuth callback → protected route passes; `./gradlew testAsvs` still green.
- **Verify command:** `/ax-verify-domain auth`.
- **Agent count:** 1 lead + 1 worker (lead writes pages, worker writes Playwright tests).
- **TDD anchor:** Playwright tests written BEFORE pages are populated; they fail until the pages exist.
- **Risks:**
  - **R:** OAuth callback timing flake. **M:** Playwright uses MSW for OAuth provider responses in CI mode; only the local-dev flow hits real providers.

### Phase 3 — Horizontalize (SP9 → SP11, parallelizable)

#### SP9 — L4 crud domain workload

- **Inputs:** SP8 done; abstractions in L2/L3 now proven for one domain.
- **Deliverables:** `templates/L4/crud/` mirroring SP8 shape; `specs/crud-frontend-l0.yaml`, `contracts/crud-ui.yaml`, `blueprints/crud-ui-manifest.yaml`.
- **Acceptance:** `/ax-verify-domain crud` exits 0; `./gradlew testCrud` stays green; Playwright e2e covers list → create → edit → delete round-trip.
- **Verify command:** `/ax-verify-domain crud`.
- **Agent count:** 1 lead + 1 worker.
- **TDD anchor:** Playwright `frontend/tests/crud/*.spec.ts` first.
- **Risks:**
  - **R:** L2 block doesn't fit (e.g., `DataTable` lacks a column-pinning prop). **M:** SP9 is allowed to file 1 retro-edit PR against `templates/L2/blocks/DataTable.tsx` IF needed; the edit must come with an ADR amendment.

#### SP10 — L4 payment domain workload

- **Inputs:** SP8/9 done.
- **Deliverables:** `templates/L4/payment/` + frontend Spec Trio for payment; idempotency-key handling on the client side; payment provider mock via MSW for tests.
- **Acceptance:** `/ax-verify-domain payment` exits 0; `./gradlew testPayment` stays green; Playwright e2e covers checkout → status → receipt; idempotency replay test passes.
- **Verify command:** `/ax-verify-domain payment`.
- **Agent count:** 1 lead + 1 worker.
- **TDD anchor:** idempotency replay test first.
- **Risks:**
  - **R:** Payment provider API change. **M:** the payment provider stays mocked via MSW for the template's e2e; real-provider verification is out of scope (matches backend's reference workload posture).

#### SP11 — L4 practices catalog viewer

- **Inputs:** SP6 L3, SP7 L2.
- **Deliverables:** `templates/L4/practices/` with `/practices`, `/practices/[ruleId]`, `/practices/families/[family]` segments. Reads `practices/AGENTS.md` and `practices-react/AGENTS.md` at build time (Server Components) and renders a browsable view.
- **Acceptance:** `/ax-verify-domain practices` exits 0; clicking through 5 random rules in Playwright resolves and shows evidence; `next build` exits 0.
- **Verify command:** `/ax-verify-domain practices`.
- **Agent count:** 1 lead + 1 worker.
- **TDD anchor:** rendering test asserts that every rule in `practices/rules/` is reachable by URL.
- **Risks:**
  - **R:** Catalog regeneration ordering (AGENTS.md changes after the viewer builds). **M:** the viewer reads from `AGENTS.md` content via Server Components at runtime, not at build-time cache — so post-build AGENTS.md updates are picked up on next request without rebuild.

### Phase 4 — Integration (SP12)

#### SP12 — End-to-end `/ax-verify` binary green

- **Inputs:** SP1–SP11 done.
- **Deliverables:**
  - `verify/run-all.sh` updated to call `/ax-verify`.
  - `verify/blueprint-completeness.sh` extended to accept `frontend` as a domain arg and to validate `templates/L4/<domain>/` integrity.
  - `verify/cold-start-test.sh` updated cold-start file list now covers frontend templates.
  - `docs/blueprints/frontend-templatization/{plan,progress,decisions,security-review,verification-log}.md` filed (matching the 5-document pattern from `docs/blueprints/payment/`).
  - `docs/blueprints/frontend-templatization/acceptance/l4-sealed-prompt.md` + `l4-sealed-rubric.md` filed for the L4 sealed sub-agent acceptance test on the **frontend templatization** as a whole (not on a specific domain — this is the meta-acceptance).
  - L3 fork simulation under `docs/blueprints/frontend-templatization/acceptance/l3-fork-simulation.md`.
- **Acceptance:** `bash verify/run-all.sh` exits 0; L4 sealed sub-agent reaches MUST_PASS ≥ 9/11 (rubric, with frontend-specific MUST items adapted from Payment's rubric); L3 fork simulation reaches "5 gates green under 300s".
- **Verify command:** `/ax-verify`.
- **Agent count:** 1 lead orchestrator (no parallelism — final integration is sequential).
- **TDD anchor:** the L3 fork simulation IS the test.
- **Risks:**
  - **R:** L4 sub-agent fails on frontend-specific discoverability (e.g., the agent doesn't know where to look for L2 blocks). **M:** sealed prompt explicitly tests "find the LoginForm block and reuse it for a new domain"; if that fails, SKILL.md / AGENTS.md docs are amended and retest.

### 5.bonus — Sub-project dependency graph

```
SP1 ──▶ SP2 ──▶ SP3 ──▶ SP4 (Phase 1 fan-in)
                          │
                          ▼
                         SP5 ──▶ SP6 ──▶ SP7 ──▶ SP8 (Phase 2 vertical slice)
                                                   │
                          ┌────────────────────────┼────────────────────┐
                          ▼                        ▼                    ▼
                         SP9  (parallel)        SP10  (parallel)      SP11  (parallel)
                          │                      │                      │
                          └──────────────────────┴──────────────────────┘
                                                 │
                                                 ▼
                                                SP12 (integration)
```

Phase 3 (SP9/10/11) can run in parallel under `/team` orchestration with 3
worker teams.

### 5.bonus2 — Estimated effort

Mirroring METHODOLOGY.md Appendix C's "7.5–10 engineering days per domain":

| Phase | SPs | Estimated days (eng + AI assist) |
|---|---|---|
| Phase 1 (foundation) | SP1–4 | 4–6 |
| Phase 2 (vertical slice) | SP5–8 | 5–7 |
| Phase 3 (horizontalize, parallel) | SP9–11 | 3–4 (parallel wall time) |
| Phase 4 (integration) | SP12 | 1–2 |
| **Total** | 12 SPs | **13–19 days** |

---

## 6. Verification Plan

### 6.1 Per-SP TDD anchor (RED → GREEN)

Every SP above lists its TDD anchor — a test file that is written first,
fails, and only goes green when the SP's deliverables are complete. This is
non-negotiable per the methodology.

### 6.2 Per-SP termination via /verification-loop

Each SP's "Verify command" maps to one Tier-1/2/3 skill. SP completion
requires `<skill> exit 0`. The Tier-1 `/ax-verify` is the only Tier-1 verify
skill; SPs may also terminate on a Tier-2 skill if the SP scope is narrower
than the full system.

### 6.3 Full test plan per SP (unit / integration / e2e / observability)

| SP | Unit (Vitest / JUnit) | Integration (RestAssured / Next.js handlers) | E2E (Playwright) | Observability / Static |
|---|---|---|---|---|
| SP1 | `npm run test` (Vitest on app shell) | `./gradlew testAsvs` cross-check | auth-login Playwright smoke | TypeScript `tsc --noEmit`, ESLint clean, build-size budget probe (300 KB JS gzipped) |
| SP2 | YAML schema lint | swagger-cli validate | n/a | METHODOLOGY.md diff review |
| SP3 | guard `.sh` self-tests | n/a | n/a | sha256 sentinel match, snapshot manifest validation |
| SP4 | skill frontmatter linter | `/ax-verify` recursive | n/a | skill graph integrity probe |
| SP5 | token contract Vitest | n/a | visual-regression at 320/768/1440 | Lighthouse smoke ≥ 90 |
| SP6 | per-template render Vitest | n/a | route-resolution Playwright | `next build` budget |
| SP7 | per-block Vitest + MSW | per-block MSW handler | block-in-isolation Playwright | ESLint plugin RuleTester pass |
| SP8 | per-form Vitest | RestAssured auth round-trip (`testAsvs`) | full auth e2e | a11y (axe) ≥ 0 violations, CWV smoke |
| SP9 | per-CRUD-block Vitest | RestAssured CRUD round-trip (`testCrud`) | full CRUD e2e | a11y + CWV |
| SP10 | idempotency Vitest | RestAssured payment round-trip (`testPayment`) | full payment e2e + replay | a11y + CWV |
| SP11 | rule-resolution Vitest | n/a (catalog is static) | catalog browse Playwright | broken-link probe |
| SP12 | n/a (orchestrator) | full `./gradlew test` | full `playwright test` | `blueprint-completeness.sh frontend` + L3 fork sim + L4 sealed sub-agent |

### 6.4 Self-application proof

The skill applies itself to itself: by SP12, `/ax-verify` is the gate that
proves `/ax-transform` shipped the frontend templates correctly. Same
recursive posture as the Payment domain proved for the backend.

---

## 7. Pre-mortem (DELIBERATE mode)

This work is high-risk: migration of a working frontend, 80+ new artifacts,
17 new skills, 4 guard extensions, and autonomous multi-agent execution in
Phase 3. Three failure scenarios:

### Scenario 1 — Skill topology becomes unmaintainable

**Failure:** 17 skills with overlapping responsibilities, unclear which one
to invoke, AGENTS.md grows to thousands of lines, prompt budget collapses
for downstream agents. The "few exposed surfaces" principle fails because
even though only 3 are Tier-1, the agents discover Tier-2/3 directly via
path patterns and end up confused.

**Likelihood:** Medium. We've never run 17 skills in one repo before.

**Detection:** SP4 acceptance includes a skill-graph integrity probe that
checks for: (a) no two skills with overlapping pathPatterns, (b) every
Tier-2 calls only Tier-3 + Tier-2-siblings (no upward calls), (c) every
Tier-1 has a clear top-of-file "use me when…" statement that an LLM can
disambiguate against the others.

**Mitigation:**
- Sealed sub-agent test at SP4 asks a context-0 agent: "I just edited
  `templates/L2/blocks/LoginForm.tsx`, which skill auto-triggers?". The
  correct answer is `/ax-verify-L2` (or `/ax-verify-react`). Any
  ambiguity → reshape pathPatterns until unique.
- Hard cap at 3 Tier-1 skills (revisit before adding a 4th).
- Quarterly review: if a Tier-2 skill has not been invoked in 90 days,
  consider merging it into a sibling.

### Scenario 2 — Next.js 16 migration breaks the auth flow mid-cycle

**Failure:** OAuth callback URL changes, react-router-dom v7 patterns don't
map cleanly, the Vite-era `frontend/src/lib/auth/` doesn't survive the App
Router server/client boundary. `./gradlew testAsvs` keeps passing (because
it's backend-only) but the actual frontend OAuth flow is broken in
production-shaped tests.

**Likelihood:** High. App Router's server/client component boundaries are
the most common source of migration regressions.

**Detection:** SP1's Playwright anchor (`login-flow.spec.ts`) is a
production-shape e2e against the real Spring Boot backend; it fails if the
OAuth round-trip drops or the callback page can't read query params.

**Mitigation:**
- SP1 keeps the existing pages' shape (URL paths, redirect targets) byte-identical.
- A read-only branch of the Vite frontend is preserved at `git tag pre-nextjs-migration` for diff-recovery.
- SP8 explicitly re-tests the auth domain end-to-end before moving to SP9; if SP8 fails, SP1's migration is rolled back or amended.

### Scenario 3 — L2 abstractions over-fit to auth and break for crud/payment

**Failure:** SP7 extracts L2 blocks (`DataTable`, `CrudCreateForm`) from
the auth-domain experience, but auth has no list view and no real CRUD
mutation; the abstractions only get stress-tested in SP9 and reveal
themselves to be wrong, forcing a retro-edit cascade.

**Likelihood:** Medium-high. This is the canonical "extract abstractions
from N=1 then break on N=2" pattern.

**Detection:** SP9 acceptance includes a deliberate audit: "list every L2
block consumed; for each, does the prop surface match the actual need or
were you forced to add an escape hatch?" If > 2 escape hatches, freeze
SP10/SP11 and run an SP9.5 to refactor L2.

**Mitigation:**
- SP7 explicitly designs `DataTable`, `FilterBar`, `Pagination`, `CrudCreateForm`, `CrudEditForm`, `CrudDeleteConfirm` against a synthetic "items" domain (not against auth). The synthetic domain fixture lives at `templates/_fixtures/items-domain/` and exercises the blocks' surface before SP9 ever runs.
- SP9 is the empirical sanity check, not the first stress test.
- L2 retro-edits are budgeted (1 amendment per block, with ADR). If a block needs > 1, escalate.

### Expanded test plan additions for DELIBERATE mode

- **Performance budgets**: each L3 page template has a Lighthouse smoke target (≥ 90 perf, ≥ 95 a11y, ≥ 95 best-practices, ≥ 90 SEO). CI runs Lighthouse on the smoke fixture on every SP6/7/8/9/10/11 PR.
- **A11y gate**: axe runs against every Playwright route; ≥ 0 critical violations is the bar.
- **Observability**: every Server Action and Route Handler logs `traceId` (mirrors `observability-micrometer-tracing` from Java side); a Playwright trace probe checks that the trace propagates from frontend → backend.
- **Bundle budget**: 150 KB JS gzipped for landing, 300 KB for app pages, 80 KB for the practices viewer (microsite).
- **Cold-start agent test**: SP12 asks an unrelated AI agent (fresh context) to perform "add a new domain end-to-end" using only the skills and AGENTS.md. If it can't reach green inside 4 hours, the docs / skills fail.

---

## 8. ADR Template (for final commit)

The final plan (after Architect + Critic loop converges) commits a single
ADR to `templates/DECISIONS.md` summarizing the whole initiative. Per-SP
ADRs (TD-2026-05-17-001..010 listed in § 4.9) are filed in SP3.

The summary ADR follows this shape:

```
## TD-2026-05-17-000 — Frontend Templatization + Full-Stack Methodology Extension

### Decision
Adopt the 4-layer frontend template model (L1 shadcn primitives, L2 ax-template
feature blocks, L3 Next.js 16 App Router page templates, L4 vertical domain
workloads) mirroring the backend Spec Trio. Migrate the existing Vite frontend
to Next.js 16 App Router in one transition (no coexistence). Install a 3-tier
skill topology (3 Tier-1 exposed + 8 Tier-2 axes + 6 Tier-3 leaf guards = 17
skills) to keep the user surface small while making every concern individually
invocable.

### Drivers
(1) AI agent self-discoverability (context-0 agent must reach green build).
(2) Migration safety (auth domain must stay green through the cycle).
(3) Catalog convergence cost (one pass = full coverage; no rework loop).

### Alternatives considered
A. Strict bottom-up (infra → L1 → L2 → L3 → L4) — late integration risk.
B. Vertical-slice first (auth end-to-end before extracting catalog) — N=1 over-fit risk.
C. Hybrid (infra → vertical slice → horizontalize → integrate) — **CHOSEN**.
D. Vite + Next.js coexistence — invalidated by Section A constraint.

### Why chosen
Hybrid (C) matches the Payment empirical cadence (P0…P11 = foundation →
vertical → horizontalize → integrate), preserves all 5 principles, and bounds
the worst-case rework to one L2-amendment budget per block in SP9/10/11.

### Consequences
- Vite is removed from the repo; `frontend/package.json` rewritten.
- `templates/` is a new top-level directory.
- 4 guard scripts get `templates/**` walking added; 2 new guards land.
- 7 upstream snapshots added (Next.js 16 App Router, Server Actions, use-cache, shadcn, TanStack Query v5, WCAG 2.2, CWV 2026).
- practices-react/ catalog grows by ≤ 10 rules; practices/ catalog grows by ≤ 4 rules.
- 17 SKILL.md files added; existing `/ax-transform` becomes a frontmatter delegate to the 3-tier graph.
- METHODOLOGY.md Appendix A gains row A.3; Appendix C Recipe B is promoted from forward-compatible to exercised.

### Follow-ups
- After SP12, run the L4 sealed sub-agent test on a fresh fork (target: 9/11 MUST + 4/6 SHOULD as the entry bar; aim for Payment's 11/11 + 6/6 over two iterations).
- Within 90 days, refresh all 7 new upstream snapshots (`time_decay_guard`).
- Within 180 days, add a 5th L4 domain (Notification is the candidate per METHODOLOGY.md Appendix C) to stress-test the full-stack methodology a second time.
```

---

## 9. Open Questions

These do not block the consensus loop; they belong in
`.omc/plans/open-questions.md` for downstream resolution.

1. **Tailwind dependency.** shadcn 2.x ships with Tailwind v4. Is Tailwind v4
   itself an ADR-worthy choice or a transitive dependency? (Assumed
   transitive; if the maintainer wants Tailwind as a first-class concern,
   add TD-2026-05-17-011.)
2. **Catalog viewer hosting.** The L4 practices viewer (SP11) can run as a
   sub-route of the main Next.js app, or as a separate static export. Default
   is sub-route; static export adds 1 dep.
3. **Storybook.** L2 blocks could benefit from Storybook but it's an extra
   tooling axis. **Assumption:** out of scope for this cycle; revisit after
   SP12.
4. **Server-side rendering of Markdown evidence.** When the practices viewer
   renders `evidence:` quoted text, it needs to render Markdown safely. MDX
   adds dep weight; raw rehype + DOMPurify is lighter. **Assumption:**
   start with rehype + DOMPurify; switch to MDX only if forced.
5. **Internationalization.** Korean enterprise context (per CLAUDE.md) may
   require Korean labels. **Assumption:** copy is English-default; i18n
   harness deferred. If maintainer disagrees, add ADR.

---

## 10. Honored Constraints (cross-check vs CLAUDE.md anti-patterns)

| CLAUDE.md anti-pattern | How this plan honors it |
|---|---|
| 거버넌스 무한루프 (governance infinite loops) | No promotion-gate docs. No "evidence bundle for the bundle". Every artifact ships with binary verification. |
| MockMvc 전용 테스트 | All backend integration tests stay RestAssured. Frontend integration uses Playwright + real Spring Boot RANDOM_PORT, or MSW for unit isolation only. |
| Fork받은 팀의 정책을 skill이 강제 | Skill remains advisory: it provides verify commands and guards, but does not require any branch policy, PR review, or merge gate beyond what the catalog already enforced. |
| 문서 0줄 / 코드 0줄 균형 | Plan ships 80+ code/config artifacts and ≤ 12 new docs (METHODOLOGY edits + DECISIONS.md ADRs + 5 blueprint docs in SP12). Code-to-doc ratio ≥ 6:1. |
| Catalog 확장 = 정상 활동 | Anticipated 4 backend rules + 10 React rules = 14 net new rules; S7 generalization audit gates each. Catalog growth is welcomed, not avoided. |
| React + Spring 둘 다 active equal partner | Both stacks ship deliverables in every phase. No archival, no deprecation labels. Backend gains 10 cross-cutting templates + up to 4 rules; frontend gains everything. |
| 단일 npm 패키지 / 단일 product framing 금지 | All artifacts are composition kit pieces. shadcn is external-owned; design tokens are internal; wrappers are thin; L2 blocks reusable independently. No `npm publish ax-template` build target proposed. |
| Adoption probability metric 금지 | No "X% adoption" estimates anywhere. Success is binary skill `exit 0`. |

---

## End of PRD draft.

> **Next step in the ralplan loop:** Architect (Step 3) runs steelman antithesis
> + tradeoff + synthesis review against this draft. Critic (Codex, Step 4)
> evaluates quality / consistency / verifiability. Iterate ≤ 5 times until both
> APPROVE, then SP1 starts under `/team` orchestration.
