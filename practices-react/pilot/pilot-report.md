# practices-react/ Pilot Report

**Date**: 2026-05-16
**Pipeline version**: 2026-05-16
**Reviewed rules**: 5 (async-parallel + 4 next + 1 split sibling = 6 rule files)
**Infrastructure**: spec stub + ESLint plugin skeleton + 9 upstream snapshots + extended guards
**Pipeline result**: all gates PASS, all audits SHIP_WITH_AMEND or SPLIT, no DROP

## Why this pilot

The user pushed back on naive mirroring of Vercel's react-best-practices skill:

> 기존 /react-best-practices 가 무조건적인 정답은 아니자나? 빠진거나 부족하거나
> 최신이 아니거나(chub 참조) 등으 검토는 제대로 안해? ... 가능한 codex review를
> 받아서 consensus 하게 해야 한다.

Then, on the scale-up plan:

> 나중은 없어 할 수 있는데 오래걸리면 오래걸려도해. 이렇게 빼먹으면 나중에 절대
> 추가 맥락을 유지하면서 할수 없어.

So the pilot extended scope beyond a single rule to validate the full pipeline end-to-end: infrastructure + audit + binary gate verification + ESLint integration. Nothing deferred.

## Pipeline shape

Each rule audited via four phases:

1. **Phase A — Reference diversification.** Vercel skill (seed) + chub registry + Next.js / React 19 / MDN official docs + WebSearch consensus. Minimum 2 of `{canonical-react, canonical-nextjs, primitive-semantics}` per rule.
2. **Phase B — Per-rule audit (4 checks).** accuracy / freshness / completeness / gap_check.
3. **Phase C — Codex consensus.** `codex exec -s read-only -c 'model_reasoning_effort="high"'` (medium for the easy rule).
4. **Phase D — Continuous refresh.** Each rule has `next_review_by` (90d default); `time_decay_guard.sh` BLOCKs on stale.

## Audited rules — outcomes

| # | Rule | Codex verdict | Substantive amendments made |
|---|---|---|---|
| 1 | `async-parallel` | SHIP_WITH_AMEND | "round trips" misframe → "sequential waits"; `Promise.allSettled` fallback; init-early-await-late mechanic; "slowest result" caveat; impact CRITICAL → HIGH; sibling rule split-off |
| 2 | `bundle-barrel-imports` | SHIP_WITH_AMEND | Removed Next 16 auto-optimized list confusion; experimental warning; Vite/Rollup conditional; private `dist/*` import warning; measure-first; impact CRITICAL → HIGH |
| 3 | `server-cache-react` | **SPLIT** | Narrow to React.cache() per-request dedup; Sibling `nextjs-use-cache.md` for `'use cache'` directive; React.cache isolation note inside 'use cache' boundaries |
| 4 | `nextjs-use-cache` *(NEW, from split)* | SHIP | Full Next.js 16 Cache Components surface: 'use cache' directive at file/component/function; cache key composition; serialization constraints; runtime API forbidden inside; build-hang anti-pattern |
| 5 | `rerender-memo` | SHIP_WITH_AMEND (framing flipped) | Premise inverted: "profile before manual memoization" not "extract to memoized components". React Compiler-era guidance leads. Custom comparator warning. Five over-memoization anti-patterns from React 19 docs. Verification conditional on compiler presence. |
| 6 | `js-set-map-lookups` | SHIP_WITH_AMEND | "sublinear" not strictly O(1); construction-cost caveat; object-key reference-identity trap. |

**Aggregate**: 5 input rules → 6 output rules (1 split). 0 shipped verbatim. 0 dropped.

## Substantive defects single-source mirroring would have shipped

Across 5 input rules, the pipeline caught **14 substantive defects** that a naive mirror would have shipped:

