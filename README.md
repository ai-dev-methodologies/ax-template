# ax-template

> **ESLint rules that catch what AI agents usually miss in React 19 / Next.js 16.**

The headline rule — `react-async-parallel` — finds independent `await` waterfalls
in Server Components and Server Actions, the dominant async-perf mistake AI
agents generate when they pattern-match pre-RSC tutorials.

Empirically validated on **19 real-world Next.js codebases** (Round 3,
2026-05-17): ~70% true-positive rate, 12/19 repos with meaningful hits, zero
FP explosion at monorepo scale. Vercel-authored references (`vercel/platforms`,
`vercel/commerce`, `vercel/ai-chatbot`, `shadcn-ui/taxonomy`,
`t3-oss/create-t3-app`) all returned 0 hits — the rule targets community-quality
code, not canonical examples. Full data:
[`practices-react/pilot/round-3-empirical-validation.md`](./practices-react/pilot/round-3-empirical-validation.md).

## Install

```bash
npm install --save-dev @ax/eslint-plugin-ax @typescript-eslint/parser
```

> **Status (2026-05-17).** Package is publish-ready (`v0.1.0`) but not yet on
> npm. Install locally from this repo during development:
> `npm install --save-dev file:/path/to/ax-template/practices-react/eslint-plugin-ax`.

