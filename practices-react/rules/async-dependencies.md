---
title: For partial-dependency graphs, chain dependent promises and aggregate with Promise.all
impact: HIGH
impactDescription: "Maximizes concurrency when some calls depend on others' outputs. Independent calls run in parallel; dependent calls start as soon as their input resolves. Third-party libraries (better-all) are an advanced option, not the primary recommendation."
tags:
  - async
  - parallelization
  - dependencies
  - promise-graph
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ASYNC-003"
verification:
  type: review
  status: manual
  notes: "Reviewer checks each promise that has dependencies: (a) root promises are initiated before any await, (b) dependent promises chain via .then() from the input promise rather than awaiting then re-initiating, (c) final await uses Promise.all over the full set."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
  completeness:
    status: complete
    amendments:
      - "Promoted dep-less promise-chain + Promise.all pattern to primary recommendation"
      - "Demoted `better-all` to optional advanced note"
      - "Removed unverified 2-10× CRITICAL claim (varies by workload)"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: async-dependencies"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-dependencies.md"
    role: seed
  - id: mdn-promise-all
    title: "MDN — Promise.all"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/all"
    role: primitive-semantics
evidence:
  - upstream_id: vercel-react-best-practices
    section: "async-dependencies"
    quote: "For operations with partial dependencies, use [...] to maximize parallelism. It automatically starts each task at the earliest possible moment."
  - source_type: external
    citation: "MDN — Promise.then for chaining; combined with Promise.all aggregates the wait at the latest possible moment"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/then"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Drop better-all as primary"
    - "Make dep-less promise chain the canonical pattern"
sibling_rules:
  - async-parallel
  - async-defer-await
---

## For partial-dependency graphs, chain dependent promises and aggregate with Promise.all

**Impact: HIGH — Maximizes concurrency when some calls depend on others' outputs. Independent calls run in parallel; dependent calls start as soon as their input resolves. Third-party libraries (`better-all`) are an advanced option, not the primary recommendation.**

### Incorrect — `profile` waits unnecessarily

```typescript
const [user, config] = await Promise.all([fetchUser(), fetchConfig()])
// Both finished. NOW we start fetching profile, but profile only needed user, not config.
const profile = await fetchProfile(user.id)
```

`fetchProfile` could have started the moment `fetchUser` resolved — config is irrelevant to it. The intermediate `await Promise.all` forces a join that's not needed by profile.

### Correct (primary) — chain dependents, aggregate at the end

```typescript
const userPromise = fetchUser()
// profilePromise starts as soon as userPromise resolves, NOT after config returns.
const profilePromise = userPromise.then((user) => fetchProfile(user.id))

const [user, config, profile] = await Promise.all([
  userPromise,
  fetchConfig(),
  profilePromise,
])
```

This is plain JS. No library. The dependent promise chains via `.then` from the input promise; `Promise.all` joins everything at the latest possible moment.

### Advanced (optional) — `better-all` for complex graphs

For projects that already use `better-all` (a third-party DSL for partial-dep parallelization), the DSL form is cleaner at scale:

```typescript
import { all } from 'better-all'

const { user, config, profile } = await all({
  async user() { return fetchUser() },
  async config() { return fetchConfig() },
  async profile() { return fetchProfile((await this.$.user).id) },
})
```

Consider adopting `better-all` **only if** (a) you have many such graphs, (b) the team agrees on the dependency, and (c) the DSL improves readability for your codebase. Otherwise stick to the dep-less form.

### Naming convention

Use `xPromise` suffix on un-awaited values. Makes intent obvious to readers — these are not values yet, they're in-flight promises being chained.

### Anti-pattern

```typescript
// BAD: re-initiating instead of chaining.
const user = await fetchUser()
const profile = await fetchProfile(user.id)   // sequential wait
const config = await fetchConfig()            // sequential wait
```

Sources:

- [Vercel: async-dependencies](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-dependencies.md)
- [MDN — Promise.all](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/all)
