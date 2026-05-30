# `practices-react/` — React 19 / Next.js 16 best-practices catalog

86 evidence-anchored rules for React 19 + Next.js 16 applications. Vercel's
`react-best-practices` skill is the seed; each rule was cross-checked against
React 19, Next.js 16, and MDN canonical docs, then codex-reviewed before
shipping. The catalog ships with a paired ESLint plugin and a binary
verification suite.

> **External validation passed**: a clean Next.js 16 + React 19.2 Todo app
> consumed the plugin via `file:` dependency and followed 11 catalog rules in
> production code — all gates green (lint 0/0, build clean, Playwright e2e
> 3/3). See `pilot/external-validation.md` for the full report.

## Quick start (downstream consumer)

Install the ESLint plugin in your Next.js / React app:

```bash
# When @ax/eslint-plugin-ax is published to npm:
npm install --save-dev eslint @ax/eslint-plugin-ax

# Until then, file: from a sibling checkout of ax-template:
npm install --save-dev eslint \
  'file:../ax-template/practices-react/eslint-plugin-ax'
```

Wire `eslint.config.mjs` (ESLint 9 flat config):

```js
import globals from 'globals'
import tsParser from '@typescript-eslint/parser'
import axPlugin from '@ax/eslint-plugin-ax'

export default [
  { ignores: ['.next', 'dist', 'node_modules'] },
  {
    files: ['app/**/*.{ts,tsx}', 'src/**/*.{ts,tsx}', 'components/**/*.{ts,tsx}'],
    languageOptions: {
      parser: tsParser,
      ecmaVersion: 2024,
      sourceType: 'module',
      globals: { ...globals.browser, ...globals.es2022, ...globals.node },
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    plugins: { ax: axPlugin },
    rules: axPlugin.configs.recommended.rules,
  },
]
```

Add a lint script and run it:

```json
{ "scripts": { "lint": "eslint ." } }
```

```bash
npm run lint
```

## Reading the catalog

- **`AGENTS.md`** — auto-generated full concatenation of every rule with a sha
  sentinel. Good for AI agent ingestion.
- **`SKILL.md`** — Claude Code skill manifest. Family table, downstream
  consumption paths, when-the-skill-activates rules.
- **`rules/*.md`** — individual rule files. Each carries an `audit` block,
  `codex_consensus` verdict, `evidence` block citing snapshots + external
  sources, `applicable_to` (react / nextjs / vite), and `verification` shape.
- **`upstream/*.snapshot.md`** — sha256-pinned snapshots of upstream docs
  (React, Next.js, MDN, Vercel). The audit trail for every rule's claims.
- **`upstream/_MANIFEST.yaml`** — snapshot index with fetched_at dates.
  `time_decay_guard.sh` BLOCKs the catalog if any snapshot is older than 90
  days without re-curation.

## Rule families

| Family | Count | Theme |
|---|---|---|
| `async-` | 6 | Eliminating waterfalls + Next.js 16 async params |
| `bundle-` | 5 | Bundle size (lazy imports, third-party defer, conditional, preload) |
| `server-` | 9 | Server-side (Cache Components, use-cache variants, parallel fetching, RSC serialization, auth actions, after()) |
| `client-` | 4 | Client data-fetching (server-state dedup, listeners, passive events, localStorage schema) |
| `rerender-` | 15 | Re-render correctness + perf (memo, derived state, refs, transitions, no-inline-components) |
| `rendering-` | 11 | Rendering performance (Activity, content-visibility, hydration, scripts, resource hints) |
| `js-` | 13 | JavaScript performance (Set/Map, immutable arrays, regex, caching, iteration) |
| `advanced-` | 3 | Advanced effect callback patterns (useEffectEvent, init-once, handler refs) |
| `nextjs-` | 2 | Next.js-specific extensions (use-cache directive, use-cache private/remote) |

## ESLint plugin — what each rule catches

| Rule | Severity | Catches |
|---|---|---|
| `ax/no-inline-component-definition` | **error** | Components defined inside other components (full remount, lost state) |
| `ax/no-array-mutate-on-state` | **error** | `.sort/.reverse/.splice` on props or `useState`-tuple arrays |
| `ax/no-falsy-numeric-render` | **error** | `numeric && <JSX>` patterns that render literal "0" / "NaN" |
| `ax/react-async-parallel` | warn | Independent consecutive awaits that could be `Promise.all` |
| `ax/no-broad-barrel-imports` | warn | Named imports from configured expensive barrel packages |
| `ax/no-array-includes-in-loop` | warn | `arr.includes()` / `.find()` inside iterator callbacks |
| `ax/prefer-functional-setstate` | warn | `setX(<expr referencing X>)` instead of `setX(curr => ...)` |

## Pipeline (for catalog maintainers)

Every rule shipped through 4 phases:

1. **Reference diversification** — Vercel seed cross-checked against React 19 / Next.js 16 / MDN canonical docs.
2. **Per-rule audit** — accuracy, freshness, completeness, gap_check.
3. **Codex consensus** — `codex exec -s read-only -c model_reasoning_effort=high`.
4. **Continuous refresh** — every rule has `next_review_by` (90d default); `time_decay_guard` BLOCKs on stale.

Binary verification:

```bash
bash practices-react/evals/run.sh
#   spec_ref_guard    every rule has spec_ref pointing to existing spec
#   time_decay_guard  every snapshot is within 90d
#   evidence_guard    every rule's evidence anchors to snapshot or external citation

(cd practices-react/eslint-plugin-ax && npm test)
#   7 RuleTester suites
```

See `pilot/pilot-report.md` for the full pipeline trail and
`pilot/external-validation.md` for the external-consumption proof.

## Adding a new rule (maintainers)

1. Capture upstream snapshot in `upstream/<id>.snapshot.md`
2. Add MANIFEST entry with SHA + bytes + URL + fetched_at + tier
3. Run the 4-phase pipeline (multi-source → audit → codex → write)
4. Write rule to `rules/<family>-<id>.md` with full frontmatter:
   - `spec_ref` pointing to `specs/react-practices-l0.yaml#<ITEM-ID>`
   - `audit` block (4 checks)
   - `codex_consensus` block with verdict + agreements
   - `evidence` block citing snapshot + external sources
   - `next_review_by` date (default 90d ahead)
5. Add corresponding spec item to `specs/react-practices-l0.yaml`
6. (optional) Add paired ESLint rule under `eslint-plugin-ax/rules/`
7. Run `bash practices-react/generate_agents.sh` to refresh AGENTS.md + SKILL.md
8. Run `bash practices-react/evals/run.sh` — all 3 gates must PASS
9. Commit atomically

## License

Apache-2.0
