# ax-template

> **Full-stack React 19 / Next.js 15 + Spring Boot 3 fork-base template that mechanically enforces development rules so AI agents can't drift off the rails.**

ax = **AI transformation**. This repo is the source of the Claude Code skill
**`/ax-transform`** and a composition kit you fork to start a new
project. Every layer of the stack ships with rule-enforcement wired in:

- **React / Next.js side** — `@ax/eslint-plugin-ax` mechanical lint (11 ESLint rules) + 99-rule
  evidence-anchored catalog (`practices-react/rules/`).
- **Spring Boot side** — `@Tag`-based JUnit + RestAssured tests
  (`./gradlew test{Domain}`) + 187-rule Java/Spring catalog
  (`practices/rules/`).
- **Spec-first contract** — every domain has a Spec Trio
  (`specs/X.yaml` + `contracts/X-openapi.yaml` + `blueprints/X-manifest.yaml`).
  AI reads the spec before writing the implementation; no spec, no merge.
- **AI agent context** — `AGENTS.md` sentinel files in `practices/` and
  `practices-react/`, auto-regenerated from rule sources with sha256-anchored
  staleness detection.
- **4 hard gates** — `spec_ref` / `substance` / `time_decay` / `evidence`
  binary checks that BLOCK rules that don't anchor to external sources
  (canonical docs, RFCs, JEPs).

## The development scenario

You're starting a new project: React frontend + Spring Boot backend
(the standard Korean enterprise stack). You want:

1. AI agents (Claude Code, Cursor, etc.) to do most of the typing.
2. AI hallucinations / stale-tutorial patterns / framework-version drift to
   be caught **mechanically**, not by code review.
3. New domains (Payment, Notification, File upload, Audit log, …) to be
   addable by following one playbook — not invented per-project.

ax-template is the codebase that gives you 1-3 from commit 0.

## The virtuous cycle

```
fork ax-template
       ↓
25 L4 domains + 11 active recipes · 187 Java rules · 99 React rules · 11 ESLint rules · 76 hard guards · L0 fork-receiver-kit · L2 rate-limit-banner · AGENTS.md sentinel
       ↓
add new domain (Payment / Notification / …)  ←——— playbook: METHODOLOGY.md (5 steps)
       ↓
AI agent writes Spring + React code for the new domain
       ↓
ESLint plugin + ./gradlew test{Domain} + Spec Trio + AGENTS.md + 4 hard gates auto-enforce
       ↓
non-conforming AI output BLOCKED at commit / push / CI
       ↓
codebase quality stays inside the template's design envelope
       ↓
new domain's rules feed back into practices/* — catalog grows
       ↓
next fork inherits a stronger catalog
       ↓
loop.
```

## What ships in the composition kit

| Layer | Asset | Mechanism |
|-------|-------|-----------|
| Backend reference workload | `backend/` — Spring Boot 3 + Java 21, 14 auth endpoints (signup/login/OAuth Google·Naver·Kakao/password reset/RBAC ADMIN·MANAGER·MEMBER), 5 CRUD endpoints, 1 rate-limit endpoint | TDD-built; per-domain `./gradlew test{Domain}` is binary pass/fail (status matrix in CLAUDE.md Build & Test) |
| Frontend reference workload | `frontend/` — React 19 + Next.js 15, OAuth UI, login pages, e2e Playwright tests | self-tests the ESLint plugin |
| Java/Spring rule catalog | `practices/` — 187 rules / 22+ categories with evidence-anchored frontmatter | runs against backend via `testPractices`; advisory probes via `practices/evals/run.sh` |
| React/Next.js rule catalog | `practices-react/` — 99 rules / 9 families, citing canonical React 19 / Next.js 16 docs | runs via 3 hard gates (`practices-react/evals/run.sh`) |
| ESLint plugin (React enforcement) | `practices-react/eslint-plugin-ax/` — 11 custom rules (incl. 3 frontend-decomposition: cross-feature / layer-direction / published-API-barrel) | RuleTester suites; install in any downstream project |
| Spec Trio (per domain) | `specs/<domain>.yaml` + `contracts/<domain>-openapi.yaml` + `blueprints/<domain>-manifest.yaml` | enforced by `spec_ref_guard.sh` — every rule must point to a spec item |
| 4 hard gates | `practices/evals/{spec_ref,substance,time_decay,evidence}_guard.sh` (Java) + `practices-react/evals/run.sh` (React) | block commits / pushes via `.githooks/{pre-commit,pre-push}` when catalog quality degrades |
| AGENTS.md sentinel | `practices/AGENTS.md` + `practices-react/AGENTS.md` (sha256-anchored, auto-regenerated) | AI agents read this first; never read stale catalog |
| `/ax-transform` Claude Code skill | `skills/ax-transform/SKILL.md` + `.claude-plugin/plugin.json` | activates the methodology when an AI agent starts work in a fork |