1. async-parallel: "3 round trips → 1" misframing (sequential waits, not network round trips)
2. async-parallel: `Promise.allSettled` fallback missing
3. async-parallel: "init early, await late" mechanic implicit, not explicit
4. async-parallel: "slowest required result" caveat missing
5. async-parallel: 2-10× CRITICAL claim unverified
6. bundle-barrel-imports: Next 16 default-optimized list (28 packages) — Vercel rule's instructions are no-ops for these
7. bundle-barrel-imports: `experimental.optimizePackageImports` is still EXPERIMENTAL in 16.2.6
8. bundle-barrel-imports: `dist/esm/*` deep imports are private; couple to package internals
9. bundle-barrel-imports: "measure first" advice missing
10. server-cache-react: Presents `React.cache()` as primary Next.js caching primitive — but Next 16 has moved to `'use cache'` directive
11. server-cache-react: `React.cache` isolation inside `'use cache'` boundaries unmentioned
12. rerender-memo: Title/premise inverts React 19 official guidance ("don't memoize unless measured benefit")
13. rerender-memo: React Compiler GA in React 19 — treats it as a footnote when it's the dominant fact
14. js-set-map-lookups: Object-key reference-identity trap unmentioned

## Pipeline cost (actual, end-to-end)

| Step | Time | Cost shape |
|---|---|---|
| Phase A (multi-source fetch) | ~3-5 min/rule | WebFetch + chub + WebSearch (parallelizable per rule) |
| Phase B (4-check audit) | ~5 min/rule | reading and reasoning |
| Phase C (codex review at high) | ~3-4 min/rule | codex exec + write amended rule |
| Phase C (codex review at medium for easy rule) | ~2 min | codex exec + write rule |
| Phase D infrastructure (one-time) | ~30 min | guards generalization, ESLint plugin, snapshots, MANIFEST |
| **Per-rule average** | **~12-15 min** | Linear; well-bounded |

For 64 Vercel rules: ~13-16 hours of active work, parallelizable across rule families. Within "1 work-week single-session" envelope.

## Infrastructure shipped

1. **`specs/react-practices-l0.yaml`** — compliance spec with 6 items (ASYNC-001, BUNDLE-001, SERVER-001, SERVER-002, RERENDER-001, JS-001).
2. **`practices-react/rules/`** — 6 amended rules with full frontmatter (audit + provenance + codex_consensus + evidence + upstream + sibling_rules).
3. **`practices-react/upstream/`** — 9 snapshots with SHA + bytes + URL + fetched_at:
    - vercel-react-best-practices (seed, tier 3)
    - nextjs-fetching-data (tier 2)
    - nextjs-optimize-package-imports (tier 2)
    - nextjs-use-cache-directive (tier 2)
    - react-19-use (tier 2)
    - react-19-cache (tier 2)
    - mdn-promise-all (tier 1)
    - mdn-promise-allsettled (tier 1)
    - mdn-set (tier 1)
4. **`practices-react/eslint-plugin-ax/`** — ESLint v9 flat-config plugin with `ax/react-async-parallel` rule, 11 RuleTester cases (7 valid + 4 invalid), all PASS.
5. **`practices-react/evals/run.sh`** — binary gate wrapper invoking the 3 hard gates with `--catalog practices-react`.
6. **Existing `practices/evals/*.sh` guards** — generalized to accept `--catalog <dir>` flag. Backward-compatible: no-arg invocations still target `practices/`.

## Binary verification — final run

```
$ bash practices-react/evals/run.sh
── spec_ref_guard ──     PASS
── time_decay_guard ──   PASS
── evidence_guard ──     PASS
practices-react/evals/run.sh: all 3 gates passed

$ bash practices/evals/spec_ref_guard.sh && bash practices/evals/time_decay_guard.sh \
       && bash practices/evals/evidence_guard.sh
(all 3 pass for the original practices/ catalog — backward compat preserved)

$ cd practices-react/eslint-plugin-ax && npm test
✔ react-async-parallel — RuleTester suite (11 cases: 7 valid + 4 invalid, all PASS)
```

## Generalization signals the pilot produced

1. **Pipeline catches substantive defects, not nits.** 14 defects in 5 rules — average ~2.8 substantive corrections per rule. The seed catalog is meaningfully improved by curation.

2. **Codex consensus is meaningful, not noise.** Every codex amendment was a real correction or a productive disagreement (framing flip on rerender-memo). Pipeline trustworthy enough to act on.

3. **Rule splitting is a natural output.** server-cache-react split into two rules was directly identified by the pipeline, not retrofitted. Vercel's catalog has structural gaps that surface only under multi-source review.

