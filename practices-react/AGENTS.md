---
sentinel:
  source_concat_sha256: "b54b14bda1fc7459c59a7566f677b952f1be95c879cf66fb3c10608c6dc35fe4"
  rule_count: 77
  generated_by: "practices-react/generate_agents.sh"
---

# Practices-React — AGENTS.md (auto-generated)

This file is auto-generated from `practices-react/rules/*.md` in lexical order.
Do not edit by hand — re-run `practices-react/generate_agents.sh` after rule changes.

## Pipeline

Every rule below shipped through a 4-phase curation pipeline:

1. **Reference diversification** — Vercel react-best-practices skill (seed) cross-checked against React 19 / Next.js 16 / MDN canonical docs.
2. **Per-rule audit** — accuracy, freshness, completeness, gap_check.
3. **Codex consensus** — independent second opinion via `codex exec -s read-only`.
4. **Continuous refresh** — each rule has `next_review_by`; time_decay_guard BLOCKs on stale.

See `practices-react/pilot/pilot-report.md` for the full audit trail.

<!-- @source rules/advanced-event-handler-refs.md -->

---
title: Store event handlers in refs as a fallback when useEffectEvent is unavailable
impact: LOW
impactDescription: "Stable subscriptions (window/document/external listeners) that need the latest handler without re-subscribing. On React 19.2+ prefer useEffectEvent; this ref pattern is the fallback for older React or non-Effect subscription APIs."
tags:
  - advanced
  - hooks
  - refs
  - event-handlers
  - optimization
  - react
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ADVANCED-001"
verification:
  type: review
  status: manual
  notes: "Reviewer checks that the ref-backed handler isn't hiding a dependency that should re-trigger the Effect; checks React version to prefer useEffectEvent when ≥19.2."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Ref-backed handler still works for stable subscriptions."
  freshness:
    status: partially-stale
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "React 19.2 stabilizes useEffectEvent — Vercel rule's 'if you're on latest React' framing now means 19.2+. Re-position as fallback."
  completeness:
    status: complete
    amendments:
      - "Prefer useEffectEvent on React 19.2+; ref pattern is fallback"
      - "Add warning: do not hide dependencies that should re-trigger the Effect"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (advanced-event-handler-refs)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/advanced-event-handler-refs.md"
    role: seed
  - id: react-19-useeffectevent
    title: "React 19 — useEffectEvent (stable in 19.2)"
    url: "https://react.dev/reference/react/useEffectEvent"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "advanced-event-handler-refs"
    quote: "Store callbacks in refs when used in effects that shouldn't re-subscribe on callback changes."
  - source_type: external
    citation: "React 19 docs — useEffectEvent (the non-stable identity acts as a runtime assertion: if your code incorrectly depends on the function identity, you'll see the Effect re-running on every render)"
    url: "https://react.dev/reference/react/useEffectEvent"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Ref pattern still works"
    - "Should reposition as fallback to useEffectEvent on 19.2+"
    - "Warn against hiding deps"
sibling_rules:
  - advanced-use-latest
---

## Store event handlers in refs as a fallback when useEffectEvent is unavailable

**Impact: LOW — Stable subscriptions that need the latest handler without re-subscribing. On React 19.2+ prefer `useEffectEvent`; this ref pattern is the fallback for older React or non-Effect subscription APIs.**

> **Positioning note.** The seed source presents this as the primary pattern with `useEffectEvent` as alternative. As of React 19.2 the priority is reversed: prefer `useEffectEvent` (sibling rule `advanced-use-latest.md`), use this ref pattern only when not on 19.2+ or when subscribing via APIs that need a stable listener identity (e.g. third-party SDKs that compare listener identity).

**Incorrect — re-subscribes every render:**

```tsx
function useWindowEvent(event: string, handler: (e: Event) => void) {
  useEffect(() => {
    window.addEventListener(event, handler)
    return () => window.removeEventListener(event, handler)
  }, [event, handler])  // handler identity changes every render
}
```

**Fallback (pre-19.2) — ref-backed handler, stable subscription:**

```tsx
function useWindowEvent(event: string, handler: (e: Event) => void) {
  const handlerRef = useRef(handler)
  useEffect(() => {
    handlerRef.current = handler
  }, [handler])

  useEffect(() => {
    const listener = (e: Event) => handlerRef.current(e)
    window.addEventListener(event, listener)
    return () => window.removeEventListener(event, listener)
  }, [event])
}
```

**Preferred (React 19.2+) — useEffectEvent:**

```tsx
import { useEffectEvent } from 'react'

function useWindowEvent(event: string, handler: (e: Event) => void) {
  const onEvent = useEffectEvent(handler)
  useEffect(() => {
    window.addEventListener(event, onEvent)
    return () => window.removeEventListener(event, onEvent)
  }, [event])
}
```

### Anti-patterns

- Hiding a real dependency. If a value should cause the Effect to re-run, keep it as a dependency. Don't use refs (or Effect Events) to silence React's reactivity rules.
- Putting the ref-read inside render code instead of inside the listener — defeats the freshness purpose.

Sources:

