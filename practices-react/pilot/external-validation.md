# External validation — ax-validation-todo

**Date**: 2026-05-17
**Validator app**: `~/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-validation-todo/`
**Stack**: Next.js 16.2.6 + React 19.2.4 + Tailwind 4 + TypeScript
**Plugin consumption**: `@ax/eslint-plugin-ax` via `file:../ax-template/practices-react/eslint-plugin-ax`
**Status**: All binary checks green

## What this validation tested

The catalog has been self-applied inside `ax-template/` since shipping. This
exercise asks the harder question: **does the catalog actually work when
fork-받은 팀 (external project) tries to use it?** Specifically:

1. Can an AI agent enter the catalog cold and find what's relevant for a
   real task?
2. Does the ESLint plugin install + activate cleanly as an external
   dependency?
3. Does following the catalog rules produce code that builds, types, lints,
   and runs in a real browser end-to-end?
4. What gaps surface only when the catalog is held at arm's length?

## Binary verification (all green)

```
npm run lint    →  0 errors / 0 warnings
npm run build   →  ✓ Compiled successfully + TypeScript clean
npm run dev     →  HTTP 200 from / with correct structural HTML
npm run e2e     →  3/3 Playwright tests pass
                   • empty state renders
                   • add → reload → todo persists (Server Action + cookie)
                   • toggle done + delete (count badge updates correctly)
```

## Catalog rules exercised in production code

| Rule | Where applied | Notes |
|---|---|---|
| `async-suspense-boundaries` | `app/page.tsx` | Page is NOT async; async `<TodoList />` streams via `<Suspense fallback={<TodoListSkeleton />}>` |
| `server-parallel-fetching` | `app/page.tsx` composition | Form + Suspense boundary siblings, no top-level await in page |
| `server-auth-actions` | `app/actions.ts` (3 actions) | Each action follows auth → validate → authorize → mutate. Cookie issuance is the auth gate |
| `server-cache-react` | `lib/owner.ts` | `getOwner = cache(async () => ...)` — request-scoped dedup of cookie read |
| `server-serialization` | `app/_components/TodoItem.tsx` | DTO-shaped props (id/title/done) — internal `createdAt` not crossed |
| `rerender-no-inline-components` | All components | Module-level definitions; no inner components |
| `rerender-derived-state-no-effect` | `app/_components/TodoList.tsx` | `completed` count derived during render, not in state |
| `rendering-conditional-render` | `app/_components/TodoList.tsx` | `total > 0 ? ... : null` (not `total && ...`) |
| `rendering-hoist-jsx` | `app/page.tsx` | `TodoListSkeleton` at module scope |
| `js-tosorted-immutable` | `lib/store.ts` | `.toSorted()` for sort views; spread for add/toggle/delete |
| `js-cache-function-results` (rationale) | `lib/store.ts` | `byOwner` Map is documented as unbounded with caveat — real app would LRU |

11 catalog rules visibly anchored in app code, all surface-level naturally
on a small Server-Action-based app. No rule felt forced or unused.

## ESLint plugin — external-dep experience

The plugin installed cleanly via `file:../ax-template/practices-react/eslint-plugin-ax`:

- `import axPlugin from '@ax/eslint-plugin-ax'` resolves from the sibling repo
- `plugins: { ax: axPlugin }` + the 7 rule names from `plugin.configs.recommended` worked first try
- Lint runs in <1s for the small codebase, 0/0 output
- No version mismatch issues even though the plugin lives outside the app's node_modules tree

This validates the **package shape** — the plugin can be consumed without being
published. Publishing to npm is mechanical from here.

## UX gaps discovered (catalog improvements to track)

These are real findings — they did not surface during self-application:

### 1. SKILL.md `pathPatterns` is anchored to the source repo

```yaml
pathPatterns:
  - 'practices-react/rules/**/*.md'
  - 'specs/react-practices-*.yaml'
  - 'frontend/src/**/*.{ts,tsx,js,jsx}'
```

For an external project the source frontend path is irrelevant. The skill
auto-activation triggers only inside the source repo. Two fixes:

- Replace `frontend/src/**/*` with a glob that captures the GENERIC pattern
  agents care about (`**/*.{ts,tsx,js,jsx}` would be too broad; better:
  `app/**/*.{ts,tsx}` + `**/{components,lib}/**/*.{ts,tsx}` for typical
  Next.js layouts).
