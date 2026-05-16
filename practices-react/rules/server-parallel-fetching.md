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
