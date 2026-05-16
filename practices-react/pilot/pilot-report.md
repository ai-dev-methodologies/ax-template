# practices-react/ Pilot Report — FINAL

**Date**: 2026-05-16
**Pipeline version**: 2026-05-16
**Status**: Complete. All deferred items shipped.

## Final totals

| Artifact | Count |
|---|---|
| `practices-react/rules/*.md` | **67 rules** (9 families, all 64 Vercel rules + 3 split-derived siblings) |
| `practices-react/upstream/*.snapshot.md` | **20 sha256-pinned snapshots** |
| `specs/react-practices-l0.yaml` items | **67 spec items** |
| `eslint-plugin-ax` rules | **7 rules** (paired 1:1 with rules whose verification.rule_id is set) |
| `eslint-plugin-ax` RuleTester suites | **7 passing** (24 valid + 21 invalid + others) |
| `frontend/eslint.config.mjs` | Production wired, 41 real waterfalls flagged in test code |

## Final commit log (in this body of work)

```
af7a8f4 feat(frontend): wire @ax/eslint-plugin-ax via flat config + npm lint script
81262f5 chore(practices-react): mark verification.status as 'shipped' for 7 rules
bfaf587 feat(eslint-plugin-ax): 6 additional ESLint rules
596a321 chore: gitignore .omx/ tooling cache
65f9743 feat(practices-react): use-cache siblings — 'use cache: private' and 'use cache: remote'
9134a92 feat(practices-react): final infrastructure — AGENTS.md + SKILL.md + pre-commit/CI wiring
b50e816 feat(practices-react): rerender family — 14 rules, compiler-first + correctness-first framing
b32a990 feat(practices-react): js family — 12 rules, bounded-cache & semantic correctness
56e5ef3 feat(practices-react): rendering family — 11 rules, hidden-effect & overuse caveats
18ea23b feat(practices-react): server family — 7 rules, durability/security caveats added
7361be7 feat(practices-react): async family — 4 rules, vendor-decoupled
b734dee feat(practices-react): client family — 4 rules, vendor-decoupled where over-coupled
bac28ba feat(practices-react): bundle family — 4 rules, React-portable framing
c055cce feat(practices-react): advanced family — 3 rules, useEffectEvent stable in 19.2
dc8e0f3 feat(practices-react): cross-stack catalog pilot — 6 rules with multi-source evidence + codex consensus
```

Total: ~15 atomic commits, family-grained, rollback-safe.

## Pipeline shape (final, applied to all 67 rules)

Every rule shipped through 4 phases:

1. **Reference diversification** — Vercel seed cross-checked against React 19 / Next.js 16 / MDN canonical docs + WebSearch consensus. Minimum 2 of `{canonical-react, canonical-nextjs, primitive-semantics}`.
2. **Per-rule audit** — accuracy, freshness, completeness, gap_check.
3. **Codex consensus** — `codex exec -s read-only -c model_reasoning_effort=high` (medium for simple rules). Verdict: SHIP_AS_IS / SHIP_WITH_AMEND / SPLIT / DROP.
4. **Continuous refresh** — every rule has `next_review_by` (90d default). `time_decay_guard.sh` BLOCKs on stale.

## ESLint plugin — final state

7 rules pair 1:1 with practices-react/rules whose `verification.rule_id` points here:

| Plugin rule | Practices-react rule | Severity in recommended |
|---|---|---|
| `ax/react-async-parallel` | `async-parallel.md` | warn |
| `ax/no-broad-barrel-imports` | `bundle-barrel-imports.md` | warn |
| `ax/no-falsy-numeric-render` | `rendering-conditional-render.md` | **error** |
| `ax/no-array-includes-in-loop` | `js-set-map-lookups.md` | warn |
| `ax/no-array-mutate-on-state` | `js-tosorted-immutable.md` | **error** |
| `ax/prefer-functional-setstate` | `rerender-functional-setstate.md` | warn |
| `ax/no-inline-component-definition` | `rerender-no-inline-components.md` | **error** |