- Add a manifest-style "downstream consumption" note documenting that this
  is the source repo and the skill should re-target paths when packaged.

### 2. SKILL.md description says "65 rules" — actual is 67

Drift from the rerender split and the use-cache siblings. The description
should be auto-generated from the rule count or audited periodically.

### 3. AGENTS.md is lexical-order — no task-driven entry

A 67-rule lexical concatenation is hard to scan when you have a specific
task ("I'm building a Server-Action-based form"). Today the AI agent reads
SKILL.md's family table, picks a few likely families, and dives. That works
but could be better. Possible improvements:

- Family-grouped index at the top of AGENTS.md (links into each rule)
- Task → rule map ("Server Action work? → SERVER-003, ASYNC-005, SERVER-007")
- `applicable_to` filtered views (`/AGENTS.md?stack=nextjs` style — but
  static markdown doesn't really do that)

### 4. AI agent UX is currently "read 67 rules then write code"

A simulated AI agent (me) does NOT actually read all 67 rules cold. The
practical flow was:

1. Open SKILL.md, see the family table
2. Identify task → families: Server Action (server family), Suspense
   (async family), composition (rerender/rendering family)
3. Open AGENTS.md, grep for relevant family prefixes
4. Apply rules during code writing, with the `applicable_to` and
   `verification.rule_id` fields as the most useful frontmatter

The catalog is rich for *reviewing* code but not optimized for *cold writing*.
Reviewing is the safer-default activation; for writing, a "starter pack" of
top-10 most-applicable rules per stack/task type would lower the activation
threshold.

### 5. `next-async-params-parallel` is referenced as sibling but not yet shipped

In `async-parallel.md` the sibling list includes `next-async-params-parallel`.
This sibling was identified in the pilot as a SPLIT output (Next.js 16 async
params should be its own rule) but never written. The Todo app didn't hit
this case (only static routes, no dynamic params), but a downstream project
with `/users/[id]/page.tsx` would feel the gap.

### 6. Cookie-issuing actions and `'use cache: private'` overlap

The Todo app needed to issue + read a cookie. The `'use cache: private'` rule
says this is the experimental-and-not-production case. In practice the issue
was simpler: we just used `cookies()` outside any cached scope. Catalog rule
SERVER-010 could benefit from a "first try this simpler form" example before
diving into the `'use cache: private'` directive.

## What I would change in the catalog as a result

Priority order:

1. **Ship `next-async-params-parallel` sibling rule** — closes the dangling sibling reference.
2. **Add a `starter-pack-nextjs.md` doc** — top 10 rules to read first for a fresh Next.js project; faster cold start for AI agents.
3. **Update SKILL.md `pathPatterns`** to capture downstream-consumption patterns (or move the source-repo patterns to a `sourcePathPatterns` field, with `pathPatterns` describing general consumer-side activation).
4. **Auto-regenerate SKILL.md rule count** as part of `generate_agents.sh` so the description never drifts.
5. **Document the consumer install pattern** — `file:` for sibling-repo install, npm publish target name, peer dep on eslint 9+.

## Generalization signal (the actual finding)

Catalog produced **production-quality code on the first pass** for a typical
Server-Action-based Next.js app. No rules felt out-of-place. ESLint plugin
worked as an external dependency. Build + types + lint + e2e all green.

The catalog is **operational**, not aspirational. The remaining work is
documentation polish + a few referenced-but-unshipped sibling rules.

## App artifacts (for reference)

- `app/page.tsx` — Suspense composition
- `app/actions.ts` — 3 Server Actions following SERVER-003 ordering
- `app/_components/{AddTodoForm,TodoItem,TodoList}.tsx` — module-level components
- `lib/{store,owner}.ts` — store + cookie-scoped owner
- `eslint.config.mjs` — flat config consuming `@ax/eslint-plugin-ax`
- `tests/e2e/todo.spec.ts` — 3 Playwright e2e tests

Commits referenced: this validation runs against ax-template HEAD = `2b54215`
(post pre-push tested + green).

Sources used during validation:
- [Next.js 16 — Fetching Data](https://nextjs.org/docs/app/getting-started/fetching-data)
- [Next.js 16 — Server Actions](https://nextjs.org/docs/app/api-reference/functions/server-actions)
- [React 19 — cache()](https://react.dev/reference/react/cache)
- [React 19 — use()](https://react.dev/reference/react/use)