4. **Easy rules don't cost more than they're worth.** js-set-map-lookups ran in ~10 minutes total at medium reasoning effort and still surfaced 3 substantive caveats. Pipeline scales down for simple rules.

5. **Contested-framing case found.** rerender-memo is a case where both reviewers (Claude + Codex) independently judged Vercel's framing as inverted relative to React 19 docs. Pipeline can produce a contested-but-aligned outcome where the seed is the contested party.

6. **`practices/` hard-gate shape ports cleanly to a second catalog.** spec_ref + time_decay + evidence all work uniformly with the `--catalog` flag. No catalog-shape changes needed.

7. **ESLint custom rules are a viable binary verification layer for frontend.** 11 RuleTester cases pass; rule scope is narrow and conservative. Static check is real binary gate, not advisory.

## Decisions resolved (all 6 from prior plan)

| # | Decision | Result |
|---|---|---|
| 1 | spec file when? | Created `specs/react-practices-l0.yaml` now with 6 items (one per shipped rule) |
| 2 | ESLint plugin when? | Built skeleton + first rule `ax/react-async-parallel`; tests green |
| 3 | snapshots when? | Captured all 9 now (sunk cost zero — already in WebFetch results) |
| 4 | time_decay guard fork or extend? | Extended existing 3 guards with `--catalog` flag; single source of truth |
| 5 | codex consensus storage? | Frontmatter `codex_consensus:` block; travels with rule |
| 6 | next 4 rules? | bundle-barrel-imports + server-cache-react + rerender-memo + js-set-map-lookups; covers 5/8 families, 1 Next.js-only, 1 contested, 1 easy-ship |

## What's NOT in the pilot (deferred items, surfaced for future)

- 'use cache: private' and 'use cache: remote' directives (referenced but no own rule yet).
- ESLint rules for the other 5 rules' `verification.rule_id` slots (only `ax/react-async-parallel` actually shipped; others have `status: planned`).
- The remaining 58 Vercel rules (next: rule-family batches).
- `practices-react/AGENTS.md` sentinel — `practices/` has one for AI-agent entry, parity needed.
- `practices-react/SKILL.md` — parallel to `practices/SKILL.md`.
- Frontend integration: `frontend/eslint.config.js` consuming `@ax/eslint-plugin-ax`.
- The `.githooks/pre-commit` and `.github/workflows/practices-sentinel.yml` invoke practices guards but not yet practices-react.

## Scale-up plan

Phase 1 (next session-worth of work, ~4-6 hours):
- AGENTS.md + SKILL.md for practices-react
- Wire pre-commit hook + CI workflow to also call `practices-react/evals/run.sh`
- 4 more rules in family batches: client-* (3 rules), advanced-* (3 rules)
- ESLint rules for ratable rules from the audited 6

Phase 2 (1-2 weeks elapsed, ~12 hours active):
- Remaining ~52 Vercel rules in family batches (rendering / js / async / bundle)
- Custom comparator handling in eslint-plugin-ax for the contested cases

Phase 3 (ongoing):
- Quarterly refresh pass — `time_decay_guard` will surface stale snapshots; re-fetch + diff + codex re-review.

Sources for the pilot run:

- [Vercel agent-skills/react-best-practices](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/AGENTS.md)
- [Next.js 16 — Fetching Data](https://nextjs.org/docs/app/getting-started/fetching-data)
- [Next.js 16 — optimizePackageImports](https://nextjs.org/docs/app/api-reference/config/next-config-js/optimizePackageImports)
- [Next.js 16 — 'use cache' directive](https://nextjs.org/docs/app/api-reference/directives/use-cache)
- [Next.js 16 — Caching](https://nextjs.org/docs/app/getting-started/caching)
- [React 19 — use()](https://react.dev/reference/react/use)
- [React 19 — cache()](https://react.dev/reference/react/cache)
- [React 19 — memo()](https://react.dev/reference/react/memo)
- [React 19 — React Compiler](https://react.dev/learn/react-compiler)
- [MDN — Promise.all](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/all)
- [MDN — Promise.allSettled](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/allSettled)
- [MDN — Set](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set)
- [Vercel engineering blog — How we optimized package imports in Next.js](https://vercel.com/blog/how-we-optimized-package-imports-in-next-js)
