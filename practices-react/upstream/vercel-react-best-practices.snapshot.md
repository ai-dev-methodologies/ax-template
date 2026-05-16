# Snapshot: Vercel react-best-practices (seed catalog)

- **source**: https://github.com/vercel-labs/agent-skills/tree/main/skills/react-best-practices
- **role**: seed-catalog
- **license**: Apache-2.0 (per repo)
- **fetched_at**: 2026-05-16T00:00:00Z
- **via**: Claude Code Skill tool (locally installed: `/Users/kyjin/.claude/plugins/cache/claude-plugins-official/vercel/0.40.0/skills/react-best-practices/`)
- **version_observed**: vercel plugin 0.40.0

## Skill structure (index)

64 rules across 8 categories, priority-ordered:

| Priority | Category | Prefix | Impact |
|----------|----------|--------|--------|
| 1 | Eliminating Waterfalls | `async-` | CRITICAL |
| 2 | Bundle Size Optimization | `bundle-` | CRITICAL |
| 3 | Server-Side Performance | `server-` | HIGH |
| 4 | Client-Side Data Fetching | `client-` | MEDIUM-HIGH |
| 5 | Re-render Optimization | `rerender-` | MEDIUM |
| 6 | Rendering Performance | `rendering-` | MEDIUM |
| 7 | JavaScript Performance | `js-` | LOW-MEDIUM |
| 8 | Advanced Patterns | `advanced-` | LOW |

## Captured rule: async-parallel.md (verbatim)

```yaml
---
title: Promise.all() for Independent Operations
impact: CRITICAL
impactDescription: 2-10× improvement
tags: async, parallelization, promises, waterfalls
---
```

> ## Promise.all() for Independent Operations
>
> When async operations have no interdependencies, execute them concurrently using `Promise.all()`.
>
> **Incorrect (sequential execution, 3 round trips):**
>
> ```typescript
> const user = await fetchUser()
> const posts = await fetchPosts()
> const comments = await fetchComments()
> ```
>
> **Correct (parallel execution, 1 round trip):**
>
> ```typescript
> const [user, posts, comments] = await Promise.all([
>   fetchUser(),
>   fetchPosts(),
>   fetchComments()
> ])
> ```

## Audit notes for this snapshot

- Vercel rule has **no version metadata** (no `lastUpdated`, no `version`, no `react_version`).
- The skill is distributed via the Claude Code plugin marketplace (`vercel:react-best-practices`); the snapshot SHA below pins this specific revision.
- "3 round trips" wording in the "Incorrect" caption is misleading per MDN's Promise.all semantics — see `mdn-promise-all.snapshot.md`.