Add to `eslint.config.mjs` (Next.js 16's flat config):

```js
import ax from '@ax/eslint-plugin-ax'
import tsParser from '@typescript-eslint/parser'

export default [
  {
    files: ['**/*.{ts,tsx,js,jsx}'],
    languageOptions: {
      parser: tsParser,
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    plugins: { ax },
    rules: {
      'ax/react-async-parallel': 'warn',
      'ax/prefer-functional-setstate': 'warn',
    },
  },
]
```

## What it catches

Verbatim from the Round 3 19-repo validation:

```ts
// dub.co — apps/web/.../route.ts
await getProgramOrThrow(...)
await prisma.payout.findUnique(...)   // ax/react-async-parallel
// → independent at the data level; Promise.all saves one round-trip
```

```ts
// trigger.dev — apps/webapp/.../LimitsPresenter.server.ts
await _replica.X.count({ where: ... })  // 4 consecutive Prisma counts —
await _replica.Y.count({ where: ... })  // ax/react-async-parallel (×3)
await _replica.Z.count({ where: ... })  // all independent, all parallelizable
await _replica.W.count({ where: ... })
```

```ts
// cal.com — apps/web/.../page.tsx
await headers()    // Next.js docs explicitly recommend
await cookies()    // Promise.all([headers(), cookies()])
// ax/react-async-parallel
```

```ts
// nextjs/saas-starter — app/(dashboard)/.../layout.tsx
<button onClick={() => setIsSidebarOpen(!isSidebarOpen)}>
//                                       ^^^^^^^^^^^^^^^
// ax/prefer-functional-setstate — risks stale closure
// → setIsSidebarOpen(curr => !curr)
```

## What it does NOT catch

Round 3 identified three FP clusters; two are fixed in the rule, one is
deferred:

| Pattern | Status |
|---------|--------|
| Interactive-CLI scripts (`import readline / inquirer / prompts / enquirer / @clack/prompts`) | **fixed** — file-level skip when these imports are detected |
| Client navigation chain (`await action(); await router.push(...) / navigate(...) / redirect(...) / signOut(...)`) | **fixed** — callee-leaf allowlist H2 |
| Supabase write-then-link via intermediate consts (semantic dependency hidden by `.map()`) | deferred — needs write-vs-read intent analysis |
| Init/gate contracts (`ensureCryptoInit() → wasm.use()`, `rateLimitCheck() → db.x()`) | deferred — too fuzzy for AST-only heuristic; use `// eslint-disable-next-line` |

Full catalog: [`practices-react/pilot/round-3-empirical-validation.md#fp-clusters-identified`](./practices-react/pilot/round-3-empirical-validation.md#fp-clusters-identified).

## All rules

| Rule | Default | Catches |
|------|---------|---------|
| `react-async-parallel` | warn (recommended) | Independent awaits in async functions / RSC / Server Actions |
| `prefer-functional-setstate` | warn (recommended) | `setX(x...)` directly referencing state — risks stale closure |
| `no-broad-barrel-imports` | warn (opt-in) | `import { ... } from 'lodash'` etc. — barrel-import perf |
| `no-falsy-numeric-render` | error (opt-in) | `count && <UI>` renders literal `0` |
| `no-array-includes-in-loop` | warn (opt-in) | `arr.includes(x)` inside `.filter`/`.map` — should be Set |
| `no-array-mutate-on-state` | error (opt-in) | `.sort` / `.reverse` / `.splice` on React state or props |
| `no-inline-component-definition` | error (opt-in) | Capitalized JSX-returning function defined inside another component |

`react-async-parallel` and `prefer-functional-setstate` ship in the
`recommended` config because both have empirical TP examples on real repos.
The remaining five are opt-in until each accumulates ≥3 unrelated real-repo
TP examples.

## How rules are authored

This repo is also the source of a 68-rule evidence-anchored catalog
(`practices-react/rules/`) and a Claude Code skill (`/ax-transform`). Every
rule:

- Anchors to a `spec_ref` in [`specs/react-practices-l0.yaml`](./specs/react-practices-l0.yaml)
- Cites upstream documentation snapshots in `practices-react/upstream/`
- Survives a 4-phase curation pipeline (multi-source diversification → audit → codex consensus → continuous time-decay refresh)
- Passes 3 binary hard gates (`spec_ref` / `time_decay` / `evidence`) — `bash practices-react/evals/run.sh`

Methodology details: [`METHODOLOGY.md`](./METHODOLOGY.md).
Curation pipeline trail: [`practices-react/pilot/pilot-report.md`](./practices-react/pilot/pilot-report.md).

## Why this exists

AI coding assistants pattern-match pre-RSC tutorials and confidently generate
sequential awaits where parallelism is trivially correct. Documentation
can't catch this — AI ignores docs. Linting can — CI fails. One sharp rule
that catches the dominant async-perf mistake beats a 50-rule "AI best
practices" bundle every time.

## Repo layout

```
ax-template/
├── practices-react/                # ACTIVE — React 19 / Next.js 16 catalog
│   ├── eslint-plugin-ax/           # @ax/eslint-plugin-ax — 7 ESLint rules
│   ├── rules/                      # 68 evidence-anchored rule .md files
│   ├── upstream/                   # Canonical doc snapshots (90d decay)
│   ├── evals/                      # spec_ref / time_decay / evidence guards
│   ├── pilot/                      # Curation pipeline + empirical validations
│   ├── AGENTS.md                   # AI agent entry point (sha sentinel)
│   └── SKILL.md                    # Subsystem skill
│
├── specs/                          # Compliance specs per domain
├── contracts/                      # OpenAPI contracts
├── blueprints/                     # Policy manifests
│
├── practices/                      # FROZEN v1.0 Java/Spring catalog (64 rules)
│   └── STATUS.md                   # Frozen status + re-thaw criteria
├── archive/
│   └── backend-reference/          # FROZEN Spring Boot reference workload
│
├── skills/ax-transform/SKILL.md    # Claude Code skill entry point
├── .claude-plugin/plugin.json      # Claude Code plugin manifest
│
├── CLAUDE.md                       # Project identity (AI-agent context)
├── METHODOLOGY.md                  # 5-step blueprint playbook
├── frontend/                       # React reference workload (plugin self-testing)
└── verify/                         # Optional verification scripts
```

## Verification

### React catalog (active)

```bash
bash practices-react/evals/run.sh                  # 3 hard gates
cd practices-react/eslint-plugin-ax && npm test    # 28 valid + 6 invalid (7 RuleTester suites)
```

### Frozen Java reference workload (regression sanity)

```bash
cd archive/backend-reference
./gradlew testAsvs        # OWASP ASVS L1 (26 items, auth)
./gradlew testCrud        # CRUD spec (7 security tests)
./gradlew testPractices   # 64 frozen Java practices rules
./gradlew testPortability # advisory: rules applied to petclinic / realworld / modulith
```

### Optional pre-commit / pre-push hooks

```bash
bash practices/scripts/install-hooks.sh
```

Wires `.githooks/pre-commit` (catalog hard gates + AGENTS.md auto-regen + testPractices on backend-reference touch) and `.githooks/pre-push` (full regression). Hooks protect catalog quality only; they do not impose any git-workflow policy.

## License

MIT — see [`.claude-plugin/plugin.json`](./.claude-plugin/plugin.json).

## Related

- [`practices-react/SKILL.md`](./practices-react/SKILL.md) — subsystem skill
- [`practices-react/pilot/round-3-empirical-validation.md`](./practices-react/pilot/round-3-empirical-validation.md) — 19-repo measurement
- [`practices-react/pilot/external-validation.md`](./practices-react/pilot/external-validation.md) — Next.js 16 Todo app downstream consumption
- [`METHODOLOGY.md`](./METHODOLOGY.md) — 5-step blueprint
- [`CLAUDE.md`](./CLAUDE.md) — project identity
- [`practices/STATUS.md`](./practices/STATUS.md) — Java catalog frozen status
- [`archive/README.md`](./archive/README.md) — Spring Boot reference workload (frozen)