## Use as a project starter

### 30-minute quickstart (fork-receiver path)

> **Step 0 — Read [`docs/IMPLEMENTATION-STATUS.md`](./docs/IMPLEMENTATION-STATUS.md) first.** It documents the current state across the 25 L4 domains: backend implementation level, frontend trio status (full / backend-only / promoted from R39 stub), and which Spec Trio fields are wired. Sealed verdict PASS validates **catalog self-discoverability by AI agents**, NOT that all backend code is production-ready. Skipping this step is the #1 cause of fork-receiver scope misjudgment.

The fastest way to evaluate: pick **one of 11 active recipes** that matches your scenario, then compose. Each recipe is a documented composition of L4 domains (auth, crud, payment, audit-log, etc.) with sealed-verdict self-discoverability.

```bash
# 1. Fork + bundle (first 5 minutes)
git clone https://github.com/ai-dev-methodologies/ax-template my-project
cd my-project
git submodule update --init   # fixtures: petclinic / realworld / modulith

# 2. Pick a recipe (open recipes/_MANIFEST.yaml or recipes/README.md)
#    11 active recipes: saas-subscription · e-commerce · crm · booking · marketplace
#                     · b2b-admin · community · lms · cms · internal-it · api-gateway-relay
cat recipes/_MANIFEST.yaml | head -40

# 3. Read your chosen recipe's RECIPE.md — every recipe has a
#    "Backend Implementation Status" table showing which L4 are
#    ready-to-run (impl) vs require fork-receiver implementation (spec-only)
cat recipes/saas-subscription/RECIPE.md     # example

# 4. Run the full catalog verification (proves the bundle is intact)
bash practices/evals/run-all-guards.sh       # 76 hard guards (all PASS expected)

# Per-domain catalog tasks — the "binary pass/fail" surface (R64+ baseline)
cd backend
./gradlew testAsvs              # GREEN — 26 ASVS items
./gradlew testCrud              # GREEN — 7 CRUD security tests
./gradlew testPractices         # GREEN — 187 rules
./gradlew testPayment           # GREEN — 29 PAYMENT items
./gradlew testRateLimit         # GREEN
./gradlew testNotification      # GREEN
./gradlew testIdentityVerification  # GREEN — 19/19 (HMAC envelope + PASS/KCB canonical extraction + VerifiedIdentity persistence + AuditLog publish + Admin GET. R54 backend residual closure. spec `domain_mode: backend_only` — no frontend trio.)
./gradlew testBilling           # GREEN — 17/17 (R21 backend impl: subscription/plan/webhook endpoints shipped)
./gradlew testPortability       # advisory — external fixture (spring-realworld-example-app) cycle, not your code
./gradlew test                  # aggregate of the above; GREEN except the advisory PortabilityCyclic external-fixture cycle
cd ..

cd frontend && npm install && npm run build && cd ..

# 5. Optional: install pre-commit + pre-push hooks (opt-in)
bash practices/scripts/install-hooks.sh
```

### Bundle for external delivery (`/ax-fork-receiver` skill)

To ship the catalog as a tarball (e.g., for downstream teams, vendor delivery, or air-gapped onboarding):

```bash
bash skills/ax-fork-receiver/scripts/bundle.sh    # produces dist/ax-template-catalog-<sha>.tar.gz
bash skills/ax-fork-receiver/scripts/smoke.sh     # validates the tarball is self-contained
```

The `/ax-fork-receiver` skill (`skills/ax-fork-receiver/SKILL.md`) wraps these in a Claude Code workflow.

## Adding a new domain (the 5-step playbook)

See [`METHODOLOGY.md`](./METHODOLOGY.md). Short version:

