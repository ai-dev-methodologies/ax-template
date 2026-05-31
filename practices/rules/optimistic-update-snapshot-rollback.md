---
title: Optimistic update MUST snapshot-and-rollback — never invalidate-only
impact: MEDIUM
impactDescription: "Invalidate-only patterns leave UI lagging one network round-trip behind every action; without rollback, transient failures leave the cache stuck in the wrong state"
tags:
  - tanstack-query
  - optimistic-update
  - mutation
  - cache-coherence
spec_ref: "specs/activity-feed-l0.yaml#ACT-MARK-001"
verification:
  type: review
  source: "templates/L4/favorites-bookmarks/app/favorite-toggle.tsx, templates/L4/favorites-bookmarks/app/(favorites)/page.tsx"
  pattern: "onMutate snapshot + setQueryData optimistic write + onError ctx.previous rollback + onSettled invalidate"
upstream:
  - "https://tanstack.com/query/latest/docs/framework/react/guides/optimistic-updates"
  - "https://tanstack.com/query/latest/docs/framework/react/guides/mutations"
evidence:
  - source_type: external
    citation: "TanStack Query v5 — Optimistic Updates via the Cache"
    url: "https://tanstack.com/query/latest/docs/framework/react/guides/optimistic-updates"
    quote: "When we want to optimistically update some state before the mutation is completed, we can use the onMutate option. ... The data returned from onMutate is passed to the onError handler so it can be used to undo the optimistic update."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "TanStack Query v5 — useMutation API"
    url: "https://tanstack.com/query/latest/docs/framework/react/guides/mutations"
    quote: "onError, retry, retryDelay, scope: { id }, onMutate(variables): ... — A function that fires before the mutation function is fired. Useful to perform optimistic updates to a resource in hopes that the mutation succeeds."
    quoted_at: "2026-05-25"
---

## Optimistic update MUST snapshot-and-rollback — never invalidate-only

**Impact: MEDIUM — invalidate-only is a lie about latency; un-rolled-back failure is a worse lie about state**

The simplest pattern for a mutation in TanStack Query is `onSuccess: () => qc.invalidateQueries(queryKey)`. It is correct but slow: the UI does not change until the invalidated query refetches over the network. For mutations whose user-perceived correctness depends on immediate visual feedback (toggles, removes, marks-as-read), this latency is unacceptable — and the in-flight window introduces a fresh race:

> Click 1 fires. Cache still says `favorited: false`. UI shows ☆. Mutation in flight.
> Click 2 within the RTT reads cache `{ favorited: false }` and fires *another* add. Duplicate-key on add, or harmless redundant DELETE on remove.

The correct pattern has four parts, all required, in this order:
1. **onMutate** — cancel any in-flight refetch of the affected key; snapshot the current cache value as a return context; write the optimistic new value into the cache.
2. **The mutation reads its decision from variables, not the cache.** Either the caller passes the direction explicitly (`mutate({ direction: 'add' })`) or onMutate captures it into the returned context before flipping. Do *not* re-read `data?.favorited` inside mutationFn — that read is what the snapshot was meant to replace.
3. **onError** — restore the snapshot from the context. Cache returns to backend truth.
4. **onSettled** — invalidate the affected key (and any cross-query family that mirrors the same state) so the next refetch reconciles against the backend.

This pattern combines latency reduction (cache flips at onMutate) with truth preservation (snapshot restoration on failure) and cross-query coherence (family-key invalidation at onSettled).

**Incorrect — invalidate-only with cache read inside mutationFn:**

```tsx
const toggle = useMutation({
  mutationFn: async () => {
    // ❌ reads cache mid-mutation — second rapid click re-reads stale value
    if (data?.favorited) await removeFavorite(...)
    else await addFavorite(...)
  },
  onSuccess: () => qc.invalidateQueries({ queryKey }),
})
```

**Correct — onMutate snapshot + direction variables + onError rollback + onSettled invalidate:**

```tsx
type Direction = 'add' | 'remove'

const toggle = useMutation({
  mutationFn: async (direction: Direction) => {
    if (direction === 'remove') await removeFavorite(...)
    else await addFavorite(...)
  },
  onMutate: async (direction) => {
    await qc.cancelQueries({ queryKey })
    const previous = qc.getQueryData<CheckResponse>(queryKey)
    qc.setQueryData<CheckResponse>(queryKey, { favorited: direction === 'add' })
    return { previous }
  },
  onError: (_err, _direction, ctx) => {
    if (ctx?.previous) qc.setQueryData(queryKey, ctx.previous)
    qc.invalidateQueries({ queryKey: ['related-list'] })  // cross-query coherence on error
  },
  onSettled: () => {
    qc.invalidateQueries({ queryKey })
    qc.invalidateQueries({ queryKey: ['related-list'] })  // family-key invalidate
  },
})

// At the click site: snapshot direction from current cache, pass into mutate:
<button onClick={() => {
  if (busy) return
  toggle.mutate(favorited ? 'remove' : 'add')
}} />
```

For a list-removal pattern (remove a row from a paginated list), the snapshot+rollback target is the list query data, and the optimistic write is a `filter()` over `items`:

```tsx
onMutate: async ({ entityType, entityId }) => {
  await qc.cancelQueries({ queryKey: ['list'] })
  const previous = qc.getQueryData<ListResponse>(['list'])
  qc.setQueryData<ListResponse>(['list'], (old) =>
    old ? { ...old, items: old.items.filter((it) => !(it.entityType === entityType && it.entityId === entityId)) } : old
  )
  return { previous }
},
onError: (_err, _vars, ctx) => {
  if (ctx?.previous) qc.setQueryData(['list'], ctx.previous)
},
onSettled: () => qc.invalidateQueries({ queryKey: ['list'] }),
```

The "no fabricated timestamps" rule (`client-must-not-fabricate-audit-timestamps`) pairs with this one: when the optimistic state includes an audit timestamp, hold the pending state in a component-local typed Set rather than writing a synthetic timestamp into the cache. The cache should only ever carry backend truth or null.

Reference: [TanStack Query v5 — Optimistic Updates](https://tanstack.com/query/latest/docs/framework/react/guides/optimistic-updates)

Reference: [TanStack Query v5 — Mutations](https://tanstack.com/query/latest/docs/framework/react/guides/mutations)