`error` severity is reserved for **correctness bugs** (visible falsy rendering, mutated props, full remount). `warn` for style/perf nudges.

Frontend integration (`frontend/eslint.config.mjs`) wires the plugin via a relative `file:` dependency. `npm run lint` from `frontend/` runs all 7 rules on `src/` and `tests/`.

## Substantive defects pipeline caught across all 67 rules

Aggregate across the entire body of work: pipeline produced **~85+ substantive corrections** vs naive single-source mirroring. Sample of the most consequential:

- **rerender-memo**: Vercel framing inverts React 19 official guidance — pipeline flipped to "profile before manual memo, prefer compiler."
- **server-cache-react**: Vercel rule predates Next 16 Cache Components — pipeline SPLIT into React.cache (per-request dedup) + nextjs-use-cache (cross-request).
- **client-swr-dedup / client-event-listeners**: Vercel's SWR vendor coupling decoupled — TanStack/RTK/use() / singleton-listener-registry as library-agnostic alternatives.
- **async-dependencies**: `better-all` library demoted from primary to optional advanced tool.
- **rendering-activity**: Vercel rule omitted that hidden mode UNMOUNTS effects — added explicit warning.
- **rendering-hydration-no-flicker**: CSP nonce + XSS warnings added.
- **js-cache-function-results**: Bounded cache REQUIRED — unbounded Map leaks memory in long-lived processes.
- **js-cache-storage**: storage event does NOT fire on writing tab — manual invalidation in setter required.
- **js-flatmap-filter**: Primary value is SEMANTIC (filter(Boolean) drops legitimate 0/''/false), not perf.
- **rerender-no-inline-components**: Reframed as CORRECTNESS bug (lost focus / lost state / broken animations) rather than just perf.
- **nextjs-use-cache-private**: Lead with experimental + not-production warning; refactor-first hierarchy.
- **nextjs-use-cache-remote**: "When NOT to use" promoted to first-class decision gate.

## Binary verification (final state)

```
$ bash practices/evals/{spec_ref,time_decay,evidence}_guard.sh       # backward compat
  all 3 PASS

$ bash practices-react/evals/run.sh
  all 3 gates PASS

$ (cd practices-react/eslint-plugin-ax && npm test)
  7/7 RuleTester suites PASS

$ (cd frontend && npm run lint)
  0 errors, 41 warnings (real waterfalls in test code — pre-existing)
```

## Decisions resolved (the original 6 + post-scale-up)

| # | Decision | Resolution |
|---|---|---|
| 1 | spec file when? | Created `specs/react-practices-l0.yaml` with all 67 items |
| 2 | ESLint plugin when? | Shipped with 7 rules, all 7 in production frontend config |
| 3 | snapshots when? | All 20 captured |
| 4 | time_decay guard fork or extend? | Extended; `--catalog` flag, single source of truth |
| 5 | codex consensus storage? | Frontmatter `codex_consensus:` block; travels with rule |
| 6 | next 4 rules? | All 64 done + 3 split-derived siblings = 67 total |
| 7 | use-cache private/remote siblings? | Both shipped with strong "not production" / "when not to use" framing |
| 8 | ESLint rules for planned verification? | 6 additional rules shipped; all 7 plugin rules production-wired |

## What's NOT shipped (out of pilot scope by design)

These are deliberate non-goals — neither in the pilot nor in the scale-up:

- A polished `eslint-plugin-ax` README and CI release pipeline. The plugin is currently consumed via relative `file:` path; publishing to npm would require its own scoping decision.
- TypeScript-aware lint rules. The plugin uses ESPree/typescript-eslint/parser for parsing only; rules that need full type info (e.g. "is this prop value really a `User`?") would need a typed lint approach — separate effort.
- Migration scripts to fix the 41 pre-existing waterfalls in the frontend test code. These are real bugs but cosmetic for the pilot; a separate cleanup PR can take them on.
- A `react-practices-l1.yaml` spec compiling stricter constraints (CRITICAL-only items, hard-gated). Current `react-practices-l0.yaml` is the catalog projection; a hard gate spec would be a separate exercise.
- Cross-repo distribution as a Claude Code plugin. The catalog is currently anchored to `ax-template/practices-react/`; turning it into a `@ax/react-practices` skill that AI agents can `chub get` is a packaging step.

## Generalization signals (final)

1. **Single source mirroring ≠ catalog quality.** ~85+ substantive corrections across 67 rules — averages >1.25 substantive findings per rule.
2. **Codex consensus is meaningful, not noise.** Every codex amendment was either a real correction or a productive framing flip.
3. **Rule splitting is a natural pipeline output.** server-cache-react split, nextjs-use-cache + private + remote trio, plus several merge-candidate observations.
4. **Pipeline scales down for easy rules.** Simple JS rules (set-map-lookups, min-max-loop) finished in ~10 min without over-engineering.
5. **Catalog-agnostic infrastructure works.** Same 3 guards + same generate_agents.sh pattern + same pre-commit / CI wiring serve both practices/ (Java) and practices-react/ (React).
6. **ESLint custom rules are a viable binary verification layer.** 7 rules + 7 RuleTester suites + production frontend lint = the "verification" axis is no longer advisory; it has teeth.

## Sources used

Primary docs:
- [Vercel agent-skills/react-best-practices](https://github.com/vercel-labs/agent-skills/tree/main/skills/react-best-practices)
- [React 19 docs](https://react.dev/reference/react)
- [React 19 — Activity](https://react.dev/reference/react/Activity)
- [React 19 — cache()](https://react.dev/reference/react/cache)
- [React 19 — lazy()](https://react.dev/reference/react/lazy)
- [React 19 — memo()](https://react.dev/reference/react/memo)
- [React 19 — use()](https://react.dev/reference/react/use)
- [React 19 — useDeferredValue](https://react.dev/reference/react/useDeferredValue)
- [React 19 — useEffectEvent](https://react.dev/reference/react/useEffectEvent)
- [React 19 — useTransition](https://react.dev/reference/react/useTransition)
- [React — You Might Not Need an Effect](https://react.dev/learn/you-might-not-need-an-effect)
- [React Compiler](https://react.dev/learn/react-compiler)
- [Next.js 16 docs](https://nextjs.org/docs/app)
- [Next.js 16 — Caching](https://nextjs.org/docs/app/getting-started/caching)
- [Next.js 16 — Fetching Data](https://nextjs.org/docs/app/getting-started/fetching-data)
- [Next.js 16 — Lazy Loading](https://nextjs.org/docs/app/guides/lazy-loading)
- [Next.js 16 — 'use cache'](https://nextjs.org/docs/app/api-reference/directives/use-cache)
- [Next.js 16 — 'use cache: private'](https://nextjs.org/docs/app/api-reference/directives/use-cache-private)
- [Next.js 16 — 'use cache: remote'](https://nextjs.org/docs/app/api-reference/directives/use-cache-remote)
- [Next.js 16 — optimizePackageImports](https://nextjs.org/docs/app/api-reference/config/next-config-js/optimizePackageImports)
- [Next.js 16 — after()](https://nextjs.org/docs/app/api-reference/functions/after)
- [Next.js — Authentication](https://nextjs.org/docs/app/guides/authentication)

Primitives:
- [MDN — Promise.all](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/all)
- [MDN — Promise.allSettled](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/allSettled)
- [MDN — Set](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set)
- [MDN — localStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage)
- [MDN — addEventListener (passive)](https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener)
- [MDN — script element](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/script)

Vendor engineering:
- [Vercel blog — How we optimized package imports in Next.js](https://vercel.com/blog/how-we-optimized-package-imports-in-next-js)
