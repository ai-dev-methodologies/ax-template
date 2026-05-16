---
name: practices-react
description: React 19 / Next.js 16 best-practices reviewer for the ax-template practices-react catalog. Triggers when editing practices-react/rules/*.md, specs/react-practices-l0.yaml, or frontend/ source files. Provides the 8-family / 65-rule evidence-anchored catalog (Vercel react-best-practices seed cross-checked against React 19 / Next.js 16 / MDN canonical docs and codex consensus).
metadata:
  priority: 4
  docs:
    - "https://react.dev/reference/react"
    - "https://nextjs.org/docs/app"
    - "https://github.com/vercel-labs/agent-skills/tree/main/skills/react-best-practices"
  pathPatterns:
    - 'practices-react/rules/**/*.md'
    - 'practices-react/upstream/**/*'
    - 'specs/react-practices-*.yaml'
    - 'frontend/src/**/*.{ts,tsx,js,jsx}'
    - 'practices-react/eslint-plugin-ax/**/*'
  bashPatterns: []
  importPatterns:
    - 'from "react"'
    - 'from "next/'
    - 'from "react-dom'
retrieval:
  aliases:
    - react practices
    - react best practices
    - nextjs practices
    - react catalog
    - practices-react
    - vercel react best practices
  intents:
    - add a new react rule
    - review react/nextjs code quality
    - check rule frontmatter
    - audit a vercel rule
    - run the curation pipeline
  entities:
    - React 19
    - Next.js 16
    - React Server Components
    - React Compiler
    - useEffectEvent
    - useDeferredValue
    - Activity component
    - 'use cache'
---

# practices-react SKILL

This is the AI agent entry-point for the **`practices-react/`** catalog: 65 React 19 / Next.js 16 best-practice rules, evidence-anchored, codex-reviewed, and continuously time-decay-guarded.

## When this skill activates

- You are editing a file under `practices-react/rules/`, `specs/react-practices-l0.yaml`, or `practices-react/upstream/`.
- You are writing or reviewing React / Next.js code (`frontend/src/**`).
- You are adding a NEW rule to the catalog.
- You are running the 4-phase curation pipeline.

## The catalog

| Family | Rules | Coverage |
|---|---|---|
| async- | 5 | Eliminating waterfalls (parallel, dependencies, defer-await, api-routes, suspense-boundaries) |
| bundle- | 5 | Bundle size (barrel-imports, dynamic-imports, defer-third-party, conditional, preload) |
| server- | 9 | Server-side (cache-react, use-cache, auth-actions, cache-lru, dedup-props, hoist-static-io, serialization, parallel-fetching, after-nonblocking) |
| client- | 4 | Client data-fetching (swr-dedup, event-listeners, passive-listeners, localstorage-schema) |
| rerender- | 15 | Re-render optimization (memo, defer-reads, dependencies, derived-state, derived-state-no-effect, functional-setstate, lazy-state-init, memo-with-default-value, simple-expression-in-memo, split-combined-hooks, move-effect-to-event, transitions, use-deferred-value, use-ref-transient-values, no-inline-components) |
| rendering- | 11 | Rendering performance (animate-svg-wrapper, content-visibility, hoist-jsx, svg-precision, hydration-no-flicker, hydration-suppress-warning, activity, conditional-render, usetransition-loading, resource-hints, script-defer-async) |
| js- | 13 | JavaScript performance (set-map-lookups, batch-dom-css, index-maps, cache-property-access, cache-function-results, cache-storage, combine-iterations, length-check-first, early-exit, hoist-regexp, min-max-loop, tosorted-immutable, flatmap-filter) |
| advanced- | 3 | Advanced patterns (event-handler-refs, init-once, use-latest) |

## Pipeline

Every rule shipped through 4 phases (see `practices-react/pilot/pilot-report.md`):

1. **Reference diversification** — Vercel seed cross-checked against React 19 / Next.js 16 / MDN canonical docs and web search consensus. Minimum 2 of `{canonical-react, canonical-nextjs, primitive-semantics}`.
2. **Per-rule audit** — accuracy, freshness, completeness, gap_check.
3. **Codex consensus** — `codex exec -s read-only -c model_reasoning_effort=high` (medium for simple rules). Verdict: SHIP_AS_IS / SHIP_WITH_AMEND / SPLIT / DROP.
4. **Continuous refresh** — every rule has `next_review_by` (90d default). `time_decay_guard.sh` BLOCKs on stale.

## Binary gates

```bash
bash practices-react/evals/run.sh
#   spec_ref_guard    every rule has spec_ref pointing to existing spec file
#   time_decay_guard  every snapshot is younger than 90d threshold
#   evidence_guard    every rule's evidence anchors to manifest or external citation
```

```bash
cd practices-react/eslint-plugin-ax && npm test
#   custom ESLint rules tested with RuleTester
```

## Adding a new rule

1. Capture upstream snapshot in `practices-react/upstream/<id>.snapshot.md`.
2. Add MANIFEST entry with SHA + bytes + URL + fetched_at + tier.
3. Run the 4-phase pipeline (multi-source → audit → codex → write).
4. Write rule to `practices-react/rules/<family>-<id>.md` with full frontmatter:
   - `spec_ref` pointing to a `specs/react-practices-l0.yaml#<ITEM-ID>`
   - `audit` block (4 checks)
   - `codex_consensus` block with verdict + agreements
   - `evidence` block citing snapshot + external sources
   - `provenance.pipeline_steps`
   - `next_review_by` date (default 90d ahead)
5. Add corresponding spec item to `specs/react-practices-l0.yaml`.
6. Run `bash practices-react/generate_agents.sh` to refresh AGENTS.md.
7. Run `bash practices-react/evals/run.sh` — all 3 gates must PASS.
8. Commit atomically.

## Cross-rule policy

- React Compiler-first: do NOT push manual `memo` / `useMemo` / `useCallback` as default. The compiler does that.
- Vendor-neutral where possible: SWR is one option among TanStack Query / RTK Query / `use()`. `better-all` is optional.
- Security caveats are first-class: `rendering-hydration-no-flicker` requires CSP nonce; `server-serialization` calls out PII/secret leak risk.
- Stale gates: any snapshot older than 90 days triggers `time_decay_guard` BLOCK — re-fetch, diff, codex-review, ship updated rule.

## Sibling pointers

- Spring/Java catalog: `practices/` (64 rules, 21 categories)
- Verifying compliance specs: `specs/react-practices-l0.yaml`, `specs/spring-practices-l0.yaml`
- Methodology: `METHODOLOGY.md` (Spec Trio + portable tests + binary verification)
- Pilot report: `practices-react/pilot/pilot-report.md` (full audit trail for the first 6 rules; pattern carries to all 65)

## Resources

- Vercel react-best-practices: https://github.com/vercel-labs/agent-skills/tree/main/skills/react-best-practices
- React 19 docs: https://react.dev
- Next.js 16 docs: https://nextjs.org/docs/app