1. **Compliance Spec** — `specs/<domain>-l<level>.yaml` (the items to satisfy)
2. **API Contract** — `contracts/<domain>-openapi.yaml` (the endpoints)
3. **Policy Manifest** — `blueprints/<domain>-manifest.yaml` (the policy values)
4. **Portable Tests** — `@Tag("<DOMAIN>")` JUnit + RestAssured tests
5. **Build Verification** — register `./gradlew test<Domain>` task

Currently: **25 L4 domains** (auth, crud, payment, audit-log, billing, feature-flags, file-storage, notification, practices, scheduled-task, search, webhook, api-key, approval-workflow, session-management, activity-feed, comment-thread, tag-categorization, favorites-bookmarks, email-outbox, multi-tenant, data-subject-rights, i18n-policy, ratelimit, realtime-policy) and **11 active recipes**. All 25 L4 documented with Spec Trio + per-domain `./gradlew test{Domain}` task. `identity-verification` is intentionally `domain_mode: backend_only` (no `templates/L4/identity-verification/` on disk — see [`practices/rules/spec-domain-mode-gates-frontend-trio.md`](./practices/rules/spec-domain-mode-gates-frontend-trio.md)). See `docs/IMPLEMENTATION-STATUS.md` for the full status taxonomy.

Shared client primitives sit at **L0** (`templates/L0/fork-receiver-kit/` — `use-caller-id.ts` / `parse-error.ts` / `entity-key.ts`) and at **L2** (`templates/L2/blocks/` — `confirm-dialog.tsx`, `rate-limit-banner.tsx`, `offline-banner.tsx`, `announce-live.tsx`, and 30+ more).

### Korean PG callback wiring (redirect-style branch)

Korean PGs (KG이니시스 / NICE페이먼츠 / KCP / Toss V1) use a redirect-style flow:
the PG POSTs `{authToken, TID, signature, ...}` back to a server-side callback URL
after the user completes card entry on the PG's hosted page. The catalog covers
this branch in three places — start here when forking for a Korean PG PoC:

1. **Spec** — [`specs/payment-l0.yaml`](./specs/payment-l0.yaml) items
   `PAYMENT-CALLBACK-001` (signature verification fail-closed → 401),
   `PAYMENT-CALLBACK-002` (idempotent replay on PG-issued TID), and
   `PAYMENT-CALLBACK-003` (allowed-state transitions: only
   `{AUTHORIZED, UNKNOWN}` → `{CAPTURED, FAILED}`).
2. **Policy** — [`blueprints/payment-manifest.yaml`](./blueprints/payment-manifest.yaml)
   `callback:` block pins the SPI name (`PaymentCallbackVerifier`),
   idempotency key source (`(provider, TID)` composite), and the audit-row
   contract (every callback — success or signature-fail — MUST emit a row).
3. **Fork guide** — [`templates/L4/payment/README.md`](./templates/L4/payment/README.md)
   "How to fork" Step 7 redirect-style branch walks through the full
   browser → PG popup → callback → ledger sequence with KG / NICE / KCP
   specifics.

Tokenization-style PGs (Stripe / Toss V2) do NOT enter the callback code path —
they use the existing `PaymentProvider.charge()` server-side call. The backend
reference workload ships `mock` provider only; the redirect-style hook
(`markCapturedFromCallback`) is deferred to R18+ per the
`PaymentProvider` interface extension PRD.

## Rules currently enforced

### Spring/Java (testPractices — 187 rules / 22+ categories)

`lang-`, `core-`, `config-`, `web-`, `http-`, `persistence-`, `transaction-`,
`migration-`, `security-`, `validation-`, `error-`, `api-`, `async-`,
`messaging-`, `cache-`, `observability-`, `actuator-`, `testing-`, `build-`,
`native-`, `arch-`, `quality-`. Every rule cites at least one external
source (Spring Docs, OWASP ASVS, RFC, JEP). Full catalog:
[`practices/AGENTS.md`](./practices/AGENTS.md).

### React/Next.js (`@ax/eslint-plugin-ax` + 99-rule catalog)