- [Vercel: advanced-event-handler-refs](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/advanced-event-handler-refs.md)
- [React 19 — useEffectEvent](https://react.dev/reference/react/useEffectEvent)


<!-- @source rules/advanced-init-once.md -->

---
title: Initialize app-wide state once at module scope, not inside a component's useEffect
impact: LOW-MEDIUM
impactDescription: "Prevents duplicate initialization under React StrictMode dev double-mount and remount scenarios. App-wide state belongs at module scope, not in component lifecycle."
tags:
  - initialization
  - useEffect
  - app-startup
  - side-effects
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ADVANCED-002"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) app-wide init (load storage / check auth) is at module scope or guarded by `didInit`, NOT in component useEffect; (b) the guard is module-level not component-level."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "useEffect([]) is unreliable under StrictMode dev double-mount; the didInit pattern is React-docs-canonical."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Pattern is stable across React 18/19; SSR/RSC contexts add the typeof-window guard but the principle holds."
  completeness:
    status: complete
    amendments:
      - "Added preferred module-init option (typeof window check outside any component)"
      - "Clarified scope: this is for app-wide one-time init, not per-component subscriptions / effects needing cleanup"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (advanced-init-once)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/advanced-init-once.md"
    role: seed
  - id: react-you-might-not-need-effect
    title: "React docs — You Might Not Need an Effect (initializing the application)"
    url: "https://react.dev/learn/you-might-not-need-an-effect"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "advanced-init-once"
    quote: "Do not put app-wide initialization that must run once per app load inside useEffect([]) of a component."
  - source_type: external
    citation: "React docs — Initializing the application (canonical didInit pattern + module-init alternative)"
    url: "https://react.dev/learn/you-might-not-need-an-effect#initializing-the-application"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Directionally right"
    - "Should mention module-init option in addition to didInit guard"
    - "Scope clarification needed (app-wide init, not component lifecycle)"
sibling_rules: []
---

## Initialize app-wide state once at module scope, not inside a component's useEffect

**Impact: LOW-MEDIUM — Prevents duplicate initialization under React StrictMode dev double-mount and remount scenarios. App-wide state belongs at module scope, not component lifecycle.**

### Scope

This rule applies to **app-wide one-time init** that should run exactly once per app load:

- Loading state from `localStorage` / `sessionStorage`
- Reading & validating an auth token
- Connecting analytics SDKs
- Pre-warming a singleton client (only if it has no per-render config)

It does **not** apply to per-component subscriptions, cleanup-bearing effects, or per-mount setup — those legitimately belong in `useEffect`.

### Incorrect — runs twice in dev, re-runs on remount

```tsx
function App() {
  useEffect(() => {
    loadFromStorage()
    checkAuthToken()
  }, [])
}
```

React docs verbatim: "However, you'll quickly discover that it runs twice in development. This can cause issues — for example, maybe it invalidates the authentication token because the function wasn't designed to be called twice."

### Correct (preferred) — module-level init, browser-guarded

```tsx
if (typeof window !== 'undefined') {
  loadFromStorage()
  checkAuthToken()
}

function App() {
  // ...
}
```

Run once per app load, naturally. The `typeof window` guard prevents the init from executing during SSR.

> **Caveat from React docs:** "Code at the top level runs once when your component is imported — even if it doesn't end up being rendered. To avoid slowdown or surprising behavior when importing arbitrary components, don't overuse this pattern. Keep app-wide initialization logic to root component modules like `App.js` or in your application's entry point."

### Correct (acceptable) — didInit guard inside useEffect

If you must keep the init inside a component (e.g. it depends on a context value), use a module-level `didInit` flag:

```tsx
let didInit = false

function App() {
  useEffect(() => {
    if (didInit) return
    didInit = true
    loadFromStorage()
    checkAuthToken()
  }, [])
}
```

This survives StrictMode's intentional dev-only double-mount because `didInit` lives at module scope, not component scope.

Sources:

- [Vercel: advanced-init-once](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/advanced-init-once.md)
- [React docs — Initializing the application](https://react.dev/learn/you-might-not-need-an-effect#initializing-the-application)


<!-- @source rules/advanced-use-latest.md -->

---
title: Use useEffectEvent (React 19.2+) for non-reactive callbacks inside Effects
impact: LOW
impactDescription: "Prevents Effect re-runs when a callback identity changes but should NOT trigger reconnection. Replaces the older useRef-backed pattern with a clean, stable API."
tags:
  - advanced
  - hooks
  - useEffectEvent
  - effects
  - optimization
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ADVANCED-003"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) React 19.2+ available, (b) useEffectEvent is only called inside Effects (never during render, never as a prop, never in dependency arrays), (c) Effect Event isn't hiding a dependency that should re-trigger the Effect."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Pattern is correct when onSearch is non-reactive and only `query` should re-trigger."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "useEffectEvent is STABLE in React 19.2 (released Oct 2025). Earlier 19.0-19.1 needed experimental_useEffectEvent."
  completeness:
    status: complete
    amendments:
      - "Stated version requirement: 19.2+ stable, 19.0-19.1 experimental_"
      - "Added 3 misuse warnings from React docs (hide deps / call during render / pass to components)"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (advanced-use-latest)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/advanced-use-latest.md"
    role: seed
  - id: react-19-useeffectevent
    title: "React 19 — useEffectEvent (stable in 19.2)"
    url: "https://react.dev/reference/react/useEffectEvent"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "advanced-use-latest"
    quote: "Access latest values in callbacks without adding them to dependency arrays. Prevents effect re-runs while avoiding stale closures."
  - source_type: external
    citation: "React docs — useEffectEvent: 'If a value should cause your Effect to re-run, keep it as a dependency. Only use Effect Events for logic that genuinely should not re-trigger your Effect.'"
    url: "https://react.dev/reference/react/useEffectEvent"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Accurate when onSearch is non-reactive"
    - "Add version requirement"
    - "Add 3 misuse warnings"
sibling_rules:
  - advanced-event-handler-refs
---

## Use useEffectEvent (React 19.2+) for non-reactive callbacks inside Effects

**Impact: LOW — Prevents Effect re-runs when a callback identity changes but should NOT trigger reconnection. Replaces the older useRef-backed pattern with a clean, stable API.**

### Version requirement

- **React 19.2+**: `useEffectEvent` is stable.
- **React 19.0–19.1**: use `experimental_useEffectEvent`.
- **React 18 and earlier**: not available; use the ref-backed fallback (sibling rule `advanced-event-handler-refs.md`).

### Incorrect — Effect re-runs on every callback change

```tsx
function SearchInput({ onSearch }: { onSearch: (q: string) => void }) {
  const [query, setQuery] = useState('')
  useEffect(() => {
    const timeout = setTimeout(() => onSearch(query), 300)
    return () => clearTimeout(timeout)
  }, [query, onSearch])   // onSearch identity may change every parent render
}
```

### Correct — Effect Event reads latest onSearch without re-running on its identity

```tsx
import { useEffectEvent } from 'react'

function SearchInput({ onSearch }: { onSearch: (q: string) => void }) {
  const [query, setQuery] = useState('')
  const onSearchEvent = useEffectEvent(onSearch)
  useEffect(() => {
    const timeout = setTimeout(() => onSearchEvent(query), 300)
    return () => clearTimeout(timeout)
  }, [query])   // onSearchEvent intentionally excluded
}
```

### Three misuse warnings (React docs verbatim)

1. **Don't use Effect Events to hide dependencies.** If a value should cause the Effect to re-run, keep it in the dep array. Effect Events are for logic that genuinely should not re-trigger.

2. **Don't call during render.** Effect Events are only legitimate inside Effects.

3. **Don't pass Effect Events to other components, and don't put them in dependency arrays.** Effect Events have intentionally unstable identity per render — this is a runtime assertion to catch misuse.

### Comparison to the ref-backed pattern (sibling rule)

| | Ref pattern | useEffectEvent |
|---|---|---|
| React version | any | 19.2+ stable |
| Mechanic | manual ref sync + read inside listener | built-in |
| Latest-value guarantee | yes | yes |
| Stable identity | yes (the ref is stable) | **intentionally NOT stable** (runtime assertion) |
| Failure mode | silent stale closures if ref sync forgotten | loud — Effect re-runs every render if misused |

Prefer `useEffectEvent` on 19.2+ — the unstable identity is a feature, not a bug.

Sources:

- [Vercel: advanced-use-latest](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/advanced-use-latest.md)
- [React 19 — useEffectEvent](https://react.dev/reference/react/useEffectEvent)


<!-- @source rules/async-api-routes.md -->

---
title: API route / Server Action specialization — auth gate first, then start independent work eagerly
impact: HIGH
impactDescription: "Handler-shaped waterfall has the same mechanic as async-parallel but adds API-specific constraints: auth ordering, request cancellation, mutation ordering, and rate-limit gates. Apply init-early-await-late within those constraints."
tags:
  - api-routes
  - server-actions
  - waterfalls
  - parallelization
  - nextjs
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ASYNC-004"
verification:
  type: review
  status: manual
  notes: "Reviewer for each API route / Server Action: (a) auth/session check before business logic, (b) independent reads started eagerly after auth, (c) mutations and side-effect-producing calls respect required ordering, (d) cancellation/abort semantics if request can be aborted."
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
      - "Explicitly framed as API-route/SA specialization of async-parallel"
      - "Added auth-first ordering constraint"
      - "Added caveats for mutations, transactions, rate limits, cancellation, ordered side effects"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: async-api-routes"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-api-routes.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "async-api-routes"
    quote: "In API routes and Server Actions, start independent operations immediately, even if you don't await them yet."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - async-parallel
  - async-dependencies
  - async-defer-await
---

## API route / Server Action specialization — auth gate first, then start independent work eagerly

**Impact: HIGH — Handler-shaped waterfall has the same mechanic as `async-parallel` but adds API-specific constraints: auth ordering, request cancellation, mutation ordering, and rate-limit gates.**

### Scope

This rule narrows `async-parallel` to the API-route / Server-Action context. The mechanic is the same (init early, await late). The constraints below are extra.

### Correct — auth first, then parallelize independent reads

```typescript
export async function GET(request: Request) {
  // 1. Cheap gates first. If auth fails, return without doing any other work.
  const session = await auth()
  if (!session?.user) return Response.json({ error: 'unauthorized' }, { status: 401 })

  // 2. Independent reads — kick off concurrently.
  const configPromise = fetchConfig()
  const dataPromise = fetchData(session.user.id)

  // 3. Join at the latest moment.
  const [config, data] = await Promise.all([configPromise, dataPromise])

  return Response.json({ data, config })
}
```

### Incorrect — sequential await waterfall

```typescript
export async function GET(request: Request) {
  const session = await auth()
  const config = await fetchConfig()        // could have started in parallel
  const data = await fetchData(session.user.id)
  return Response.json({ data, config })
}
```

### API-specific constraints — don't apply init-early-await-late blindly

1. **Auth must come first.** Never start business reads/writes before the auth gate resolves. A pending-auth-but-already-started DB write is the canonical "Hyrum's law" footgun.

2. **Mutations have ordering.** `UPDATE x` then `UPDATE y` may differ from running both with `Promise.all` if the second depends on side effects of the first (triggers, denormalized fields).

3. **Transactions scope a unit.** Don't fire un-awaited promises outside a transaction and expect them to be part of it. Either await sequentially inside the transaction, or commit before parallelizing.

4. **Rate-limit gates count requests.** Starting 3 DB calls eagerly to "save latency" may exhaust a connection pool, paradoxically making the route slower. Profile under load.

5. **Request cancellation.** If the client aborts (closed tab, network failure), `request.signal` should propagate to all in-flight fetches you started. Otherwise you continue paying for work nobody is waiting for.

```typescript
export async function GET(request: Request) {
  const session = await auth()
  if (!session?.user) return Response.json({ error: 'unauthorized' }, { status: 401 })

  const signal = request.signal
  const configPromise = fetchConfig({ signal })
  const dataPromise = fetchData(session.user.id, { signal })

  const [config, data] = await Promise.all([configPromise, dataPromise])
  return Response.json({ data, config })
}
```

6. **Server Actions can be invoked multiple times concurrently.** A mutation that doesn't tolerate concurrent invocation needs idempotency keys or a serializing primitive — outside the scope of this rule.

### When NOT to parallelize

- The "independent" calls share a transaction or critical-section lock.
- One call's success is a precondition for the next (then it's a dependency — see `async-dependencies`).
- The downstream service can't handle the parallel load — sequential is the polite path.

Sources:

- [Vercel: async-api-routes](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-api-routes.md)


<!-- @source rules/async-defer-await.md -->

---
title: Move `await` into the branch that actually uses the result; place cheap guards first
impact: HIGH
impactDescription: "Avoids blocking on data the early-return path discards. Especially valuable when the skip path is common (cache hits, permission denials, validation failures) or the deferred operation is expensive."
tags:
  - async
  - await
  - conditional
  - early-return
  - optimization
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ASYNC-002"
verification:
  type: review
  status: manual
  notes: "Reviewer checks each async function: (a) cheap guards run before expensive awaits, (b) awaited results are referenced on every path that reaches them, (c) intentional side-effecting awaits (auth, validation, transaction) are not deferred."
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
      - "Added side-effect caveat: don't defer awaits whose purpose IS the side effect (auth/validation/log/transaction)"
      - "Clarified relationship to async-parallel (different concern: necessity, not parallelism)"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: async-defer-await"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-defer-await.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "async-defer-await"
    quote: "Move await operations into the branches where they're actually used to avoid blocking code paths that don't need them."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - async-parallel
  - async-api-routes
---

## Move `await` into the branch that actually uses the result; place cheap guards first

**Impact: HIGH — Avoids blocking on data the early-return path discards. Especially valuable when the skip path is common (cache hits, permission denials, validation failures) or the deferred operation is expensive.**

### Incorrect — blocks both branches

```typescript
async function handleRequest(userId: string, skipProcessing: boolean) {
  const userData = await fetchUserData(userId)
  if (skipProcessing) return { skipped: true }
  return processUserData(userData)
}
```

### Correct — early-return first, then fetch

```typescript
async function handleRequest(userId: string, skipProcessing: boolean) {
  if (skipProcessing) return { skipped: true }
  const userData = await fetchUserData(userId)
  return processUserData(userData)
}
```

### Correct — cheapest validation first, then independent work in parallel

```typescript
async function updateResource(resourceId: string, userId: string) {
  const resource = await getResource(resourceId)
  if (!resource) return { error: 'Not found' }

  // Permission check only after resource exists
  const permissions = await fetchPermissions(userId)
  if (!permissions.canEdit) return { error: 'Forbidden' }

  return updateResourceData(resource, permissions)
}
```

### Side-effect caveat — do NOT defer these

Some `await`s exist for the side effect, not the return value. Don't reorder them:

- **Auth/session validation** at the top of a handler — moving it past business logic creates security bugs.
- **Input validation** that throws on bad shapes — better to fail fast than after expensive work.
- **Audit logs** that must record every request — deferring past an early return loses logs.
- **Transaction setup** (`BEGIN TRANSACTION`) — must precede the work it scopes.

The rule is "defer awaits whose **result** is unused on the early path", not "defer all awaits".

### Relationship to async-parallel

This is a different concern from async-parallel (sibling rule):
- `async-parallel` says: independent work should run **at the same time**.
- `async-defer-await` says: unneeded work should not run **at all**.

Both can apply at once. After cheap guards pass, kick off the required independent work in parallel.

Sources:

- [Vercel: async-defer-await](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-defer-await.md)


<!-- @source rules/async-dependencies.md -->

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


<!-- @source rules/async-parallel.md -->

---
title: Initiate independent promises early, then await with Promise.all (or allSettled)
impact: HIGH
impactDescription: "Eliminates avoidable sequential-await waterfalls in async data flows; preserves correctness around partial failure"
tags:
  - async
  - parallelization
  - promises
  - waterfalls
  - react
  - nextjs
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ASYNC-001"
verification:
  type: eslint
  rule_id: "ax/react-async-parallel"
  status: shipped
  notes: "Custom ESLint rule planned; until shipped, peer-review checkpoint"
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Core pattern confirmed by both Next.js 16.2.6 docs and MDN; Vercel's 'fewer round trips' wording corrected — Promise.all does not reduce round trips, it removes per-await sequential blocking."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Anchored to Next.js 16.2.6 and React 19. Re-review when Next.js 17 or React 20 publishes a parallel-fetch guidance change."
  completeness:
    status: complete
    amendments:
      - "Added Promise.allSettled fallback for partial-failure tolerance (per Next.js docs explicit callout)"
      - "Made 'initiate early, await late' the primary mechanic (per Next.js docs canonical example)"
      - "Clarified that Promise.all waits for the slowest required result (per MDN)"
      - "Cross-referenced React 19 promise-as-prop + use() pattern (related rule async-suspense-boundaries)"
  gap_check:
    status: split
    note: "Next.js 16 async params (params: Promise<T>) interaction with Promise.all is a separate concern; tracked as sibling rule next-async-params-parallel."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (rule: async-parallel)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-parallel.md"
    role: "seed"
  - id: nextjs-fetching-data
    title: "Next.js 16 docs — Fetching Data"
    url: "https://nextjs.org/docs/app/getting-started/fetching-data"
    version: "16.2.6"
    fetched: "2026-05-13"
    role: "canonical-example"
  - id: react-19-use
    title: "React 19 — use() hook reference"
    url: "https://react.dev/reference/react/use"
    role: "boundary-pattern"
  - id: mdn-promise-all
    title: "MDN — Promise.all"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/all"
    role: "primitive-semantics"
  - id: mdn-promise-allsettled
    title: "MDN — Promise.allSettled"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/allSettled"
    role: "partial-failure-fallback"
evidence:
  - upstream_id: vercel-react-best-practices
    section: "async-parallel"
    quote: "When async operations have no interdependencies, execute them concurrently using Promise.all()."
  - upstream_id: nextjs-fetching-data
    section: "Parallel data fetching"
    quote: "Start multiple requests by calling fetch, then await them with Promise.all. Requests begin as soon as fetch is called."
  - upstream_id: nextjs-fetching-data
    section: "Parallel data fetching — Good to know"
    quote: "If one request fails when using Promise.all, the entire operation will fail. To handle this, you can use the Promise.allSettled method instead."
  - upstream_id: react-19-use
    section: "Server Component data fetching"
    quote: "Prefer creating Promises in Server Components and passing them to Client Components over creating Promises in Client Components."
  - upstream_id: mdn-promise-all
    section: "Description"
    quote: "It rejects when any of the input's promises rejects, with this first rejection reason."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=high"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "accuracy/freshness/completeness/gap_check verdicts confirmed"
    - "Promise.all is an aggregation primitive, not a parallelization trigger"
    - "Next.js async params deserves a separate sibling rule"
    - "'init early, await late' stays folded into this rule (not over-extracted)"
  amendments_required:
    - "Replace 'fewer round trips' framing — it is sequential-wait elimination, not network round trip reduction"
    - "Add Promise.allSettled / partial failure caveat"
    - "Note that Promise.all waits for the slowest required result"
    - "Caveat the unverified '2-10×' impact claim — benchmark not anchored"
sibling_rules:
  - next-async-params-parallel
  - async-suspense-boundaries
  - async-dependencies
---

## Initiate independent promises early, then await with Promise.all (or allSettled)

**Impact: HIGH — Eliminates avoidable sequential-await waterfalls in async data flows; preserves correctness around partial failure**

The mechanic is "initiate early, await late". `Promise.all` does not start work — it
aggregates. Work begins the moment a promise-returning call is made. A sequential
chain of `await` statements blocks each call until the previous one resolves, even
when the calls are independent. Calling them all first (without `await`) lets them
run concurrently; aggregating with `Promise.all` then waits for the **slowest required
result**. Use `Promise.allSettled` when partial success is acceptable — `Promise.all`
rejects the entire aggregate on the first rejection.

> **Note on impact claim:** The seed source labels this CRITICAL with a "2–10×"
> improvement claim. The improvement is real but the multiplier is workload-dependent
> (it equals N for N independent equal-latency calls). The rule is downgraded to
> HIGH here because the multiplier is unverified per-call-site and depends on the
> distribution of latencies, not on this rule alone.

**Incorrect — sequential awaits block each call (waterfall):**

```typescript
// Each await blocks the next request from starting.
// Total latency ≈ sum of all latencies.
export default async function Page() {
  const user = await fetchUser()
  const posts = await fetchPosts()
  const comments = await fetchComments()
  return <Dashboard user={user} posts={posts} comments={comments} />
}
```

**Correct — initiate early, await late:**

```typescript
// All three requests fire as soon as the calls are made.
// Total latency ≈ MAX of the three latencies, not the sum.
export default async function Page() {
  const userPromise = fetchUser()
  const postsPromise = fetchPosts()
  const commentsPromise = fetchComments()

  const [user, posts, comments] = await Promise.all([
    userPromise,
    postsPromise,
    commentsPromise,
  ])

  return <Dashboard user={user} posts={posts} comments={comments} />
}
```

**Partial-failure tolerance — use Promise.allSettled when one failure should not
collapse the whole render:**

```typescript
const settled = await Promise.allSettled([
  fetchUser(),
  fetchOptionalRecommendations(),
  fetchNotices(),
])

const user = settled[0].status === 'fulfilled' ? settled[0].value : null
const recs = settled[1].status === 'fulfilled' ? settled[1].value : []
const notices = settled[2].status === 'fulfilled' ? settled[2].value : []
```

### React 19 / Next.js 16 nuances

- In a Server Component, prefer `async`/`await` over `use()` for direct fetching.
  Create promises in the Server Component and pass them to Client Components as
  props when you want streaming via Suspense — see sibling rule
  `async-suspense-boundaries`.
- In Next.js 16, route `params` and `searchParams` are promises themselves. If a
  call depends on a param value, await the param first; if the call does not depend
  on it, include the param promise in the same `Promise.all` to avoid an extra
  sequential await — see sibling rule `next-async-params-parallel`.
- By default the Next.js App Router renders sibling layouts and pages in parallel,
  so this rule is about avoiding waterfalls **within** a single component body, not
  across the route tree.

### When the pattern does not apply

- Truly dependent operations: `const user = await fetchUser(); const posts = await
  fetchPostsForUser(user.id)` — parallelization here would be wrong. For partial
  dependency graphs, `Promise.all` can leave easy wins on the table; the
  `async-dependencies` rule covers the partial-dependency case.
- Side-effecting writes with ordering requirements: aggregation discards the
  ordering signal of sequential awaits.

### Verification

- Static check (planned): custom ESLint rule `ax/react-async-parallel` flags two
  or more consecutive top-level `await` statements that share no `await`-bound
  identifiers, inside the same async function body, where each awaited expression
  is a call (i.e., independent network or DB I/O).
- Manual: code review checkpoint until the ESLint rule ships.


<!-- @source rules/async-suspense-boundaries.md -->

---
title: Stream wrapper UI fast — fetch in Server Components, pass promises down, resolve with use() inside Suspense
impact: HIGH
impactDescription: "Wrapper layout renders immediately; data-dependent regions stream in via Suspense. Pattern: Server Component creates promise → passes to Client Component as prop → Client Component reads with use() inside a Suspense boundary."
tags:
  - async
  - suspense
  - streaming
  - server-components
  - use-hook
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ASYNC-005"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) async functions are Server Components (not Client), (b) promises are created in Server Components (not in Client Component render), (c) Suspense boundary wraps the data-needing region (not the whole page) when partial streaming is the goal, (d) layout shift caveats applied (skeleton sized close to final content)."
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
    notes: "React 19 use() stable; Next.js 16 canonical pattern for streaming Client Component data is exactly this shape."
  completeness:
    status: complete
    amendments:
      - "Clarified Server vs Client Component boundaries (async = SC only)"
      - "Promise creation must be in SC, not Client Component render (recreated every render)"
      - "Layout-shift caveat tied to skeleton sizing"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: async-suspense-boundaries"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-suspense-boundaries.md"
    role: seed
  - id: react-19-use
    title: "React 19 — use()"
    url: "https://react.dev/reference/react/use"
    role: canonical-react
  - id: nextjs-fetching-data
    title: "Next.js 16 — Fetching Data (streaming with use())"
    url: "https://nextjs.org/docs/app/getting-started/fetching-data"
    role: canonical-nextjs
evidence:
  - upstream_id: vercel-react-best-practices
    section: "async-suspense-boundaries"
    quote: "Instead of awaiting data in async components before returning JSX, use Suspense boundaries to show the wrapper UI faster while data loads."
  - upstream_id: react-19-use
    section: "Promise creation site"
    quote: "Prefer creating Promises in Server Components and passing them to Client Components over creating Promises in Client Components. Promises created in Client Components are recreated on every render."
  - upstream_id: nextjs-fetching-data
    section: "Client Components — use() API"
    quote: "Start by fetching data in your Server component, and pass the promise to your Client Component as prop. [...] use the use API to read the promise."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - async-parallel
  - server-cache-react
  - nextjs-use-cache
---

## Stream wrapper UI fast — fetch in Server Components, pass promises down, resolve with use() inside Suspense

**Impact: HIGH — Wrapper layout renders immediately; data-dependent regions stream in via Suspense.**

### Server vs Client Component scope

- `async function Page()` and other async components are **Server Components only**. Client Components must not be `async`.
- **Create promises in Server Components.** Promises created in Client Components are recreated every render — they don't share, don't cache, and re-fetch.

### Incorrect — async page awaits data before returning any JSX

```tsx
async function Page() {
  const data = await fetchData() // wrapper blocked
  return (
    <div>
      <Sidebar />
      <Header />
      <div><DataDisplay data={data} /></div>
      <Footer />
    </div>
  )
}
```

Sidebar / Header / Footer have to wait for `fetchData` even though only the middle needs it.

### Correct (single consumer) — Suspense boundary around data-needing region

```tsx
import { Suspense } from 'react'

function Page() {
  return (
    <div>
      <Sidebar />
      <Header />
      <Suspense fallback={<Skeleton />}>
        <DataDisplay />
      </Suspense>
      <Footer />
    </div>
  )
}

async function DataDisplay() {  // Server Component
  const data = await fetchData()
  return <div>{data.content}</div>
}
```

Sidebar / Header / Footer ship immediately; DataDisplay streams in.

### Correct (shared promise, multi consumer) — `use()` in Client Components

```tsx
// Server Component
import Posts from '@/app/ui/posts'

export default function Page() {
  const postsPromise = getPosts()   // not awaited — passed as a promise
  return (
    <Suspense fallback={<Skeleton />}>
      <Posts posts={postsPromise} />
      <PostsSummary posts={postsPromise} />
    </Suspense>
  )
}
```

```tsx
// Client Component
'use client'
import { use } from 'react'

export default function Posts({ posts }: { posts: Promise<Post[]> }) {
  const list = use(posts)
  return <ul>{list.map(p => <li key={p.id}>{p.title}</li>)}</ul>
}
```

`Posts` and `PostsSummary` share one resolution of `getPosts()` because they share the same promise reference. One fetch, two consumers.

### Promise creation site — the critical rule

```tsx
// BAD: promise created in Client Component render — recreated every render → infinite re-suspend.
'use client'
function Bad() {
  const posts = fetch('/api/posts').then(r => r.json())  // ❌ new promise each render
  const data = use(posts)
}

// GOOD: promise created in Server Component, passed down as a prop.
function Page() {
  const posts = fetchPosts()
  return <Bad posts={posts} />
}
```

### When NOT to use Suspense streaming

- **Critical above-the-fold content** that the user must see before interactivity. SEO content too.
- **Tiny fast queries** — the Suspense overhead may exceed the latency saved.
- **Layout-shift-sensitive surfaces** — the skeleton-to-content swap can shift the page. Size the skeleton close to the final content if you do stream.

### Layout-shift mitigation

If you stream, the fallback should occupy the same approximate bounding box as the resolved content. Otherwise you trade waiting-then-pop for layout-shift-then-jump — different UX, often not better.

Sources:

- [Vercel: async-suspense-boundaries](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-suspense-boundaries.md)
- [React 19 — use()](https://react.dev/reference/react/use)
- [Next.js 16 — Fetching Data (Client Component use() pattern)](https://nextjs.org/docs/app/getting-started/fetching-data)


<!-- @source rules/bundle-barrel-imports.md -->

---
title: Avoid expensive package barrel imports when the bundler does not already optimize them
impact: HIGH
impactDescription: "Reduces dev startup/prebundle time, build time, and cold-start latency by avoiding load of thousands of unused re-exports. Frameworks may already auto-optimize the most common offenders — measure before broad rewrites."
tags:
  - bundle
  - imports
  - tree-shaking
  - barrel-files
  - performance
  - react
  - nextjs
  - vite
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-BUNDLE-001"
verification:
  type: eslint
  rule_id: "ax/no-broad-barrel-imports"
  status: shipped
  notes: "Custom ESLint rule planned: flag `import { ... } from 'X'` for X in a configurable allowlist of known-expensive packages, with an escape hatch for packages already auto-optimized by the project's bundler. Until shipped: peer-review checkpoint."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Vercel-measured 200-800ms cost is real but specific to their benchmarks; Codex flagged universalization of the numbers as misleading. Vercel's `dist/esm/...` 'Correct' example is private to the package and can break across versions."
  freshness:
    status: partially-stale
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Next.js 16.2.6 auto-optimizes ~28 packages by default — many libraries listed in the Vercel rule are no-ops in current Next.js. optimizePackageImports remains experimental in 16.2.6."
  completeness:
    status: complete
    amendments:
      - "Distinguish dev startup/prebundle cost from production bundle size"
      - "List Next.js 16.2.6 default-optimized packages so no-op config is avoided"
      - "Flag experimental status of optimizePackageImports"
      - "Add Vite/Rollup/esbuild guidance"
      - "Warn against private dist/... deep imports; prefer documented subpath exports"
      - "Add 'measure first' advice before broad rewrites"
      - "Document modularizeImports as legacy/custom-bundler fallback"
  gap_check:
    status: complete
    note: "Vite-specific handling folded in as conditional section; private-deep-import caveat surfaced as a first-class concern. No sibling rule needed."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (rule: bundle-barrel-imports)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-barrel-imports.md"
    role: "seed"
  - id: nextjs-optimize-package-imports
    title: "Next.js 16 — experimental.optimizePackageImports"
    url: "https://nextjs.org/docs/app/api-reference/config/next-config-js/optimizePackageImports"
    version: "16.2.6"
    fetched: "2026-05-13"
    role: "canonical-nextjs"
  - id: vercel-blog-barrel-imports
    title: "How we optimized package imports in Next.js (Vercel engineering blog)"
    url: "https://vercel.com/blog/how-we-optimized-package-imports-in-next-js"
    role: "benchmark-evidence"
evidence:
  - upstream_id: vercel-react-best-practices
    section: "bundle-barrel-imports"
    quote: "Popular icon and component libraries can have up to 10,000 re-exports in their entry file."
  - upstream_id: nextjs-optimize-package-imports
    section: "optimizePackageImports"
    quote: "Adding a package to experimental.optimizePackageImports will only load the modules you are actually using, while still giving you the convenience of writing import statements with many named exports."
  - upstream_id: nextjs-optimize-package-imports
    section: "Experimental warning"
    quote: "This feature is currently experimental and subject to change, it's not recommended for production."
  - source_type: external
    citation: "Vercel engineering blog — How we optimized package imports in Next.js (200-800ms import cost measurements for React packages)"
    url: "https://vercel.com/blog/how-we-optimized-package-imports-in-next-js"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=high"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Core barrel-cost issue is real; numbers are bundler/package/version specific"
    - "Next 16 default-optimization list invalidates blanket CRITICAL impact"
    - "Distinguish prod bundle size vs dev prebundle/startup"
    - "One rule body sufficient if conditional guidance is included"
  amendments_required:
    - "Replace absolute benchmark numbers with 'measured for some packages; varies'"
    - "List Next.js 16 default-optimized packages explicitly"
    - "Note optimizePackageImports experimental status"
    - "Add Vite/Rollup/esbuild conditional"
    - "Warn against private dist/* imports; prefer documented exports"
    - "Add 'measure first' counsel"
    - "Downgrade impact CRITICAL → HIGH"
sibling_rules:
  - bundle-dynamic-imports
---

## Avoid expensive package barrel imports when the bundler does not already optimize them

**Impact: HIGH — Reduces dev startup/prebundle time, build time, and cold-start latency by avoiding load of thousands of unused re-exports. Modern frameworks may already auto-optimize the most common offenders — measure before broad rewrites.**

Barrel files (`index.js` files that do `export * from './...'`) are an ergonomic
authoring pattern. Their cost depends on the bundler. Some bundlers analyze the
entire export graph at import time even when only a few symbols are used,
inflating dev startup and (occasionally) production bundles. Vercel measured
**200–800 ms** of import overhead for some React packages with up to 10,000
re-exports per barrel; results vary by bundler, package, and version. Treat the
numbers as evidence the cost exists, not as a universal multiplier.

> **Note on the impact downgrade.** The seed source labels this CRITICAL. Codex
> review surfaced that Next.js 16.2.6 already default-optimizes ~28 of the
> commonly-cited offenders, so the cost a real project sees today is workload-
> and toolchain-specific. Downgraded to HIGH; escalate back to CRITICAL only
> when measurement shows large regression.

### Decision tree (measure first, then act)

1. **Run the bundler's import-cost diagnostic** (Next.js `analyze`, Vite
   `build --report`, webpack-bundle-analyzer, `import-cost` plugin). If the
   library is not on a hot path, stop.
2. **Check if your bundler already optimizes it.** Next.js 16.2.6 default-
   optimizes these without config:
   `lucide-react`, `date-fns`, `lodash-es`, `ramda`, `antd`, `react-bootstrap`,
   `ahooks`, `@ant-design/icons`, `@headlessui/react`, `@headlessui-float/react`,
   `@heroicons/react/{20/solid,24/solid,24/outline}`, `@visx/visx`,
   `@tremor/react`, `rxjs`, `@mui/material`, `@mui/icons-material`, `recharts`,
   `react-use`, `@material-ui/core`, `@material-ui/icons`, `@tabler/icons-react`,
   `mui-core`, `react-icons/*`, `effect`, `@effect/*`. **For these, no action
   needed.**
3. **Otherwise, prefer a documented public subpath import** if the package
   exposes one via its `exports` field. Do **not** reach into `dist/...` private
   paths — those break on minor version bumps and may stop working when the
   package adds an `exports` map.
4. **If no documented subpath exists**, add the package to
   `experimental.optimizePackageImports` (Next.js) or rely on Vite/Rollup
   tree-shaking. Note that `optimizePackageImports` remains **experimental** in
   Next.js 16.2.6 — not recommended for production without validation.

### Correct patterns

**Default-optimized in Next.js — keep the ergonomic barrel import:**

```tsx
// No action needed. Next 16 default-optimizes lucide-react.
import { Check, X, Menu } from 'lucide-react'
```

**Library exposes a documented subpath via `exports`:**

```tsx
// Public, version-stable subpath as documented by the package.
import Button from '@mui/material/Button'
import TextField from '@mui/material/TextField'
```

**Add to optimizePackageImports for uncommon offenders (Next.js):**

```js
// next.config.js
module.exports = {
  experimental: {
    optimizePackageImports: ['some-niche-icon-pack'],
  },
}
```

### Incorrect patterns

**Reaching into private internal paths:**

```tsx
// BAD: dist/esm/* is not part of the package's public API.
// Will break on package upgrades; may stop resolving when the package adds
// an "exports" field that hides internals.
import Check from 'lucide-react/dist/esm/icons/check'
```

**Adding a no-op config entry:**

```js
// BAD: lucide-react and @mui/material are already in Next 16's default list.
// This entry does nothing and creates the false impression of intervention.
module.exports = {
  experimental: {
    optimizePackageImports: ['lucide-react', '@mui/material'],
  },
}
```

### Vite / Rollup / esbuild

- Vite uses Rollup for production builds — tree-shaking is generally effective
  for ESM packages. The cost barrel imports impose on a Vite app is mostly in
  **dev pre-bundling** (esbuild dependency optimization), not production bundle
  size.
- If dev startup is slow, profile with `vite dev --debug=resolve` and consider
  documented subpath imports for the heaviest offenders.
- For CommonJS packages, tree-shaking is less reliable; lean toward documented
  subpath imports or `modularizeImports`-style transforms (custom plugin).
- Do not import deep private paths solely to "help" Vite — the cost is small in
  production and you trade for upgrade fragility.

### Legacy / custom bundlers

`modularizeImports` is the pre-Next-13.5 transform that remaps named imports to
per-module imports at build time. Use it when:

- the project uses a non-Next bundler that does not auto-optimize, and
- the package does not expose documented subpaths, and
- profiling shows the cost matters.

### Verification

- Static check (planned): custom ESLint rule `ax/no-broad-barrel-imports`. Maintains
  an allowlist of packages already optimized by the project's bundler (read from
  `eslint.config.js` plugin options). Flags `import { ... } from 'X'` only when X
  is on the project's "known expensive, not auto-optimized" list and the import
  is not from a documented subpath.
- Manual: bundle-analyzer reports for every commit that adds a new top-level
  dependency.

Sources for this rule:

- [Vercel agent-skills: bundle-barrel-imports](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-barrel-imports.md)
- [Next.js 16 — optimizePackageImports](https://nextjs.org/docs/app/api-reference/config/next-config-js/optimizePackageImports)
- [Vercel engineering blog — How we optimized package imports in Next.js](https://vercel.com/blog/how-we-optimized-package-imports-in-next-js)


<!-- @source rules/bundle-conditional.md -->

---
title: Load feature modules only when the feature is activated
impact: HIGH
impactDescription: "Keeps optional feature code out of the initial bundle entirely. Module loads the moment the feature is genuinely needed (toggle on, settings open, data threshold crossed) — not before."
tags:
  - bundle
  - conditional-loading
  - lazy-loading
  - feature-gates
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-BUNDLE-004"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) the feature is genuinely optional (not on the default path), (b) the gating condition reliably triggers when needed, (c) loading-state UI exists, (d) failure path handles import rejection."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Mechanic (dynamic import inside useEffect gated by condition) is correct."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Bare import() is framework-portable. Works in Next, Vite, Webpack, Rollup, esbuild."
  completeness:
    status: complete
    amendments:
      - "Removed `typeof window !== 'undefined'` inside useEffect — effects only run on the client, so the guard is dead code"
      - "Distinguished loading UI components (use React.lazy/next/dynamic) vs non-UI modules (use plain import())"
      - "Scoped to 'activation', distinct from 'intent prefetch' (sibling bundle-preload)"
  gap_check:
    status: complete
    note: "Distinct from bundle-preload: this rule loads ON activation, preload loads BEFORE activation based on intent."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: bundle-conditional"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-conditional.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "bundle-conditional"
    quote: "Load large data or modules only when a feature is activated."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Remove typeof window from useEffect"
    - "Distinguish UI vs non-UI module loading"
    - "Scope clearly: activation, not prefetch"
sibling_rules:
  - bundle-dynamic-imports
  - bundle-preload
---

## Load feature modules only when the feature is activated

**Impact: HIGH — Keeps optional feature code out of the initial bundle entirely. Module loads the moment the feature is genuinely needed (toggle on, settings open, data threshold crossed) — not before.**

### Correct — non-UI module gated on activation

```tsx
'use client'
import { useEffect, useState } from 'react'

function AnimationPlayer({
  enabled,
  setEnabled,
}: {
  enabled: boolean
  setEnabled: (b: boolean) => void
}) {
  const [frames, setFrames] = useState<Frame[] | null>(null)

  useEffect(() => {
    if (!enabled || frames) return
    import('./animation-frames.js')
      .then((m) => setFrames(m.frames))
      .catch(() => setEnabled(false))
  }, [enabled, frames, setEnabled])

  if (!enabled) return null
  if (!frames) return <FrameLoadingSkeleton />
  return <Canvas frames={frames} />
}
```

### Correct — UI component gated on activation

```tsx
import { lazy, Suspense } from 'react'

const SettingsDrawer = lazy(() => import('./settings-drawer'))

function App() {
  const [open, setOpen] = useState(false)
  return (
    <>
      <button onClick={() => setOpen(true)}>Settings</button>
      {open && (
        <Suspense fallback={<DrawerSkeleton />}>
          <SettingsDrawer onClose={() => setOpen(false)} />
        </Suspense>
      )}
    </>
  )
}
```

### Why not `typeof window !== 'undefined'`?

`useEffect` runs only on the client. The guard is dead code inside an effect. The guard IS legitimate at module-top-level when you import a browser-only module unconditionally — that's a different case, covered indirectly by next/dynamic's `ssr: false`.

### When this rule applies

- Feature is opt-in (admin tools, advanced editor mode, debug overlays).
- Module is non-trivial (≥10–20 KB minified). Below that, the dynamic-import overhead may exceed the savings.
- Module use is reliably correlated with the activation gate (no flicker between gate states).

### Choosing UI vs non-UI

| Module kind | Pattern |
|---|---|
| React component to render | `React.lazy` + Suspense (portable) or `next/dynamic` |
| Data, processor, utility lib | bare `import('./module')` inside effect/handler |
| Heavy WASM / worker / shader | bare `import()` of the bootstrap module |

Sources:

- [Vercel: bundle-conditional](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-conditional.md)


<!-- @source rules/bundle-defer-third-party.md -->

---
title: Defer non-critical third-party SDK init or script loading until after hydration
impact: MEDIUM
impactDescription: "Removes analytics/logging/error-tracking code from the initial render-blocking path. Prefer official integration APIs (next/script, vendor-recommended loader) when available; dynamic import() of SDK modules for general libraries."
tags:
  - bundle
  - third-party
  - analytics
  - defer
  - scripts
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-BUNDLE-003"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) the third-party isn't needed for initial render, (b) preferred path is the framework's official integration API (next/script, etc.) if one exists, (c) component-level dynamic() only used when actually rendering a component."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified-with-nuance
    last_verified: "2026-05-16"
    notes: "Directionally right. Vercel rule conflates 'deferring SDK initialization' with 'wrapping a provider component in dynamic()' — the right pattern depends on whether you're rendering a component or just loading a library."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Next.js has dedicated script-loading primitives (next/script with strategy options). Most vendors also have framework-specific integration packages."
  completeness:
    status: complete
    amendments:
      - "Distinguish script loading vs SDK module import vs provider component"
      - "Prefer official integration APIs (next/script, vendor packages)"
      - "Use dynamic import() for SDK modules — not next/dynamic, which is for components"
      - "Removed 'loads after hydration' overclaim — timing depends on implementation"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: bundle-defer-third-party"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-defer-third-party.md"
    role: seed
  - id: nextjs-lazy-loading
    title: "Next.js 16 — Lazy Loading guide"
    url: "https://nextjs.org/docs/app/guides/lazy-loading"
    role: canonical-nextjs
evidence:
  - upstream_id: vercel-react-best-practices
    section: "bundle-defer-third-party"
    quote: "Analytics, logging, and error tracking don't block user interaction. Load them after hydration."
  - source_type: external
    citation: "Next.js 16 — Lazy Loading: external libraries can be loaded on demand using import() function; pattern of dynamic import inside event handlers / effects"
    url: "https://nextjs.org/docs/app/guides/lazy-loading"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Distinguish script vs module vs provider"
    - "Prefer official integration APIs"
    - "Use dynamic import() for SDK modules"
sibling_rules:
  - bundle-dynamic-imports
---

## Defer non-critical third-party SDK init or script loading until after hydration

**Impact: MEDIUM — Removes analytics/logging/error-tracking code from the initial render-blocking path. Prefer official integration APIs (`next/script`, vendor-recommended loader) when available; dynamic `import()` of SDK modules for general libraries.**

### Three distinct shapes — pick the right one

1. **Loading a `<script>` tag** (Google Analytics, Tag Manager, vendor pixels) → use the framework's script primitive.
2. **Importing an SDK module** (Sentry, PostHog, Mixpanel client library) → dynamic `import()` inside an effect or event handler.
3. **Rendering a vendor's React provider component** (`<Analytics />`) → `next/dynamic` (or `React.lazy` + Suspense).

### Pattern 1 — script tag with framework primitive

```tsx
// app/layout.tsx (Next.js)
import Script from 'next/script'

export default function RootLayout({ children }) {
  return (
    <html>
      <body>
        {children}
        <Script
          src="https://example.com/analytics.js"
          strategy="afterInteractive"
        />
      </body>
    </html>
  )
}
```

### Pattern 2 — dynamic import() of SDK module

```tsx
'use client'
import { useEffect } from 'react'

function AnalyticsInit() {
  useEffect(() => {
    void (async () => {
      const { init, trackPageview } = await import('@vendor/analytics-sdk')
      init({ token: process.env.NEXT_PUBLIC_VENDOR_TOKEN })
      trackPageview()
    })()
  }, [])
  return null
}
```

### Pattern 3 — vendor provider component, deferred render

```tsx
import dynamic from 'next/dynamic'

const Analytics = dynamic(
  () => import('@vercel/analytics/react').then((m) => m.Analytics),
  { ssr: false },
)

export default function RootLayout({ children }) {
  return (
    <html>
      <body>
        {children}
        <Analytics />
      </body>
    </html>
  )
}
```

### Incorrect — static import in root layout for non-critical third-party

```tsx
import { Analytics } from '@vercel/analytics/react'

export default function RootLayout({ children }) {
  return (
    <html>
      <body>
        {children}
        <Analytics />   {/* In the initial bundle, regardless of need */}
      </body>
    </html>
  )
}
```

### Anti-patterns

- Wrapping a non-component import in `next/dynamic`. Use plain `import()` for SDK modules.
- Using `ssr: false` to hide a real SSR bug in the third-party — fix the root cause first.
- Loading 5+ analytics scripts on one page — each adds connection cost; prefer one server-side analytics gateway if possible.

Sources:

- [Vercel: bundle-defer-third-party](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-defer-third-party.md)
- [Next.js 16 — Lazy Loading](https://nextjs.org/docs/app/guides/lazy-loading)


<!-- @source rules/bundle-dynamic-imports.md -->

---
title: Lazy-load heavy client-only components via React.lazy/Suspense or next/dynamic
impact: HIGH
impactDescription: "Reduces initial JS payload and improves TTI. May improve LCP only when the deferred code is not on the LCP critical path. Use for below-the-fold or interaction-gated client components."
tags:
  - bundle
  - dynamic-import
  - code-splitting
  - react-lazy
  - next-dynamic
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-BUNDLE-002"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) the component is genuinely heavy and off the initial render path, (b) lazy declaration at module level (not inside another component), (c) Suspense fallback present, (d) ssr:false used only for Client Components."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified-with-framing
    last_verified: "2026-05-16"
    notes: "Mechanic correct; Vercel rule too Next-specific. React.lazy + Suspense works for vanilla React/Vite."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Both next/dynamic and React.lazy work in Next.js 16 App Router. Server Components are already auto-code-split — lazy applies to Client Components."
  completeness:
    status: complete
    amendments:
      - "Reframed: 'lazy-load heavy client-only or below-the-fold UI'"
      - "Added React.lazy + Suspense as the generic React option"
      - "Soft impact wording: TTI gains reliable, LCP gains only when deferred code isn't on LCP path"
      - "Clarified ssr:false is Client Component only"
      - "Noted Server Components are auto-code-split — lazy is for Client Components"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: bundle-dynamic-imports"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-dynamic-imports.md"
    role: seed
  - id: nextjs-lazy-loading
    title: "Next.js 16 — Lazy Loading guide"
    url: "https://nextjs.org/docs/app/guides/lazy-loading"
    role: canonical-nextjs
  - id: react-19-lazy
    title: "React 19 — lazy() reference"
    url: "https://react.dev/reference/react/lazy"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "bundle-dynamic-imports"
    quote: "Use next/dynamic to lazy-load large components not needed on initial render."
  - source_type: external
    citation: "Next.js 16 docs — next/dynamic is a composite of React.lazy() and Suspense"
    url: "https://nextjs.org/docs/app/guides/lazy-loading"
  - source_type: external
    citation: "React 19 docs — Do NOT declare lazy components inside other components (state reset on re-renders); declare at module top level"
    url: "https://react.dev/reference/react/lazy"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Reframe as portable (React.lazy generic, next/dynamic Next-specific)"
    - "Soften LCP impact claim"
    - "Note Server Components auto-code-split"
sibling_rules:
  - bundle-defer-third-party
  - bundle-conditional
  - bundle-preload
---

## Lazy-load heavy client-only components via React.lazy/Suspense or next/dynamic

**Impact: HIGH — Reduces initial JS payload and improves TTI. May improve LCP only when the deferred code is not on the LCP critical path. Use for below-the-fold or interaction-gated client components.**

### Scope

This rule applies to **Client Components** that are heavy AND not needed on the initial render. Server Components are already auto-code-split by Next.js — lazy patterns don't apply there.

### Correct — React.lazy + Suspense (portable across React/Vite/Next)

```tsx
import { lazy, Suspense } from 'react'

// Module-top-level. Never inside another component (resets state on re-renders).
const MonacoEditor = lazy(() =>
  import('./monaco-editor').then((m) => ({ default: m.MonacoEditor })),
)

function CodePanel({ code }: { code: string }) {
  return (
    <Suspense fallback={<EditorSkeleton />}>
      <MonacoEditor value={code} />
    </Suspense>
  )
}
```

### Correct — next/dynamic (Next.js specific, composite of React.lazy + Suspense)

```tsx
import dynamic from 'next/dynamic'

const MonacoEditor = dynamic(
  () => import('./monaco-editor').then((m) => m.MonacoEditor),
  { ssr: false, loading: () => <EditorSkeleton /> },
)
// ssr:false only works for Client Components.
```

### Incorrect — static import in initial bundle

```tsx
import { MonacoEditor } from './monaco-editor' // ~300KB ships in main chunk

function CodePanel({ code }: { code: string }) {
  return <MonacoEditor value={code} />
}
```

### Anti-patterns

- Declaring `lazy()` inside a component body — React docs warn this causes state reset on re-renders. Always module top-level.
- Using `ssr: false` in a Server Component — Next will error.
- Lazy-loading a component that's actually on the LCP critical path — defers the visible content. Profile first.

### Choosing between React.lazy and next/dynamic

| | React.lazy | next/dynamic |
|---|---|---|
| Works in | React 18+ (any framework) | Next.js only |
| Loading state | Suspense fallback | `loading:` option (or Suspense) |
| SSR control | none (always prerendered) | `ssr: false` for Client Components |
| Named exports | needs `.then(m => ({ default: m.X }))` | `.then(m => m.X)` directly |
| Server Components | client-only | client-only (`ssr: false` only) |

Sources:

- [Vercel: bundle-dynamic-imports](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-dynamic-imports.md)
- [Next.js 16 — Lazy Loading](https://nextjs.org/docs/app/guides/lazy-loading)
- [React 19 — lazy()](https://react.dev/reference/react/lazy)


<!-- @source rules/bundle-preload.md -->

---
title: Prefetch heavy modules on strong user-intent signals (hover, focus, viewport, likely next step)
impact: MEDIUM
impactDescription: "Reduces perceived latency by spending bandwidth/CPU EARLY based on confident-intent signals, before the user clicks. Latency tradeoff, not a bundle-size win."
tags:
  - bundle
  - prefetch
  - preload
  - user-intent
  - hover
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-BUNDLE-005"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) the intent signal is strong (hover, focus, click likely-next-page) — not 'every user'; (b) no preload on initial page load for all users (that's a static import); (c) cleanup not required (browser/bundler dedupes preloaded modules)."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Event-driven import() warms the bundle. Bundlers/browsers dedupe so subsequent import() returns the same promise."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Works with all modern bundlers. Modern alternative: <link rel='modulepreload'> from server response — not in this rule's scope."
  completeness:
    status: complete
    amendments:
      - "Removed unnecessary `typeof window` guard from client-only event handlers"
      - "Added 'preload only on strong intent' caution against bandwidth waste"
      - "Framed as latency tradeoff, not bundle-size reduction"
  gap_check:
    status: complete
    note: "Distinct from bundle-conditional (which loads ON activation, not before)."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: bundle-preload"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-preload.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "bundle-preload"
    quote: "Preload heavy bundles before they're needed to reduce perceived latency."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Strong-intent only"
    - "Latency tradeoff framing"
    - "Remove redundant typeof window"
sibling_rules:
  - bundle-conditional
  - bundle-dynamic-imports
---

## Prefetch heavy modules on strong user-intent signals (hover, focus, viewport, likely next step)

**Impact: MEDIUM — Reduces perceived latency by spending bandwidth/CPU EARLY based on confident-intent signals, before the user clicks. Latency tradeoff, not a bundle-size win.**

### Correct — hover/focus prefetch on a button

```tsx
'use client'

function EditorButton({ onClick }: { onClick: () => void }) {
  const preload = () => {
    void import('./monaco-editor')
  }
  return (
    <button onMouseEnter={preload} onFocus={preload} onClick={onClick}>
      Open Editor
    </button>
  )
}
```

### Correct — preload behind a feature flag, scoped to a provider

```tsx
'use client'
import { useEffect } from 'react'

function FlagsProvider({ children, flags }: Props) {
  useEffect(() => {
    if (!flags.editorEnabled) return
    void import('./monaco-editor').then((m) => m.init())
  }, [flags.editorEnabled])

  return <FlagsContext.Provider value={flags}>{children}</FlagsContext.Provider>
}
```

### Incorrect — preload for all users on every page load

```tsx
function EveryPage() {
  useEffect(() => {
    // BAD: spends bandwidth even for users who will never open the editor.
    void import('./monaco-editor')
  }, [])
  return null
}
```

If a module is going to load for every user on every page, it should be a static import (i.e. it's not really a lazy-load candidate).

### Strong intent signals — pick from

- `onMouseEnter` / `onFocus` on a button or link
- IntersectionObserver triggering when an "Open X" CTA enters the viewport
- Routing hints: user is on `/dashboard`, the only meaningful next click is `/dashboard/editor` → prefetch its bundle when dashboard renders
- A feature flag that is provably correlated with feature use within the session

### Weak signals — avoid

- Page load itself (not intent — that's a static import in disguise)
- Hover on a generic page area without a specific CTA
- "User is logged in" (not a feature-use signal)

### Why no `typeof window` guard in client handlers

`onMouseEnter` / `onFocus` event handlers only execute on the client. Likewise `useEffect` runs only on the client. The guard is dead code in these positions. Keep guards only at module top level when importing a browser-only module unconditionally.

### Latency tradeoff, not bundle savings

Preload doesn't reduce the bundle. It changes WHEN the bytes load. If your hit-rate (preload → actual use) is low, you've paid for unused work. Profile in real traffic before preloading aggressively.

Sources:

- [Vercel: bundle-preload](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-preload.md)


<!-- @source rules/client-event-listeners.md -->

---
title: One global event listener per (target, event, options); many subscribers
impact: LOW
impactDescription: "Avoids N listeners for N hook instances. Single listener fan-outs to a Set of callbacks. Implementation can be plain useEffect + module registry, useSyncExternalStore, or a subscription library primitive — SWR is not required."
tags:
  - client
  - event-listeners
  - subscription
  - performance
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-CLIENT-002"
verification:
  type: review
  status: manual
  notes: "Reviewer flags useEffect that addEventListener on a global target (window/document) from a hook used in multiple components. Confirms the hook routes through a singleton subscribe registry."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified-with-vendor-decouple
    last_verified: "2026-05-16"
    notes: "Pattern is correct; useSWRSubscription is one implementation, not the requirement."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
  completeness:
    status: complete
    amendments:
      - "Decoupled from SWR; mentioned plain useEffect, useSyncExternalStore, library subscription as options"
      - "Named the primitive: 'singleton listener + subscriber registry'"
      - "Added cleanup-on-zero-subscribers detail"
      - "Keyed by (target, event, options) — passive listeners need a separate registry from non-passive"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: client-event-listeners"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-event-listeners.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "client-event-listeners"
    quote: "Use useSWRSubscription() to share global event listeners across component instances."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Pattern right, SWR requirement unnecessary"
    - "Name singleton-listener-plus-registry primitive"
    - "Add zero-subscriber cleanup"
sibling_rules:
  - client-swr-dedup
  - advanced-event-handler-refs
---

## One global event listener per (target, event, options); many subscribers

**Impact: LOW — Avoids N listeners for N hook instances. Single listener fan-outs to a Set of callbacks. Implementation can be plain useEffect + module registry, useSyncExternalStore, or a subscription library primitive.**

### The mechanic

A hook used in many components shouldn't register a new global listener per instance. Instead:

1. Keep a **module-level registry** keyed by event identity (e.g. `'window:keydown'`).
2. The hook just **adds itself** to the registry on mount, **removes** on unmount.
3. The **first** subscriber installs the underlying `addEventListener`.
4. The **last** unsubscriber removes it (cleanup on zero-count).

### Correct — plain useEffect + module-level registry

```tsx
type Cb = (e: KeyboardEvent) => void
const cbs = new Set<Cb>()
let attached = false

function attachIfNeeded() {
  if (attached) return
  attached = true
  window.addEventListener('keydown', dispatch)
}
function detachIfNeeded() {
  if (cbs.size > 0) return
  attached = false
  window.removeEventListener('keydown', dispatch)
}
function dispatch(e: KeyboardEvent) {
  cbs.forEach((cb) => cb(e))
}

export function useGlobalKeydown(callback: Cb) {
  useEffect(() => {
    cbs.add(callback)
    attachIfNeeded()
    return () => {
      cbs.delete(callback)
      detachIfNeeded()
    }
  }, [callback])
}
```

Now `useGlobalKeydown` can be used in any number of components without N listeners.

### Correct — useSyncExternalStore (when you want the value, not just a callback)

```tsx
import { useSyncExternalStore } from 'react'

const listeners = new Set<() => void>()
let lastKey: string | null = null

function getSnapshot() { return lastKey }
function subscribe(notify: () => void) {
  if (listeners.size === 0) {
    window.addEventListener('keydown', (e) => {
      lastKey = e.key
      listeners.forEach((fn) => fn())
    })
  }
  listeners.add(notify)
  return () => listeners.delete(notify)
}

export function useLastPressedKey() {
  return useSyncExternalStore(subscribe, getSnapshot, () => null)
}
```

### Correct — library primitive (SWR's useSWRSubscription, observables, etc.)

Acceptable if the project already uses such a library. Don't add a new dependency just for this pattern.

### Incorrect — N instances = N listeners

```tsx
function useKeyboardShortcut(key: string, cb: () => void) {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.metaKey && e.key === key) cb()
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [key, cb])
  // 10 components using this = 10 keydown listeners.
}
```

### Keying the registry

Use separate registries when the event subscription options differ (passive vs not, capture phase vs bubble). `(target, eventName, options)` is the natural key.

Sources:

- [Vercel: client-event-listeners](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-event-listeners.md)


<!-- @source rules/client-localstorage-schema.md -->

---
title: Version localStorage keys, wrap every access in try-catch, store minimal fields
impact: MEDIUM
impactDescription: "Schema evolution without conflicts. Survives Safari/Firefox private mode (which throws on setItem) and quota overflow. Reduces storage size and prevents accidental persistence of tokens/PII."
tags:
  - client
  - localStorage
  - storage
  - versioning
  - data-minimization
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-CLIENT-004"
verification:
  type: review
  status: manual
  notes: "Reviewer checks every localStorage access: (a) key has a version suffix, (b) wrapped in try-catch, (c) value is a minimal field set (no tokens, no PII, no full server objects), (d) typeof window guard if the access can run during SSR, (e) migration path from prior versions documented."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Pattern matches MDN guidance: localStorage can throw (Safari private mode, quota, disabled storage). Versioning + minimal payload + try-catch is the safe form."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
  completeness:
    status: complete
    amendments:
      - "Added SSR guard: typeof window check at module top-level OR inside an effect"
      - "Made try-catch behavior explicit (return null / fall back to default)"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: client-localstorage-schema"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-localstorage-schema.md"
    role: seed
  - id: mdn-localstorage
    title: "MDN — Window.localStorage"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage"
    role: primitive-semantics
evidence:
  - upstream_id: vercel-react-best-practices
    section: "client-localstorage-schema"
    quote: "Always wrap in try-catch: getItem() and setItem() throw in incognito/private browsing (Safari, Firefox), when quota exceeded, or when disabled."
  - source_type: external
    citation: "MDN — Window.localStorage (may throw a SecurityError when storage is disabled or quota is exceeded; not available during SSR — no `window` object)"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Solid rule"
    - "Add SSR guard"
    - "Make try-catch fallback explicit"
sibling_rules: []
---

## Version localStorage keys, wrap every access in try-catch, store minimal fields

**Impact: MEDIUM — Schema evolution without conflicts. Survives Safari/Firefox private mode (which throws on `setItem`) and quota overflow. Reduces storage size and prevents accidental persistence of tokens/PII.**

### Three required practices

1. **Version every key.** `userConfig:v2` not `userConfig`. Schema can evolve; old data can be migrated or discarded explicitly.
2. **Wrap every access in try-catch.** `localStorage` throws in Safari private mode (`setItem`), Firefox private mode under some conditions, quota overflow, and when storage is disabled.
3. **Store minimal fields.** Pick the few keys the UI needs. Never persist tokens, PII, or full server objects.

Plus, when access can run during SSR (Next.js): **guard against `window` being undefined.**

### Correct

```tsx
const KEY = 'userConfig:v2'

type Config = { theme: 'light' | 'dark'; language: string }
const DEFAULT: Config = { theme: 'light', language: 'en' }

export function loadConfig(): Config {
  if (typeof window === 'undefined') return DEFAULT
  try {
    const raw = window.localStorage.getItem(KEY)
    return raw ? (JSON.parse(raw) as Config) : DEFAULT
  } catch {
    return DEFAULT
  }
}

export function saveConfig(config: Config): void {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(KEY, JSON.stringify(config))
  } catch {
    // private mode / quota / disabled — silently degrade
  }
}

// Migration from v1 to v2, run once at app boot (see advanced-init-once)
export function migrateConfig(): void {
  if (typeof window === 'undefined') return
  try {
    const v1 = window.localStorage.getItem('userConfig:v1')
    if (!v1) return
    const old = JSON.parse(v1)
    saveConfig({
      theme: old.darkMode ? 'dark' : 'light',
      language: old.lang ?? 'en',
    })
    window.localStorage.removeItem('userConfig:v1')
  } catch {
    // best effort
  }
}
```

### Incorrect

```tsx
// No version. No try-catch. Stores 20-field object including a token.
localStorage.setItem('userConfig', JSON.stringify(fullUserResponse))
const data = JSON.parse(localStorage.getItem('userConfig'))
```

What goes wrong:
- Safari private mode → `setItem` throws → uncaught error breaks the page.
- Schema change later → old data deserialized as new shape → runtime crashes.
- Token in localStorage → XSS exfiltration risk.
- SSR → `localStorage` undefined → hydration error.

### Storing minimal fields — example

```tsx
// User object has 20+ fields; only persist what the UI reads.
function cachePrefs(user: FullUser) {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(
      'prefs:v1',
      JSON.stringify({
        theme: user.preferences.theme,
        notifications: user.preferences.notifications,
      }),
    )
  } catch {}
}
```

Tokens, role flags, internal IDs — leave server-side. They expire, can be revoked, and don't belong in long-lived browser storage.

Sources:

- [Vercel: client-localstorage-schema](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-localstorage-schema.md)
- [MDN — localStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage)


<!-- @source rules/client-passive-event-listeners.md -->

---
title: "Use passive listeners on touch/wheel events that do not need preventDefault"
impact: MEDIUM
impactDescription: "Tells the browser the listener will not call preventDefault, allowing it to scroll immediately instead of waiting for the listener to run. Modern Chrome already defaults document-level touchstart/touchmove to passive; the rule still matters for explicit listeners on custom scroll containers and for cross-browser clarity."
tags:
  - client
  - event-listeners
  - scrolling
  - performance
  - touch
  - wheel
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-CLIENT-003"
verification:
  type: review
  status: manual
  notes: "Reviewer flags addEventListener('touchstart'|'touchmove'|'wheel', ...) without an options argument. If the listener calls preventDefault, explicit { passive: false } is required and reviewer confirms the use-case (custom swipe/zoom)."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "passive:true means preventDefault is a no-op (the browser ignores it)."
  freshness:
    status: current-with-nuance
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Chrome 56+ (2017) made touchstart/touchmove on document/window default to passive. Custom scroll containers and other engines still need explicit options."
  completeness:
    status: complete
    amendments:
      - "Added Chrome default-passive nuance"
      - "Added explicit { passive: false } pattern for legitimate preventDefault use"
      - "Scoped to cancelable scroll/touch/wheel listeners"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: client-passive-event-listeners"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-passive-event-listeners.md"
    role: seed
  - id: mdn-addeventlistener-passive
    title: "MDN — EventTarget.addEventListener passive option"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener#passive"
    role: primitive-semantics
evidence:
  - upstream_id: vercel-react-best-practices
    section: "client-passive-event-listeners"
    quote: "Add { passive: true } to touch and wheel event listeners to enable immediate scrolling."
  - source_type: external
    citation: "MDN — EventTarget.addEventListener (passive option indicates the listener will never call preventDefault; calling it has no effect)"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener#passive"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Add Chrome default-passive nuance"
    - "Don't make passive sound universally required"
    - "Explicit passive:false when cancellation is intentional"
sibling_rules:
  - client-event-listeners
---

## Use `{ passive: true }` on touch/wheel listeners that do not need preventDefault

**Impact: MEDIUM — Tells the browser the listener will not call `preventDefault`, allowing it to scroll immediately instead of waiting for the listener to run. Modern Chrome already defaults document-level `touchstart`/`touchmove` to passive; the rule still matters for explicit listeners on custom scroll containers and for cross-browser clarity.**

### When passive helps

The browser must determine whether your listener will cancel the scroll. Without `passive`, it waits for the listener to run. With `passive: true`, the browser proceeds immediately.

Use passive when the listener is for:

- analytics / metrics
- logging
- updating UI state that doesn't cancel scroll
- any handler that genuinely doesn't call `preventDefault`

### Correct — passive listeners

```tsx
useEffect(() => {
  const onTouch = (e: TouchEvent) => trackTouch(e.touches[0].clientX)
  const onWheel = (e: WheelEvent) => trackScroll(e.deltaY)

  document.addEventListener('touchstart', onTouch, { passive: true })
  document.addEventListener('wheel', onWheel, { passive: true })

  return () => {
    document.removeEventListener('touchstart', onTouch)
    document.removeEventListener('wheel', onWheel)
  }
}, [])
```

### Correct — explicit `{ passive: false }` when you genuinely need preventDefault

```tsx
useEffect(() => {
  const onWheel = (e: WheelEvent) => {
    if (!shouldHandleZoom(e)) return
    e.preventDefault()  // would be a no-op under passive
    customZoom(e.deltaY)
  }
  document.addEventListener('wheel', onWheel, { passive: false })
  return () => document.removeEventListener('wheel', onWheel)
}, [])
```

`{ passive: false }` is required for custom swipe/zoom/pull-to-refresh gestures or any handler that may cancel the scroll/gesture.

### Default-passive caveat (Chrome 56+, since 2017)

Chrome made `touchstart` / `touchmove` listeners on `document` and `window` default to `passive: true`. Firefox and Safari followed. **For these targets, an unset option already implies passive.** The rule still has teeth for:

- Custom scroll containers (`element.addEventListener('touchstart', ...)`).
- `wheel` listeners (not auto-passive on document in most engines).
- Cross-engine consistency — older browsers and embedded webviews may differ.

Explicit options remove ambiguity at no cost.

### Anti-pattern

```tsx
useEffect(() => {
  const onTouch = (e: TouchEvent) => {
    if (someCondition(e)) e.preventDefault()   // silently ignored if browser made this passive
    track(e)
  }
  document.addEventListener('touchstart', onTouch)
}, [])
```

Either you genuinely want to cancel and need `{ passive: false }`, or you don't and should declare `{ passive: true }` explicitly.

Sources:

- [Vercel: client-passive-event-listeners](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-passive-event-listeners.md)
- [MDN — addEventListener passive option](https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener#passive)


<!-- @source rules/client-swr-dedup.md -->

---
title: Deduplicate client-side server-state requests with a server-state cache (SWR / TanStack Query / RTK Query / framework primitive)
impact: MEDIUM-HIGH
impactDescription: "Multiple component instances asking for the same data share one in-flight request and one cache entry. Avoids fan-out of identical fetches and per-instance state machines. Library choice is implementation detail; the catalog encodes the practice."
tags:
  - client
  - server-state
  - data-fetching
  - deduplication
  - swr
  - tanstack-query
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-CLIENT-001"
verification:
  type: review
  status: manual
  notes: "Reviewer flags any new useEffect+fetch+setState pattern in code that has access to a server-state cache library. New direct fetches need justification (one-off, init-only, or out-of-cache scope)."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified-with-vendor-decouple
    last_verified: "2026-05-16"
    notes: "Mechanic correct; Vercel framing overcouples to SWR. Catalog must encode the practice, not the library."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "React 19 use() + Suspense covers some read patterns but does not replace mutations, revalidation, retries, cache invalidation."
  completeness:
    status: complete
    amendments:
      - "Decoupled from SWR — present SWR / TanStack Query / RTK Query / use() as implementation options"
      - "Clarified use() is read-only; doesn't replace mutation/revalidation surface"
      - "Added 'one-off / init-only' acceptable exception"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: client-swr-dedup"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-swr-dedup.md"
    role: seed
  - id: nextjs-fetching-data
    title: "Next.js 16 — Fetching Data (Client Components: SWR or TanStack Query)"
    url: "https://nextjs.org/docs/app/getting-started/fetching-data"
    role: canonical-nextjs
evidence:
  - upstream_id: vercel-react-best-practices
    section: "client-swr-dedup"
    quote: "SWR enables request deduplication, caching, and revalidation across component instances."
  - upstream_id: nextjs-fetching-data
    section: "Client Components"
    quote: "You can use a community library like SWR or React Query to fetch data in Client Components."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Vendor-decouple"
    - "Retitle generically"
    - "use() doesn't replace mutations"
sibling_rules:
  - client-event-listeners
---

## Deduplicate client-side server-state requests with a server-state cache (SWR / TanStack Query / RTK Query / framework primitive)

**Impact: MEDIUM-HIGH — Multiple component instances asking for the same data share one in-flight request and one cache entry. Avoids fan-out of identical fetches and per-instance state machines. Library choice is implementation detail; the catalog encodes the practice.**

### The practice

For any **server-state** read in a Client Component that may appear in multiple components or routes during one session, use a deduplicating cache layer. Plain `useEffect + fetch + setState` should be reserved for genuinely one-off init reads or fetches that are out of the cache's natural scope.

### Implementation choices

| Library / primitive | Strengths |
|---|---|
| **SWR** | Tight Next.js integration (Vercel), simple API, stale-while-revalidate by default |
| **TanStack Query** (`@tanstack/react-query`) | Framework-agnostic, rich query/mutation/infinite/optimistic surface |
| **RTK Query** | Redux Toolkit-native, codegen options |
| **React 19 `use(promise)` + `<Suspense>`** | Built-in; no extra dep — **reads only**; does NOT cover mutations, retries, optimistic updates, cache invalidation |

### Correct — SWR

```tsx
import useSWR from 'swr'

function UserList() {
  const { data: users } = useSWR('/api/users', fetcher)
  // every UserList renders shares the same request and cache entry
}
```

### Correct — TanStack Query

```tsx
import { useQuery } from '@tanstack/react-query'

function UserList() {
  const { data: users } = useQuery({
    queryKey: ['users'],
    queryFn: () => fetch('/api/users').then((r) => r.json()),
  })
}
```

### Correct — React 19 `use()` + Suspense (read-only)

```tsx
// Server Component creates the promise; Client Component consumes it.
// Multiple Client Component consumers of the same promise share the resolve.
'use client'
import { use } from 'react'

function UserList({ usersPromise }: { usersPromise: Promise<User[]> }) {
  const users = use(usersPromise)
  // ...
}
```

This is genuinely deduplicating because the promise is the cache key — same promise reference = same await.

### Incorrect — direct fetch in useEffect, per-instance state

```tsx
function UserList() {
  const [users, setUsers] = useState<User[]>([])
  useEffect(() => {
    fetch('/api/users').then((r) => r.json()).then(setUsers)
  }, [])
  // Five <UserList>s on one page = five identical /api/users requests + five private caches.
}
```

### When direct fetch is acceptable

- One-off boot-time init (read once, store in module-level state — covered by `advanced-init-once`).
- Endpoint is outside the server-state cache scope (e.g. a one-shot debug ping).
- Library not yet adopted in the project — even then, plan the migration.

Sources:

- [Vercel: client-swr-dedup](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-swr-dedup.md)
- [Next.js 16 — Client Component data fetching (mentions both SWR and React Query)](https://nextjs.org/docs/app/getting-started/fetching-data)
- [React 19 — use()](https://react.dev/reference/react/use)


<!-- @source rules/combobox-respects-hangul-ime-composition.md -->

---
title: "Combobox / autocomplete must suppress onChange filtering during IME composition (한글 IME guard)"
rule_id: combobox-respects-hangul-ime-composition
impact: HIGH
impactDescription: "Korean IME fires multiple onChange events per keystroke during syllable composition; filtering on partial input produces wrong matches and degrades UX for Korean users"
tags:
  - combobox
  - ime
  - hangul
  - accessibility
  - korean
  - l1-component
applicable_to:
  - react
  - nextjs
provenance_class: internal_design
protects_template_id: templates/L1/components/combobox.tsx
failing_fixture_path: practices/evals/fixtures/combobox-respects-hangul-ime-composition/fail_fires_during_composition/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-CLIENT-001"
verification:
  type: review
  status: manual
  notes: "Combobox onChange handler must check composingRef.current or nativeEvent.isComposing before invoking the filter/search. onCompositionStart must set the guard; onCompositionEnd must clear it and fire the deferred filter."
evidence:
  - upstream_id: mdn-addeventlistener-passive
    section: "CompositionEvent — isComposing property"
    quote: "isComposing"
  - source_type: external
    citation: "MDN Web Docs — CompositionEvent: compositionstart / compositionend lifecycle for CJK input method editors"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/CompositionEvent"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "W3C UI Events specification §CompositionEvent — IME composition lifecycle (compositionstart, compositionupdate, compositionend)"
    url: "https://www.w3.org/TR/uievents/#events-composition-types"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Combobox / autocomplete must suppress `onChange` filtering during IME composition (한글 IME guard)

**Impact: HIGH — Korean (한글) input via IME fires 2-4 `onChange` events per character while the user is mid-syllable. Filtering on these partial values produces wrong matches ('ㅎ', '하', then '한' for a single keystroke) and causes visible flickering in the dropdown.**

Korean syllables are composed from up to three jamo components (초성/중성/종성). The IME emits:
1. `compositionstart` — user begins composing
2. Multiple `compositionupdate` events — each jamo stroke triggers an `input`/`change` event
3. `compositionend` — syllable committed, final character available

Filtering the option list on `compositionupdate` values produces meaningless partial tokens. The filter must only run after `compositionend`.

### The violation — onChange fires during IME composition

```typescript
// ❌ WRONG — no isComposing guard; filters fire on 'ㅎ', '하', '한' for one keystroke
"use client";
export default function Combobox({ options, onSelect }) {
  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const val = e.target.value;
    setQuery(val);
    // BUG: fires on compositionupdate — incomplete syllable triggers filter
    setFiltered(options.filter(o => o.includes(val)));
  }
  return <input type="text" onChange={handleChange} />;
}
```

### Correct — composition guard suppresses filter during IME

```typescript
// ✅ CORRECT — filter fires only after composition is committed
"use client";
import { useState, useRef } from "react";

export default function Combobox({ options, onSelect }: ComboboxProps) {
  const [query, setQuery] = useState("");
  const [filtered, setFiltered] = useState<string[]>([]);
  const composingRef = useRef(false); // true while IME is mid-composition

  function handleCompositionStart() { composingRef.current = true; }

  function handleCompositionEnd(e: React.CompositionEvent<HTMLInputElement>) {
    composingRef.current = false;
    // Fire filter once syllable is committed (compositionend value is final)
    const val = (e.target as HTMLInputElement).value;
    setFiltered(options.filter(o => o.includes(val)));
  }

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const val = e.target.value;
    setQuery(val);
    if (composingRef.current) return; // skip filter during CJK composition
    setFiltered(options.filter(o => o.includes(val)));
  }

  return (
    <input
      type="text"
      value={query}
      onChange={handleChange}
      onCompositionStart={handleCompositionStart}
      onCompositionEnd={handleCompositionEnd}
    />
  );
}
```

### Why this rule exists

First captured in SP14 combobox implementation. Korean users type approximately 30% of all characters via IME. Without the guard, a combobox filtering on `compositionupdate` fires a network search for every jamo stroke, causing:
1. Unnecessary search requests (3-4x traffic for Korean input)
2. Incorrect intermediate results visible in the dropdown
3. Perceived UX jank as the dropdown flickers between partial matches

The `onCompositionEnd` pattern is also required for Chinese (Pinyin/Zhuyin) and Japanese (Hiragana/Katakana) IME input — the same guard handles all CJK scripts.

Reference: [MDN Web Docs — CompositionEvent](https://developer.mozilla.org/en-US/docs/Web/API/CompositionEvent)

Reference: [W3C UI Events §CompositionEvent](https://www.w3.org/TR/uievents/#events-composition-types)


<!-- @source rules/js-batch-dom-css.md -->

---
title: Group DOM writes before reads; prefer className over imperative inline style
impact: MEDIUM
impactDescription: "Interleaving style writes with layout reads (offsetWidth, getBoundingClientRect, getComputedStyle) forces synchronous reflows. In React, prefer state-driven className over imperative ref.style mutations entirely."
tags: [javascript, dom, css, performance, reflow, layout-thrashing]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-002"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness: { status: complete, amendments: ["Prefer React state/className over ref.style", "Mention requestAnimationFrame for frame-level coordination"] }
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-batch-dom-css"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-batch-dom-css.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-batch-dom-css"
    quote: "Avoid interleaving style writes with layout reads."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Group DOM writes before reads; prefer className over imperative inline style

**Impact: MEDIUM — Interleaving style writes with layout reads forces synchronous reflows. In React, the deeper fix is to drive style with state + `className`, not imperative `ref.style` mutations.**

### Incorrect — interleaved write/read forces multiple reflows

```typescript
element.style.width = '100px'
const w = element.offsetWidth           // forces reflow
element.style.height = '200px'
const h = element.offsetHeight          // forces reflow
```

### Correct (read-then-write, or write-then-read)

```typescript
// read phase
const r = element.getBoundingClientRect()
// write phase
element.style.width = '100px'
element.style.height = '200px'
```

### Best — declarative className in React

```tsx
function Box({ highlighted }: { highlighted: boolean }) {
  return <div className={highlighted ? 'highlighted-box' : ''}>Content</div>
}
```

Direct `ref.style` writes belong only in genuinely imperative cases (animations driven by raw `requestAnimationFrame`, native DOM API integration, focus/scroll positioning).

### When refs + raw style is justified

- Coordinating frame-level work with `requestAnimationFrame`.
- Pre-measuring DOM for portal/popover placement (read-only in an effect).
- Imperative third-party libraries that own a DOM region.

Even then: keep all writes batched, all reads batched, transitions between phases at frame boundaries.

Sources:
- [Vercel: js-batch-dom-css](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-batch-dom-css.md)
- [Layout thrashing reference (paul irish gist)](https://gist.github.com/paulirish/5d52fb081b3570c81e3a)


<!-- @source rules/js-cache-function-results.md -->

---
title: Memoize pure deterministic function results in a bounded module-level Map; never store user/tenant-scoped data
impact: LOW-MEDIUM
impactDescription: "Avoids recomputation when the same pure function is called many times with the same inputs (slugify, parseColor, formatDate). Cache MUST be bounded (LRU or explicit max size) and MUST NOT key on user/tenant-scoped data without scoping."
tags: [javascript, cache, memoization, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-005"
verification: { type: review, status: manual, notes: "Reviewer checks: (a) function is pure & deterministic, (b) cache has explicit max size or LRU, (c) cache key doesn't leak across users/tenants, (d) cache doesn't grow unboundedly in long-lived processes." }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Required bounded cache (LRU / max size / TTL)"
      - "Forbid user/tenant-sensitive keys without scoping"
      - "Pure deterministic functions only"
      - "Include locale/options/version in key when relevant"
  gap_check:
    status: complete
    note: "Narrow scope: pure client/shared JS function memoization. Server caches use sibling rules (server-cache-react, nextjs-use-cache, server-cache-lru)."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-cache-function-results"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-cache-function-results.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-cache-function-results"
    quote: "Use a module-level Map to cache function results when the same function is called repeatedly with the same inputs during render."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [server-cache-react, server-cache-lru, nextjs-use-cache]
---

## Memoize pure deterministic function results in a bounded module-level Map

**Impact: LOW-MEDIUM — Useful for pure repeated work; the cache MUST be bounded.**

### When this applies

- Function is **pure** — same inputs → same output, no side effects.
- Inputs are **primitives or stable references**.
- Function is called many times during render or in a hot path.
- Examples: `slugify`, `parseColor`, `formatDate(locale, value)`, `escapeRegex`, hash functions on stable strings.

### Correct — bounded LRU

```typescript
import { LRUCache } from 'lru-cache'

const slugCache = new LRUCache<string, string>({ max: 500 })

export function cachedSlugify(text: string): string {
  const hit = slugCache.get(text)
  if (hit) return hit
  const result = slugify(text)
  slugCache.set(text, result)
  return result
}
```

### Correct — small bounded Map (no library)

```typescript
const MAX = 100
const cache = new Map<string, string>()

export function cachedFormat(input: string): string {
  if (cache.has(input)) return cache.get(input)!
  if (cache.size >= MAX) cache.clear()   // simple bounded behavior
  const result = format(input)
  cache.set(input, result)
  return result
}
```

### Incorrect — unbounded Map (memory leak)

```typescript
// BAD: grows forever, no eviction. Long-lived processes (Next.js servers) leak memory.
const cache = new Map<string, string>()
export function cachedSlugify(text: string): string {
  if (!cache.has(text)) cache.set(text, slugify(text))
  return cache.get(text)!
}
```

### Forbidden cache keys

- User-derived strings without scoping (admin sees other user's data — leak)
- Tenant-derived data without tenant id in key (tenant leak)
- Locale-sensitive output without locale in key (wrong language served)
- Time-sensitive output without TTL (stale data)

### Key composition for parametrized functions

```typescript
const cache = new LRUCache<string, string>({ max: 500 })

export function cachedFormat(value: number, locale: string): string {
  const key = `${locale}|${value}`            // locale in key
  const hit = cache.get(key)
  if (hit !== undefined) return hit
  const result = new Intl.NumberFormat(locale).format(value)
  cache.set(key, result)
  return result
}
```

### Sibling rules

- Server-side per-request dedup → `server-cache-react` (React.cache).
- Server-side cross-request → `nextjs-use-cache` (Next 16) or `server-cache-lru` (fallback).
- This rule: pure JS / client-side function memoization, scoped narrowly.

Sources:
- [Vercel: js-cache-function-results](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-cache-function-results.md)


<!-- @source rules/js-cache-property-access.md -->

---
title: Cache deep stable property paths outside hot loops; length caching is mostly noise on modern engines
impact: LOW
impactDescription: "Reduces deep property lookups in hot loops when the path is stable across iterations. Modern V8/SpiderMonkey/JavaScriptCore already optimize many cases — apply only when profiling shows real cost."
tags: [javascript, loops, optimization, caching]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-004"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Modern engines inline-cache property access; the 'cache length' guidance is legacy from older engines."
  completeness: { status: complete, amendments: ["Downplay 'cache length' for modern engines", "Apply only to genuinely hot loops with deep stable paths", "Prefer clarity over micro-opt"] }
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-cache-property-access"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-cache-property-access.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-cache-property-access"
    quote: "Cache object property lookups in hot paths."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Cache deep stable property paths outside hot loops

**Impact: LOW — Reduces deep property lookups in hot loops when the path is stable.**

### When this rule applies

- Deeply nested access (`obj.a.b.c.d`) inside a loop body.
- The path is **stable** — none of `a`/`b`/`c`/`d` change within the loop.
- The loop is genuinely hot (>~10K iterations or per-frame).

In any other case, prefer clarity. Modern engines (V8 / SpiderMonkey / JavaScriptCore) inline-cache property access well; the optimization may not show up in real benchmarks.

### Correct — deep path hoisted

```typescript
const config = obj.config.settings.value
for (let i = 0; i < arr.length; i++) {
  process(config)
}
```

### Modern caveat — `length` caching

```typescript
// Old advice: cache length for "faster" loops.
// Modern engines: irrelevant. Prefer clarity:
for (const x of arr) { /* ... */ }
// or
for (let i = 0; i < arr.length; i++) { /* ... */ }
```

The classic `const len = arr.length` micro-optimization predates inline caching. On modern engines, it's noise. Reserve for proven hot-loop-mutation-of-length cases.

### Anti-pattern — hoisting unstable paths

```typescript
const items = obj.config.settings.items  // ← if obj.config changes during loop, this is stale
for (let i = 0; i < N; i++) {
  obj.config.settings.someFlag = true
  process(items)
}
```

If the path isn't stable, don't hoist. Stale reads are worse than slow loops.

Sources:
- [Vercel: js-cache-property-access](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-cache-property-access.md)


<!-- @source rules/js-cache-storage.md -->

---
title: Cache repeated synchronous browser-storage reads in memory; invalidate on local writes and cross-tab storage events
impact: LOW-MEDIUM
impactDescription: "localStorage / document.cookie reads are synchronous and not free. Cache in memory for hot paths, but invalidate on local writes (storage event doesn't fire on the writing tab) and best-effort revalidate on focus/visibility."
tags: [javascript, localStorage, storage, caching, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-006"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "SSR guard required (window undefined on server)"
      - "Same-tab storage events do NOT fire on the writing tab — invalidate on local writes manually"
      - "Cookies can change via server Set-Cookie — visibility revalidation is best-effort"
      - "Keep localStorage and cookie examples separate (different invalidation paths)"
  gap_check:
    status: complete
    note: "Sibling client-localstorage-schema covers correctness/versioning/SSR; this rule is the performance dimension."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-cache-storage"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-cache-storage.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-cache-storage"
    quote: "localStorage, sessionStorage, and document.cookie are synchronous and expensive. Cache reads in memory."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [client-localstorage-schema]
---

## Cache repeated synchronous browser-storage reads in memory; invalidate on local writes and cross-tab events

**Impact: LOW-MEDIUM — Storage reads are synchronous and not free. Cache them, but invalidate correctly.**

### localStorage — cache + write-through

```typescript
const cache = new Map<string, string | null>()

export function getLocalItem(key: string): string | null {
  if (typeof window === 'undefined') return null   // SSR guard
  if (cache.has(key)) return cache.get(key) ?? null
  const value = window.localStorage.getItem(key)
  cache.set(key, value)
  return value
}

export function setLocalItem(key: string, value: string): void {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(key, value)
    cache.set(key, value)   // keep cache in sync — storage event won't fire here
  } catch {}
}

// Invalidate on cross-tab change (the storage event fires ONLY on other tabs)
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (e) => {
    if (e.key) cache.delete(e.key)
  })
}
```

### Critical invalidation rule

- `storage` event fires on OTHER tabs, NOT on the tab that called `setItem`. You must update the cache manually on every write in the same tab.
- `visibilitychange` invalidation is best-effort — useful for cookie data that the server might set, but doesn't guarantee freshness.

### Cookie — different invalidation path

```typescript
let cookieCache: Record<string, string> | null = null

export function getCookie(name: string): string | undefined {
  if (typeof document === 'undefined') return undefined   // SSR guard
  if (cookieCache === null) {
    cookieCache = Object.fromEntries(
      document.cookie.split('; ').filter(Boolean).map((c) => {
        const i = c.indexOf('=')
        return i < 0 ? [c, ''] : [c.slice(0, i), decodeURIComponent(c.slice(i + 1))]
      }),
    )
  }
  return cookieCache[name]
}

// Cookies can be set server-side (Set-Cookie header on AJAX/navigation responses)
// → best-effort revalidation on focus/visibility
if (typeof document !== 'undefined') {
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') cookieCache = null
  })
}
```

### Incorrect — every call re-reads

```typescript
function getTheme() {
  return localStorage.getItem('theme') ?? 'light'   // 10× = 10 storage reads
}
```

### Sibling rule

`client-localstorage-schema` covers correctness (versioning, try-catch, minimal fields, SSR safety). This rule is the **performance** dimension — combine both for production code.

Sources:
- [Vercel: js-cache-storage](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-cache-storage.md)


<!-- @source rules/js-combine-iterations.md -->

---
title: Combine multiple .filter/.map passes over the same array into one loop when the array is large or hot
impact: LOW-MEDIUM
impactDescription: "Three .filter() calls iterate three times and allocate three arrays. One for-of loop with three branches iterates once and allocates only the needed result arrays. Apply when the array is large or the path is hot — not as a blanket rule."
tags: [javascript, arrays, loops, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-007"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Don't replace readable pipelines by default — apply to large/hot arrays"
      - "Preserve exact semantics: order, short-circuiting, holes, side-effects"
      - "Consider reduce only when it improves clarity over named for-of"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-combine-iterations"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-combine-iterations.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-combine-iterations"
    quote: "Multiple .filter() or .map() calls iterate the array multiple times. Combine into one loop."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [js-flatmap-filter]
---

## Combine multiple .filter/.map passes when the array is large or hot

**Impact: LOW-MEDIUM — Three `.filter` calls = three iterations + three allocations. One `for-of` = one iteration. Apply when the array is large or the path is hot, not as a blanket rule.**

### Incorrect — 3 iterations + 3 allocations

```typescript
const admins = users.filter((u) => u.isAdmin)
const testers = users.filter((u) => u.isTester)
const inactive = users.filter((u) => !u.isActive)
```

### Correct — 1 iteration, 3 small result arrays

```typescript
const admins: User[] = []
const testers: User[] = []
const inactive: User[] = []

for (const user of users) {
  if (user.isAdmin) admins.push(user)
  if (user.isTester) testers.push(user)
  if (!user.isActive) inactive.push(user)
}
```

### Don't apply blanket

For small arrays (`< ~100` items) or non-hot paths, the readability of `.filter().filter().filter()` outweighs the perf cost. Keep the rule for:

- Lists ≥ ~1K items.
- Render-frequent hot paths.
- Stream-style processing where allocations matter.

### Preserve semantics

Be careful when consolidating:
- **Short-circuiting**: if `.filter` was followed by `.find` or `.some`, the consolidated loop must `break` on the right condition.
- **Order**: `.filter` preserves input order; if your loop puts items into different result arrays, those individual arrays also preserve order — fine.
- **Holes**: sparse arrays (`new Array(5)`) skip with `.filter` but show as `undefined` with `for-of`. Match the semantics you actually need.
- **Side-effects**: chained methods make each pass's side effects visible separately; a single loop interleaves them.

Sources:
- [Vercel: js-combine-iterations](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-combine-iterations.md)


<!-- @source rules/js-early-exit.md -->

---
title: Return on first failure unless the API contract requires collecting all errors
impact: LOW-MEDIUM
impactDescription: "Skips unnecessary processing once the result is determined. Sibling of async-defer-await for sync control flow. Don't apply when the consumer needs the full set of errors (form validation showing all field issues at once)."
tags: [javascript, functions, optimization, early-return, control-flow]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-009"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Exception: collect-all when UX/API contract needs full diagnostics"
      - "Cross-link to async-defer-await (async analog)"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-early-exit"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-early-exit.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-early-exit"
    quote: "Return early when result is determined to skip unnecessary processing."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [async-defer-await]
---

## Return on first failure unless the API contract requires collecting all errors

**Impact: LOW-MEDIUM — Sync analog of `async-defer-await`. Stop work once the answer is known.**

### Incorrect — keeps processing after the answer is known

```typescript
function validate(users: User[]) {
  let hasError = false
  let errorMessage = ''
  for (const u of users) {
    if (!u.email) { hasError = true; errorMessage = 'Email required' }
    if (!u.name)  { hasError = true; errorMessage = 'Name required' }
  }
  return hasError ? { valid: false, error: errorMessage } : { valid: true }
}
```

### Correct — return immediately

```typescript
function validate(users: User[]) {
  for (const u of users) {
    if (!u.email) return { valid: false, error: 'Email required' }
    if (!u.name) return { valid: false, error: 'Name required' }
  }
  return { valid: true }
}
```

### Exception — collect-all when the consumer needs it

Form validation that shows every field error at once should collect, not short-circuit:

```typescript
function collectFieldErrors(users: User[]): FieldError[] {
  const errors: FieldError[] = []
  for (let i = 0; i < users.length; i++) {
    if (!users[i].email) errors.push({ row: i, field: 'email', msg: 'Required' })
    if (!users[i].name) errors.push({ row: i, field: 'name', msg: 'Required' })
  }
  return errors
}
```

If the consumer just wants "valid yes/no" or "first error", short-circuit. If the consumer needs the full diagnostic set, collect.

### Cross-rule

`async-defer-await` is the async equivalent — don't await data that the early-return path discards.

Sources:
- [Vercel: js-early-exit](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-early-exit.md)


<!-- @source rules/js-flatmap-filter.md -->

---
title: "Prefer flatMap over map().filter(Boolean) — semantic clarity + single pass"
impact: LOW-MEDIUM
impactDescription: "One pass instead of two, no intermediate array. Also avoids the .filter(Boolean) semantic trap (filters out falsy primitives that may be legitimate values: 0, '', false). NOT always faster — flatMap allocates small wrapper arrays."
tags: [javascript, arrays, flatMap, filter, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-013"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Don't claim 'universally faster' — flatMap allocates small arrays"
      - "Main win is semantic: avoids filter(Boolean) dropping legitimate 0/''/false"
      - "If output should remain nested, wrap as [[y]]"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-flatmap-filter"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-flatmap-filter.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-flatmap-filter"
    quote: "Chaining .map().filter(Boolean) creates an intermediate array and iterates twice."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [js-combine-iterations]
---

## Prefer flatMap over map().filter(Boolean) — semantic clarity + single pass

**Impact: LOW-MEDIUM — One iteration instead of two; correctly handles falsy-but-valid values.**

### The `filter(Boolean)` trap

```typescript
// ❌ Drops legitimate values: 0, '', false, NaN
const userIds = users
  .map((u) => u.isActive ? u.id : null)
  .filter(Boolean)

// If a user.id is 0 (yes, this happens), it's dropped.
```

`filter(Boolean)` removes all falsy values, not just `null`. For ID arrays containing `0`, empty-string keys, or boolean-valued payloads, this is a silent data-loss bug.

### Correct — flatMap

```typescript
const userIds = users.flatMap((u) =>
  u.isActive ? [u.id] : [],
)
```

`[u.id]` is wrapped in an array even when `u.id` is `0` / `''` / `false` — no semantic loss.

### When NOT to claim "faster"

`flatMap` allocates a small wrapper array (`[]` or `[y]`) per element. For very large hot arrays where allocation matters, a plain for-loop may beat both `.map().filter()` and `.flatMap()`. Benchmark before claiming wins.

The main win of this rule is **semantic clarity**, not raw speed.

### If output should remain nested

```typescript
// flatMap flattens ONE level. If your value is itself an array and should
// remain nested:
nodes.flatMap((n) => n.tags.length > 0 ? [[n.id, n.tags]] : [])
//                                       ^^             ^^
//                                       wrap as nested array
```

### More examples

```typescript
// Parse, keeping only valid numbers
const numbers = strings.flatMap((s) => {
  const n = parseInt(s, 10)
  return Number.isNaN(n) ? [] : [n]
})

// Extract from success responses
const emails = responses.flatMap((r) =>
  r.success ? [r.data.email] : [],
)
```

Sources:
- [Vercel: js-flatmap-filter](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-flatmap-filter.md)


<!-- @source rules/js-hoist-regexp.md -->

---
title: Hoist static RegExp to module scope; for prop-dependent regex use useMemo; beware /g lastIndex
impact: LOW-MEDIUM
impactDescription: "new RegExp() inside render allocates and recompiles. Hoist if static; memoize if pattern depends on props. Global /g and sticky /y regexes have mutable lastIndex state — don't share across calls."
tags: [javascript, regexp, optimization, memoization]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-010"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Static patterns → hoist; dynamic patterns from props → useMemo"
      - "Reset lastIndex on /g and /y if shared across calls (or avoid sharing)"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-hoist-regexp"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-hoist-regexp.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-hoist-regexp"
    quote: "Don't create RegExp inside render. Hoist to module scope or memoize with useMemo()."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Hoist static RegExp; for dynamic patterns use useMemo; beware /g lastIndex

**Impact: LOW-MEDIUM — Don't allocate a new RegExp per render.**

### Correct — static pattern hoisted

```tsx
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function isEmail(s: string) {
  return EMAIL_RE.test(s)
}
```

### Correct — dynamic pattern memoized

```tsx
function Highlighter({ text, query }: Props) {
  const re = useMemo(
    () => new RegExp(`(${escapeRegex(query)})`, 'gi'),
    [query],
  )
  // ...
}
```

### Incorrect — new RegExp on every render

```tsx
function Highlighter({ text, query }: Props) {
  const re = new RegExp(`(${query})`, 'gi')   // recompiled every render + injection-prone
  // ...
}
```

Bonus bug: building a regex from untrusted `query` without escaping makes it user-controlled. Always `escapeRegex` the parts.

### The `/g` / `/y` lastIndex footgun

Global and sticky regex instances carry mutable `lastIndex` state:

```typescript
const RE = /foo/g
RE.test('foo')   // true, lastIndex = 3
RE.test('foo')   // false, lastIndex was 3, no match from there → reset to 0
RE.test('foo')   // true again, lastIndex = 3
```

Either:
- Don't share a `/g` regex across calls.
- Use `String.prototype.matchAll` (returns iterator, doesn't mutate the regex).
- Reset before use: `RE.lastIndex = 0`.

Sources:
- [Vercel: js-hoist-regexp](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-hoist-regexp.md)


<!-- @source rules/js-index-maps.md -->

---
title: Index-by-id Map for joining two collections — O(n²) .find loops become O(n)
impact: LOW-MEDIUM
impactDescription: "Build a Map keyed by id once (O(n)); each lookup is then O(1). For 1000 rows × 1000 lookups: 1M ops → 2K ops. Sibling of js-set-map-lookups (membership) — this one is for joins."
tags: [javascript, map, indexing, optimization, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-003"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness: { status: complete, amendments: ["Cross-link to js-set-map-lookups (membership-vs-join distinction)", "Map build cost noted", "Key identity matters (primitive keys preferred)"] }
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-index-maps"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-index-maps.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-index-maps"
    quote: "Multiple .find() calls by the same key should use a Map."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [js-set-map-lookups]
---

## Index-by-id Map for joining two collections

**Impact: LOW-MEDIUM — Build Map once (O(n)), then all lookups are O(1).**

### Incorrect (O(n) per `.find`, called N times)

```typescript
const enriched = orders.map((o) => ({
  ...o,
  user: users.find((u) => u.id === o.userId),
}))
```

### Correct (O(n) build + O(1) lookup × N)

```typescript
const userById = new Map(users.map((u) => [u.id, u]))
const enriched = orders.map((o) => ({
  ...o,
  user: userById.get(o.userId),
}))
```

### When it pays off

Build cost is O(n). Only worth it when:
- You're doing **multiple lookups** against the same collection.
- The collection is non-trivial (≥ ~50 items).
- Either / both are in a hot path.

For a single lookup, `find` is fine.

### Sibling distinction

- `js-set-map-lookups` covers **membership** (`Set.has`).
- This rule covers **join** (`Map.get(id)` returns the row).

### Key identity caveats

- Primitive keys (`string`, `number`) use SameValueZero — `1 === 1`.
- Object keys use reference identity — `{ id: 1 } !== { id: 1 }`. Index by a primitive (the id), not the whole object.

Sources:
- [Vercel: js-index-maps](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-index-maps.md)


<!-- @source rules/js-length-check-first.md -->

---
title: Cheap length compare before expensive array equality (sort, serialize, deep compare)
impact: MEDIUM-HIGH
impactDescription: "O(1) length check filters out the vast majority of inequal cases before the O(n log n) sort or O(n) deep compare runs. Big wins in change-detection hot paths."
tags: [javascript, arrays, performance, comparison]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-008"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Clarify ordered vs unordered comparison — sorted-join trick assumes unordered"
      - "Avoid JSON.stringify equality for objects with unstable key order or non-JSON values"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-length-check-first"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-length-check-first.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-length-check-first"
    quote: "When comparing arrays with expensive operations (sorting, deep equality, serialization), check lengths first."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Cheap length compare before expensive array equality

**Impact: MEDIUM-HIGH — O(1) check filters out most inequal cases before O(n log n) or O(n) work.**

### Incorrect — always runs the expensive path

```typescript
function hasChanges(current: string[], original: string[]) {
  return current.toSorted().join() !== original.toSorted().join()
}
```

### Correct — length first

```typescript
function hasChanges(current: string[], original: string[]) {
  if (current.length !== original.length) return true
  return current.toSorted().join() !== original.toSorted().join()
}
```

### Ordered vs unordered comparison

The `toSorted().join()` trick assumes **unordered** equality (the arrays are equal as sets). For **ordered** equality:

```typescript
function arraysEqualOrdered<T>(a: T[], b: T[]) {
  if (a.length !== b.length) return false
  for (let i = 0; i < a.length; i++) {
    if (!Object.is(a[i], b[i])) return false
  }
  return true
}
```

### `JSON.stringify` equality is a trap

```typescript
// BAD: key order is engine-dependent, NaN/undefined/functions don't round-trip,
// Date/Map/Set serialize losslessly only sometimes.
JSON.stringify(a) === JSON.stringify(b)
```

Use a real deep-equal library (`fast-deep-equal`) or write a typed comparator.

Sources:
- [Vercel: js-length-check-first](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-length-check-first.md)


<!-- @source rules/js-min-max-loop.md -->

---
title: Single-pass loop for min/max — O(n) instead of O(n log n) sort; Math.min/max spread only for small arrays
impact: LOW
impactDescription: "Sorting an entire array to find one element is O(n log n) plus O(n) allocation. A single pass is O(n) with no allocation. Math.min(...arr) / Math.max(...arr) hits engine argument-count limits at ~125K-640K elements depending on the engine."
tags: [javascript, arrays, performance, sorting, algorithms]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-011"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Handle empty array and NaN explicitly"
      - "Math.min/max spread only for small bounded arrays"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-min-max-loop"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-min-max-loop.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-min-max-loop"
    quote: "Finding the smallest or largest element only requires a single pass through the array. Sorting is wasteful and slower."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Single-pass loop for min/max — O(n) instead of O(n log n) sort

**Impact: LOW — Sort to find one element is wasteful; one pass suffices.**

### Incorrect — sort just to find latest

```typescript
function latest(items: Item[]) {
  const sorted = [...items].sort((a, b) => b.updatedAt - a.updatedAt)
  return sorted[0]
}
```

### Correct — single pass

```typescript
function latest(items: Item[]): Item | null {
  if (items.length === 0) return null
  let best = items[0]
  for (let i = 1; i < items.length; i++) {
    if (items[i].updatedAt > best.updatedAt) best = items[i]
  }
  return best
}
```

### Correct — both extrema in one pass

```typescript
function range(items: Item[]) {
  if (items.length === 0) return { oldest: null, newest: null }
  let oldest = items[0]
  let newest = items[0]
  for (let i = 1; i < items.length; i++) {
    if (items[i].updatedAt < oldest.updatedAt) oldest = items[i]
    if (items[i].updatedAt > newest.updatedAt) newest = items[i]
  }
  return { oldest, newest }
}
```

### Math.min/max with spread — small arrays only

```typescript
const numbers = [5, 2, 8, 1, 9]
const min = Math.min(...numbers)
const max = Math.max(...numbers)
```

Spread call-argument limit is engine-dependent (Chrome ~125K, Safari ~640K, Node varies). For potentially-large arrays, use the loop form. Throwing `RangeError: Maximum call stack size exceeded` is the failure mode.

### NaN handling

`Math.min` and `>` comparisons propagate `NaN` — a single `NaN` poisons the result. If your data may have `NaN`s, filter first:

```typescript
const cleaned = data.filter((x) => !Number.isNaN(x))
```

### Empty arrays

`Math.min()` returns `Infinity`, `Math.max()` returns `-Infinity`. Handle empty input explicitly to avoid surprise.

Sources:
- [Vercel: js-min-max-loop](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-min-max-loop.md)


<!-- @source rules/js-set-map-lookups.md -->

---
title: Use Set/Map for repeated membership lookups
impact: LOW-MEDIUM
impactDescription: "Avoids repeated linear scans. Build the Set/Map once, then use has/get instead of array includes/find. Pays off when the lookup is repeated; the build itself is O(n)."
tags:
  - javascript
  - set
  - map
  - data-structures
  - performance
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-001"
verification:
  type: eslint
  rule_id: "ax/no-array-includes-in-loop"
  status: shipped
  notes: "Custom ESLint rule planned: flag `array.includes(...)` or `array.find(...)` inside an iteration callback (.filter / .map / for-of / while) when the same array is closed over and not mutated in the loop. Until shipped: peer review checkpoint."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
  easy_rule_test: "Selected as the 'simple JS rule' case to validate the pipeline does not over-engineer simple rules. Result: pipeline produced a tight, well-caveated rule in ~10 minutes; pipeline cost scales appropriately."
audit:
  accuracy:
    status: verified-with-shorthand
    last_verified: "2026-05-16"
    notes: "TC39 spec guarantees sublinear (not strictly O(1)) Set/Map access. Engines commonly implement near-O(1) hash tables. 'O(n) → O(1)' is acceptable catalog shorthand if the formal note appears."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Stable JS primitive — not version-sensitive."
  completeness:
    status: complete
    amendments:
      - "Added construction-cost caveat (Set/Map build is O(n))"
      - "Added formal sublinear note (not strictly O(1) per spec)"
      - "Added object-key reference-identity trap"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (rule: js-set-map-lookups)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-set-map-lookups.md"
    role: "seed"
  - id: mdn-set
    title: "MDN — Set"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set"
    role: "primitive-semantics"
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-set-map-lookups"
    quote: "Convert arrays to Set/Map for repeated membership checks."
  - source_type: external
    citation: "MDN — Set (spec guarantee: 'access times that are sublinear on the number of elements in the collection')"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set"
  - source_type: external
    citation: "MDN — Set (Set.has is, on average, faster than Array.prototype.includes for length equal to set size; object keys use reference identity, primitives use SameValueZero)"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "All four audit verdicts confirmed"
    - "Three caveats are minimum needed (construction, sublinear formal, object reference)"
    - "Easy rule pipeline cost was appropriate; no over-engineering"
sibling_rules:
  - js-index-maps
---

## Use Set/Map for repeated membership lookups

**Impact: LOW-MEDIUM — Avoids repeated linear scans. Build the Set/Map once, then use `has`/`get` instead of scanning an array with `includes`/`find`. Pays off when the lookup is repeated; the build itself is O(n).**

### When it pays off

The Set/Map build cost is O(n). The lookup savings only matter when you do **many** lookups against the same collection. Two checks against a Set you just built is a wash; one check is a regression.

Rule of thumb: build a Set/Map outside the loop, use it inside the loop.

### Correct

```typescript
// Built once, used N times → wins linearly with N.
const allowedIds = new Set(['a', 'b', 'c' /*, ...*/])
const allowed = items.filter((item) => allowedIds.has(item.id))
```

### Incorrect

```typescript
// O(n × m): each includes() rescans the full allowedIds array.
const allowedIds = ['a', 'b', 'c' /*, ...*/]
const allowed = items.filter((item) => allowedIds.includes(item.id))
```

### Caveats

- **Formal complexity is sublinear, not strictly O(1).** TC39 only guarantees "access times that are sublinear on the number of elements in the collection." Engines commonly implement near-O(1) hash tables; treat "O(1) average" as engineering shorthand, not a spec guarantee.
- **Build cost is O(n).** If your lookup happens once, an array scan is fine — you save nothing by building a Set first.
- **Object keys match by reference identity, not value.** Primitive keys use SameValueZero (so `NaN === NaN` for Set purposes). For collections keyed by domain objects, prefer storing an id (primitive) and comparing by id.

```typescript
const cache = new Set<{ id: number }>()
cache.add({ id: 1 })
cache.has({ id: 1 })   // false — different object reference

// Prefer keying on the primitive id:
const cacheById = new Set<number>()
cacheById.add(1)
cacheById.has(1)       // true
```

### Verification

- Static check (planned): custom ESLint rule `ax/no-array-includes-in-loop`. Flags `arr.includes(x)` or `arr.find(...)` inside `.filter`, `.map`, `for-of`, or `while` when the array is closed over and not mutated inside the loop.
- Manual: bundle/profiler review for hot iteration sites.

Sources for this rule:

- [Vercel agent-skills: js-set-map-lookups](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-set-map-lookups.md)
- [MDN — Set](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set)


<!-- @source rules/js-tosorted-immutable.md -->

---
title: Use ES2023 immutable array methods (.toSorted/.toReversed/.toSpliced/.with) for React state and props
impact: MEDIUM-HIGH
impactDescription: ".sort/.reverse/.splice mutate in place — they corrupt props/state arrays and cause stale-closure bugs in React. The ES2023 immutable variants return a new array. Fallback for older targets: spread + sort."
tags: [javascript, arrays, immutability, react, state, mutation]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-012"
verification:
  type: lint
  rule_id: "ax/no-array-mutate-on-state"
  status: shipped
  notes: "Custom ESLint rule planned: flag arr.sort()/arr.reverse()/arr.splice() where arr is a prop or state-derived value."
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Chrome 110+, Safari 16+, Firefox 115+, Node 20+."
  completeness:
    status: complete
    amendments:
      - "State ES2023 support requirement"
      - "Fallback: [...arr].sort() / [...arr].reverse() / slice for older targets"
      - "Remind that .toSorted needs a comparator for numeric sort"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-tosorted-immutable"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-tosorted-immutable.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-tosorted-immutable"
    quote: ".sort() mutates the array in place, which can cause bugs with React state and props."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Use ES2023 immutable array methods for React state and props

**Impact: MEDIUM-HIGH — `.sort` / `.reverse` / `.splice` mutate. Mutating props is a React contract violation.**

### Incorrect — mutates the prop

```tsx
function UserList({ users }: { users: User[] }) {
  // ❌ .sort() mutates the parent's users array
  const sorted = useMemo(
    () => users.sort((a, b) => a.name.localeCompare(b.name)),
    [users],
  )
  return <ul>{sorted.map(renderUser)}</ul>
}
```

### Correct — immutable variant (ES2023)

```tsx
function UserList({ users }: { users: User[] }) {
  const sorted = useMemo(
    () => users.toSorted((a, b) => a.name.localeCompare(b.name)),
    [users],
  )
  return <ul>{sorted.map(renderUser)}</ul>
}
```

### The full ES2023 set

| Mutating | Immutable |
|---|---|
| `arr.sort(cmp)` | `arr.toSorted(cmp)` |
| `arr.reverse()` | `arr.toReversed()` |
| `arr.splice(start, count, ...items)` | `arr.toSpliced(start, count, ...items)` |
| `arr[i] = v` | `arr.with(i, v)` |

### Fallback for older targets (or polyfill)

```typescript
// Pre-ES2023
const sortedCopy = [...arr].sort((a, b) => a.value - b.value)
const reversedCopy = [...arr].reverse()
```

`[...arr]` makes a shallow copy, then mutating the copy is safe.

### Numeric sort needs a comparator

```typescript
const nums = [10, 2, 1]
nums.toSorted()              // ['1', '10', '2'] — string sort by default
nums.toSorted((a, b) => a - b)  // [1, 2, 10]
```

### Browser support

- Chrome 110+ (2023)
- Safari 16.4+ (2023)
- Firefox 115+ (2023)
- Node 20+ (2023)

Below those targets: use the spread fallback.

Sources:
- [Vercel: js-tosorted-immutable](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-tosorted-immutable.md)


<!-- @source rules/l2-prefer-data-prop-over-direct-fetch.md -->

---
title: "L2 data blocks — receive data as prop; never call fetch() or useQuery() inline"
impact: HIGH
impactDescription: "Calling fetch() or useQuery() inside an L2 data block binds it to a specific endpoint URL or query key, breaking layer-decoupling and making the block untestable without a running backend."
tags:
  - l2-layer
  - data-fetching
  - decoupling
  - data-blocks
  - tanstack-query
applicable_to:
  - nextjs
  - react
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-L2-002"
verification:
  type: review
  status: manual
  notes: "For each L2 data block, verify: (a) no fetch() or useQuery() calls inside the component, (b) data is accepted as a typed prop (data: Row[]), (c) loading/error state is accepted as props not derived from a query hook."
provenance:
  pilot: true
  pipeline_version: "2026-05-18"
  pipeline_steps: [implementation_observed, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-18"
  freshness:
    status: current
    last_verified: "2026-05-18"
    next_review_by: "2026-08-16"
  completeness:
    status: complete
    amendments:
      - "Observed during SP7 implementation: DataTable, FilterBar, Pagination, SearchInput all required this discipline"
  gap_check:
    status: complete
evidence:
  - upstream_id: tanstack-query-v5
    section: "Overview — separation of fetching and UI"
    quote: "React Query makes fetching, caching, synchronizing and updating server state in your React applications a breeze."
sibling_rules:
  - l2-prefer-onsubmit-prop
  - async-api-routes
  - client-swr-dedup
---

## L2 data blocks — receive data as prop; never call `fetch()` or `useQuery()` inline

**Impact: HIGH — Inlining a fetch or query hook inside an L2 block binds it to a URL and query key, destroying reusability and testability.**

### The violation (do NOT do this in L2)

```typescript
// ❌ WRONG — L2 block fetching its own data
import { useQuery } from '@tanstack/react-query'

export default function DataTable() {
  // Hardcoded endpoint = domain coupling
  const { data, isLoading } = useQuery({
    queryKey: ['products'],
    queryFn: () => fetch('/api/products').then(r => r.json()),
  })
  return <table>...</table>
}
```

### Correct — data received as props

```typescript
// ✅ CORRECT — L2 block renders whatever data the caller provides
export interface DataTableProps<Row> {
  data: Row[]                            // caller fetches, block renders
  isLoading?: boolean                    // caller's loading state
  onSort?: (state: SortState) => void    // caller handles server-sort
  getRowKey: (row: Row) => string
  columns: ColumnDef<Row>[]
}

export default function DataTable<Row>({ data, isLoading, ...}: DataTableProps<Row>) {
  return <table aria-busy={isLoading}>...</table>
}
```

### L4 owns the fetch + query

```typescript
// app/(app)/products/page.tsx — L4 fetches and passes data down
import { useQuery } from '@tanstack/react-query'
import DataTable from 'templates/L2/blocks/data-table'

export default function ProductsPage() {
  const [sort, setSort] = useState<SortState>()
  const { data, isLoading } = useQuery({
    queryKey: ['products', sort],
    queryFn: () => fetchProducts(sort),
  })
  return (
    <DataTable
      data={data ?? []}
      isLoading={isLoading}
      sort={sort}
      onSort={setSort}
      columns={PRODUCT_COLUMNS}
      getRowKey={p => p.id}
    />
  )
}
```

### Why this rule exists

During SP7 block implementation, DataTable, FilterBar, Pagination, and SearchInput were natural candidates for inline TanStack Query usage. Keeping data as a prop:

1. **Tests without a backend** — pass `data={mockRows}` directly in unit tests.
2. **Works with any server-state library** — TanStack Query, SWR, RSC, manual fetch — caller decides.
3. **Supports any endpoint** — same DataTable renders products, orders, or users.
4. **Decouples pagination strategy** — cursor vs. offset pagination stays in L4.

### Layer enforcement

L2 data blocks must not contain:
- `import { useQuery } from '@tanstack/react-query'`
- `import useSWR from 'swr'`
- `fetch(...)` calls
- `import ... from 'app/...''` (any backend URL binding)


<!-- @source rules/l2-prefer-onsubmit-prop.md -->

---
title: "L2 form blocks — accept onSubmit prop; never import server actions directly"
impact: HIGH
impactDescription: "Importing a server action or fetch inside an L2 block couples the block to a domain, breaking the layer contract and preventing reuse across domains."
tags:
  - l2-layer
  - server-actions
  - decoupling
  - form-blocks
  - nextjs
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-L2-001"
verification:
  type: review
  status: manual
  notes: "For each L2 form block, verify: (a) no `import ... from 'app/actions/...'` or `import ... from 'lib/...'` in the block file, (b) the form accepts an `onSubmit` callback prop, (c) the callback prop is typed in the exported interface. See check-imports.sh for the static enforcement in ax-verify-L2."
provenance:
  pilot: true
  pipeline_version: "2026-05-18"
  pipeline_steps: [implementation_observed, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-18"
  freshness:
    status: current
    last_verified: "2026-05-18"
    next_review_by: "2026-08-16"
  completeness:
    status: complete
    amendments:
      - "Observed during SP7 implementation: LoginForm, SignupForm, CrudCreateForm, CrudEditForm, PaymentCheckoutForm all required this discipline"
  gap_check:
    status: complete
evidence:
  - upstream_id: nextjs-server-actions-16
    section: "Server Actions — calling server actions"
    quote: "Server Actions can be called using the action attribute in a <form> element or in event handlers."
sibling_rules:
  - l2-prefer-data-prop-over-direct-fetch
  - async-api-routes
provenance_class: internal_design
protects_template_id: templates/L2/blocks/
failing_fixture_path: practices/evals/fixtures/l2-prefer-onsubmit-prop/fail_server_action_import/
decided_at: "2026-05-18"
---

## L2 form blocks — accept `onSubmit` prop; never import server actions directly

**Impact: HIGH — Importing a server action inside an L2 block couples the block to a specific domain and breaks the layer contract.**

### The violation (do NOT do this in L2)

```typescript
// ❌ WRONG — L2 block importing a server action directly
import { loginAction } from 'app/actions/auth'
import { createProductAction } from 'app/actions/products'

export default function LoginForm() {
  async function handleSubmit(formData: FormData) {
    await loginAction(formData)  // domain import — block is no longer reusable
  }
  return <form action={handleSubmit}>...</form>
}
```

### Correct — props-only callback

```typescript
// ✅ CORRECT — L2 block accepts onSubmit from caller (L4 injects the action)
export interface LoginFormProps {
  onSubmit: (values: { email: string; password: string }) => void
  isLoading?: boolean
  errorMessage?: string
}

export default function LoginForm({ onSubmit, isLoading, errorMessage }: LoginFormProps) {
  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    onSubmit({ email, password })
  }
  return <form onSubmit={handleSubmit}>...</form>
}
```

### L4 wires the action to the prop

```typescript
// app/(auth)/login/page.tsx — L4 provides the domain glue
import LoginForm from 'templates/L2/blocks/login-form'
import { loginAction } from './actions'

export default function LoginPage() {
  return (
    <LoginForm
      onSubmit={async (values) => {
        await loginAction(values)
      }}
    />
  )
}
```

### Why this rule exists

During SP7 block implementation, every auth, CRUD, and payment form block was a candidate for inlining a server action call. Keeping `onSubmit` as a prop kept each block:

1. **Domain-agnostic** — LoginForm works for any auth domain without modification.
2. **Testable** — tests pass a spy function; no server action mocking needed.
3. **Layer-clean** — ax-verify-L2 `check-imports.sh` enforces the import boundary statically.

### Layer enforcement

`bash skills/ax-verify-L2/scripts/check-imports.sh` fails with `ILLEGAL_IMPORT` if any L2 file contains an import referencing `templates/L3/`, `templates/L4/`, or `app/`.


<!-- @source rules/next-async-params-parallel.md -->

---
title: "Next.js 16 async params — await params alongside independent server work, not before"
impact: MEDIUM
impactDescription: "In Next.js 16, route params and searchParams are Promises. If next-step work is independent of the param value, include the param promise in the same Promise.all rather than awaiting it sequentially. Only param-dependent work blocks on await."
tags:
  - async
  - parallelization
  - nextjs
  - app-router
  - params
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ASYNC-006"
verification:
  type: review
  status: manual
  notes: "Reviewer flags any `const { x } = await params` immediately followed by independent server work that doesn't reference x — those should be aggregated into one Promise.all."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus_inherited]
  split_origin: "async-parallel codex review explicitly endorsed creating this sibling rule (verbatim: 'Next.js async params deserves a separate sibling rule')."
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-17"
    notes: "Next.js 16 App Router signature: `params: Promise<...>`. Evidence in nextjs-fetching-data.snapshot.md."
  freshness:
    status: current
    last_verified: "2026-05-17"
    next_review_by: "2026-08-15"
    notes: "Async params introduced in Next.js 15; default in Next.js 16."
  completeness:
    status: complete
    amendments:
      - "Distinguish param-dependent (sequential needed) vs param-independent (parallelizable) next-step work"
      - "Add type signature reminder: params: Promise<{ id: string }> in Next 16"
      - "Cross-link parent rule async-parallel"
  gap_check:
    status: complete
upstream:
  - id: nextjs-fetching-data
    title: "Next.js 16 — Fetching Data (params are Promise<T>)"
    url: "https://nextjs.org/docs/app/getting-started/fetching-data"
    role: canonical-nextjs
evidence:
  - upstream_id: nextjs-fetching-data
    section: "Next.js 16-specific: async params"
    quote: "Route handler signature: params: Promise<{ username: string }>. The page must await params before use; this is a separate sequential await that can be parallelized with other independent work."
  - upstream_id: nextjs-fetching-data
    section: "Parallel data fetching"
    quote: "Start multiple requests by calling fetch, then await them with Promise.all. Requests begin as soon as fetch is called."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=high (inherited from async-parallel review)"
  reviewed_at: "2026-05-16"
  verdict: SHIP_AS_PRE_ENDORSED_SIBLING
  agreements:
    - "Endorsed during async-parallel review as a needed split"
    - "Rationale verbatim: 'await params/searchParams alongside independent server work to avoid a needless waterfall'"
    - "'only start param-dependent work after the params resolve'"
sibling_rules:
  - async-parallel
  - async-dependencies
  - async-defer-await
---

## Next.js 16 async params — aggregate, don't sequence

**Impact: MEDIUM — `params` and `searchParams` are Promises in Next.js 16. Treat them as one more independent promise to aggregate via `Promise.all`, not as a precondition to await before anything else can start.**

### The shape that changed

```tsx
// Next.js 16 App Router signatures
export default async function Page({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>
  searchParams: Promise<{ sort?: string }>
}) {
  // ...
}
```

`params` is now a `Promise<{ id: string }>`, not `{ id: string }`. Same for `searchParams`. Awaiting them costs one tick of the microtask queue per param object — small per call, but it stacks if you also need to do other independent server work.

### Incorrect — params block independent work

```tsx
export default async function Page({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  // ❌ Sequential: config fetch waits for params to resolve, even though
  // config has nothing to do with the route id.
  const { id } = await params
  const config = await fetchConfig()
  const post = await fetchPost(id)
  return <Article config={config} post={post} />
}
```

`fetchConfig()` could have started at the same moment as `params`. Instead it waits for params to resolve, then for `fetchPost` (which legitimately depends on id), then renders.

### Correct — param-dependent work waits; param-independent work parallelizes

```tsx
export default async function Page({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  // Initiate everything that doesn't depend on params alongside params itself.
  const configPromise = fetchConfig()
  const { id } = await params

  // Post fetch depends on id — start it now (after params resolved).
  const postPromise = fetchPost(id)

  // Aggregate at the latest moment.
  const [config, post] = await Promise.all([configPromise, postPromise])

  return <Article config={config} post={post} />
}
```

`fetchConfig` and the params resolution run concurrently. `fetchPost` waits for params (genuinely dependent). The final aggregate joins them.

### When the next-step work is ALL param-independent

If nothing else needs `id`, you can include `params` directly in the `Promise.all`:

```tsx
export default async function Page({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const [{ id }, config, recommendations] = await Promise.all([
    params,
    fetchConfig(),
    fetchRecommendations(),
  ])
  return <Article id={id} config={config} recommendations={recommendations} />
}
```

`Promise.all` destructures the resolved param object inline. One await for three independent values.

### `searchParams` is the same shape

```tsx
export default async function ListPage({
  searchParams,
}: {
  searchParams: Promise<{ q?: string; sort?: string }>
}) {
  const [{ q, sort }, totalCount] = await Promise.all([
    searchParams,
    fetchTotalCount(),
  ])
  // ...
}
```

### Type-safety reminder

TypeScript catches missing `await` on params at compile time — using `params.id` instead of `(await params).id` is a type error. The risk is not a missing await; it's a NEEDLESSLY SEQUENTIAL await blocking independent work.

### Cross-rule scope

- `async-parallel` — the parent rule (init early, await late, aggregate via `Promise.all`).
- This rule — the Next.js-16-specific specialization for `params` / `searchParams`.
- `async-dependencies` — when there's a partial dependency graph (some calls depend, some don't).
- `async-defer-await` — when work might not be needed at all (early-return path).

Sources:

- [Next.js 16 — Fetching Data (async params)](https://nextjs.org/docs/app/getting-started/fetching-data)


<!-- @source rules/nextjs-use-cache-private.md -->

---
title: "use cache: private — experimental escape hatch; refactor runtime APIs out of cached scopes first; not production-recommended in 16.2.6"
impact: LOW
impactDescription: "Per-client browser-memory cache that allows cookies()/headers()/searchParams inside a cached scope. Lost on reload, never stored server-side, not a durable per-user cache. Use ONLY when refactoring the runtime read out is impractical or compliance forbids server storage."
tags:
  - server
  - cache
  - nextjs
  - cache-components
  - use-cache-private
  - experimental
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-010"
verification:
  type: review
  status: manual
  notes: "Reviewer rejects 'use cache: private' unless (a) the function genuinely cannot have its runtime read refactored out, OR (b) compliance forbids server-side storage. Explicit ADR justifying the choice required. Confirms stale >= 30s and use is not in a Route Handler."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Escape-hatch framing aligns with docs explicit guidance. Not durable, not per-user server cache."
  freshness:
    status: experimental-not-production
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Feature is experimental in 16.2.6, depends on runtime prefetching which is not yet stable. Re-review when stabilized."
  completeness:
    status: complete
    amendments:
      - "Lead with experimental status + not-production warning"
      - "Refactor-first hierarchy: try moving runtime read out before reaching for this"
      - "Hard constraints: no Route Handlers, no custom cache handlers, connection() forbidden, stale >= 30s"
      - "Per-client browser-memory only, lost on reload"
  gap_check:
    status: complete
    note: "Main misuse risk: 'private' easily misread as 'durable per-user server cache'. It is NOT that."
upstream:
  - id: nextjs-use-cache-private
    title: "Next.js 16 — 'use cache: private' directive"
    url: "https://nextjs.org/docs/app/api-reference/directives/use-cache-private"
    role: canonical-nextjs
  - id: nextjs-use-cache-directive
    title: "Next.js 16 — 'use cache' (parent directive)"
    url: "https://nextjs.org/docs/app/api-reference/directives/use-cache"
    role: canonical-nextjs
evidence:
  - upstream_id: nextjs-use-cache-private
    section: "Experimental status"
    quote: "This feature is currently experimental and subject to change, it's not recommended for production."
  - upstream_id: nextjs-use-cache-private
    section: "Storage model"
    quote: "However, results are never stored on the server, they're cached only in the browser's memory and do not persist across page reloads."
  - upstream_id: nextjs-use-cache-private
    section: "When to use"
    quote: "Reach for 'use cache: private' when: You want to cache a function that already accesses runtime data, and refactoring to move the runtime access outside and pass values as arguments is not practical."
  - upstream_id: nextjs-use-cache-private
    section: "Constraints"
    quote: "It is not possible to configure custom cache handlers for 'use cache: private'."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Frame as escape hatch, not a recommended cache strategy"
    - "Lead with experimental + not-production warning"
    - "Strong 'refactor first' hierarchy"
    - "Cache scope bluntly stated: browser-memory only, per-client, reload-lost"
sibling_rules:
  - nextjs-use-cache
  - nextjs-use-cache-remote
  - server-cache-react
---

## 'use cache: private' is an experimental escape hatch — refactor first

**Impact: LOW — Narrow tool, not production-recommended in 16.2.6.**

> **Experimental in Next.js 16.2.6** (per official docs: "This feature is currently
> experimental and subject to change, it's not recommended for production"). The
> feature depends on runtime prefetching, which is not yet stable. Treat as
> opt-in for compliance / unrefactorable corners only.

### Refactor-first hierarchy

Before reaching for `'use cache: private'`, try in order:

1. **Move runtime read out of the cached scope.** Read `cookies()` / `headers()` / `searchParams` in the calling component and pass the resolved value as an argument to a regular `'use cache'` function. See sibling rule `nextjs-use-cache` and the Next.js "Working with runtime APIs" guide.
2. **Use a `<Suspense>` boundary** to defer the runtime read to request time while keeping cached children separate.
3. **Only after the above fail or are impractical**, consider `'use cache: private'`. Document the choice in an ADR.

### What this directive actually is

- **Per-client cache** — lives in the user's browser memory only.
- **Not stored on the server** — useful for compliance ("we can't cache this data server-side") but means cache utilization is zero across users.
- **Lost on page reload** — not durable.
- **Server still re-executes the function on every render** — the cache only helps client-side navigation, not server work.

### First try the simpler form — `cookies()` outside any cached scope

Most cookie reads need NO caching directive at all. Read in the Server
Component / Server Action / Route Handler and use the value directly:

```tsx
// app/page.tsx — read cookie in an UNCACHED Server Component, pass value
// to other layers as a primitive argument.
import { cookies } from 'next/headers'

export default async function Page() {
  const theme = (await cookies()).get('theme')?.value ?? 'light'
  return <Hero theme={theme} />
}
```

```tsx
// app/actions.ts — read cookie inside a Server Action, the simplest
// "cookie-scoped data" pattern. No caching involved.
'use server'
import { cookies } from 'next/headers'
import { saveFor } from '@/lib/store'

export async function save(formData: FormData) {
  const owner = (await cookies()).get('owner')?.value
  if (!owner) return
  saveFor(owner, formData.get('value') as string)
}
```

If you only need to consume the cookie value (no caching required), these are
the right forms. `'use cache: private'` enters the picture only when ALL of
the following hold: (a) the function genuinely benefits from caching, (b)
refactoring the runtime read out (per the parent `'use cache'` rule) is
impractical, and (c) you accept the experimental status documented above.

External-validation evidence: the ax-validation-todo app (a Server-Action
based Todo) issues and reads a cookie on every action call without ever
reaching for `'use cache: private'` — the simpler form was sufficient.

### Correct usage (when justified)

```tsx
// app/product/[id]/page.tsx
import { Suspense } from 'react'
import { cookies } from 'next/headers'
import { cacheLife, cacheTag } from 'next/cache'

async function getRecommendations(productId: string) {
  'use cache: private'
  cacheTag(`recommendations-${productId}`)
  cacheLife({ stale: 60 }) // >= 30s required for runtime prefetching

  const sessionId = (await cookies()).get('session-id')?.value || 'guest'
  return getPersonalizedRecommendations(productId, sessionId)
}

export default async function ProductPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params
  return (
    <>
      <ProductDetails id={id} />
      <Suspense fallback={<div>Loading recommendations...</div>}>
        <Recommendations productId={id} />
      </Suspense>
    </>
  )
}
```

### Hard constraints (build-time errors)

- **Not allowed in Route Handlers.**
- **Cannot configure a custom cache handler.**
- **`connection()` is forbidden inside** (provides connection-specific data that cannot be safely cached).
- **`stale` time must be >= 30 seconds** for runtime prefetching to work.

### Allowed inside this scope (verbatim from docs)

| API | `use cache` | `'use cache: private'` |
|---|---|---|
| `cookies()` | No | **Yes** |
| `headers()` | No | **Yes** |
| `searchParams` | No | **Yes** |
| `connection()` | No | No |

### What 'use cache: private' is NOT

- NOT a per-user server cache (that's `'use cache: remote'` keyed by user — but see sibling rule's warning about high-cardinality keys).
- NOT durable across page reloads.
- NOT a shared cache across users.
- NOT a replacement for proper data refactor.

### Anti-pattern — reaching for private when refactor is feasible

```tsx
// BAD: trivial refactor available — read theme outside, pass as arg
async function getThemedContent() {
  'use cache: private'
  const theme = (await cookies()).get('theme')?.value
  return cms.getThemedHero(theme)
}
```

Refactor:

```tsx
// GOOD: regular 'use cache' with the runtime value passed as an arg
async function getThemedContent(theme: string) {
  'use cache'
  cacheLife({ expire: 3600 })
  return cms.getThemedHero(theme)
}

// In the consuming component:
async function Hero() {
  const theme = (await cookies()).get('theme')?.value ?? 'light'
  const content = await getThemedContent(theme)
  return <Banner data={content} />
}
```

The regular `'use cache'` form now caches **per theme** — far better hit rate than per-client.

Sources:

- [Next.js — 'use cache: private'](https://nextjs.org/docs/app/api-reference/directives/use-cache-private)
- [Next.js — Working with runtime APIs](https://nextjs.org/docs/app/getting-started/caching#working-with-runtime-apis)


<!-- @source rules/nextjs-use-cache-remote.md -->

---
title: "use cache: remote — shared durable caching across server instances; gate on hit-rate and cost first"
impact: HIGH
impactDescription: "Stores cache entries in a remote handler (Redis/KV) for cross-instance persistence and high cache utilization. Pays infrastructure + lookup latency cost. ONLY use when work is expensive/rate-limited AND cache keys have few unique values."
tags:
  - server
  - cache
  - nextjs
  - cache-components
  - use-cache-remote
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-011"
verification:
  type: review
  status: manual
  notes: "Reviewer enforces the decision gate: (a) work is expensive/rate-limited/flaky, (b) cache keys have low cardinality (high hit rate expected), (c) cache scope is shared (NOT per-user). Reviewer confirms nesting rules and that cacheHandlers is configured if self-hosting."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Durable shared cache, not per-user. Adds infra + network cost."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Stable since v16.0.0 with Cache Components."
  completeness:
    status: complete
    amendments:
      - "Promote 'when NOT to use' to first-class decision gate"
      - "Cache-scope bluntly: shared across all users; never per-user (use 'use cache: private' for that)"
      - "Nesting matrix complete: remote-in-remote OK, remote-in-regular OK, remote+private forbidden in both directions"
      - "Cache-key cardinality principle with examples"
  gap_check:
    status: complete
    note: "Biggest practical failure mode is choosing remote when keys are mostly-unique — paid latency for near-zero hits."
upstream:
  - id: nextjs-use-cache-remote
    title: "Next.js 16 — 'use cache: remote' directive"
    url: "https://nextjs.org/docs/app/api-reference/directives/use-cache-remote"
    role: canonical-nextjs
  - id: nextjs-use-cache-directive
    title: "Next.js 16 — 'use cache' (parent directive)"
    url: "https://nextjs.org/docs/app/api-reference/directives/use-cache"
    role: canonical-nextjs
evidence:
  - upstream_id: nextjs-use-cache-remote
    section: "What it solves"
    quote: "The 'use cache: remote' directive lets you declaratively specify that a cached output should be stored in a remote cache instead of in-memory, providing durable caching shared across all server instances."
  - upstream_id: nextjs-use-cache-remote
    section: "Tradeoffs"
    quote: "This comes with tradeoffs: infrastructure cost and network latency during cache lookups."
  - upstream_id: nextjs-use-cache-remote
    section: "Cache-key principle"
    quote: "Be thoughtful about which values you include in cache keys. Each unique value creates a separate cache entry, reducing cache utilization."
  - upstream_id: nextjs-use-cache-remote
    section: "Nesting rules"
    quote: "Remote caches cannot be nested inside private caches ('use cache: private'). Private caches cannot be nested inside remote caches."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Promote 'when NOT to use' to first-class decision gate"
    - "Cache-key cardinality is the second most important content"
    - "Shared scope must be explicit; never confuse with per-user"
    - "Nesting matrix must be exact"
sibling_rules:
  - nextjs-use-cache
  - nextjs-use-cache-private
  - server-cache-lru
  - server-cache-react
---

## 'use cache: remote' for shared durable caching — gate on hit rate and cost first

**Impact: HIGH (when correctly applied) — Cross-instance durable cache layer for expensive shared work. Wrong gate decision turns this into paid latency for near-zero hits.**

### Decision gate (per Next.js docs)

**Reach for `'use cache: remote'` ONLY when ALL of these hold:**

1. The work is genuinely expensive (slow DB query, rate-limited API, flaky service, costly computation).
2. Cache keys have **low cardinality** (few unique values → high hit rate).
3. The cache scope is **shared across users** (never per-user — that's `'use cache: private'`).
4. Data doesn't change so frequently that hits go stale immediately.

**Do NOT reach for `'use cache: remote'` when:**

- You already have a server-side key-value store wrapping your data layer (regular `'use cache'` may suffice).
- Operations are already fast (< 50ms) due to proximity or local access — the lookup might be slower than the original work.
- Cache keys are mostly-unique per request (search filters, price ranges, user IDs) — utilization will be near-zero.
- Data changes faster than the cache TTL (seconds/minutes) — most lookups are stale.

### Cache-key cardinality is the implementation gate

The single most common failure mode is caching on a high-cardinality dimension. Fix by **caching on the low-cardinality dimension and filtering in memory**.

```tsx
// BAD: cache per (category, minPrice). minPrice has thousands of values → near-zero hit rate.
async function getProducts(category: string, minPrice: number) {
  'use cache: remote'
  return db.products.findByCategoryAndPrice(category, minPrice)
}
```

```tsx
// GOOD: cache per category (few values), filter price in memory.
async function getProductsByCategory(category: string) {
  'use cache: remote'
  cacheTag(`products-${category}`)
  return db.products.findByCategory(category)
}

async function ProductList({ category, minPrice }: Props) {
  const products = await getProductsByCategory(category)
  return <List items={minPrice ? products.filter((p) => p.price >= minPrice) : products} />
}
```

Same principle for user-specific data: extract the low-cardinality preference (language, currency) and cache on that, not on `sessionId`.

### Configuration

```ts
// next.config.ts
import type { NextConfig } from 'next'
const nextConfig: NextConfig = { cacheComponents: true }
export default nextConfig
```

Cache handler is configured via [`cacheHandlers`](https://nextjs.org/docs/app/api-reference/config/next-config-js/cacheHandlers). Hosting providers (Vercel etc.) typically configure this automatically; self-hosters wire their own (Redis / KV / etc.).

### Comparison with siblings

| Feature | `'use cache'` | `'use cache: remote'` | `'use cache: private'` |
|---|---|---|---|
| Server-side caching | in-memory or custom handler | remote handler (Redis/KV) | none |
| Cache scope | shared across users | shared across users | per-client (browser) |
| `cookies()`/`headers()` inside | No | No | Yes |
| Server cache utilization | may be low outside static shell | high (shared across instances) | n/a |
| Extra cost | none | infrastructure + lookup latency | none |
| Production-ready | yes | yes (since 16.0.0) | **no** (experimental) |

### Nesting rules (build-time enforced)

| Outer / Inner | `'use cache'` | `'use cache: remote'` | `'use cache: private'` |
|---|---|---|---|
| `'use cache'` | OK | OK | OK |
| `'use cache: remote'` | OK | OK | **FORBIDDEN** |
| `'use cache: private'` | OK | **FORBIDDEN** | OK |

Private and remote cannot nest in either direction.

### Correct example — rate-limited CMS

```tsx
import { cookies } from 'next/headers'
import { cacheLife } from 'next/cache'

async function WelcomeMessage() {
  // Language is a small-cardinality preference (~10-50 values)
  const language = (await cookies()).get('language')?.value || 'en'
  const content = await getCMSContent(language)
  return <div>{content.welcomeMessage}</div>
}

async function getCMSContent(language: string) {
  'use cache: remote'
  cacheLife({ expire: 3600 })
  // ~10 cache entries for ~10 languages, shared across ALL users
  return cms.getHomeContent(language)
}
```

### Platform support (verbatim)

| Deployment | Supported |
|---|---|
| Node.js server | Yes |
| Docker | Yes |
| Static export | **No** |
| Adapters | Yes |

### Invalidation

Use `cacheTag('...')` inside the cached function and `revalidateTag('...')` from a Server Action / Route Handler.

Sources:

- [Next.js — 'use cache: remote'](https://nextjs.org/docs/app/api-reference/directives/use-cache-remote)
- [Next.js — cacheHandlers](https://nextjs.org/docs/app/api-reference/config/next-config-js/cacheHandlers)


<!-- @source rules/nextjs-use-cache.md -->

---
title: Use the 'use cache' directive for Next.js 16 Cache Components persistent caching
impact: HIGH
impactDescription: "Provides persistent, cross-request, tag-revalidatable caching with compiler-generated cache keys. The Next.js 16 framework-recommended caching primitive for App Router. Replaces most use cases previously served by React.cache() in Next.js apps."
tags:
  - server
  - cache
  - nextjs
  - cache-components
  - use-cache
  - rsc
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-002"
verification:
  type: review
  status: manual
  notes: "Static rule could detect: 'use cache' inside file/function/component, configured `cacheComponents: true` in next.config, no cookies()/headers()/searchParams direct access inside 'use cache' scope, non-serializable arg patterns. None in pilot scope. Manual review until ESLint rule ships."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
  split_origin: "Created from Vercel server-cache-react split. Vercel's seed catalog has no rule for 'use cache' directive — this is a gap_check finding shipped as a new sibling rule."
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Mechanic, cache-key composition, serialization constraints, runtime-API forbidden list, React.cache isolation — all verified verbatim against Next.js 16.2.6 use-cache directive page."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "'use cache' was introduced experimental in v15.0.0 and enabled with Cache Components in v16.0.0. Anchored to next@16.2.6."
  completeness:
    status: complete
    amendments:
      - "Documented cache key composition (build ID + function ID + args + closures)"
      - "Documented serialization constraints (different systems for args vs return)"
      - "Forbidden inside-cache APIs (cookies / headers / searchParams)"
      - "React.cache isolation rule"
      - "Runtime caching by deployment environment (serverless vs self-hosted)"
      - "Build-hang anti-pattern (passing runtime Promises into cached scope)"
      - "On-demand invalidation surface (cacheTag / updateTag / revalidateTag)"
  gap_check:
    status: complete
    note: "Sibling rule for React.cache (server-cache-react.md) covers the non-Next case. 'use cache: private' and 'use cache: remote' are referenced but not catalog rules of their own (yet) — flag in pilot-report for future."
upstream:
  - id: nextjs-use-cache-directive
    title: "Next.js 16 — 'use cache' directive API reference"
    url: "https://nextjs.org/docs/app/api-reference/directives/use-cache"
    role: "canonical-nextjs"
  - id: react-19-cache
    title: "React 19 — cache() API (for isolation context)"
    url: "https://react.dev/reference/react/cache"
    role: "canonical-react"
evidence:
  - upstream_id: nextjs-use-cache-directive
    section: "Apply level"
    quote: "The use cache directive allows you to mark a route, React component, or a function as cacheable."
  - upstream_id: nextjs-use-cache-directive
    section: "Cache keys"
    quote: "When a cached function references variables from outer scopes, those variables are automatically captured and bound as arguments, making them part of the cache key."
  - upstream_id: nextjs-use-cache-directive
    section: "Runtime APIs forbidden"
    quote: "Cached functions and components cannot directly access runtime APIs like cookies(), headers(), or searchParams. Instead, read these values outside the cached scope and pass them as arguments."
  - upstream_id: nextjs-use-cache-directive
    section: "React.cache isolation"
    quote: "React.cache operates in an isolated scope inside use cache boundaries. Values stored via React.cache outside a use cache function are not visible inside it."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=high"
  reviewed_at: "2026-05-16"
  verdict: SPLIT (this rule is one half of the split — see server-cache-react.md for the other)
  agreements:
    - "Need a dedicated Next 16 'use cache' rule"
    - "Define boundary between this and React.cache() crisply"
    - "Vercel fetch-dedup claim still holds for dedup but caching semantics changed under Cache Components"
sibling_rules:
  - server-cache-react
---

## Use the 'use cache' directive for Next.js 16 Cache Components persistent caching

**Impact: HIGH — Provides persistent, cross-request, tag-revalidatable caching with compiler-generated cache keys. The Next.js 16 framework-recommended caching primitive for App Router. Replaces most use cases previously served by `React.cache()` in Next.js apps.**

### Enable

Cache Components is opt-in. Add to `next.config.ts`:

```ts
import type { NextConfig } from 'next'
const nextConfig: NextConfig = { cacheComponents: true }
export default nextConfig
```

`use cache` does not work without this flag set. (Version history: introduced experimental in v15.0.0; enabled with Cache Components in v16.0.0.)

### Apply at three levels

**File level — all exports cached:**

```tsx
'use cache'
export default async function Page() { /* ... */ }
```
> When used at file level, all function exports must be `async`.

**Component level:**

```tsx
export async function BlogPosts() {
  'use cache'
  const posts = await fetch('/api/posts').then((r) => r.json())
  return <ul>{posts.map((p) => <li key={p.id}>{p.title}</li>)}</ul>
}
```

**Function level:**

```tsx
export async function getProducts() {
  'use cache'
  return db.query('SELECT * FROM products')
}
```

### Cache key composition

The compiler-generated cache key includes:

1. **Build ID** — invalidates everything on build
2. **Function ID** — secure hash of function location + signature
3. **Serializable arguments** — props (component) or call arguments (function)
4. **HMR refresh hash** (dev only)
5. **Closed-over variables** are auto-captured and bound as arguments — they enter the key

```tsx
async function Component({ userId }: { userId: string }) {
  const getData = async (filter: string) => {
    'use cache'
    // userId (closure) + filter (arg) both in the key.
    return fetch(`/api/users/${userId}/data?filter=${filter}`)
  }
  return getData('active')
}
```

### Serialization constraints

| | Argument | Return value |
|---|---|---|
| Serialization system | RSC (more restrictive) | RCC (less restrictive) |
| Primitives | ✓ | ✓ |
| Plain objects, arrays, Dates, Maps, Sets, TypedArrays, ArrayBuffers | ✓ | ✓ |
| React elements | pass-through only | ✓ |
| Class instances | ✗ | ✗ |
| Functions | pass-through only | pass-through only |
| Symbols, WeakMaps, WeakSets, URL | ✗ | ✗ |

**Pass-through pattern**: accept `children` / Server Actions as props without reading them inside the cached body. Their value cannot affect the cache entry.

### Forbidden inside cached scopes

- `cookies()`
- `headers()`
- `searchParams`

Read these **outside** the cached scope and pass values as arguments. (If absolutely required, see `'use cache: private'` directive — out of pilot scope.)

### React.cache is isolated inside 'use cache'

```tsx
import { cache } from 'react'

const store = cache(() => ({ current: null as string | null }))

function Parent() {
  const shared = store()
  shared.current = 'set by parent'
  return <Child />
}

async function Child() {
  'use cache'
  const shared = store()
  return <div>{shared.current}</div>  // null — isolated scope
}
```

Use function arguments to pass data into `use cache`, not closed-over React.cache stores. See sibling rule `server-cache-react.md`.

### Revalidation

Default profile:

| stale (client) | revalidate (server) | expire |
|---|---|---|
| 5 min | 15 min | never |

Override with `cacheLife()` and tag with `cacheTag()`:

```tsx
import { cacheLife, cacheTag } from 'next/cache'

export async function getProducts() {
  'use cache'
  cacheLife('hours')
  cacheTag('products')
  return db.query('SELECT * FROM products')
}
```

Invalidate from a Server Action:

```tsx
'use server'
import { updateTag } from 'next/cache'

export async function publishProduct(input: FormData) {
  await db.products.insert(input)
  updateTag('products')   // invalidates all 'products'-tagged caches
}
```

### Runtime caching by deployment

| Environment | Behavior |
|---|---|
| Serverless | Cache entries typically don't persist across requests; each request may be a fresh instance |
| Self-hosted | Cache entries persist; control with `cacheMaxMemorySize` |
| `'use cache: remote'` | Platform-provided durable handler (Redis/KV) — extra latency, platform fees |

### Build hang anti-pattern

If your build hangs ~50s, you are passing a runtime-data Promise (cookies / headers / dynamic fetch) into a cached scope. Awaiting it during prerender deadlocks. Fix: await runtime data **outside** the cached function, then pass the resolved value as an argument.

### Platform support

| Target | Supported |
|---|---|
| Node.js server | Yes |
| Docker | Yes |
| Static export | **No** |
| Adapters | Platform-specific |

### Verification

- Manual review until ESLint rule ships. Detectable patterns: `'use cache'` directive presence, `cacheComponents: true` in config, no `cookies()`/`headers()`/`searchParams` direct access inside cached scope.
- Integration verification: build the project with `next build` and observe the "static shell" output; `use cache` boundaries should produce prerendered HTML chunks. Cache-Components-aware framework already enforces several of these constraints with a build-time error.

Sources for this rule:

- [Next.js 16 — 'use cache' directive](https://nextjs.org/docs/app/api-reference/directives/use-cache)
- [Next.js 16 — Caching guide](https://nextjs.org/docs/app/getting-started/caching)
- [React 19 — cache() (for isolation context)](https://react.dev/reference/react/cache)


<!-- @source rules/no-hardcoded-user-facing-string-in-l4.md -->

---
title: "User-facing strings in L4 templates must use t() — no hardcoded Korean or natural-language literals"
rule_id: no-hardcoded-user-facing-string-in-l4
impact: HIGH
impactDescription: "Hardcoded natural-language strings in L4 templates break i18n: the app cannot switch between ko-KR and en-US, and all Korean text appears in English-locale builds."
tags:
  - i18n
  - locale
  - korean
  - l4-template
  - l2-block
applicable_to:
  - react
  - nextjs
provenance_class: internal_design
applies_to: paths_created_after_2026-05-18
excludes:
  - templates/L4/auth/**
  - templates/L4/crud/**
  - templates/L4/payment/**
  - templates/L4/practices/**
  - templates/L4/notification/**
  - templates/L4/audit-log/**
  - templates/L4/file-storage/**
  - templates/L4/search/**
protects_template_id: templates/L2/blocks/translation-boundary.tsx
failing_fixture_path: practices-react/evals/fixtures/no_hardcoded_i18n/fail_korean_literal/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-I18N-001"
verification:
  type: regex_scan
  pattern: "Korean Unicode range \\u3131-\\u3163 \\uAC00-\\uD7A3 in JSX outside t() wrapper"
  status: fixture_driven
  notes: |
    Fixture _run.sh implements the check via a Python regex scan.
    Pass fixture: uses t('key') — exits 0.
    Fail fixture: contains <button>결제하기</button> — exits 1.
    Existing-l4-must-skip: rule excludes pre-2026-05-18 L4 paths — exits 0.
evidence:
  - source_type: upstream_id
    upstream_id: next-intl-2026-05
    section: "useTranslations"
    quote: "export function PayButton() { const t = useTranslations('Payment'); return <button>{t('submit')}</button>; }"
  - source_type: external
    citation: "next-intl docs — using t() for all user-visible text to enable locale switching"
    url: "https://next-intl.dev/docs/usage/messages"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Unicode — Hangul syllable block U+AC00 to U+D7A3; Hangul jamo U+3131 to U+3163"
    url: "https://unicode.org/charts/PDF/UAC00.pdf"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
next_review_by: "2026-11-18"
---

## User-facing strings in L4 templates must use `t()` — no hardcoded Korean or natural-language literals

**Impact: HIGH — Hardcoded Korean (한글) string literals in JSX break the i18n contract: the application cannot switch to English locale, and templates become non-reusable for non-Korean enterprise deployments.**

**Scope (Option β):** This rule applies **only to files created on or after 2026-05-18**. Existing L4 domains (auth, crud, payment, practices, notification, audit-log, file-storage, search) are explicitly excluded — their string migration is deferred to a future P1 sprint.

### The violation — hardcoded Korean literal in JSX

```tsx
// ❌ WRONG — hardcoded 한글 literal; breaks ko-KR ↔ en-US switching
export default function PaymentPage() {
  return (
    <div>
      <h1>결제</h1>
      <button>결제하기</button>     {/* ← hardcoded Korean, not t() */}
      <p>금액을 입력해 주세요.</p>  {/* ← hardcoded Korean */}
    </div>
  )
}
```

### Correct — all user-facing text via `t()`

```tsx
// ✅ CORRECT — locale-aware strings via next-intl t()
'use client'
import { useTranslations } from 'next-intl'

export default function PaymentPage() {
  const t = useTranslations('Payment')
  return (
    <div>
      <h1>{t('title')}</h1>
      <button>{t('submit')}</button>
      <p>{t('amountPrompt')}</p>
    </div>
  )
}
```

Corresponding message file (`messages/ko.json`):
```json
{
  "Payment": {
    "title": "결제",
    "submit": "결제하기",
    "amountPrompt": "금액을 입력해 주세요."
  }
}
```

English translation (`messages/en.json`):
```json
{
  "Payment": {
    "title": "Payment",
    "submit": "Pay Now",
    "amountPrompt": "Please enter the amount."
  }
}
```

### Detect the violation

Pattern: Korean Unicode characters (`ㄱ–ㅣ` jamo, `가–힣` syllables) appearing in JSX string literals **outside** a `t()` function call.

The `_run.sh` fixture script implements this as a Python regex scan:
- Regex: `[ㄱ-ㅣ가-힣]` in `.tsx`/`.jsx` files
- Exclusion: if the Korean text appears as an argument to `t(` (i.e., inside `t('...')` or `t("...")`) it is permitted
- Exclusion: pre-2026-05-18 L4 domains are skipped entirely

### Why this rule exists

Korean enterprise forks of ax-template must support at minimum two locales: `ko-KR` (default) and `en-US`. Hardcoded Korean strings in new L4 domains:
1. Break the locale switch — switching to English still renders Korean text
2. Create template coupling — templates become Korea-only instead of fork-adaptable
3. Fail the composition-kit promise — a US fork of the template cannot replace strings without modifying component code

The `TranslationBoundary` L2 block (see `templates/L2/blocks/translation-boundary.tsx`) wraps subtrees that depend on translations and provides a graceful fallback when messages fail to load.

See also: `blueprints/i18n-policy-manifest.yaml` for the full locale policy including KRW formatting rules.


<!-- @source rules/no-l4-cross-import.md -->

---
title: "L4 domain pages must not import from other L4 domains"
rule_id: no-l4-cross-import
impact: HIGH
impactDescription: "Cross-importing between L4 domains creates tight coupling, makes domains non-independently deployable, and creates circular dependency risks that break tree-shaking and code-splitting"
tags:
  - l4-layer
  - domain-isolation
  - imports
  - architecture
applicable_to:
  - nextjs
  - react
provenance_class: internal_design
protects_template_id: templates/L4/
failing_fixture_path: practices/evals/fixtures/no-l4-cross-import/fail_cross_import/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ADVANCED-001"
verification:
  type: review
  status: manual
  notes: "No file under templates/L4/<domain-A>/ may import from templates/L4/<domain-B>/. Shared cross-cutting concerns (auth state, user context) must be sourced from shared hooks (hooks/), context providers (providers/), or L1/L2 components — never from another L4 domain."
evidence:
  - upstream_id: nextjs-app-router-16
    section: "App Router — route segments as independent modules"
    quote: "route segments"
  - source_type: external
    citation: "Next.js documentation — Domain-driven architecture: each feature domain should be self-contained with no cross-domain imports at the route layer"
    url: "https://nextjs.org/docs/app/building-your-application/routing/colocation"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Vercel best practices — Vertical slice architecture: L4 domains are independent vertical slices; cross-slice imports create coupling that breaks hot reloading and incremental static regeneration"
    url: "https://vercel.com/blog/how-we-optimized-package-imports-in-next-js"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## L4 domain pages must not import from other L4 domains

**Impact: HIGH — L4 domains are independent vertical slices. Cross-domain imports couple their deployment, break tree-shaking between route segments, and create circular dependency risks as each domain grows.**

L4 is the feature layer — auth, payment, notification, file-storage, crud are all L4 domains. Each domain owns its pages, server actions, and domain-specific components. Cross-importing means that changing domain A's internals can silently break domain B, and that both domains must be bundled together even when only one changes.

### The violation — L4 payment importing from L4 auth

```typescript
// ❌ WRONG — templates/L4/payment/PaymentPage.tsx imports from L4/auth
"use client";
// VIOLATION: importing auth domain's store and components directly
import { useAuthStore } from "templates/L4/auth/store/authStore";
import { AuthGuard } from "templates/L4/auth/components/AuthGuard";

export default function PaymentPage() {
  const { user } = useAuthStore(); // couples payment bundle to auth bundle
  return <AuthGuard><div>Pay for {user?.name}</div></AuthGuard>;
}
```

### Correct — shared hooks for cross-cutting concerns

```typescript
// ✅ CORRECT — payment uses shared hooks, not L4/auth internals
"use client";
// Shared hooks in hooks/ are the contract layer between L4 domains
import { useCurrentUser } from "hooks/useCurrentUser";
import { useRequireAuth } from "hooks/useRequireAuth";

export default function PaymentPage() {
  const user = useCurrentUser();   // shared contract — no auth bundle coupling
  useRequireAuth();                 // redirects if not authenticated

  return <div>Pay for {user?.name}</div>;
}
```

### Allowed import directions from L4

| Source (inside L4/domain) | Target | Allowed? |
|---|---|---|
| `templates/L4/payment/` | `templates/L1/components/` | ✅ |
| `templates/L4/payment/` | `templates/L2/blocks/` | ✅ |
| `templates/L4/payment/` | `templates/L3/pages/` | ✅ |
| `templates/L4/payment/` | `hooks/`, `providers/`, `lib/` | ✅ |
| `templates/L4/payment/` | `templates/L4/auth/` | ❌ violation |
| `templates/L4/payment/` | `templates/L4/notification/` | ❌ violation |

### Why this rule exists

During SP8-SP11 (L4 domain implementation) all cross-cutting concerns (auth state, current user, toast queue, error boundary) were moved to `hooks/` and `providers/`. Any L4 domain that directly imports from another L4 domain is bypassing this shared layer and re-coupling.

Reference: [Next.js App Router — route colocation](https://nextjs.org/docs/app/building-your-application/routing/colocation)

Reference: [Failing fixture: practices/evals/fixtures/no-l4-cross-import/fail_cross_import/PaymentPage.tsx](practices/evals/fixtures/no-l4-cross-import/fail_cross_import/PaymentPage.tsx)


<!-- @source rules/no-rrn-in-form-fields.md -->

---
title: "Frontend forms must not include RRN (주민등록번호) input fields by default"
rule_id: no-rrn-in-form-fields
impact: CRITICAL
impactDescription: "RRN is Sensitive Personal Information under 개인정보보호법 §24; collecting it through a standard form field without explicit legal basis and consent gate is a compliance violation"
tags:
  - privacy
  - pii
  - rrn
  - forms
  - locked_constraint
  - korean-compliance
applicable_to:
  - react
  - nextjs
provenance_class: locked_constraint
protects_template_id: templates/L2/blocks/
failing_fixture_path: practices/evals/fixtures/no-rrn-in-form-fields/fail_rrn_field/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-CLIENT-004"
verification:
  type: review
  status: manual
  notes: "Static check: grep -r 'name=\"rrn\"\\|name=\"주민\\|id=\"rrn\"' templates/L2/ templates/L4/ must return zero matches. If identity verification is required, it must be in a dedicated KYC component with explicit legal-basis display and PII-handling review."
evidence:
  - source_type: external
    citation: "개인정보보호법 제24조 — 고유식별정보의 처리 제한: RRN (주민등록번호) is a unique identification number; its collection requires explicit legal basis, separate consent, and technical/administrative safeguards"
    url: "https://www.law.go.kr/법령/개인정보보호법"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "KISA 개인정보보호법 가이드라인 — 주민등록번호 처리: 기업은 법령에 특별한 규정이 있는 경우가 아닌 한 주민등록번호를 처리할 수 없음"
    url: "https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "행정안전부 주민등록법 제7조의5 — 주민등록번호의 사용 제한: 정보통신서비스 제공자는 원칙적으로 주민등록번호를 수집·이용할 수 없음"
    url: "https://www.law.go.kr/법령/주민등록법"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Frontend forms must not include RRN (주민등록번호) input fields by default

**Impact: CRITICAL — 개인정보보호법 §24 classifies the Resident Registration Number as a unique identification information (고유식별정보). Collecting it through a standard form field without explicit legal basis and a dedicated consent gate is a compliance violation carrying administrative penalties up to ₩30M per violation.**

This rule is a **locked constraint** derived from statute. It cannot be relaxed by project-level override.

### The violation — standard form with RRN field

```tsx
// ❌ WRONG — form with name="rrn" input field
export default function RegistrationForm() {
  return (
    <form>
      <input name="name" ... />
      <input name="email" type="email" ... />
      {/* VIOLATION: RRN collected as a standard form field */}
      <input
        name="rrn"
        id="rrn"
        placeholder="000000-0000000"
        type="text"
      />
      <button type="submit">Register</button>
    </form>
  );
}
```

### Correct — registration form without RRN field

```tsx
// ✅ CORRECT — collect only minimum required information
export default function RegistrationForm() {
  return (
    <form>
      <input name="name" ... />
      <input name="email" type="email" ... />
      {/* CORRECT: no RRN field
          If identity verification is later required, use <KycVerificationModal/>
          which includes: legal-basis disclosure + separate consent + audit trail */}
      <button type="submit">Register</button>
    </form>
  );
}
```

### If identity verification is required

Use the dedicated `<KycVerificationModal>` component with mandatory legal-basis display:

```tsx
// ✅ CORRECT — KYC flow with explicit consent gate
import { KycVerificationModal } from "templates/L2/blocks/kyc-verification-modal";

export default function OnboardingPage() {
  const [kycOpen, setKycOpen] = useState(false);
  return (
    <div>
      <button onClick={() => setKycOpen(true)}>Verify Identity</button>
      <KycVerificationModal
        open={kycOpen}
        legalBasis="본인 확인을 위해 주민등록번호 뒷자리를 수집합니다 (개인정보보호법 §24)"
        onVerified={(result) => { /* result contains only a verification token, not RRN */ }}
        onClose={() => setKycOpen(false)}
      />
    </div>
  );
}
```

### Why this rule exists

개인정보보호법 §24 and 주민등록법 §7의5 impose strict restrictions:
1. **Collection prohibition** — The RRN may not be collected without specific legal authorization (주민등록법 §7의5).
2. **Consent requirement** — A separate, explicit consent gate is required (개인정보보호법 §18).
3. **Encryption requirement** — If collected, must be stored encrypted (개인정보보호법 §29).
4. **Penalties** — Unauthorized collection triggers mandatory breach notification and fines up to ₩30M per violation.

Standard form fields do not satisfy any of these requirements. A `<KycVerificationModal>` component with legal-basis disclosure and audit logging is the only acceptable collection path.

Frontend companion to: `no-rrn-logging.md` in `practices/rules/`.

Reference: [개인정보보호법 제24조 — 고유식별정보의 처리 제한](https://www.law.go.kr/법령/개인정보보호법)

Reference: [KISA 개인정보보호법 가이드라인 — 주민등록번호 처리](https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO)


<!-- @source rules/prefer-feature-gate-over-env-check.md -->

---
title: "Feature flag checks must use FeatureGate or the feature-flags API — not process.env"
rule_id: prefer-feature-gate-over-env-check
impact: HIGH
impactDescription: "Direct process.env checks for feature flags bypass the runtime admin UI, require redeployment to toggle, and cannot be dynamically controlled without rebuilding the app."
tags:
  - feature-flags
  - runtime-control
  - process-env
  - l4-template
  - l2-block
applicable_to:
  - react
  - nextjs
provenance_class: internal_design
applies_to: paths_created_after_2026-05-18
protects_template_id: templates/L2/blocks/feature-gate.tsx
failing_fixture_path: practices-react/evals/fixtures/feature_gate/fail_process_env_check/
spec_ref: "specs/feature-flags-frontend-l0.yaml#FF-FE-004"
verification:
  type: regex_scan
  pattern: "process\\.env\\.(NEXT_PUBLIC_)?FEATURE_|process\\.env\\.(NEXT_PUBLIC_)?FF_"
  status: fixture_driven
  notes: |
    Fixture _run.sh implements the check via a Python regex scan.
    Pass fixture: uses FeatureGate component — exits 0.
    Fail fixture: uses process.env.NEXT_PUBLIC_FEATURE_NEW_CHECKOUT — exits 1.
evidence:
  - source_type: external
    citation: "Next.js Docs — Environment variables and the limitation of build-time NEXT_PUBLIC_ variables (cannot be changed at runtime without rebuild)"
    url: "https://nextjs.org/docs/app/building-your-application/configuring/environment-variables"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Martin Fowler — Feature Toggles (aka Feature Flags): Release toggles should be dynamic and externally managed, not baked into the build artifact"
    url: "https://martinfowler.com/articles/feature-toggles.html"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
next_review_by: "2026-11-18"
---

## Feature flag checks must use `FeatureGate` or the feature-flags API — not `process.env`

**Impact: HIGH — `process.env` feature flags require a full rebuild + redeployment to change. Runtime feature flag control via the admin API allows instant toggling without downtime.**

**Scope:** This rule applies **only to files created on or after 2026-05-18**. The feature-flags domain L4 templates are the canonical implementation.

### The violation — `process.env` for feature flag control

```tsx
// ❌ WRONG — build-time constant; cannot toggle without redeployment
const isNewCheckoutEnabled = process.env.NEXT_PUBLIC_FEATURE_NEW_CHECKOUT === 'true'

export default function CheckoutPage() {
  if (!isNewCheckoutEnabled) return <LegacyCheckout />
  return <NewCheckout />
}
```

### Correct — use `FeatureGate` (client-side) or middleware (server-side)

**Client-side gate:**
```tsx
// ✅ CORRECT — runtime-controlled via admin API; no rebuild needed
import { FeatureGate } from '@/templates/L2/blocks/feature-gate'

export default function CheckoutPage() {
  return (
    <FeatureGate name="new-checkout" fallback={<LegacyCheckout />}>
      <NewCheckout />
    </FeatureGate>
  )
}
```

**Server-side gate (middleware):**
```ts
// ✅ CORRECT — evaluated at request time in Next.js middleware
// templates/L4/feature-flags/middleware.ts
const FLAGGED_ROUTES: Record<string, string> = {
  '/new-checkout': 'new-checkout',
}
```

### Why this rule exists

| | `process.env` | FeatureGate / API |
|--|--|--|
| Toggle without rebuild | ❌ No | ✅ Yes |
| Admin UI control | ❌ No | ✅ Yes |
| Fail-closed on unknown flag | ❌ No (depends on default) | ✅ Yes |
| Runtime observability | ❌ No | ✅ Yes |
| Emergency kill-switch | ❌ Slow (redeploy) | ✅ Instant |

The `FeatureGate` L2 block (see `templates/L2/blocks/feature-gate.tsx`) fetches
`GET /api/v1/feature-flags/{name}/active` at render time. The result is cached
in the backend (Caffeine 30s TTL) so evaluation is fast and consistent.

See `blueprints/feature-flags-manifest.yaml` for the full feature-flags domain policy.

### Detect the violation

Pattern: `process.env.NEXT_PUBLIC_FEATURE_*` or `process.env.NEXT_PUBLIC_FF_*` in `.tsx`/`.jsx` files.

The `_run.sh` fixture script in `practices-react/evals/fixtures/feature_gate/` implements this as a Python regex scan.


<!-- @source rules/rendering-activity.md -->

---
title: Use Activity (React 19.2+) for expensive UI that toggles visibility frequently — be aware hidden mode unmounts effects
impact: MEDIUM
impactDescription: "Preserves DOM and state for hidden components while deferring their updates. Effects are unmounted on hide and remounted on show — design subscriptions accordingly."
tags: [rendering, activity, visibility, state-preservation, react-19]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-007"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) component is on React 19.2+, (b) hidden mode acceptable for component's effects (they will unmount/remount), (c) used for genuinely-toggling UI (tabs/drawers/panels), (d) hidden updates being deferred is acceptable."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Activity stable in React 19.2 (released Oct 2025)."
  completeness:
    status: complete
    amendments:
      - "Hidden mode unmounts effects (Vercel rule didn't say this)"
      - "Hidden updates are deferred"
      - "Use for tabs/drawers/panels, not for hidden subscriptions that must keep running"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-activity"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-activity.md"
    role: seed
  - id: react-19-activity
    title: "React 19.2 — <Activity>"
    url: "https://react.dev/reference/react/Activity"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-activity"
    quote: "Use React's <Activity> to preserve state/DOM for expensive components that frequently toggle visibility."
  - source_type: external
    citation: "React 19.2 — Activity supports 'visible' and 'hidden' modes; hidden hides children, unmounts effects, and defers all updates until React has nothing left to work on"
    url: "https://react.dev/reference/react/Activity"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules: []
---

## Use Activity (React 19.2+) for expensive UI that toggles visibility frequently — hidden mode unmounts effects

**Impact: MEDIUM — Preserves DOM and state. Hidden mode unmounts effects and defers updates.**

### Mode semantics (per React 19.2 docs)

- **`visible`**: children render, effects mount, updates process normally.
- **`hidden`**: children stay in DOM with state preserved, effects **unmount**, updates **defer** until React is idle.

The state-and-DOM preservation is what `display: none` can't give you (which loses focus/scroll position/uncontrolled inputs). Activity is a React-aware version.

### Correct — tabs / drawers / panels that toggle often

```tsx
import { Activity } from 'react'

function Sidebar({ open }: { open: boolean }) {
  return (
    <Activity mode={open ? 'visible' : 'hidden'}>
      <ExpensiveSidebarContent />
    </Activity>
  )
}
```

On every close-and-reopen: state in `ExpensiveSidebarContent` is preserved, DOM stays mounted, effects re-run.

### Incorrect — using Activity for components whose effects MUST keep running

```tsx
// BAD: hidden mode unmounts useEffect — the subscription tears down when sidebar closes.
function NotificationsSidebar({ open }: { open: boolean }) {
  return (
    <Activity mode={open ? 'visible' : 'hidden'}>
      <NotificationsListener />   {/* subscribes to a websocket */}
    </Activity>
  )
}
```

If the subscription must keep running when the panel is hidden, don't use Activity. Lift the subscription to a parent, or use `display: none` (loses some benefits) or keep the component visible behind a CSS hide.

### Use cases

Good fits:
- Tabs with expensive inner state (form drafts, scrolled lists)
- Drawers/panels users toggle 10× per session
- Multi-step wizards where the user can navigate back
- Modals with heavy inner content that should re-open instantly

Bad fits:
- Components whose effects need to run while hidden (subscriptions, timers, periodic polls)
- Components where the hidden update being deferred is unacceptable

### Notes

- Doesn't replace virtualization. Doesn't unload the DOM.
- The React team flags more modes as future work — current API has just `visible`/`hidden`.

Sources:
- [Vercel: rendering-activity](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-activity.md)
- [React 19.2 — Activity](https://react.dev/reference/react/Activity)


<!-- @source rules/rendering-animate-svg-wrapper.md -->

---
title: For whole-SVG transform/opacity animations, animate a wrapper div instead of the <svg> element
impact: LOW
impactDescription: "Many browser compositors handle div transforms better than SVG transforms. Applies only to whole-asset animations (rotate the whole icon). Internal path/shape animation still belongs on SVG elements."
tags: [rendering, svg, css, animation, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-001"
verification:
  type: review
  status: manual
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Scoped to whole-SVG transform/opacity"
      - "Excluded internal path/shape animation"
      - "Added will-change during active animation only"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-animate-svg-wrapper"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-animate-svg-wrapper.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-animate-svg-wrapper"
    quote: "Wrap SVG in a <div> and animate the wrapper instead."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules: []
---

## For whole-SVG transform/opacity animations, animate a wrapper div instead of the <svg> element

**Impact: LOW — Many browser compositors handle `div` transforms better than `<svg>` transforms. Applies only to whole-asset animations (rotate the whole icon). Internal path/shape animation still belongs on SVG elements.**

### Scope

This rule applies when you're animating the **entire SVG as a unit** with CSS `transform` / `opacity` / `translate` / `rotate` / `scale`. It does NOT apply to:
- Animating individual `<path>`, `<circle>`, `<rect>` etc. within the SVG.
- SMIL animation (`<animate>`, `<animateTransform>`).
- CSS animations targeting inner SVG attributes.

### Correct — wrapper-level transform

```tsx
function Spinner() {
  return (
    <div className="animate-spin">
      <svg viewBox="0 0 24 24" width={24} height={24}>
        <circle cx="12" cy="12" r="10" stroke="currentColor" />
      </svg>
    </div>
  )
}
```

### Incorrect — transform on the <svg>

```tsx
function Spinner() {
  return (
    <svg className="animate-spin" viewBox="0 0 24 24" width={24} height={24}>
      <circle cx="12" cy="12" r="10" stroke="currentColor" />
    </svg>
  )
}
```

### `will-change` only during active animation

```css
.animate-spin {
  animation: spin 1s linear infinite;
  will-change: transform;   /* tell browser to promote layer */
}

/* Remove will-change when animation ends — see CSS spec recommendation */
.animate-spin.idle {
  will-change: auto;
}
```

`will-change` is not free — it allocates a compositor layer. Apply only while animating; remove after.

Sources:
- [Vercel: rendering-animate-svg-wrapper](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-animate-svg-wrapper.md)


<!-- @source rules/rendering-conditional-render.md -->

---
title: For numeric/falsy-tricky conditions use ternary or explicit boolean cast; `&&` is fine for real booleans
impact: LOW
impactDescription: "`0 && <X />` renders the literal '0'. Use `count > 0 ? <X /> : null` or `Boolean(count) && <X />`. Don't ban `&&` outright."
tags: [rendering, conditional, jsx, falsy-values, correctness]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-008"
verification:
  type: lint
  rule_id: "ax/no-falsy-numeric-render"
  status: shipped
  notes: "Custom ESLint rule planned: flag `numeric && <JSX>` patterns; safe `boolean && <JSX>` left alone."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Don't ban && for real booleans — only for numeric/falsy-tricky conditions"
      - "Acceptable: Boolean(x) && <JSX>"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-conditional-render"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-conditional-render.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-conditional-render"
    quote: "Use explicit ternary operators (? :) instead of && for conditional rendering when the condition can be 0, NaN, or other falsy values that render."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules: []
---

## For numeric/falsy-tricky conditions use ternary or explicit boolean cast; `&&` is fine for real booleans

**Impact: LOW — `0 && <X />` renders the literal "0". `NaN && <X />` renders "NaN". `'' && <X />` renders the empty string (visible nothing, but a stray text node). Use ternary or explicit boolean cast for these.**

### Incorrect — numeric condition

```tsx
function Badge({ count }: { count: number }) {
  return <div>{count && <span className="badge">{count}</span>}</div>
}
// count = 0 → renders <div>0</div>
```

### Correct — ternary

```tsx
function Badge({ count }: { count: number }) {
  return <div>{count > 0 ? <span className="badge">{count}</span> : null}</div>
}
```

### Correct — explicit boolean cast

```tsx
function Badge({ count }: { count: number }) {
  return <div>{Boolean(count) && <span className="badge">{count}</span>}</div>
}
```

### Don't ban `&&` for real booleans

`&&` is perfectly safe (and idiomatic) when the left side is a real boolean:

```tsx
// PERFECTLY FINE
function Toolbar({ canEdit }: { canEdit: boolean }) {
  return <div>{canEdit && <EditButton />}</div>
}

function List({ items }: { items: Item[] }) {
  return <ul>{items.length > 0 && items.map(...)}</ul>
}
```

### Rule of thumb

- Left side is a primitive `boolean` (literal, comparison result, ! coerced) → `&&` is safe.
- Left side might be `number`, `string`, `NaN`, `null`, `undefined`, or a value of unknown type → use ternary or `Boolean(...)`.

### TypeScript helps

A strict-typed boolean prop won't have the numeric trap. The trap shows up most often with:

```tsx
{user.unreadCount && <Dot />}             // number
{search.results.length && <Heading />}     // number
{maybeArray.length && maybeArray.map(...)} // number
```

All three are fixed by either `> 0` comparison or `Boolean()` cast.

Sources:
- [Vercel: rendering-conditional-render](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-conditional-render.md)


<!-- @source rules/rendering-content-visibility.md -->

---
title: Use content-visibility for long static sections, paired with realistic contain-intrinsic-size
impact: HIGH
impactDescription: "Skips layout/paint for off-screen sections. NOT a list virtualization replacement — DOM nodes still exist and consume memory/event budget."
tags: [rendering, css, content-visibility, long-lists]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-002"
verification:
  type: review
  status: manual
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Distinguished from list virtualization (windowing libraries)"
      - "Required realistic contain-intrinsic-size"
      - "Noted DOM/memory/event costs unchanged"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-content-visibility"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-content-visibility.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-content-visibility"
    quote: "Apply content-visibility: auto to defer off-screen rendering."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules: []
---

## Use content-visibility for long static sections, paired with realistic contain-intrinsic-size

**Impact: HIGH — Skips layout/paint for off-screen sections. NOT a list virtualization replacement.**

### Correct

```css
.message-item {
  content-visibility: auto;
  contain-intrinsic-size: 0 80px;   /* approximate height when not yet rendered */
}
```

```tsx
function MessageList({ messages }: { messages: Message[] }) {
  return (
    <div className="overflow-y-auto h-screen">
      {messages.map((m) => (
        <div key={m.id} className="message-item">
          <Avatar user={m.author} />
          <div>{m.content}</div>
        </div>
      ))}
    </div>
  )
}
```

Browser skips paint/layout for off-screen `.message-item` elements. Visible ones render normally.

### Realistic `contain-intrinsic-size` is required

Without `contain-intrinsic-size`, the browser collapses off-screen elements to zero, causing the scrollbar to misrepresent total content height. Set an approximate height matching the rendered item. Wrong sizes cause scroll-jumps when items resolve.

### NOT a virtualization replacement

`content-visibility` keeps all DOM nodes in the tree:
- Memory cost: full DOM tree retained.
- Event budget: scroll/resize handlers see all elements.
- Selectors/queries: `document.querySelectorAll` returns all.

For truly huge lists (tens of thousands of items), use a virtualization library (TanStack Virtual, react-window) that mounts only visible items.

`content-visibility` shines for medium lists (hundreds) where virtualization adds complexity disproportionate to gain.

### Browser support

Chrome 85+ (2020), Firefox 125+ (2024), Safari 18+ (2024). For older Safari, treat as progressive enhancement.

Sources:
- [Vercel: rendering-content-visibility](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-content-visibility.md)


<!-- @source rules/rendering-hoist-jsx.md -->

---
title: Compiler hoists static JSX automatically — manual hoist only for compiler-off projects or generated blobs
impact: LOW
impactDescription: "Manual hoist of static JSX out of components avoids re-creation per render. React Compiler does this automatically in React 19+ projects."
tags: [rendering, jsx, static, optimization, react-compiler]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-003"
verification:
  type: review
  status: manual
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "React Compiler GA makes manual hoist unnecessary in compiler-enabled projects."
  completeness:
    status: complete
    amendments:
      - "Lead with compiler-first framing"
      - "Forbid hoisting JSX that depends on props/state/context/hooks/locale/auth/theme"
      - "Narrowed to compiler-off projects or genuinely static generated blobs"
  gap_check:
    status: complete
    note: "Strong overlap with rerender-memo. Cross-link."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-hoist-jsx"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-hoist-jsx.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-hoist-jsx"
    quote: "Extract static JSX outside components to avoid re-creation."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - rerender-memo
---

## Compiler hoists static JSX automatically — manual hoist only for compiler-off projects or generated blobs

**Impact: LOW — React Compiler (GA in React 19) auto-hoists. Manual hoist is a fallback.**

### Decision order

1. **React Compiler enabled?** Stop. The compiler handles this. Manual hoist adds noise.
2. **Compiler off + JSX is genuinely static** (no props, state, context, hooks, locale, auth, theme dependencies)? Manual hoist is acceptable.

### Forbidden hoists (always wrong)

JSX that closes over any of these MUST stay inside the component:

- Props
- State
- Context values
- Hook results
- Locale / i18n
- Theme / dark-mode
- Auth state
- Anything else that changes between renders

Hoisting such JSX freezes the value at module-load time → wrong content forever.

### Correct (compiler off + static blob)

```tsx
const STATIC_SKELETON = (
  <div className="animate-pulse h-20 bg-gray-200" />
)

function Container({ loading }: { loading: boolean }) {
  return <div>{loading && STATIC_SKELETON}</div>
}
```

### Incorrect (compiler off + depends on theme)

```tsx
// BAD: theme changes; hoisted JSX is frozen to whatever theme was at module load.
const FROZEN = (
  <div className={isDarkMode() ? 'dark' : 'light'}>Hello</div>
)
```

### Incorrect (compiler on + manual hoist)

```tsx
// Redundant noise. The compiler already does this.
const SKELETON = <div className="..." />
function X() { return loading ? SKELETON : null }
```

### Especially useful (compiler off) for large inline SVGs

Inline `<svg>` with many children can be expensive to recreate. If the SVG is static, hoisting is meaningful.

Sources:
- [Vercel: rendering-hoist-jsx](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-hoist-jsx.md)
- [React Compiler](https://react.dev/learn/react-compiler)


<!-- @source rules/rendering-hydration-no-flicker.md -->

---
title: Inline-script prehydration for deterministic boot values (theme/auth-shell) — never for fetched data or user-controlled values; honor CSP
impact: MEDIUM
impactDescription: "Eliminates flicker for client-only boot values (theme class, color scheme) without breaking SSR. Narrow scope: deterministic, tiny, non-user-controlled values only."
tags: [rendering, ssr, hydration, localStorage, flicker, csp]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-005"
verification:
  type: review
  status: manual
  notes: "Reviewer enforces: (a) script accesses ONLY deterministic boot keys (theme/color-scheme), (b) values are escaped or strictly typed, (c) CSP nonce on the script, (d) no fetch / heavy logic / untrusted data inside, (e) framework primitive used if available."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Narrowed scope to deterministic boot values"
      - "Added XSS / CSP nonce requirement"
      - "Forbade fetch and untrusted-data interpolation inside"
      - "Recommended framework primitives where available"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-hydration-no-flicker"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-hydration-no-flicker.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-hydration-no-flicker"
    quote: "Inject a synchronous script that updates the DOM before React hydrates."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - rendering-hydration-suppress-warning
  - client-localstorage-schema
---

## Inline-script prehydration for deterministic boot values — never for fetched data or user-controlled values

**Impact: MEDIUM — Narrow tool. Use for theme/color-scheme/known-auth-shell ONLY.**

### Correct — theme class prehydration

```tsx
function ThemeWrapper({ children, nonce }: { children: ReactNode; nonce: string }) {
  return (
    <>
      <div id="theme-wrapper">{children}</div>
      <script
        nonce={nonce}
        dangerouslySetInnerHTML={{
          __html: `
            (function() {
              try {
                var t = localStorage.getItem('theme:v1');
                var el = document.getElementById('theme-wrapper');
                if (el && (t === 'dark' || t === 'light')) el.className = t;
              } catch (e) {}
            })();
          `,
        }}
      />
    </>
  )
}
```

Notes:
- Whitelist allowed values inline (`'dark' || 'light'`) — never inject user-controlled strings.
- CSP nonce attached.
- Script body is small and deterministic.

### Forbidden inside the inline script

- **Untrusted/user-controlled data interpolation.** Any value coming from a cookie/header/storage MUST be parsed and validated against an allowlist before use. Don't string-interpolate.
- **`fetch()` / `XMLHttpRequest` / `import()`.** Boot scripts must be synchronous and deterministic.
- **Heavy logic.** Run as little code as possible.
- **Errors swallowed silently** — the script does need a try/catch but log the error in dev.

### CSP requirement

If your CSP includes `script-src 'self'` without `'unsafe-inline'`, you must attach a per-request nonce. React's `nonce` prop on `<script>` propagates to the rendered tag.

```typescript
// In a Server Component or middleware
const nonce = randomBytes(16).toString('base64')
// Headers().set('Content-Security-Policy', `script-src 'nonce-${nonce}' ...`)
```

### Framework primitives — prefer when available

- Next.js: `next-themes` library handles theme prehydration safely.
- Some UI frameworks ship their own prehydration helpers.
- Use them. Less to audit, less to misconfigure.

### Anti-pattern — flicker

```tsx
function ThemeWrapper({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState('light')
  useEffect(() => {
    const stored = localStorage.getItem('theme')
    if (stored) setTheme(stored)  // visible flash from light → stored
  }, [])
  return <div className={theme}>{children}</div>
}
```

### Anti-pattern — SSR break

```tsx
function ThemeWrapper({ children }: { children: ReactNode }) {
  const theme = localStorage.getItem('theme')   // throws on server
  return <div className={theme || 'light'}>{children}</div>
}
```

Sources:
- [Vercel: rendering-hydration-no-flicker](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-hydration-no-flicker.md)


<!-- @source rules/rendering-hydration-suppress-warning.md -->

---
title: suppressHydrationWarning on the smallest element with intentional server/client text mismatch
impact: LOW-MEDIUM
impactDescription: "Silences noise for known unavoidable text differences (timestamps, locale formatting, randomized ids). Must not mask structural mismatches — those are real bugs."
tags: [rendering, hydration, ssr, nextjs]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-006"
verification:
  type: review
  status: manual
  notes: "Reviewer rejects suppressHydrationWarning if (a) applied to a parent element wrapping more than text, (b) used to mask a structural mismatch, (c) used when deterministic SSR or client-only render is a viable alternative."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Apply only to the smallest element holding the mismatched text"
      - "Listed acceptable cases (timestamps, locale, randomized IDs)"
      - "Forbade masking structural mismatches"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-hydration-suppress-warning"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-hydration-suppress-warning.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-hydration-suppress-warning"
    quote: "Wrap the dynamic text in an element with suppressHydrationWarning to prevent noisy warnings."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - rendering-hydration-no-flicker
---

## suppressHydrationWarning on the smallest element with intentional server/client text mismatch

**Impact: LOW-MEDIUM — Narrow escape hatch for known-unavoidable text differences.**

### Acceptable cases

- Timestamps formatted with the user's local time zone
- Locale-dependent number/currency formatting
- Randomized IDs generated independently on server/client
- Theme class derived from a prehydration script (see `rendering-hydration-no-flicker`)

### Correct — smallest element

```tsx
function Timestamp({ iso }: { iso: string }) {
  return (
    <span suppressHydrationWarning>
      {new Date(iso).toLocaleString()}
    </span>
  )
}
```

The suppression is scoped to the `<span>` containing the dynamic text. Surrounding structure is still checked.

### Incorrect — too broad

```tsx
// BAD: silences hydration warnings for the entire panel.
// A real structural mismatch deeper in <Panel> would now go undetected.
function Panel({ iso }: { iso: string }) {
  return (
    <section suppressHydrationWarning>
      {/* ...lots of structure... */}
      <span>{new Date(iso).toLocaleString()}</span>
    </section>
  )
}
```

### Forbidden — masking structural mismatches

```tsx
// BAD: the issue is conditional rendering shape diverges between server and client.
// suppressHydrationWarning hides the warning but the resulting DOM is wrong.
<div suppressHydrationWarning>
  {isClient ? <Drawer /> : <SidebarPlaceholder />}
</div>
```

Fix the divergence: either server-render the same thing and progressively enhance, or render the variant only on the client (`useEffect` + state).

### Prefer alternatives where possible

- **Deterministic SSR.** Pass the timestamp value in already-formatted form from the server.
- **Client-only render.** `useEffect` + state ensures the value renders only after hydration. Comes with the flicker risk addressed by `rendering-hydration-no-flicker`.

`suppressHydrationWarning` should be the third choice, used only when neither alternative fits.

Sources:
- [Vercel: rendering-hydration-suppress-warning](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-hydration-suppress-warning.md)


<!-- @source rules/rendering-resource-hints.md -->

---
title: Use react-dom resource-hint APIs in Server Components/layouts for critical resources; avoid hinting everything
impact: HIGH
impactDescription: "Server-side resource hints arrive in the HTML before the client even gets the document. preconnect/preload for critical resources; prefetchDNS for speculative; preinit for stylesheets/scripts that must execute early. Overuse harms the very metric you're trying to optimize."
tags: [rendering, preload, preconnect, prefetch, resource-hints, react-19]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-010"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) hints are in Server Components or layout/root context, (b) preload/preinit limited to critical above-the-fold resources, (c) prefetchDNS/preconnect limited to origins actually needed soon, (d) framework primitives (Next.js metadata) used where stronger."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Required Server Component / layout / top-level intent"
      - "Warned against hinting every route/asset (overuse penalty)"
      - "Distinguished preload/preinit (active) from prefetchDNS/preconnect (speculative)"
      - "Preferred framework primitives where stronger"
  gap_check:
    status: complete
    note: "Cross-link to bundle-preload (which is component-level lazy preload, not resource-hint API)."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-resource-hints"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-resource-hints.md"
    role: seed
  - id: react-dom-resource-hints
    title: "React DOM — Resource Preloading APIs"
    url: "https://react.dev/reference/react-dom"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-resource-hints"
    quote: "These are especially useful in server components to start loading resources before the client even receives the HTML."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - bundle-preload
---

## Use react-dom resource-hint APIs in Server Components/layouts for critical resources

**Impact: HIGH — Server-rendered hints arrive in the HTML before the client receives the document. But overuse causes the very latency penalty you're trying to avoid.**

### API surface (React 19 stable)

| API | Side effect | Use case |
|---|---|---|
| `prefetchDNS(href)` | DNS resolve | speculative origin you'll connect to later |
| `preconnect(href)` | DNS + TCP + TLS | API/CDN you'll fetch from soon |
| `preload(href, { as, type, crossOrigin })` | fetch resource | critical font/style/script needed on current page |
| `preloadModule(href, { as })` | fetch ESM module | likely-next route's JS module |
| `preinit(href, { as })` | fetch + execute stylesheet/script | critical CSS that must apply before paint |
| `preinitModule(href)` | fetch + execute ESM | ESM that must run before render |

### Correct — Server Component / root layout

```tsx
import { preconnect, prefetchDNS, preload, preinit } from 'react-dom'

export default function RootLayout({ children }: { children: ReactNode }) {
  // Speculative origins — DNS only
  prefetchDNS('https://analytics.example.com')

  // Origins we'll hit on this page — full handshake
  preconnect('https://api.example.com')

  // Critical above-the-fold font
  preload('/fonts/inter.woff2', {
    as: 'font',
    type: 'font/woff2',
    crossOrigin: 'anonymous',
  })

  // Critical CSS that must apply before paint
  preinit('/styles/critical.css', { as: 'style' })

  return (
    <html>
      <body>{children}</body>
    </html>
  )
}
```

### Correct — speculative preload on user intent

```tsx
'use client'
import { preloadModule } from 'react-dom'

function Nav() {
  return (
    <a
      href="/dashboard"
      onMouseEnter={() => preloadModule('/dashboard.js', { as: 'script' })}
    >
      Dashboard
    </a>
  )
}
```

### Overuse penalty

Hints compete for bandwidth on a constrained connection. Excessive `preload` / `preinit` can:
- Delay the critical path resources you actually need.
- Make Lighthouse complain about "wasted bytes".
- On mobile, exhaust the connection pool.

Rules of thumb:
- `prefetchDNS` is cheapest — still don't hint every origin in the codebase.
- `preconnect` for ≤ 5 origins per page.
- `preload` / `preinit` for the ABOVE-the-fold critical path only.
- `preloadModule` only on strong intent signals.

### Framework primitives

Next.js has stronger primitives in some cases:
- `next/font` handles font preloading + display swap natively.
- `Metadata.preconnect` / metadata API may emit hints from a route's metadata export.
- `<Link prefetch>` covers route-level prefetch.

Use these where applicable; reach for the react-dom API for cases the framework doesn't cover.

### Sibling rule

`bundle-preload` is component-level lazy preload (dynamic `import()` on hover). `rendering-resource-hints` (this rule) is the lower-level HTML resource-hint API. Different abstraction layer, different audit.

Sources:
- [Vercel: rendering-resource-hints](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-resource-hints.md)
- [React DOM — Resource Preloading APIs](https://react.dev/reference/react-dom)


<!-- @source rules/rendering-script-defer-async.md -->

---
title: Mark script tags defer or async (or use next/script with a strategy); type="module" is deferred by default
impact: HIGH
impactDescription: "Render-blocking scripts kill TTFP/TTI. defer for DOM-dependent ordered scripts, async for independent (analytics), next/script with strategy in Next.js. Critical inline boot scripts (theme prehydration) are an exception — they must run before hydration."
tags: [rendering, script, defer, async, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-011"
verification:
  type: review
  status: manual
  notes: "Reviewer flags any <script> without defer/async/type=module/dangerouslySetInnerHTML in the document head. Confirms next/script is used where Next-available. Critical inline boot scripts are allowed when justified."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Noted type=\"module\" defers by default"
      - "Carved exception for critical inline boot scripts (theme prehydration)"
      - "Cross-linked to bundle-defer-third-party for SDK loading"
  gap_check:
    status: complete
    note: "Overlaps bundle-defer-third-party for the script-tag-loading case; this rule covers the lower-level defer/async attributes."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-script-defer-async"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-script-defer-async.md"
    role: seed
  - id: mdn-script-element
    title: "MDN — <script> element"
    url: "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/script"
    role: primitive-semantics
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-script-defer-async"
    quote: "Script tags without defer or async block HTML parsing while the script downloads and executes."
  - source_type: external
    citation: "MDN — script element: For module scripts, the defer attribute has no effect since they are deferred by default"
    url: "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/script#defer"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - bundle-defer-third-party
  - rendering-hydration-no-flicker
---

## Mark script tags defer or async (or use next/script); type="module" defers by default

**Impact: HIGH — A bare `<script src="…">` blocks HTML parsing.**

### Attribute semantics

| Attribute | Downloads | Executes | Order preserved |
|---|---|---|---|
| (none) | blocks parse | inline as discovered | yes |
| `defer` | parallel | after HTML parse | yes |
| `async` | parallel | as soon as ready | NO |
| `type="module"` | parallel | after HTML parse | yes (deferred by default) |

### Correct — explicit attributes

```tsx
<head>
  {/* Analytics — independent, no order dep */}
  <script src="https://example.com/analytics.js" async />
  {/* Utils — DOM-dependent or order-dependent */}
  <script src="/scripts/utils.js" defer />
  {/* Module — defers by default; the defer attribute is redundant but not wrong */}
  <script type="module" src="/scripts/app.js" />
</head>
```

### Correct — next/script in Next.js

Prefer the framework primitive — it handles strategy + de-duplication + Suspense compatibility:

```tsx
import Script from 'next/script'

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html>
      <body>
        {children}
        <Script src="https://example.com/analytics.js" strategy="afterInteractive" />
        <Script src="/scripts/utils.js" strategy="beforeInteractive" />
      </body>
    </html>
  )
}
```

### Incorrect — bare script blocks rendering

```tsx
<head>
  <script src="https://example.com/analytics.js" />
  <script src="/scripts/utils.js" />
</head>
```

Both block HTML parse until they download and execute.

### Critical inline boot scripts — exception

Some scripts MUST run before hydration to avoid flicker (theme prehydration; see `rendering-hydration-no-flicker`). Those are intentionally synchronous, dangerouslySetInnerHTML, and small. Defer/async would defeat the purpose.

Constraints on this exception:
- Small (< ~1 KB).
- Deterministic, no fetch, no user-controlled interpolation.
- CSP nonce attached.

### Choosing defer vs async

- `defer` when execution order matters, or the script depends on the DOM being parsed.
- `async` when the script is independent (analytics, error tracking, ad pixels) and order is irrelevant.
- When in doubt, `defer` is the safer default — order is preserved.

### Cross-rule scope

`bundle-defer-third-party` covers the higher-level "load this vendor library after hydration" decision (which often resolves to `next/script strategy="afterInteractive"`). This rule covers the raw `<script>` attribute decision when you're directly writing one.

Sources:
- [Vercel: rendering-script-defer-async](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-script-defer-async.md)
- [MDN — <script>](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/script)


<!-- @source rules/rendering-svg-precision.md -->

---
title: Run SVGs through SVGO with measured precision; require visual diff for logos/charts/thin strokes
impact: LOW
impactDescription: "Reduces SVG file size. Blanket --precision=1 visibly degrades icons, maps, and charts. Use measured config and visual-regression gate."
tags: [rendering, svg, optimization, svgo, assets]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-004"
verification:
  type: review
  status: manual
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Removed blanket --precision=1 recommendation"
      - "Required visual-regression check for logos/charts/maps/thin-stroke art"
      - "Noted higher precision preserved for animated SVGs and viewBox-critical art"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-svg-precision"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-svg-precision.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-svg-precision"
    quote: "Reduce SVG coordinate precision to decrease file size."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules: []
---

## Run SVGs through SVGO with measured precision; require visual diff for logos/charts/thin strokes

**Impact: LOW — Reduces SVG file size. Blanket `--precision=1` visibly degrades icons, maps, and charts.**

### Correct — measured SVGO config

```bash
# Default precision 3 is a safe starting point. Drop only after diffing.
npx svgo --precision=3 --multipass icon.svg

# For interface icons that are simple shapes, precision=1 is often OK.
npx svgo --precision=1 --multipass simple-icon.svg
```

### Visual regression gate

Logos, maps, charts, thin-stroke line art, and any artwork at small viewBox values MUST be diffed visually before precision reduction is committed. Acceptable workflow:

1. Optimize with the proposed precision.
2. Render before/after at the actual target sizes used in the UI.
3. Reject if any artifact is visible.

### Preserve precision for

- Animated SVGs — precision loss compounds across keyframes
- Logos at multiple sizes — visible at small renders
- Charts and visualizations — data fidelity matters
- Maps and complex paths — corners get rounded off
- Tiny viewBoxes where 0.1 unit difference is visible

### Don't ship precision changes by `--precision=1` default

The Vercel rule's example uses `--precision=1` as if it's universally safe. It's not. Default to `--precision=3`, justify any reduction with a visual diff.

### Automation

Add SVGO as a pre-commit hook on `*.svg` files with a measured `svgo.config.js` and a visual-diff CI step (Playwright snapshot or Percy/Chromatic) for the asset directory.

Sources:
- [Vercel: rendering-svg-precision](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-svg-precision.md)
- [SVGO](https://github.com/svg/svgo)


<!-- @source rules/rendering-usetransition-loading.md -->

---
title: useTransition for non-urgent UI updates (search/filter/navigation) — not a replacement for network-lifecycle loading state
impact: LOW
impactDescription: "Marks state updates as transitions so React keeps the prior UI responsive while the new state is computed. Built-in isPending. NOT a substitute for explicit loading state on network fetches, uploads, mutations."
tags: [rendering, transitions, useTransition, loading, state, react-19]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-009"
verification:
  type: review
  status: manual
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Reframed: transition is for non-urgent UI updates that should be interruptible"
      - "Network-lifecycle loading state (uploads, mutations, imperative async) still uses explicit useState"
      - "Suspense + framework pending state is the preferred path for data fetching"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-usetransition-loading"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-usetransition-loading.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-usetransition-loading"
    quote: "Use useTransition instead of manual useState for loading states."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - async-suspense-boundaries
---

## useTransition for non-urgent UI updates — not a replacement for network-lifecycle loading state

**Impact: LOW — Marks state updates as transitions so React keeps the prior UI responsive while the new state is computed. NOT a substitute for explicit loading state on network fetches, uploads, mutations.**

### When useTransition is the right tool

- A search input where typing updates immediately but results filter lazily
- Tab navigation that triggers a heavy re-render
- A filter/sort change on a long list
- Any UI update that's expensive but interruptible

The key: the update is **interruptible** (a new keystroke supersedes the prior one) and the user should see the OLD state while the NEW state computes.

### Correct — search with interruptible filter

```tsx
import { useState, useTransition } from 'react'

function Search() {
  const [query, setQuery] = useState('')
  const [isPending, startTransition] = useTransition()

  return (
    <>
      <input
        value={query}
        onChange={(e) => {
          setQuery(e.target.value)  // urgent — input must feel instant
          startTransition(() => {
            // Marked as non-urgent; React can interrupt this on next keystroke.
            setFilteredResults(filterBig(e.target.value))
          })
        }}
      />
      {isPending && <Spinner />}
      <FilteredResultsList />
    </>
  )
}
```

### Not the right tool for network lifecycle

useTransition is **not** an async lifecycle manager. For:
- HTTP fetches with error/timeout/cancellation
- File uploads with progress
- Optimistic mutations with rollback

…use explicit `useState` for the loading state, or better, the data-fetch library's pending/error/data shape (TanStack Query, SWR), or React 19 `use(promise)` + Suspense (sibling rule `async-suspense-boundaries`).

### Don't wrap async/await inside startTransition

```tsx
// BAD: useTransition is synchronous-update-oriented. Wrapping an async function
// here can mislead readers and obscure error/cancel semantics.
startTransition(async () => {
  const data = await fetch(...)
  setResults(data)
})
```

If you have an actual async fetch, use `useActionState` (React 19) for forms, or framework data primitives, or your data library's mutation hook.

### Comparison

| Need | Tool |
|---|---|
| Interruptible UI update (filter, search, tab) | `useTransition` |
| Network fetch / mutation / upload | data library (TanStack/SWR) OR `useActionState` OR explicit `useState` |
| Read-only data dependency | `<Suspense>` + `use(promise)` |
| Pending state on Server Action / form | `useActionState` / `useFormStatus` |

Sources:
- [Vercel: rendering-usetransition-loading](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-usetransition-loading.md)
- [React 19 — useTransition](https://react.dev/reference/react/useTransition)


<!-- @source rules/rerender-defer-reads.md -->

---
title: Don't subscribe to dynamic state (useSearchParams, etc.) when you only read it inside a callback
impact: MEDIUM
impactDescription: "Reactive hooks (useSearchParams, useSyncExternalStore-derived) re-render their consumers on every change. If the value is only consumed inside an event handler, read it on-demand from window.location.search / document.cookie instead."
tags: [rerender, searchParams, localStorage, optimization]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-002"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness: { status: complete, amendments: ["Client-only — window.location.search only inside browser callbacks", "If UI must react to URL changes, keep useSearchParams()"] }
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-defer-reads"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-defer-reads.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-defer-reads"
    quote: "Don't subscribe to dynamic state (searchParams, localStorage) if you only read it inside callbacks."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Don't subscribe to dynamic state when you only read it inside a callback

**Impact: MEDIUM — Reactive subscriptions re-render the consumer on every change. If the value is only used at click time, read on-demand.**

### Incorrect — subscribes to every URL change

```tsx
'use client'
import { useSearchParams } from 'next/navigation'

function ShareButton({ chatId }: { chatId: string }) {
  const searchParams = useSearchParams()   // subscribes to every URL change
  return (
    <button onClick={() => shareChat(chatId, { ref: searchParams.get('ref') })}>
      Share
    </button>
  )
}
```

### Correct — read on demand inside the handler

```tsx
'use client'
function ShareButton({ chatId }: { chatId: string }) {
  return (
    <button
      onClick={() => {
        const params = new URLSearchParams(window.location.search)
        shareChat(chatId, { ref: params.get('ref') })
      }}
    >
      Share
    </button>
  )
}
```

### When you DO need the subscription

If the **UI** (rendered output) must reflect the value — e.g. the button label changes when the URL changes — keep `useSearchParams()`. The rule applies only when the value is consumed strictly inside a handler.

Same logic applies to `localStorage` (no React subscription anyway; the rule is "don't bake a sync-hook around it for a callback-only read") and any cookie-watcher hook.

Sources:
- [Vercel: rerender-defer-reads](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-defer-reads.md)


<!-- @source rules/rerender-dependencies.md -->

---
title: Depend on the primitive value your Effect actually reads — not the parent object — and don't use this to hide real deps
impact: LOW
impactDescription: "useEffect([user.id]) re-runs only when id changes; useEffect([user]) re-runs on any field change. Honest narrowing reduces churn. Dishonest narrowing (skip deps your Effect reads) is a bug."
tags: [rerender, useEffect, dependencies, optimization]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-004"
verification: { type: review, status: manual, notes: "Reviewer enforces react-hooks/exhaustive-deps; checks that narrowed deps reflect what the Effect actually reads." }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Narrow only when Effect actually reads only that field"
      - "For non-reactive callback dependencies on 19.2+, use useEffectEvent"
      - "Cross-link with rerender-derived-state (threshold case)"
  gap_check: { status: complete, note: "Overlaps rerender-derived-state on threshold case — kept separate as 'narrow deps' vs 'subscribe to derived signal'." }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-dependencies"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-dependencies.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-dependencies"
    quote: "Specify primitive dependencies instead of objects to minimize effect re-runs."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [rerender-derived-state, advanced-use-latest]
---

## Depend on the primitive value the Effect reads — and never use this to hide real deps

**Impact: LOW — Narrowing deps is honest only when the Effect truly reads just that field.**

### Correct

```tsx
useEffect(() => {
  log(user.id)               // only reads user.id
}, [user.id])                // only that primitive is in the deps
```

### Incorrect — wasteful

```tsx
useEffect(() => {
  log(user.id)               // still only reads user.id
}, [user])                   // but re-runs on every user-field change
```

### Forbidden — dishonest narrowing

```tsx
// BAD: Effect actually reads user.role too. Listing only user.id silences the
// exhaustive-deps rule but creates a stale-closure bug.
useEffect(() => {
  if (user.role === 'admin') doAdminThing(user.id)
}, [user.id])
```

Keep `react-hooks/exhaustive-deps` ESLint rule on. If you find yourself adding eslint-disable comments to silence it, you're either:
- Hiding a real dep (bug), or
- Working around a need for `useEffectEvent` (React 19.2+, see sibling rule `advanced-use-latest`).

### Threshold case — see sibling rule

If the Effect uses a continuous value via a threshold comparison (`width < 768`), prefer subscribing to the derived signal directly (`useMediaQuery`). See `rerender-derived-state`.

Sources:
- [Vercel: rerender-dependencies](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-dependencies.md)


<!-- @source rules/rerender-derived-state-no-effect.md -->

---
title: Derive values during render, not in state synced via Effect
impact: MEDIUM
impactDescription: "If a value can be computed from current props/state, compute it during render. Storing it in state and syncing via useEffect adds an extra render, causes drift bugs, and is the canonical 'You Might Not Need an Effect' anti-pattern."
tags: [rerender, derived-state, useEffect, state, you-might-not-need-an-effect]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-006"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness: { status: complete, amendments: ["Lead with React docs framing", "useMemo only for genuinely expensive derivation with profiler evidence"] }
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-derived-state-no-effect"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-derived-state-no-effect.md"
    role: seed
  - id: react-you-might-not-need-effect
    title: "React docs — You Might Not Need an Effect"
    url: "https://react.dev/learn/you-might-not-need-an-effect"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-derived-state-no-effect"
    quote: "If a value can be computed from current props/state, do not store it in state or update it in an effect."
  - source_type: external
    citation: "React docs — Updating state based on props or state (the You Might Not Need an Effect canonical 'derive during render' guidance)"
    url: "https://react.dev/learn/you-might-not-need-an-effect#updating-state-based-on-props-or-state"
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_AS_IS }
sibling_rules: [rerender-move-effect-to-event]
---

## Derive during render, not via state + Effect

**Impact: MEDIUM — Canonical React docs guidance ("You Might Not Need an Effect"). Extra render + drift risk.**

### Incorrect — redundant state, drift-prone

```tsx
function Form() {
  const [first, setFirst] = useState('First')
  const [last, setLast] = useState('Last')
  const [full, setFull] = useState('')

  useEffect(() => {
    setFull(`${first} ${last}`)        // extra render after each change; can drift if any path forgets to update
  }, [first, last])

  return <p>{full}</p>
}
```

### Correct — derive during render

```tsx
function Form() {
  const [first, setFirst] = useState('First')
  const [last, setLast] = useState('Last')
  const full = `${first} ${last}`      // no extra render, no drift possible
  return <p>{full}</p>
}
```

### When `useMemo` is justified

Only when the derivation is **genuinely expensive** AND profiler evidence shows the cost matters:

```tsx
const sortedItems = useMemo(
  () => items.toSorted(expensiveComparator),
  [items],
)
```

For cheap derivations (string concat, simple arithmetic, boolean), don't reach for `useMemo` — the memo overhead exceeds the derivation cost (see sibling rule `rerender-simple-expression-in-memo`).

### Related rule

`rerender-move-effect-to-event` covers the sibling case: "if it's not derivation but a user-action side effect, the side effect belongs in an event handler, not in state + Effect".

Sources:
- [Vercel: rerender-derived-state-no-effect](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-derived-state-no-effect.md)
- [React — You Might Not Need an Effect](https://react.dev/learn/you-might-not-need-an-effect)


<!-- @source rules/rerender-derived-state.md -->

---
title: Subscribe to the semantic signal you actually need, not the continuous value behind it
impact: MEDIUM
impactDescription: "When the UI only cares about a threshold (mobile vs desktop), subscribe to the derived boolean (useMediaQuery), not the raw value (useWindowWidth). Re-render only on transitions, not on every pixel."
tags: [rerender, derived-state, media-query, optimization]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-005"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Don't replace width when exact pixel value is needed for layout math"
      - "Mention overlap with rerender-dependencies (narrowing); this rule is 'choose the right hook'"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-derived-state"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-derived-state.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-derived-state"
    quote: "Subscribe to derived boolean state instead of continuous values to reduce re-render frequency."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [rerender-dependencies]
---

## Subscribe to the semantic signal, not the continuous value

**Impact: MEDIUM — Re-render on transitions, not on every micro-change.**

### Incorrect

```tsx
function Sidebar() {
  const width = useWindowWidth()    // re-renders on every resize pixel
  const isMobile = width < 768
  return <nav className={isMobile ? 'mobile' : 'desktop'} />
}
```

### Correct

```tsx
function Sidebar() {
  const isMobile = useMediaQuery('(max-width: 767px)')   // re-renders only on transition
  return <nav className={isMobile ? 'mobile' : 'desktop'} />
}
```

### When you DO need the continuous value

If the UI renders the exact pixel value, or uses it for layout math (e.g. `style={{ width: width / 2 }}`), keep `useWindowWidth`. The rule applies when the UI uses ONLY the derived boolean.

### Related rule

`rerender-dependencies` (narrow deps to primitives) targets the same goal from a different angle: narrowing what an Effect reads. This rule is "pick the right subscription primitive in the first place".

Sources:
- [Vercel: rerender-derived-state](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-derived-state.md)


<!-- @source rules/rerender-functional-setstate.md -->

---
title: Use setState(prev => …) when the new state depends on the current state — primary win is correctness (no stale closure)
impact: MEDIUM
impactDescription: "Functional update form prevents stale-closure bugs and lets useCallback omit the state from its dependency array. The rerender benefit (stable callback identity) is secondary."
tags: [react, hooks, useState, useCallback, callbacks, closures, correctness]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-007"
verification:
  type: lint
  rule_id: "ax/prefer-functional-setstate"
  status: shipped
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Lead with stale-closure correctness, not rerender"
      - "Stable callback only helps when callback identity is observable (passed to memoized child)"
      - "Don't imply functional updater itself reduces renders"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-functional-setstate"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-functional-setstate.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-functional-setstate"
    quote: "When updating state based on the current state value, use the functional update form of setState instead of directly referencing the state variable."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Functional setState when the new state depends on current — primary win is correctness

**Impact: MEDIUM — Eliminates the most common React stale-closure bug class.**

### Incorrect — stale closure + recreated callback

```tsx
function TodoList() {
  const [items, setItems] = useState(initialItems)

  const addItems = useCallback((newItems: Item[]) => {
    setItems([...items, ...newItems])       // closes over `items` at callback creation
  }, [items])                                // → must depend on items → recreated each items change

  const removeItem = useCallback((id: string) => {
    setItems(items.filter((x) => x.id !== id))   // stale closure if deps omitted
  }, [])                                          // ❌ missing dep — uses initial items forever

  return <ItemsEditor items={items} onAdd={addItems} onRemove={removeItem} />
}
```

### Correct — functional update, no stale closure, stable callback

```tsx
function TodoList() {
  const [items, setItems] = useState(initialItems)

  const addItems = useCallback((newItems: Item[]) => {
    setItems((curr) => [...curr, ...newItems])  // always uses latest state
  }, [])                                         // no items dep needed

  const removeItem = useCallback((id: string) => {
    setItems((curr) => curr.filter((x) => x.id !== id))
  }, [])

  return <ItemsEditor items={items} onAdd={addItems} onRemove={removeItem} />
}
```

### Why this is primarily correctness, not perf

- **Stale closures** are real bugs: data loss, lost updates, wrong calculations.
- **Stable callback identity** only matters when:
  - The callback is passed to a memoized child (`memo`), or
  - It's a dependency of another `useCallback`/`useMemo`/`useEffect`.

Without those, stable identity is free but invisible. The functional updater pays for itself in bug prevention regardless.

### When direct updates are fine

- `setCount(0)` — static value.
- `setName(newName)` — value comes from props/args only, doesn't depend on prior state.

### Compiler note

React Compiler can stabilize some of these patterns. Functional updates remain the right correctness practice — compiler doesn't fix stale closures, it just adjusts memoization.

Sources:
- [Vercel: rerender-functional-setstate](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-functional-setstate.md)


<!-- @source rules/rerender-lazy-state-init.md -->

---
title: Pass a function to useState when the initial value requires heavy computation
impact: MEDIUM
impactDescription: "useState(expensive()) runs expensive() on every render even though only the first call's result is used. useState(() => expensive()) runs it only on mount. StrictMode dev double-mount may call the initializer twice — make it pure."
tags: [react, hooks, useState, performance, initialization]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-008"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Scope to heavy sync initializers"
      - "Lazy init for cheap literals is unnecessary noise"
      - "StrictMode dev double-mount may call initializer twice — make pure"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-lazy-state-init"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-lazy-state-init.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-lazy-state-init"
    quote: "Without the function form, the initializer runs on every render even though the value is only used once."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_AS_IS }
sibling_rules: []
---

## Pass a function to useState when the initial value requires heavy computation

**Impact: MEDIUM — Compiler-irrelevant; avoids repeating expensive sync work each render.**

### Incorrect — runs every render

```tsx
function FilteredList({ items }: { items: Item[] }) {
  const [index, setIndex] = useState(buildSearchIndex(items))  // runs on every render
  // ...
}

function UserProfile() {
  const [settings] = useState(
    JSON.parse(localStorage.getItem('settings') ?? '{}'),       // runs on every render
  )
  // ...
}
```

### Correct — runs on mount only

```tsx
function FilteredList({ items }: { items: Item[] }) {
  const [index, setIndex] = useState(() => buildSearchIndex(items))
  // ...
}

function UserProfile() {
  const [settings] = useState(() => {
    const stored = localStorage.getItem('settings')
    return stored ? JSON.parse(stored) : {}
  })
  // ...
}
```

### When lazy init is unnecessary

- Cheap literals: `useState(0)`, `useState('')`, `useState({})`.
- Direct prop references: `useState(props.initial)`.
- Simple defaults: `useState({ open: false })`.

The function form here is noise, not a win.

### StrictMode caveat

In React StrictMode (dev only), the initializer may run twice as part of intentional double-invocation. Make initializers pure — no side effects, no logging, no DOM mutations. Reading from `localStorage` is acceptable (idempotent read).

### Use cases

- Building search indexes / Map / Set from props.
- Reading and parsing from `localStorage` / `sessionStorage`.
- Reading from the DOM (rare; only when SSR-safe).
- Heavy sync transformations of large input.

Sources:
- [Vercel: rerender-lazy-state-init](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-lazy-state-init.md)


<!-- @source rules/rerender-memo-with-default-value.md -->

---
title: When a memoized component has a non-primitive default prop value, extract the default to a module constant
impact: MEDIUM
impactDescription: "Default value expressions like `onClick = () => {}` or `items = []` create a new reference every render, defeating React.memo's shallow-equality check. Extract to module constant so the reference is stable."
tags: [rerender, memo, optimization]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-003"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Frame as 'when a memoized boundary already exists'"
      - "Don't make this a memo-promotion rule (see rerender-memo)"
      - "Compiler may already handle this in React 19 + Compiler projects"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-memo-with-default-value"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-memo-with-default-value.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-memo-with-default-value"
    quote: "When memoized component has a default value for some non-primitive optional parameter [...] calling the component without that parameter results in broken memoization."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [rerender-memo]
---

## When a memoized component already exists, extract non-primitive default prop values to module constants

**Impact: MEDIUM — Scope: only applies to components that already have a memoization boundary. This rule does NOT recommend adding `memo()` — see `rerender-memo` for that policy.**

### Incorrect — default `() => {}` defeats memo

```tsx
const UserAvatar = memo(function UserAvatar({
  onClick = () => {},        // ❌ new function each render
}: { onClick?: () => void }) {
  // ...
})

<UserAvatar />   // memo compares props: onClick !== prevOnClick → re-renders
```

### Correct — module-level constant default

```tsx
const NOOP = () => {}

const UserAvatar = memo(function UserAvatar({
  onClick = NOOP,            // ✅ stable reference
}: { onClick?: () => void }) {
  // ...
})
```

Same logic for `arr = []`, `obj = {}` defaults. Hoist them.

### Don't reach for `memo` to "fix" this

If the component isn't already memoized, you may not need memo at all. Read sibling rule `rerender-memo` — React Compiler usually makes manual memo unnecessary. Only apply this rule when a memo boundary already exists and is justified.

### Compiler-era nuance

React Compiler (GA in React 19) may auto-stabilize the default expression as well. With compiler on, manual constant extraction is redundant. Verify by profiling.

Sources:
- [Vercel: rerender-memo-with-default-value](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-memo-with-default-value.md)


<!-- @source rules/rerender-memo.md -->

---
title: Profile before manual memoization — prefer React Compiler, pure rendering, and local state
impact: MEDIUM
impactDescription: "Manual memo()/useMemo() is a fallback optimization, not a default design choice. In React 19 + Compiler era, most manual memoization is unnecessary or harmful. Reach for memo() only after profiling shows perceptible lag and the props are stable."
tags:
  - rerender
  - memo
  - useMemo
  - react-compiler
  - optimization
  - performance
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-001"
verification:
  type: review
  status: manual
  notes: "Static detection of 'unnecessary memo' is unreliable (false positives common). Verification combines: (a) React DevTools Profiler evidence in the PR description, (b) check whether React Compiler is enabled — if so, prefer compiler-managed memoization and only ship manual memo for custom comparator cases or explicit cleanup migration."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
  contested: true
  contested_axis: "(Claude + Codex) vs Vercel seed — both reviewers independently judged Vercel's framing inverts React 19 official guidance"
audit:
  accuracy:
    status: verified-with-framing-flip
    last_verified: "2026-05-16"
    notes: "Vercel's 'extract to memoized child for early return' example is technically valid, but the rule's title and premise present manual memoization as a default move when React 19 docs make it a fallback. Verdict: the underlying mechanic is correct; the framing inverts the priority."
  freshness:
    status: stale
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Vercel rule treats React Compiler as a one-line footnote. As of May 2026 React Compiler is GA in React 19; this is the dominant fact for the topic. Catalog rule must lead with compiler-first guidance."
  completeness:
    status: complete
    amendments:
      - "Inverted premise: profile-first, compiler-first, manual memo as last resort"
      - "Added 'always new prop' trap (object/function/array literals defeat memoization)"
      - "Added deep equality custom-comparator warning (slow, brittle)"
      - "Added React Compiler migration guidance (leave existing memos during migration, remove in cleanup)"
      - "Listed React 19 alternatives: pure rendering, children composition, local state, fewer Effects"
      - "Verification conditional on whether React Compiler is enabled"
  gap_check:
    status: complete
    note: "Custom comparator case folded in as a warning section rather than spawning a sibling rule (per codex)."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (rule: rerender-memo)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-memo.md"
    role: "seed"
  - id: react-19-memo
    title: "React 19 — memo() API reference"
    url: "https://react.dev/reference/react/memo"
    role: "canonical-react"
  - id: react-compiler
    title: "React 19 — React Compiler"
    url: "https://react.dev/learn/react-compiler"
    role: "canonical-react"
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-memo"
    quote: "Extract expensive work into memoized components to enable early returns before computation."
  - source_type: external
    citation: "React 19 — memo() reference, 'When to use memo' / 'When not to use memo' (verbatim guidance: 'You should only rely on memo as a performance optimization. If your code doesn't work without it, find the underlying problem and fix it first.')"
    url: "https://react.dev/reference/react/memo"
  - source_type: external
    citation: "React 19 — memo() reference, 'always different props' trap ('memo is completely useless if the props passed to your component are always different, such as if you pass an object or a plain function defined during rendering')"
    url: "https://react.dev/reference/react/memo"
  - source_type: external
    citation: "React 19 — React Compiler GA documentation (compiler applies automatic memoization equivalent to memo + useMemo; manual memo can be safely removed)"
    url: "https://react.dev/learn/react-compiler"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=high"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Vercel rule is lossy with counter-recommendation pressure, not flatly false"
    - "Premise should be inverted"
    - "Custom comparator folded in as warning, not separate rule"
    - "Verification conditional on React Compiler presence"
  framing_flip:
    from: "Extract to Memoized Components (memo as default move)"
    to: "Profile Before Manual Memoization (memo as last resort)"
sibling_rules: []
---

## Profile before manual memoization — prefer React Compiler, pure rendering, and local state

**Impact: MEDIUM — Manual `memo()`/`useMemo()` is a fallback optimization, not a default design choice. In React 19 + Compiler era, most manual memoization is unnecessary or harmful. Reach for `memo()` only after profiling shows perceptible lag and the props are stable.**

> **Note on framing.** The seed source frames this rule as "extract expensive
> work into memoized components." Both reviewers (Claude and Codex)
> independently judged that framing as an inversion of React 19's official
> guidance. React docs say: "**You should only rely on memo as a performance
> optimization. If your code doesn't work without it, find the underlying
> problem and fix it first.**" The ax catalog flips the framing accordingly.

### Decision order — try these first

1. **Check whether React Compiler is enabled** (`@babel/plugin-react-compiler` / `babel-plugin-react-compiler` configured). If yes:
    - Do not add manual `memo()` / `useMemo()` / `useCallback()` for ordinary re-render optimization. The compiler does it.
    - Exception: custom comparator (`memo(C, areEqual)`) — compiler does not replicate this. Keep / add manual only when you actually need custom comparison.
    - Migration policy: leave existing manual memos in place during compiler adoption to avoid regressions; remove them in a dedicated cleanup pass with profiler verification.

2. **If no compiler**, prefer code shapes that make memoization unnecessary:
    - Accept JSX as `children` so wrappers do not re-render with the children's identity.
    - Keep state local; don't lift state higher than needed.
    - Maintain pure rendering — fix bugs that show up as visible artifacts on re-render, don't mask them with `memo`.
    - Avoid unnecessary Effects and Effect dependencies.

3. **Only then**: profile with React DevTools. If a specific interaction shows perceptible lag AND the offending component's props are stable, consider manual `memo()`.

### When manual memo() is justified

- Component re-renders **often** with the **same exact props**.
- Re-render cost is **measurable** (profiler evidence).
- Props are primitives or stable references (not literals constructed in the parent every render).
- Parent has already been examined for "always-new" prop sources.

### Common traps that defeat or harm memo()

**"Always new" props (one is enough to defeat the whole component):**

```tsx
// BAD: { theme: 'dark' } is a new object every parent render — memo never hits.
<Child options={{ theme: 'dark' }} />

// BAD: inline arrow recreated every render.
<Child onClick={() => doThing()} />

// GOOD: stable reference.
const OPTIONS = { theme: 'dark' }
<Child options={OPTIONS} />
```

> React docs: "a single value that's 'always new' is enough to break memoization for an entire component."

**Custom comparators are dangerous:**

```tsx
// DANGEROUS: deep equality runs on every parent render. May freeze the app
// when data shapes grow. Profile before shipping; never use this for nested
// data structures.
export default memo(MyComponent, (prev, next) =>
  deepEqual(prev.data, next.data),
)
```

> React docs: "Deep equality checks can become incredibly slow and can freeze your app for many seconds if someone changes the data structure later."

If you must use a custom comparator: compare **every** prop explicitly (omitting one is a correctness bug), benchmark against just letting React re-render, and document the choice next to the call.

### The seed rule's extracted-child pattern — narrow application

Vercel's "Correct" example (extracting an expensive child into a `memo()` wrapper so the parent can do an early-return before the work) is valid in a narrow case:

- React Compiler is **off**, AND
- Profiling shows the child's render is expensive, AND
- The parent legitimately does an early return that should skip the child entirely, AND
- The child's props are stable.

In that case, the extraction is sound. But in the React Compiler era this case shrinks toward zero — the compiler already memoizes the child if its props are stable, so the early return in the parent already avoids the work without an explicit `memo()` wrapper.

**Incorrect (seed's "Correct" applied indiscriminately):**

```tsx
// In a Compiler-enabled project, this manual memo is redundant and adds
// per-render overhead.
const UserAvatar = memo(function UserAvatar({ user }: { user: User }) {
  return <Avatar id={computeAvatarId(user)} />
})
```

**Correct (in a no-compiler project, with profile evidence the avatar render is expensive):**

```tsx
const UserAvatar = memo(function UserAvatar({ user }: { user: User }) {
  return <Avatar id={computeAvatarId(user)} />
})

function Profile({ user, loading }: Props) {
  if (loading) return <Skeleton />
  return <UserAvatar user={user} />
}
```

### What this rule does NOT recommend

- "Memoize as much as possible" — explicitly counter-recommended by React docs (readability cost, fragility, may interact poorly with Compiler).
- Using `useMemo` for cheap primitives (`useMemo(() => a + b, [a, b])` is more expensive than the addition).
- Adding `memo()` to fix a re-render-visible bug — that's masking; fix the bug.

### Verification

Conditional on whether React Compiler is enabled:

- **Compiler ON**: PR review checks for manual `memo` / `useMemo` / `useCallback` additions; rejects unless (a) custom comparator, or (b) explicit migration-leave-in-place comment.
- **Compiler OFF**: PR review checks for profiler evidence in the description before accepting new memo. Stable-prop audit on the parent's call site.

Both modes: deep-equality custom comparators require explicit justification + benchmark.

Sources for this rule:

- [Vercel agent-skills: rerender-memo (seed, framing flipped per audit)](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-memo.md)
- [React 19 — memo() API reference](https://react.dev/reference/react/memo)
- [React 19 — React Compiler](https://react.dev/learn/react-compiler)


<!-- @source rules/rerender-move-effect-to-event.md -->

---
title: Side effects triggered by user actions belong in event handlers, not state + Effect
impact: MEDIUM
impactDescription: "Canonical React docs pattern. Modeling 'user clicked submit' as state→Effect causes the Effect to re-run on unrelated dep changes (theme/context) and may duplicate the action. Run it in the handler."
tags: [rerender, useEffect, events, side-effects, dependencies, you-might-not-need-an-effect]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-011"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness: { status: complete }
  gap_check: { status: complete, note: "Sibling rerender-derived-state-no-effect; both anchored in You Might Not Need an Effect" }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-move-effect-to-event"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-move-effect-to-event.md"
    role: seed
  - id: react-you-might-not-need-effect
    title: "React docs — You Might Not Need an Effect (should this code move to an event handler?)"
    url: "https://react.dev/learn/you-might-not-need-an-effect"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-move-effect-to-event"
    quote: "If a side effect is triggered by a specific user action (submit, click, drag), run it in that event handler."
  - source_type: external
    citation: "React docs — Should this code move to an event handler? (canonical 'derive from action' guidance)"
    url: "https://react.dev/learn/removing-effect-dependencies#should-this-code-move-to-an-event-handler"
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_AS_IS }
sibling_rules: [rerender-derived-state-no-effect]
---

## Side effects from user actions belong in event handlers, not state + Effect

**Impact: MEDIUM — Canonical React docs guidance.**

### Incorrect — action modeled as state + Effect

```tsx
function Form() {
  const [submitted, setSubmitted] = useState(false)
  const theme = useContext(ThemeContext)

  useEffect(() => {
    if (submitted) {
      post('/api/register')          // re-runs if theme changes after submit!
      showToast('Registered', theme)
    }
  }, [submitted, theme])

  return <button onClick={() => setSubmitted(true)}>Submit</button>
}
```

Bug: if `theme` changes after submit (legitimate UI update), the Effect re-runs and re-submits.

### Correct — handler

```tsx
function Form() {
  const theme = useContext(ThemeContext)

  function handleSubmit() {
    post('/api/register')
    showToast('Registered', theme)
  }

  return <button onClick={handleSubmit}>Submit</button>
}
```

### What Effects ARE for (kept for clarity)

- **Synchronization with external systems**: subscriptions, websockets, browser APIs.
- **Side effects caused by rendering / mounting**, not by user actions.
- **Cleanup on unmount**.

If the side effect's cause is "user did X", it goes in the handler. If the cause is "this thing rendered" or "this state value reached a sync point with the outside world", it goes in an Effect.

### Decision question (per React docs)

> "Did this code happen because the user did something specific?"

- Yes → event handler.
- No, it's part of rendering / sync → Effect.

Sources:
- [Vercel: rerender-move-effect-to-event](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-move-effect-to-event.md)
- [React — Should this code move to an event handler?](https://react.dev/learn/removing-effect-dependencies#should-this-code-move-to-an-event-handler)


<!-- @source rules/rerender-no-inline-components.md -->

---
title: Never define a component inside another component — it remounts on every parent render and destroys state
impact: HIGH
impactDescription: "A new function literal is a new component type. React unmounts the old instance and mounts a fresh one — input focus is lost, animations restart, effects re-run, scroll position resets. Define child components at module scope; pass data as props."
tags: [rerender, components, remount, performance, correctness]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-015"
verification:
  type: lint
  rule_id: "ax/no-inline-component-definition"
  status: shipped
  notes: "Custom ESLint rule planned: flag function declarations inside other function components whose return type is JSX, except small inline render helpers explicitly returning array-of-JSX nodes."
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness: { status: complete, amendments: ["Lead with remount/lost state, not perf", "Allow tiny render helpers that are not component types"] }
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-no-inline-components"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-no-inline-components.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-no-inline-components"
    quote: "Defining a component inside another component creates a new component type on every render. React sees a different component each time and fully remounts it, destroying all state and DOM."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_AS_IS }
sibling_rules: []
---

## Never define a component inside another component

**Impact: HIGH — This is a correctness bug, not just perf. New component type each render → remount → lost state.**

### Incorrect — Avatar/Stats remount on every UserProfile render

```tsx
function UserProfile({ user, theme }) {
  const Avatar = () => <img src={user.avatarUrl} className={theme === 'dark' ? 'avatar-dark' : 'avatar-light'} />
  const Stats = () => <div><span>{user.followers}</span><span>{user.posts}</span></div>
  return <div><Avatar /><Stats /></div>
}
```

Every time `UserProfile` renders, `Avatar` and `Stats` are new component types. React's reconciler treats them as completely different components → unmounts old instance → mounts new instance.

### Symptoms of this bug

- Input fields **lose focus** on every keystroke (parent state update → input child remounts → DOM input element replaced).
- Animations **restart unexpectedly** (mount → animation start; remount → animation start again).
- `useEffect` cleanup + setup run **on every parent render**.
- Scroll position **resets** inside the inner component.
- State accumulated in the inner component (e.g. accordion open/closed) is wiped.

### Correct — define at module scope; pass data as props

```tsx
function Avatar({ src, theme }: { src: string; theme: 'light' | 'dark' }) {
  return <img src={src} className={theme === 'dark' ? 'avatar-dark' : 'avatar-light'} />
}

function Stats({ followers, posts }: { followers: number; posts: number }) {
  return <div><span>{followers}</span><span>{posts}</span></div>
}

function UserProfile({ user, theme }) {
  return (
    <div>
      <Avatar src={user.avatarUrl} theme={theme} />
      <Stats followers={user.followers} posts={user.posts} />
    </div>
  )
}
```

### Inline render helpers (not component types) are OK

```tsx
// FINE: this is a render helper, not a component type. React doesn't treat it as a component
// because we call it directly, not via JSX.
function ItemList({ items }: { items: Item[] }) {
  const renderItem = (item: Item) => <li key={item.id}>{item.name}</li>
  return <ul>{items.map(renderItem)}</ul>
}
```

The boundary: anything returned via `<Foo />` (JSX with capital-letter element) becomes a component type and triggers this bug. Anything called as `renderFoo(x)` is just a function call — no remount.

### Why "access parent variables" isn't a reason

The temptation is "I want this child to see `user` without passing it as a prop." Pass it as a prop. The cost (clearer interface) is negligible; the cost of inline definition (remount-on-everything) is huge.

Sources:
- [Vercel: rerender-no-inline-components](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-no-inline-components.md)


<!-- @source rules/rerender-simple-expression-in-memo.md -->

---
title: Don't useMemo a primitive-result expression — the memo overhead exceeds the cost
impact: LOW-MEDIUM
impactDescription: "useMemo allocates a closure and a dependency array, runs shallow-equality comparison, and only THEN runs your function. For `a || b` or `a + b`, this is more expensive than just re-running the expression."
tags: [rerender, useMemo, optimization, react-compiler]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-009"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Compiler-first: manual useMemo usually redundant in React 19 + Compiler"
      - "Exception: measured-expensive computation OR referential stability required by external API"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-simple-expression-in-memo"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-simple-expression-in-memo.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-simple-expression-in-memo"
    quote: "Calling useMemo and comparing hook dependencies may consume more resources than the expression itself."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_AS_IS }
sibling_rules: [rerender-memo]
---

## Don't useMemo a primitive-result expression — the memo costs more than the work

**Impact: LOW-MEDIUM — useMemo has overhead; cheap expressions pay it for no gain.**

### Incorrect

```tsx
function Header({ user, notifications }: Props) {
  const isLoading = useMemo(
    () => user.isLoading || notifications.isLoading,
    [user.isLoading, notifications.isLoading],
  )
  return isLoading ? <Skeleton /> : <Real />
}
```

`useMemo` allocates: a function closure, a dependency array, a hook slot. On every render it shallow-compares the deps. The expression `a || b` is one CPU cycle.

### Correct

```tsx
function Header({ user, notifications }: Props) {
  const isLoading = user.isLoading || notifications.isLoading
  return isLoading ? <Skeleton /> : <Real />
}
```

### Compiler-era framing

React Compiler (GA in React 19) auto-memoizes intermediate values. Manual `useMemo` for primitives is doubly redundant when compiler is on — it adds noise AND a duplicate cache layer.

### Exceptions — when useMemo IS justified

1. **Measured-expensive computation** — sort a million items, build a graph, run a parser. Profile first.
2. **Referential stability required by external API** — passing a value to a memoized child where prop equality is the optimization, OR using it as a `useEffect` dependency that should NOT re-run.

If neither applies, skip the memo.

Sources:
- [Vercel: rerender-simple-expression-in-memo](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-simple-expression-in-memo.md)


<!-- @source rules/rerender-split-combined-hooks.md -->

---
title: Split useMemo/useEffect when independent tasks have different dependencies; don't split tightly coupled logic
impact: MEDIUM
impactDescription: "A combined hook reruns the entire body when any dependency changes. Splitting independent tasks means each runs only when its own deps change. Don't split coupled logic into noise."
tags: [rerender, useMemo, useEffect, dependencies, optimization]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-010"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Split only when tasks are TRULY independent"
      - "Avoid splitting tightly coupled logic into noise"
      - "Compiler may handle some of these automatically"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-split-combined-hooks"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-split-combined-hooks.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-split-combined-hooks"
    quote: "A combined hook reruns all tasks when any dependency changes, even if some tasks don't use the changed value."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Split useMemo/useEffect when independent tasks have different deps

**Impact: MEDIUM — Independent tasks get independent invalidation.**

### Incorrect — combined useMemo

```tsx
const sorted = useMemo(() => {
  const filtered = products.filter((p) => p.category === category)
  return filtered.toSorted((a, b) =>
    sortOrder === 'asc' ? a.price - b.price : b.price - a.price,
  )
}, [products, category, sortOrder])   // changing sortOrder re-runs filter
```

### Correct — split by dependency boundary

```tsx
const filtered = useMemo(
  () => products.filter((p) => p.category === category),
  [products, category],
)

const sorted = useMemo(
  () => filtered.toSorted((a, b) =>
    sortOrder === 'asc' ? a.price - b.price : b.price - a.price,
  ),
  [filtered, sortOrder],
)
```

Now changing `sortOrder` only re-sorts; filter result is reused.

### Same pattern for useEffect

```tsx
// BAD: both side effects re-run when either dep changes
useEffect(() => {
  analytics.trackPageView(pathname)
  document.title = `${pageTitle} | My App`
}, [pathname, pageTitle])

// GOOD: independent invalidation
useEffect(() => analytics.trackPageView(pathname), [pathname])
useEffect(() => { document.title = `${pageTitle} | My App` }, [pageTitle])
```

### Don't split for the sake of it

If logic is **tightly coupled** — second step needs the result of the first, computed mid-render — splitting becomes noise. Profile-driven decision.

### Compiler note

React Compiler may auto-track finer dependency boundaries in compiled code. Apply this rule consciously in compiler-off projects; with compiler on, profile before adding splits.

Sources:
- [Vercel: rerender-split-combined-hooks](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-split-combined-hooks.md)


<!-- @source rules/rerender-transitions.md -->

---
title: Use startTransition for non-urgent state updates that affect rendering — not for imperative bookkeeping
impact: MEDIUM
impactDescription: "startTransition marks a state update as non-urgent, letting React keep urgent updates (typing, clicks) responsive. For imperative work (scroll position storage, mouse tracking), prefer refs / requestAnimationFrame / throttle."
tags: [rerender, transitions, startTransition, performance, concurrent]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-012"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Use for non-urgent state that affects RENDERING"
      - "Don't use for imperative scroll bookkeeping — prefer refs/throttling there"
      - "Not a perf cure-all"
  gap_check: { status: complete, note: "Vercel's scroll-position example is the wrong flagship (codex finding). Reframed example used." }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-transitions"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-transitions.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-transitions"
    quote: "Mark frequent, non-urgent state updates as transitions to maintain UI responsiveness."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  notes: "Vercel's scroll example is weak — typically refs/throttle is the right tool there."
sibling_rules: [rerender-use-deferred-value, rerender-use-ref-transient-values, rendering-usetransition-loading]
---

## Use startTransition for non-urgent state updates that affect rendering

**Impact: MEDIUM — Keeps urgent updates responsive while non-urgent work yields.**

### Use cases

- A filter UI: typing in the search box is urgent; the filtered list update is non-urgent (but it RENDERS something — list/chart updates).
- A tab switch: the active-tab indicator is urgent; the new tab's content render is non-urgent.
- Anything where one state update is interruptible by the next.

### Correct — filter state behind input

```tsx
import { startTransition, useState } from 'react'

function Search({ items }: { items: Item[] }) {
  const [query, setQuery] = useState('')
  const [filtered, setFiltered] = useState(items)

  function onChange(e: ChangeEvent<HTMLInputElement>) {
    setQuery(e.target.value)               // urgent: input must echo
    startTransition(() => {
      setFiltered(items.filter((i) => i.name.includes(e.target.value)))
    })
  }

  return (
    <>
      <input value={query} onChange={onChange} />
      <List items={filtered} />
    </>
  )
}
```

### Wrong tool — imperative bookkeeping

```tsx
// BAD: scroll position tracking. There's no render dependency on scrollY here.
// Use a ref + throttle / rAF instead.
function ScrollTracker() {
  const [scrollY, setScrollY] = useState(0)
  useEffect(() => {
    const handler = () => startTransition(() => setScrollY(window.scrollY))
    window.addEventListener('scroll', handler, { passive: true })
    return () => window.removeEventListener('scroll', handler)
  }, [])
  // ... but nothing here actually rerenders based on scrollY meaningfully
}
```

If `scrollY` doesn't drive a visible render, store it in a ref (see `rerender-use-ref-transient-values`). `startTransition` is for state that DOES drive a render but isn't the urgent one.

### Comparison

| Tool | Use case |
|---|---|
| `startTransition` | non-urgent state update that drives a render |
| `useDeferredValue` | derived render lagging behind urgent input |
| `useRef` | value that should NOT drive a render |
| Throttle / rAF | imperative imperative-throttle scroll/mouse |

### Not a perf cure-all

`startTransition` doesn't make code faster. It changes scheduling priority. If your filter is genuinely slow (sorting a million items), fix the algorithm first.

Sources:
- [Vercel: rerender-transitions](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-transitions.md)
- [React — startTransition](https://react.dev/reference/react/startTransition)


<!-- @source rules/rerender-use-deferred-value.md -->

---
title: useDeferredValue + useMemo for expensive derived renders behind urgent input — fix the algorithm first if hot
impact: MEDIUM
impactDescription: "Keeps the input snappy by letting the derived expensive render lag behind. Must wrap the expensive computation in useMemo with the deferred value as dependency — otherwise the optimization doesn't apply."
tags: [rerender, useDeferredValue, optimization, concurrent]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-013"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Always wrap the expensive computation in useMemo with the DEFERRED value"
      - "Fix algorithmic cost first for very large datasets"
      - "Don't combine with manual memo unless profiler proves it"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-use-deferred-value"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-use-deferred-value.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-use-deferred-value"
    quote: "When user input triggers expensive computations or renders, use useDeferredValue to keep the input responsive."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [rerender-transitions]
---

## useDeferredValue + useMemo for expensive derived renders behind urgent input

**Impact: MEDIUM — Snappy input + expensive derived view, without throttling.**

### Correct

```tsx
import { useDeferredValue, useMemo, useState } from 'react'

function Search({ items }: { items: Item[] }) {
  const [query, setQuery] = useState('')
  const deferredQuery = useDeferredValue(query)

  const filtered = useMemo(
    () => items.filter((it) => fuzzyMatch(it, deferredQuery)),
    [items, deferredQuery],                    // ← key: depends on DEFERRED, not query
  )

  const isStale = query !== deferredQuery

  return (
    <>
      <input value={query} onChange={(e) => setQuery(e.target.value)} />
      <div style={{ opacity: isStale ? 0.7 : 1 }}>
        <ResultsList results={filtered} />
      </div>
    </>
  )
}
```

### Critical — the useMemo dep must be the DEFERRED value

```tsx
// BAD: useMemo deps on the urgent `query` → re-runs fuzzyMatch immediately, defeating the purpose.
const filtered = useMemo(
  () => items.filter((it) => fuzzyMatch(it, query)),
  [items, query],
)
```

Wrap the expensive computation in `useMemo` AND list `deferredQuery` (or the deferred derived form) as the dependency. Otherwise React's deferred-value scheduling doesn't help.

### Show staleness if the gap is visible

`isStale = query !== deferredQuery` lets you dim/blur the result while it catches up. Common UX touch.

### Fix algorithmic cost first for very large datasets

`useDeferredValue` re-schedules work. It doesn't make work faster. For 100K+ items, build an index (Map / search index), virtualize the list, or move the work to a worker. `useDeferredValue` is a smoothing layer over already-reasonable algorithms.

### Sibling rules

- `rerender-transitions` (startTransition): use when you want to *trigger* a non-urgent update from a handler.
- `rerender-use-deferred-value` (this rule): use when a derived render is the expensive thing and you can't easily separate the trigger from the work.

Sources:
- [Vercel: rerender-use-deferred-value](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-use-deferred-value.md)
- [React — useDeferredValue](https://react.dev/reference/react/useDeferredValue)


<!-- @source rules/rerender-use-ref-transient-values.md -->

---
title: useRef for transient values that don't drive rendering; useState only for values the UI must reflect
impact: MEDIUM
impactDescription: "Updating a ref does not trigger a re-render. For mouse positions, intervals, transient flags, and any value the UI doesn't render based on, use a ref. Use state ONLY for values that should cause UI updates."
tags: [rerender, useref, state, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-014"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness: { status: complete }
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-use-ref-transient-values"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-use-ref-transient-values.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-use-ref-transient-values"
    quote: "When a value changes frequently and you don't want a re-render on every update [...] store it in useRef instead of useState."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_AS_IS }
sibling_rules: [rerender-transitions]
---

## useRef for transient values that don't drive rendering

**Impact: MEDIUM — State is for the UI; refs are for everything else.**

### Incorrect — re-renders on every mouse move

```tsx
function Tracker() {
  const [lastX, setLastX] = useState(0)
  useEffect(() => {
    const onMove = (e: MouseEvent) => setLastX(e.clientX)
    window.addEventListener('mousemove', onMove)
    return () => window.removeEventListener('mousemove', onMove)
  }, [])
  return <div style={{ position: 'fixed', left: lastX, top: 0, width: 8, height: 8, background: 'black' }} />
}
```

Re-renders 60+ times per second whenever the mouse moves.

### Correct — ref + imperative DOM update

```tsx
function Tracker() {
  const dotRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const onMove = (e: MouseEvent) => {
      if (dotRef.current) {
        dotRef.current.style.transform = `translateX(${e.clientX}px)`
      }
    }
    window.addEventListener('mousemove', onMove)
    return () => window.removeEventListener('mousemove', onMove)
  }, [])
  return (
    <div
      ref={dotRef}
      style={{ position: 'fixed', left: 0, top: 0, width: 8, height: 8, background: 'black', transform: 'translateX(0)' }}
    />
  )
}
```

Zero re-renders. The DOM is mutated directly; React doesn't see this and doesn't care.

### Decision: state vs ref

| Question | Answer → use |
|---|---|
| Does the UI render based on this value? | `useState` |
| Does it just need to be remembered? | `useRef` |
| Does it change very frequently? | usually `useRef` |
| Should it appear in dependency arrays? | usually `useState` |

### Pair with requestAnimationFrame for surfacing to UI

If the value eventually needs to appear in the UI (e.g. a debounced "last position" display), accumulate in a ref and flush to state on a rAF tick:

```tsx
useEffect(() => {
  let pending = false
  const onMove = (e: MouseEvent) => {
    lastXRef.current = e.clientX
    if (pending) return
    pending = true
    requestAnimationFrame(() => {
      pending = false
      setDisplayX(lastXRef.current)   // re-render at most once per frame
    })
  }
  // ...
}, [])
```

Sources:
- [Vercel: rerender-use-ref-transient-values](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-use-ref-transient-values.md)


<!-- @source rules/server-after-nonblocking.md -->

---
title: Use after() for best-effort post-response work (logs / analytics / cleanup) — never for critical operations
impact: MEDIUM
impactDescription: "Faster response times by deferring non-critical side effects until after the response ships. after() is best-effort; do not use as a durable queue for billing, notifications, or guaranteed side effects."
tags:
  - server
  - async
  - logging
  - analytics
  - side-effects
  - after
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-009"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) work inside after() is best-effort acceptable, (b) critical/billable work is NOT inside after() — it's awaited before response or sent to a durable queue, (c) platform timeout/cold-shutdown budget considered."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
  completeness:
    status: complete
    amendments:
      - "Added durability warning: not for critical/guaranteed side effects"
      - "Added platform max-duration / cold-shutdown caveat"
      - "Noted behavior on static prerender / revalidation"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: server-after-nonblocking"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-after-nonblocking.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "server-after-nonblocking"
    quote: "Use Next.js's after() to schedule work that should execute after a response is sent."
  - source_type: external
    citation: "Next.js docs — after() runs even if the response fails or redirects; works in Server Components, Server Actions, and Route Handlers"
    url: "https://nextjs.org/docs/app/api-reference/functions/after"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - async-api-routes
---

## Use after() for best-effort post-response work — never for critical operations

**Impact: MEDIUM — Faster response times by deferring non-critical side effects until after the response ships. `after()` is best-effort; do not use as a durable queue for billing, notifications, or guaranteed side effects.**

### Correct use cases

- Analytics / metrics events
- Audit logging where occasional loss is acceptable
- Cache warming
- Cleanup tasks
- Best-effort notifications where missing one isn't critical

### Incorrect — logging blocks the response

```tsx
export async function POST(request: Request) {
  await updateDatabase(request)

  const userAgent = request.headers.get('user-agent') || 'unknown'
  await logUserAction({ userAgent })   // adds latency the user feels

  return Response.json({ status: 'success' })
}
```

### Correct — log after response

```tsx
import { after } from 'next/server'
import { headers, cookies } from 'next/headers'

export async function POST(request: Request) {
  await updateDatabase(request)

  after(async () => {
    const ua = (await headers()).get('user-agent') ?? 'unknown'
    const session = (await cookies()).get('session-id')?.value ?? 'anonymous'
    logUserAction({ ua, session })
  })

  return Response.json({ status: 'success' })
}
```

### Durability — what after() does NOT guarantee

- The function instance may be killed shortly after the response ships. Long-running `after()` work may be truncated.
- Platform max-duration limits apply. Vercel functions have a budget; exceeding it cancels in-flight work.
- `after()` is **not** a durable queue. If the work is critical (billing, payment confirmation, transactional email), use:
  - A durable queue (SQS, BullMQ, Inngest, QStash)
  - An external scheduler (cron, Trigger.dev)
  - Await the work before responding (accept the latency)

### Behavior on static prerender / revalidation

`after()` callbacks during static generation / ISR revalidation execute as part of the build/revalidation step, not at request time. Don't rely on per-request context (cookies, headers) without checking the call site is request-time.

### Available in (per Next docs)

- Server Components
- Server Actions / Server Functions
- Route Handlers

### Anti-pattern — billing inside after()

```tsx
// BAD: if the function instance dies before this runs, the user got the
// service for free.
after(async () => {
  await chargeCustomer(orderId, amount)
})
```

Charge before responding, or send to a durable queue that the billing worker processes. `after()` is **best effort**.

Sources:

- [Vercel: server-after-nonblocking](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-after-nonblocking.md)
- [Next.js docs — after()](https://nextjs.org/docs/app/api-reference/functions/after)


<!-- @source rules/server-auth-actions.md -->

---
title: Authenticate inside every Server Action — they are public mutation endpoints
impact: CRITICAL
impactDescription: "Server Actions can be invoked directly; middleware and layout guards do not protect them. Auth + authz + input validation must happen INSIDE each action."
tags:
  - server
  - server-actions
  - authentication
  - security
  - authorization
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-003"
verification:
  type: review
  status: manual
  notes: "Reviewer checks every `'use server'` function: (a) authenticate early, (b) validate untrusted input before using it, (c) authorize ownership/role before mutation, (d) return/throw a 401-style error on failure."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "`unauthorized()` helper is experimental — requires `experimental.authInterrupts`. Use a plain return/throw without it."
  completeness:
    status: complete
    amendments:
      - "Ordering: authenticate → validate → authorize → mutate"
      - "Noted `unauthorized()` experimental gate; fallback returns/throws plain 401"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: server-auth-actions"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-auth-actions.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "server-auth-actions"
    quote: "Server Actions are exposed as public endpoints, just like API routes. Always verify authentication and authorization inside each Server Action."
  - source_type: external
    citation: "Next.js Authentication guide — treat Server Actions with same security considerations as public-facing API endpoints"
    url: "https://nextjs.org/docs/app/guides/authentication"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - async-api-routes
---

## Authenticate inside every Server Action — they are public mutation endpoints

**Impact: CRITICAL — Server Actions (`'use server'` functions) can be invoked directly by any client; middleware and layout guards do not protect them. Auth, authz, and input validation must happen INSIDE each action.**

### Order: authenticate → validate → authorize → mutate

```typescript
'use server'
import { verifySession } from '@/lib/auth'
import { z } from 'zod'

const updateProfileSchema = z.object({
  userId: z.string().uuid(),
  name: z.string().min(1).max(100),
  email: z.string().email(),
})

export async function updateProfile(input: unknown) {
  // 1. AUTHENTICATE — does this caller have a valid session?
  const session = await verifySession()
  if (!session) return { error: 'Unauthorized', status: 401 }

  // 2. VALIDATE — refuse malformed input before using it
  const data = updateProfileSchema.parse(input)

  // 3. AUTHORIZE — does this session have permission for THIS data?
  if (session.user.id !== data.userId) {
    return { error: 'Can only update own profile', status: 403 }
  }

  // 4. MUTATE
  await db.user.update({
    where: { id: data.userId },
    data: { name: data.name, email: data.email },
  })
  return { success: true }
}
```

### Incorrect — no auth, anyone on the internet can call this

```typescript
'use server'
export async function deleteUser(userId: string) {
  await db.user.delete({ where: { id: userId } })
  return { success: true }
}
```

### Notes on Next.js helpers

- `unauthorized()` from `next/navigation` is currently **experimental** — gated by `experimental.authInterrupts` in next.config. Without the flag, return or throw a plain 401-shaped result.
- Don't rely on middleware-only auth. Middleware can be bypassed by direct Server Action invocation.
- Don't rely on layout-level guards. The action is independent of the layout that imported it.

Sources:

- [Vercel: server-auth-actions](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-auth-actions.md)
- [Next.js Authentication guide](https://nextjs.org/docs/app/guides/authentication)


<!-- @source rules/server-cache-lru.md -->

---
title: Manual LRU cache for cross-request sharing — fallback when Cache Components is unavailable
impact: MEDIUM
impactDescription: "Caches data across sequential requests within a warm process instance. In Next.js 16+ prefer 'use cache' / 'use cache: remote'. LRU remains useful when Cache Components is not available, or when you deliberately want best-effort in-process caching scoped to a warm instance."
tags:
  - server
  - cache
  - lru
  - cross-request
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-004"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) Cache Components not available or explicitly inappropriate, (b) LRU is module-level with TTL, (c) cache keys don't leak across tenants/users, (d) no expectation of cross-instance coherence."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: repositioned
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Next 16 Cache Components ('use cache') is the framework path for durable caching. LRU is now a fallback, not the primary."
  completeness:
    status: complete
    amendments:
      - "Repositioned as fallback to 'use cache' / 'use cache: remote'"
      - "Added deployment matrix (Fluid Compute / serverless / multi-instance)"
      - "Warned about cross-instance non-coherence"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: server-cache-lru"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-cache-lru.md"
    role: seed
  - id: nextjs-use-cache-directive
    title: "Next.js 16 — 'use cache' (the preferred primary in Next 16+)"
    url: "https://nextjs.org/docs/app/api-reference/directives/use-cache"
    role: canonical-nextjs
evidence:
  - upstream_id: vercel-react-best-practices
    section: "server-cache-lru"
    quote: "React.cache() only works within one request. For data shared across sequential requests, use an LRU cache."
  - upstream_id: nextjs-use-cache-directive
    section: "Runtime caching considerations"
    quote: "If the default in-memory cache isn't enough, consider 'use cache: remote' which allows platforms to provide a dedicated cache handler"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - server-cache-react
  - nextjs-use-cache
---

## Manual LRU cache for cross-request sharing — fallback when Cache Components is unavailable

**Impact: MEDIUM — In Next.js 16+ prefer `'use cache'` (sibling rule). LRU remains useful when Cache Components is unavailable, or when you want best-effort in-process caching scoped to a warm instance.**

### Decision order

1. **Next.js 16+ with Cache Components**: use `'use cache'` (sibling rule `nextjs-use-cache.md`).
2. **Need durable cross-instance cache**: use `'use cache: remote'` (Redis/KV via platform handler).
3. **No Cache Components available** OR **deliberate best-effort in-process cache**: manual LRU.
4. **Need per-request dedup only**: `React.cache()` (sibling rule `server-cache-react.md`).

### Manual LRU pattern

```typescript
import { LRUCache } from 'lru-cache'

const userCache = new LRUCache<string, User>({
  max: 1000,
  ttl: 5 * 60 * 1000,  // 5 minutes
})

export async function getUser(id: string) {
  const cached = userCache.get(id)
  if (cached) return cached

  const user = await db.user.findUnique({ where: { id } })
  if (user) userCache.set(id, user)
  return user
}
```

### Deployment matrix

| Environment | LRU effectiveness |
|---|---|
| Vercel Fluid Compute / warm Node server | High — multiple concurrent requests share the same module instance |
| Traditional serverless | Low — cold starts re-execute module code; cache is empty per invocation |
| Multi-instance (e.g. multiple containers) | Low — caches are not coherent across instances |

For cross-instance coherence: Redis/Memcached (or `'use cache: remote'` in Next 16+).

### Non-coherence warning

LRU is **not coherent** across processes. If you write data in one instance and read in another, you'll get stale results. This is acceptable for:
- Reference data (rarely changes — e.g. country list, feature flags).
- "Cache miss is cheap" data (DB read with index).

It is NOT acceptable for:
- Counters / rate limits (each instance has its own count → quota bypass).
- Recently-mutated state (different instances see different states).
- Anything you need a single source of truth for.

### Cache-key safety

Tenant / user / role must be part of the cache key when caching scoped data. `cache.get('user:123')` is fine; `cache.get('settings')` for a multi-tenant app leaks settings between tenants.

Sources:

- [Vercel: server-cache-lru](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-cache-lru.md)
- [Next.js 16 — 'use cache' (preferred primary)](https://nextjs.org/docs/app/api-reference/directives/use-cache)
- [lru-cache (npm)](https://github.com/isaacs/node-lru-cache)


<!-- @source rules/server-cache-react.md -->

---
title: Use React.cache() for per-request, in-process deduplication of non-fetch server work
impact: MEDIUM
impactDescription: "Eliminates duplicate execution of expensive non-fetch async work (DB queries, auth lookups, file I/O, pure computations) across components in a single request. Narrow scope: server-only, request-scoped, in-process; not a substitute for Next.js Cache Components."
tags:
  - server
  - cache
  - react-cache
  - deduplication
  - rsc
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-001"
verification:
  type: review
  status: manual
  notes: "Static detection is unreliable: any module-scope `cache(fn)` wrapping looks correct from AST alone. Verification requires reviewer confirming (a) function defined at module level, (b) primitive args or stable references, (c) Server-Component-only usage, (d) not used inside a 'use cache' boundary (where React.cache is isolated)."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
  split_origin: "Vercel server-cache-react seed rule — split into this rule + nextjs-use-cache.md"
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Mechanic (shallow equality / Object.is / per-request invalidation / module-level definition) matches React 19 docs verbatim."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "React.cache() unchanged in React 19; narrowed scope reflects Next.js 16 caching-model evolution rather than React-side change."
  completeness:
    status: complete
    amendments:
      - "Added React 19 specifics: server-only, errors cached, separate cache per cache() call, primitives or same reference"
      - "Added boundary: do not use inside 'use cache' (Next.js) — React.cache isolated there"
      - "Removed implicit claim that React.cache() is Next.js's recommended caching primitive (it is not in Next 16)"
      - "Repositioned: per-request in-process dedup ONLY; for cross-request caching use 'use cache' directive (sibling rule)"
  gap_check:
    status: split
    note: "Cross-request/durable caching is the sibling rule nextjs-use-cache.md. Fetch memoization (Next.js built-in) is documented as a related no-op pattern but does not need its own catalog rule."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (rule: server-cache-react)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-cache-react.md"
    role: "seed"
  - id: react-19-cache
    title: "React 19 — cache() API reference"
    url: "https://react.dev/reference/react/cache"
    role: "canonical-react"
  - id: nextjs-use-cache-directive
    title: "Next.js 16 — 'use cache' directive (for boundary/isolation context)"
    url: "https://nextjs.org/docs/app/api-reference/directives/use-cache"
    role: "canonical-nextjs"
evidence:
  - upstream_id: react-19-cache
    section: "Scope"
    quote: "cache is only for use with React Server Components."
  - upstream_id: react-19-cache
    section: "Lifetime"
    quote: "React will invalidate the cache for all memoized functions for each server request."
  - upstream_id: react-19-cache
    section: "Per-call cache"
    quote: "Each call to cache creates a new function. This means that calling cache with the same function multiple times will return different memoized functions that do not share the same cache."
  - upstream_id: react-19-cache
    section: "Arguments"
    quote: "If your arguments are not primitives (ex. objects, functions, arrays), ensure you're passing the same object reference."
  - upstream_id: nextjs-use-cache-directive
    section: "React.cache isolation"
    quote: "React.cache operates in an isolated scope inside use cache boundaries. Values stored via React.cache outside a use cache function are not visible inside it."
  - upstream_id: vercel-react-best-practices
    section: "server-cache-react"
    quote: "Use React.cache() for server-side request deduplication. Authentication and database queries benefit most."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=high"
  reviewed_at: "2026-05-16"
  verdict: SPLIT
  agreements:
    - "Vercel rule mechanic is correct"
    - "Severely stale on Next.js positioning"
    - "Should split, not amend"
    - "Do not mark React.cache() deprecated"
  boundary:
    react_cache: "RSC per-request in-process dedup of non-fetch server work; module-scope definition; isolated inside 'use cache' boundaries"
    use_cache_directive: "Next.js 16 Cache Components persistent caching with cacheLife / cacheTag / updateTag"
sibling_rules:
  - nextjs-use-cache
---

## Use React.cache() for per-request, in-process deduplication of non-fetch server work

**Impact: MEDIUM — Eliminates duplicate execution of expensive non-fetch async work (DB queries, auth lookups, file I/O, pure computations) across components in a single request. Narrow scope: server-only, request-scoped, in-process; not a substitute for Next.js Cache Components.**

### Scope discipline

`React.cache()` is a request-scoped, in-process memoization primitive for **Server Components only**. It is NOT:

- a substitute for Next.js 16's `'use cache'` directive (sibling rule `nextjs-use-cache.md`),
- a persistent cache (lives one request),
- usable in Client Components (no Server context, no cache access),
- usable inside a `'use cache'` boundary (React.cache is isolated there per Next docs),
- a replacement for built-in `fetch` request memoization (Next.js dedupes identical `fetch` calls automatically).

Use it for: database client calls, auth/session lookups, expensive synchronous computations turned async, file-system reads, any non-fetch async work that may be invoked from multiple components in one request.

### Correct usage

**Module-level definition (REQUIRED):**

```tsx
// app/lib/user.ts
import { cache } from 'react'
import { db } from '@/lib/db'
import { auth } from '@/lib/auth'

// Defined ONCE at module level. Every Server Component import gets
// the SAME memoized function and therefore shares the cache.
export const getCurrentUser = cache(async () => {
  const session = await auth()
  if (!session?.user?.id) return null
  return db.user.findUnique({ where: { id: session.user.id } })
})
```

**Calling from multiple Server Components in one request:**

```tsx
// app/header.tsx
import { getCurrentUser } from '@/lib/user'

export async function Header() {
  const user = await getCurrentUser()    // First call: query runs
  return <header>Welcome, {user?.name}</header>
}

// app/sidebar.tsx
import { getCurrentUser } from '@/lib/user'

export async function Sidebar() {
  const user = await getCurrentUser()    // Same request: cache hit, no query
  return <aside>{user?.name}</aside>
}
```

### Incorrect patterns

**Defining inside a component — defeats sharing:**

```tsx
// BAD: every render creates a new cache() — siblings do not share.
export async function Profile() {
  const getUser = cache(async () => db.user.findUnique({ where: { id: 1 } }))
  return <div>{(await getUser()).name}</div>
}
```

**Inline object arguments — every call is a cache miss:**

```tsx
// BAD: { id: 1 } is a new reference every call.
const getUser = cache(async (params: { id: number }) =>
  db.user.findUnique({ where: { id: params.id } }),
)

await getUser({ id: 1 })   // miss
await getUser({ id: 1 })   // miss again — new object reference
```

```tsx
// GOOD: primitive arg uses value equality.
const getUser = cache(async (id: number) =>
  db.user.findUnique({ where: { id } }),
)

await getUser(1)   // miss
await getUser(1)   // hit
```

**Using React.cache inside a 'use cache' boundary — values do not cross:**

```tsx
// BAD: shared.current will read as null inside Child because
// React.cache is isolated inside 'use cache' scopes.
import { cache } from 'react'

const store = cache(() => ({ current: null as string | null }))

function Parent() {
  const shared = store()
  shared.current = 'set by parent'
  return <Child />
}

async function Child() {
  'use cache'
  const shared = store()
  return <div>{shared.current}</div>  // null
}
```

Pass data into a `'use cache'` scope via function arguments — see sibling rule `nextjs-use-cache.md`.

### Errors are cached too

Per React 19 docs: if the cached function throws for given arguments, the error is memoized and re-thrown on next call with same arguments — for the duration of the request. If you call `getCurrentUser()` and the DB is down, every subsequent call within that request will re-throw without re-hitting the DB. This is usually what you want; be aware of it.

### When NOT to use React.cache()

- For `fetch()` calls in Next.js — `fetch` is already request-memoized.
- For cross-request caching — use `'use cache'` (Next.js) or an external cache.
- For Client Component work — `React.cache()` does nothing client-side.
- Inside a `'use cache'` boundary — the cache is isolated there.
- For "expensive computation across renders in a Client Component" — that is `useMemo()`.

### Verification

- Static detection is unreliable. Reviewers must confirm: (a) module-scope `cache(fn)` definition, (b) primitive arguments or stable references, (c) Server-Component-only call sites, (d) not nested inside a `'use cache'` boundary.
- A targeted lint rule could check (a) (module-scope-ness of `cache(...)` calls). Not in current pilot scope.

Sources for this rule:

- [Vercel agent-skills: server-cache-react](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-cache-react.md)
- [React 19 — cache() API](https://react.dev/reference/react/cache)
- [Next.js 16 — 'use cache' (React.cache isolation note)](https://nextjs.org/docs/app/api-reference/directives/use-cache)


<!-- @source rules/server-dedup-props.md -->

---
title: Don't break RSC prop reference-dedup with sort/filter/map at the Server→Client boundary
impact: LOW
impactDescription: "RSC→Client serialization dedupes payloads by object reference. .toSorted/.filter/.map create new references and force re-serialization. For large duplicated payloads, pass canonical refs and transform in the client."
tags:
  - server
  - rsc
  - serialization
  - props
  - optimization
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-005"
verification:
  type: review
  status: manual
  notes: "Reviewer flags Client Component props where the same data is passed twice in different shapes (e.g. items + sortedItems). Confirms transformation can move client-side without harming the page."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
  completeness:
    status: complete
    amendments:
      - "Scoped to LARGE duplicated props — small ones aren't worth the rule"
      - "Warned that client transform increases client JS/work — not free"
      - "Reference-sharing primitive depends on RSC implementation details"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: server-dedup-props"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-dedup-props.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "server-dedup-props"
    quote: "RSC→client serialization deduplicates by object reference, not value. Same reference = serialized once; new reference = serialized again."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - server-serialization
---

## Don't break RSC prop reference-dedup with sort/filter/map at the Server→Client boundary

**Impact: LOW — RSC→Client serialization dedupes payloads by object reference. `.toSorted` / `.filter` / `.map` create new references and force re-serialization. For large duplicated payloads, pass canonical refs and transform in the client.**

### When this rule pays off

Only when you're passing the same data in two different shapes to a Client Component. For small arrays (< ~100 primitives, < ~20 small objects), the bytes saved are negligible — skip the rule.

### Operations that BREAK reference dedup

- Arrays: `.toSorted()`, `.filter()`, `.map()`, `.slice()`, spread `[...arr]`
- Objects: spread `{...obj}`, `Object.assign()`, `structuredClone()`, `JSON.parse(JSON.stringify())`

Each of these returns a new reference even if the values are identical.

### Incorrect — sends 6 strings for a 3-string array

```tsx
// RSC
<ClientList usernames={usernames} sorted={usernames.toSorted()} />
```

### Correct — pass once, transform client-side

```tsx
// RSC
<ClientList usernames={usernames} />
```

```tsx
// Client
'use client'
import { useMemo } from 'react'

export default function ClientList({ usernames }: { usernames: string[] }) {
  const sorted = useMemo(() => [...usernames].sort(), [usernames])
  // render
}
```

### Type matters for impact

| Prop shape | Dedup impact |
|---|---|
| `string[]` / `number[]` / `boolean[]` | HIGH — primitives fully duplicated |
| `object[]` | LOW — array structure duplicates, but nested objects still share refs |
| Single deeply-nested object | LOW — already a single reference at the top |

### Caveat — client transform isn't free

Moving the sort/filter/map to the client costs client JS and client CPU. If the data is small or the transformation is cheap, the savings of fewer bytes don't beat the cost of running the transformation on potentially thousands of devices.

Use this rule when:
- The array is large (hundreds of items).
- The transformation runs frequently on the client anyway (you'd `useMemo` it regardless).
- Network egress / RSC payload weight is the bottleneck.

### Exception — when to derive on the server anyway

- Transformation is **expensive** (sort a million items) — do once on server.
- Client **only needs the derived shape** — original is wasted bytes.
- Server runs the same transformation multiple times per session (cache it on server).

Sources:

- [Vercel: server-dedup-props](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-dedup-props.md)


<!-- @source rules/server-hoist-static-io.md -->

---
title: Hoist truly static asset I/O to module scope; never hoist request-, user-, or tenant-scoped data
impact: HIGH
impactDescription: "Module-scope I/O runs once per process instance, not per request. Reduces latency for fonts/templates/bundled config. Hoisting request-scoped data is a correctness bug."
tags:
  - server
  - io
  - performance
  - static-assets
  - module-scope
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-006"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) hoisted asset is genuinely static across all requests, all users, all tenants; (b) Edge runtime constraints respected (no fs); (c) module-level fetch doesn't bypass intended Next cache/revalidation."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
  completeness:
    status: complete
    amendments:
      - "Forbid hoisting request/user/tenant/secret-rotating data — correctness bug"
      - "Edge runtime constraints (no fs)"
      - "Module-level fetch can bypass Next cache/revalidation semantics"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: server-hoist-static-io"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-hoist-static-io.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "server-hoist-static-io"
    quote: "Module-level code runs once when the module is first imported, not on every request."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules: []
---

## Hoist truly static asset I/O to module scope; never hoist request-, user-, or tenant-scoped data

**Impact: HIGH — Module-scope I/O runs once per process instance, not per request. Reduces latency for fonts/templates/bundled config. Hoisting request-scoped data is a correctness bug.**

### When to hoist

Hoist I/O at module top level when the asset is:

- **Immutable** across all requests / users / tenants for the lifetime of the process.
- Examples: fonts for `ImageResponse`, logo bytes, bundled `template.html`, build-time generated constants, static config files.

### Correct — module-level promise; awaited per request

```typescript
// app/api/og/route.tsx
import { ImageResponse } from 'next/og'

// Module-level — runs once per process instance.
const fontDataPromise = fetch(
  new URL('./fonts/Inter.ttf', import.meta.url),
).then((r) => r.arrayBuffer())

const logoDataPromise = fetch(
  new URL('./images/logo.png', import.meta.url),
).then((r) => r.arrayBuffer())

export async function GET() {
  const [font, logo] = await Promise.all([fontDataPromise, logoDataPromise])
  return new ImageResponse(
    <div style={{ fontFamily: 'Inter' }}>
      <img src={logo as unknown as string} />
      Hello World
    </div>,
    { fonts: [{ name: 'Inter', data: new Uint8Array(font) }] },
  )
}
```

### Correct — sync `readFileSync` for tiny config

```typescript
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const config = JSON.parse(
  readFileSync(join(process.cwd(), 'config/runtime.json'), 'utf-8'),
)

export async function processRequest(data: Data) {
  return render(data, config)
}
```

### Incorrect — fetches on every request

```typescript
export async function GET() {
  const fontData = await fetch(new URL('./fonts/Inter.ttf', import.meta.url))
    .then((r) => r.arrayBuffer())
  // ...
}
```

### Forbidden hoist — these are NOT static

| Data | Why not |
|---|---|
| Current user / session | Request-scoped |
| Tenant config | Per-tenant, mutates per workspace |
| Rotating secrets (JWT keys, API tokens) | Must re-read on rotation |
| Feature flags that change live | Hoisting freezes them at process boot |
| Request cookies / headers | Request-scoped |
| Database queries against user data | Request-scoped |

Hoisting any of these creates one of: tenant data leak, stale secrets, can't roll out flags, security incident.

### Edge runtime caveat

`node:fs` doesn't exist on Edge. For Edge-targeted routes, use `import.meta.url` + `fetch` patterns or import the asset directly via bundler (e.g. `import font from './Inter.ttf'`).

### Module-level fetch and Next cache

A module-level `fetch(...)` is NOT wrapped by Next's per-request cache/revalidation pipeline. If you intended `next: { revalidate: 60 }` semantics, module hoisting bypasses it. Either:
- Hoist only truly immutable assets that don't need revalidation.
- Keep request-scoped fetch in the route and use Next's caching options.
- Use Cache Components (`'use cache'`) for managed cached fetches.

### Deployment

| Runtime | Effect of module-level hoist |
|---|---|
| Long-running Node (incl. Vercel Fluid Compute) | Loaded once at boot; shared across requests. Max gain. |
| Traditional serverless cold starts | Re-loaded per cold start; reused per warm invocation. Partial gain. |

Sources:

- [Vercel: server-hoist-static-io](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-hoist-static-io.md)


<!-- @source rules/server-parallel-fetching.md -->

---
title: Parallelize Server Component fetches via composition, Promise.all, or Suspense streaming
impact: CRITICAL
impactDescription: "Server Components render top-down; sequential awaits create whole-route waterfalls. Three patterns to parallelize: sibling async children, slot/composition with Suspense, Promise.all inside one component."
tags:
  - server
  - rsc
  - parallel-fetching
  - composition
  - waterfalls
applicable_to:
  - nextjs
  - react
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-008"
verification:
  type: review
  status: manual
  notes: "Reviewer checks each route's Server Component tree: (a) sequential awaits in the same component are only for genuinely dependent data, (b) sibling sub-components are independent and can render in parallel, (c) Suspense boundaries wrap regions that legitimately need to wait."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
  completeness:
    status: complete
    amendments:
      - "Listed 3 patterns: sibling composition, slot/children + Suspense, Promise.all in one component"
      - "Caveat: dependent fetches stay sequential; not every waterfall is wrong"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: server-parallel-fetching"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-parallel-fetching.md"
    role: seed
  - id: nextjs-fetching-data
    title: "Next.js 16 — Fetching Data (parallel fetching)"
    url: "https://nextjs.org/docs/app/getting-started/fetching-data"
    role: canonical-nextjs
evidence:
  - upstream_id: vercel-react-best-practices
    section: "server-parallel-fetching"
    quote: "React Server Components execute sequentially within a tree. Restructure with composition to parallelize data fetching."
  - upstream_id: nextjs-fetching-data
    section: "Parallel data fetching"
    quote: "By default, layouts and pages are rendered in parallel. So each segment starts fetching data as soon as possible."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - async-parallel
  - async-suspense-boundaries
---

## Parallelize Server Component fetches via composition, Promise.all, or Suspense streaming

**Impact: CRITICAL — Server Components render top-down. Sequential awaits in a parent component create waterfalls that block the whole route.**

### Pattern 1 — Sibling async children (parallel by default)

```tsx
async function Header() {
  const data = await fetchHeader()
  return <div>{data.title}</div>
}

async function Sidebar() {
  const items = await fetchSidebarItems()
  return <nav>{items.map(renderItem)}</nav>
}

export default function Page() {
  return (
    <>
      <Header />
      <Sidebar />
    </>
  )
}
```

`Page` itself is not `async`. `Header` and `Sidebar` are siblings — their awaits run concurrently.

### Pattern 2 — Slot / children composition with Suspense

```tsx
function Layout({ children }: { children: ReactNode }) {
  return (
    <>
      <Header />
      <Suspense fallback={<SidebarSkeleton />}>{children}</Suspense>
    </>
  )
}

export default function Page() {
  return (
    <Layout>
      <Sidebar />
    </Layout>
  )
}
```

Header renders immediately; Sidebar streams in when ready. Children-slot composition gives the Layout author no control over whether children awaits — that's the point.

### Pattern 3 — Same component, Promise.all

```tsx
async function Page() {
  const [user, posts] = await Promise.all([
    fetchUser(userId),
    fetchPosts(userId),  // independent — doesn't need user data
  ])
  return <Dashboard user={user} posts={posts} />
}
```

When the parallel fetches are conceptually one component's job (and they're independent), `Promise.all` inside the component is the simplest form.

### Incorrect — Page awaits, blocking its children

```tsx
export default async function Page() {
  const header = await fetchHeader()
  return (
    <div>
      <div>{header.title}</div>
      <Sidebar />     {/* Sidebar's fetch waits for header */}
    </div>
  )
}

async function Sidebar() {
  const items = await fetchSidebarItems()
  // ...
}
```

`Sidebar`'s fetch only starts after `fetchHeader` resolves and `Page` returns. The waterfall is invisible in the source — Server Components are sequential by default within a single component body.

### Caveat — dependent fetches MUST stay sequential

```tsx
async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params
  const user = await fetchUser(id)
  const posts = await fetchPosts(user.workspaceId)  // depends on user
  // sequential — correct
}
```

If the second fetch genuinely needs the first's output, sequential is the right shape. For PARTIAL dependencies, see sibling rule `async-dependencies` (promise chain + Promise.all).

### Cross-rule scope

- `async-parallel` — generic init-early-await-late within one function.
- `async-suspense-boundaries` — when to stream UI for slow regions.
- `server-parallel-fetching` (this rule) — how to restructure Server Component TREES to parallelize.

Sources:

- [Vercel: server-parallel-fetching](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-parallel-fetching.md)
- [Next.js 16 — Fetching Data](https://nextjs.org/docs/app/getting-started/fetching-data)


<!-- @source rules/server-serialization.md -->

---
title: Pass minimal client DTOs across the RSC→Client boundary — never whole server entities
impact: HIGH
impactDescription: "RSC props are serialized into the RSC stream/payload sent to the browser. Passing whole entities (full user/order/product objects) bloats the payload and may leak secrets, internal IDs, and role metadata."
tags:
  - server
  - rsc
  - serialization
  - props
  - security
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-007"
verification:
  type: review
  status: manual
  notes: "Reviewer checks every Client Component prop crossing RSC boundary: (a) shape is a project-defined DTO, not an ORM/session entity, (b) no internal IDs / secrets / role flags / audit fields included that the client doesn't need."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
  completeness:
    status: complete
    amendments:
      - "Phrased as RSC stream/payload (transport-agnostic)"
      - "Added security framing: secrets, internal IDs, role metadata leak risk"
      - "Suggested DTO/view-model pattern"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: server-serialization"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-serialization.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "server-serialization"
    quote: "Only pass fields that the client actually uses."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - server-dedup-props
---

## Pass minimal client DTOs across the RSC→Client boundary — never whole server entities

**Impact: HIGH — RSC props are serialized into the RSC stream/payload sent to the browser. Whole-entity props bloat the payload AND can leak secrets, internal IDs, role flags, and audit metadata.**

### Incorrect — 50-field user entity, client uses one field

```tsx
async function Page() {
  const user = await fetchUser()  // returns 50 fields incl. hashedPassword
  return <Profile user={user} />
}
```

```tsx
'use client'
function Profile({ user }: { user: User }) {
  return <div>{user.name}</div>
}
```

Two bugs:
1. **Payload weight**: 49 unused fields shipped to the browser.
2. **Security leak**: `hashedPassword`, `mfaSecret`, `internalRoleId`, `auditLog`, `lastFailedLoginIp` — every field on the entity is now in the page source.

### Correct — explicit client DTO

```tsx
// app/types/client-dtos.ts
export type ClientUserDTO = { name: string; avatarUrl: string }

// Server Component
async function Page() {
  const user = await fetchUser()
  const dto: ClientUserDTO = { name: user.name, avatarUrl: user.avatarUrl }
  return <Profile user={dto} />
}
```

```tsx
'use client'
function Profile({ user }: { user: ClientUserDTO }) {
  return <div>{user.name}</div>
}
```

### Forbidden field categories

Never serialize across RSC→Client:
- Auth secrets: password hashes, MFA secrets, session tokens, API keys
- Role/permission internals: full role objects, ACL trees, internal IDs
- PII not displayed: SSN, government IDs, full DOB, full address (unless the component renders it)
- Audit metadata: created_by, internal_notes, soft_delete_metadata
- Other users' data: even if "the API returned it", filter it before passing

### Pattern — DTOs near the boundary

Define DTOs in a dedicated module (`app/types/client-dtos.ts` or per-feature `dto.ts`). The DTO is a contract: anything not on the type doesn't cross the boundary.

```typescript
// Mapping helper
export function toClientUserDTO(user: ServerUser): ClientUserDTO {
  return { name: user.name, avatarUrl: user.avatarUrl }
}
```

Then in the Server Component: `<Profile user={toClientUserDTO(user)} />`. The mapping function is the audit point — review changes there see security implications obviously.

### Related rule

Sibling rule `server-dedup-props` covers the (lower-impact) case of passing the same data in two shapes; this rule covers the (higher-impact) case of passing too much data in one shape.

Sources:

- [Vercel: server-serialization](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-serialization.md)


<!-- @source rules/traceid-propagated-client.md -->

---
title: "Server Actions must include traceId in error responses so the client can correlate failures with server logs"
rule_id: traceid-propagated-client
impact: HIGH
impactDescription: "Without traceId in the Server Action error response, the client has no correlation handle — users cannot provide support teams with the information needed to find the server log"
tags:
  - tracing
  - server-actions
  - observability
  - error-handling
  - nextjs
applicable_to:
  - nextjs
provenance_class: internal_design
protects_template_id: templates/L4/
failing_fixture_path: practices/evals/fixtures/traceid-propagated-client/fail_no_traceid/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-003"
verification:
  type: review
  status: manual
  notes: "All Server Action return types must include a traceId field. The error branch must populate it from headers().get('x-trace-id') or crypto.randomUUID(). The success branch may omit traceId or include it for full observability."
evidence:
  - upstream_id: nextjs-server-actions-16
    section: "Server Actions — error handling and return types"
    quote: "Server Actions can return serializable values"
  - source_type: external
    citation: "W3C Trace Context — trace-id as a correlation identifier propagated across service boundaries including browser-to-server calls"
    url: "https://www.w3.org/TR/trace-context/#trace-id"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Next.js documentation — Server Actions error handling: return a result object with error field so callers can handle failures gracefully"
    url: "https://nextjs.org/docs/app/building-your-application/data-fetching/server-actions-and-mutations#error-handling"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Server Actions must include `traceId` in error responses so the client can correlate failures with server logs

**Impact: HIGH — The client receives a generic error message but has no handle to find the server log. The `traceId` closes this loop: the error UI can display "Error ref: \<traceId\>" and support can pull the exact server log line.**

This rule is the client-side counterpart to `traceid-in-error-response.md` in `practices/rules/`. Both sides of the request lifecycle must propagate the trace ID: the backend sets it in `ProblemDetail.traceId`; the Server Action sets it in the return value's `traceId` field.

### The violation — error returned without traceId

```typescript
// ❌ WRONG — Server Action returns error without traceId
"use server";

interface LoginResult {
  success: boolean;
  error?: string;
  // MISSING: traceId — client UI has no correlation handle
}

export async function loginAction(formData: FormData): Promise<LoginResult> {
  try {
    await authenticate(formData);
    return { success: true };
  } catch (err) {
    // VIOLATION: error returned without traceId
    return { success: false, error: "Authentication failed" };
  }
}
```

### Correct — traceId propagated from request headers

```typescript
// ✅ CORRECT — traceId sourced from incoming request headers
"use server";
import { headers } from "next/headers";

interface LoginResult {
  success: boolean;
  error?: string;
  traceId?: string; // always present — client can display "Error ref: <traceId>"
}

export async function loginAction(formData: FormData): Promise<LoginResult> {
  const traceId = (await headers()).get("x-trace-id") ?? crypto.randomUUID();
  try {
    await authenticate(formData);
    return { success: true, traceId };
  } catch (err) {
    // CORRECT: traceId in error branch for client correlation
    return { success: false, error: "Authentication failed", traceId };
  }
}
```

### Client error UI displays traceId

```typescript
// Error boundary or form error display
if (!result.success) {
  toast.error(`Login failed. Reference: ${result.traceId}`);
}
```

### Why this rule exists

Without `traceId`:
- User sees "Authentication failed" but can provide no correlation data to support.
- Support team must search logs by approximate timestamp — unreliable with concurrent users.

With `traceId`:
- User quotes "Error ref: a1b2c3d4" to support.
- Support finds the exact structured log entry and root cause in seconds.

The trace ID comes from `x-trace-id` header (populated by the `TraceIdFilter` on the Java backend or by Next.js middleware). When absent, `crypto.randomUUID()` generates a client-side ID that at least identifies the specific invocation.

Pairs with: `traceid-in-error-response.md` (Java backend `@ExceptionHandler` counterpart).

Reference: [W3C Trace Context — trace-id propagation](https://www.w3.org/TR/trace-context/#trace-id)

Reference: [Next.js Server Actions — error handling](https://nextjs.org/docs/app/building-your-application/data-fetching/server-actions-and-mutations#error-handling)


<!-- @source rules/virtualized-table-when-rowcount-gt-1000.md -->

---
title: "DataTable with more than 1000 rows must use VirtualizedTable"
rule_id: virtualized-table-when-rowcount-gt-1000
impact: HIGH
impactDescription: "Rendering >1000 DOM rows simultaneously causes INP > 500ms and Time to Interactive > 3s on mid-range devices; VirtualizedTable keeps the live DOM under ~30 rows regardless of dataset size"
tags:
  - performance
  - datatable
  - virtualization
  - cwv
  - l2-block
applicable_to:
  - react
  - nextjs
provenance_class: internal_design
protects_template_id: templates/L2/blocks/virtualized-table.tsx
failing_fixture_path: practices/evals/fixtures/virtualized-table-when-rowcount-gt-1000/fail_plain_datatable/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-001"
verification:
  type: review
  status: manual
  notes: "Any component that passes data.length > 1000 to DataTable must be refactored to use VirtualizedTable. The threshold is 1000 rows; if the data can theoretically exceed 1000 rows (e.g., paginated query with large page size or a CSV import flow), use VirtualizedTable proactively."
evidence:
  - upstream_id: tanstack-virtual-2026-05
    section: "TanStack Virtual — virtualizing large lists"
    quote: "virtualizing"
  - upstream_id: cwv-2026
    section: "INP — Interaction to Next Paint threshold"
    quote: "INP"
  - source_type: external
    citation: "web.dev Core Web Vitals — INP: rendering more than ~1000 DOM nodes in a single interaction causes INP to exceed the 200ms good threshold on mid-range Android devices"
    url: "https://web.dev/articles/inp"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "TanStack Virtual documentation — Row virtualization: only renders rows visible in the viewport, reducing DOM nodes from N to ~20-30 regardless of dataset size"
    url: "https://tanstack.com/virtual/latest/docs/introduction"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## DataTable with more than 1000 rows must use VirtualizedTable

**Impact: HIGH — Rendering >1000 `<tr>` elements at once causes layout thrashing and INP > 500ms. VirtualizedTable keeps ~20-30 live DOM rows via TanStack Virtual regardless of dataset size.**

### The violation — plain DataTable with large dataset

```typescript
// ❌ WRONG — 5000 rows in a plain DataTable = 5000 <tr> in the DOM
"use client";
import { DataTable } from "templates/L2/blocks/data-table";

const MOCK_PRODUCTS = generateMockProducts(5000);

export default function ProductsPage() {
  // VIOLATION: INP > 500ms, Time to Interactive > 3s on mid-range devices
  return (
    <DataTable
      data={MOCK_PRODUCTS}    // 5000 DOM rows — all rendered simultaneously
      columns={PRODUCT_COLUMNS}
      getRowKey={(row) => row.id}
    />
  );
}
```

### Correct — VirtualizedTable for large datasets

```typescript
// ✅ CORRECT — only ~20 visible rows in the DOM at any time
"use client";
import { VirtualizedTable } from "templates/L2/blocks/virtualized-table";

const MOCK_PRODUCTS = generateMockProducts(5000);

export default function ProductsPage() {
  return (
    <VirtualizedTable
      data={MOCK_PRODUCTS}     // 5000 rows loaded in memory, ~20-30 rendered
      estimateSize={() => 48}  // estimated row height for virtual scroll math
      columns={PRODUCT_COLUMNS}
      getRowKey={(row) => row.id}
    />
  );
}
```

### Row count decision matrix

| Row count | Component | Rationale |
|---|---|---|
| < 100 | `DataTable` | DOM cost negligible |
| 100 – 1000 | `DataTable` | Acceptable for desktop; monitor INP |
| > 1000 | `VirtualizedTable` | **Required** — INP risk exceeds 200ms threshold |
| Unknown / unbounded | `VirtualizedTable` | Defensive: server pagination may be bypassed |

### Why this rule exists

SP15 delivered `VirtualizedTable` (backed by `@tanstack/react-virtual`) exactly for this scenario. The threshold of 1000 rows is based on profiling on Moto G4-class devices: rendering 1000+ `<tr>` elements in a single paint causes DOM layout time > 200ms, pushing INP into the "needs improvement" band.

The `DataTable` component is appropriate for:
- Admin tables with server-side pagination (< 100 visible rows)
- Detail views, comparison tables, form result tables

The `VirtualizedTable` component is required for:
- Product catalogs, user lists, log viewers, audit trails (large unbounded datasets)
- Any table where the full dataset might be loaded client-side (export preview, CSV import review)

Reference: [web.dev — INP (Interaction to Next Paint)](https://web.dev/articles/inp)

Reference: [TanStack Virtual — Introduction](https://tanstack.com/virtual/latest/docs/introduction)