| Rule | Default | Catches |
|------|---------|---------|
| `react-async-parallel` | warn (recommended) | Independent awaits in async functions / RSC / Server Actions |
| `prefer-functional-setstate` | warn (recommended) | `setX(x...)` referencing state — risks stale closure |
| `no-broad-barrel-imports` | warn (opt-in) | barrel imports from `lodash` etc. |
| `no-falsy-numeric-render` | error (opt-in) | `count && <UI>` renders literal `0` |
| `no-array-includes-in-loop` | warn (opt-in) | `arr.includes(x)` inside `.filter`/`.map` — should be Set |
| `no-array-mutate-on-state` | error (opt-in) | `.sort`/`.reverse`/`.splice` on React state or props |
| `no-inline-component-definition` | error (opt-in) | Capitalized JSX-returning function defined inside another component |

`react-async-parallel` was empirically validated on 19 real-world Next.js
codebases — see
[`practices-react/pilot/round-3-empirical-validation.md`](./practices-react/pilot/round-3-empirical-validation.md)
(~70% TP rate; Vercel-authored references all 0 hits).

## Repo layout

```
ax-template/
├── .claude-plugin/plugin.json      # Claude Code plugin manifest
├── skills/ax-transform/SKILL.md    # /ax-transform skill entry point
│
├── CLAUDE.md                       # Project identity (AI-agent context, top-level)
├── METHODOLOGY.md                  # 5-step blueprint for adding new domains
│
├── specs/                          # Compliance specs per domain
│   ├── auth-asvs-l1.yaml
│   ├── crud-l0.yaml
│   ├── ratelimit-l0.yaml
│   ├── spring-practices-l0.yaml
│   ├── react-practices-l0.yaml
│   └── portable-test-template/
├── contracts/                      # OpenAPI contracts
│   ├── auth-openapi.yaml
│   ├── crud-openapi.yaml
│   └── ratelimit-openapi.yaml
├── blueprints/                     # Policy manifests
│   ├── auth-manifest.yaml
│   ├── crud-manifest.yaml
│   └── ratelimit-manifest.yaml
│
├── practices/                      # Java/Spring catalog
│   ├── rules/                      # 187 rule.md files
│   ├── upstream/                   # External doc snapshots
│   ├── evals/                      # 4 hard gates + 76 hard guards
│   ├── AGENTS.md                   # AI agent entry point (sha sentinel, auto-regen)
│   ├── SKILL.md                    # subsystem skill
│   ├── MAINTAINER.md               # catalog maintainer guide
│   └── DECISIONS.md                # rule provenance trail
│
├── practices-react/                # React/Next.js catalog
│   ├── rules/                      # 99 rule.md files
│   ├── upstream/                   # External doc snapshots
│   ├── eslint-plugin-ax/           # @ax/eslint-plugin-ax — 11 ESLint rules
│   ├── evals/                      # 3 hard gates
│   ├── AGENTS.md                   # AI agent entry point (sha sentinel, auto-regen)
│   ├── SKILL.md                    # subsystem skill
│   └── pilot/                      # curation pipeline + empirical validations
│
├── backend/                        # Spring Boot reference workload (auth + crud + payment + practices impl; single-tenant default — see docs/IMPLEMENTATION-STATUS.md)
├── frontend/                       # React reference workload (OAuth UI + e2e)
├── verify/                         # Optional verification scripts
└── docs/archive/                   # Historical governance documents
```

## What the template does NOT impose

Catalog quality is mechanically enforced. Human-collaboration policy is the
fork-받은 팀의 자율:

- Git workflow (branch protection, PR policy, merge strategy, force-push rules)
- Deployment / release / staging policy
- Team code-review or sign-off requirements
- Any CI gate beyond the catalog quality probes

The hard gates protect the rules from rotting; they don't impose how your
team works together.

## License

MIT — see [`.claude-plugin/plugin.json`](./.claude-plugin/plugin.json).

## Related

- [`CLAUDE.md`](./CLAUDE.md) — top-level project identity for AI agents
- [`METHODOLOGY.md`](./METHODOLOGY.md) — 5-step blueprint for adding new domains
- [`skills/ax-transform/SKILL.md`](./skills/ax-transform/SKILL.md) — skill entry point
- [`practices/MAINTAINER.md`](./practices/MAINTAINER.md) — Java catalog maintainer guide
- [`practices/DECISIONS.md`](./practices/DECISIONS.md) — Java rule provenance trail
- [`practices-react/SKILL.md`](./practices-react/SKILL.md) — React subsystem skill
- [`practices-react/pilot/round-3-empirical-validation.md`](./practices-react/pilot/round-3-empirical-validation.md) — `react-async-parallel` 19-repo measurement
