---
title: Background-polled pages MUST expose dataUpdatedAt + aria-busy on mutations
impact: HIGH
impactDescription: "Any TanStack Query useQuery with refetchInterval that hides the dataUpdatedAt timestamp gives operators a confidently-stale view; mutations on the same page without aria-busy break WCAG SC 4.1.3 status-message expectations for screen readers tracking the operation outcome."
tags:
  - accessibility
  - wcag
  - tanstack-query
  - background-poll
  - data-freshness
  - aria-busy
spec_ref: "specs/scheduled-task-l0.yaml#SCHED-EXECUTE-001"
verification:
  guard: background_poll_refresh_state_guard.sh
  source: "practices/evals/background_poll_refresh_state_guard.sh (R82b — 44th hard guard)"
  pattern: "useQuery({ refetchInterval, ... }) — every match MUST be paired with a dataUpdatedAt reference in the same React function body; every mutation button on the same page MUST set aria-busy until settled"
upstream:
  - "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
  - "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
evidence:
  - source_type: external
    citation: "TanStack Query v5 — useQuery options (refetchInterval / dataUpdatedAt)"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
    quote: "refetchInterval: number | false | ((query: Query) => number | false | undefined) — If set to a number, all queries will continuously refetch at this frequency in milliseconds"
    quoted_at: "2026-05-26"
  - upstream_id: wcag-22-techniques-2026-05
    section: "SC 4.1.3 Status Messages (Level AA)"
    quote: "status messages can be programmatically"
---

## Background-polled pages MUST expose dataUpdatedAt + aria-busy on mutations

**Impact: HIGH — operators trust a stale dashboard; screen reader users miss mutation-result status.**

R50 (`incident-dashboard-background-poll-plus-refresh`) introduced the "poll-in-background + visible last-updated timestamp + Refresh button" trio for incident dashboards. R82 generalises the requirement to **every** background-polled page, not only incident dashboards, and adds the partner requirement for any mutation triggered from that page:

1. **`dataUpdatedAt` MUST be visible.** Any `useQuery({...refetchInterval: <ms>...})` MUST destructure `dataUpdatedAt` and render it in the page header (or an equivalent always-visible region). A timestamp the operator can read silently confirms the polling cadence; without it, a network blip or query-error retry can leave the data older than the polling interval, and the operator has no way to notice.

2. **Mutations triggered from the page MUST set `aria-busy="true"` on the trigger button until the mutation settles.** Screen-reader users who initiate a mutation (Retry, Approve, Revoke, Force-Cancel) on a background-polled page would otherwise hear only the polled-query updates and the mutation status would be silent. WCAG SC 4.1.3 Status Messages requires programmatic status conveyance; `aria-busy` is the canonical mechanism for "operation in flight" on the triggering control.

This is the bridge between TanStack Query's freshness model and WCAG 2.2's status-messages requirement. R50 covers the freshness side for the SRE persona; R82 covers the screen-reader / mutation-status side for the accessibility persona.

**Incorrect — refetchInterval without visible dataUpdatedAt, mutation button without aria-busy:**

```tsx
const { data } = useQuery({
  queryKey: ['retry-queue'],
  queryFn: fetchQueue,
  refetchInterval: 5_000,
})

const retryMutation = useMutation({ mutationFn: retryRow })

<button onClick={() => retryMutation.mutate(row.id)}>
  Retry
</button>
```

The operator cannot tell whether the visible PENDING count is 5 s old or 5 min old. A screen-reader user clicks Retry and hears nothing for the duration of the request — the next thing they hear is the polled refetch result, which may or may not reflect their click.

**Correct — dataUpdatedAt rendered, aria-busy reflects the mutation lifecycle:**

```tsx
const { data, dataUpdatedAt, refetch } = useQuery({
  queryKey: ['retry-queue'],
  queryFn: fetchQueue,
  refetchInterval: 5_000,
  refetchIntervalInBackground: true,
})

const retryMutation = useMutation({ mutationFn: retryRow })

<header className="flex items-center gap-2">
  <span aria-live="polite" className="text-xs">
    {dataUpdatedAt ? `Updated ${new Date(dataUpdatedAt).toLocaleTimeString()}` : ''}
  </span>
  <button onClick={() => refetch()} className="text-xs">Refresh</button>
</header>

<button
  type="button"
  aria-busy={retryMutation.isPending || undefined}
  aria-disabled={retryMutation.isPending || undefined}
  onClick={() => { if (retryMutation.isPending) return; retryMutation.mutate(row.id); }}
>
  {retryMutation.isPending ? 'Retrying…' : 'Retry'}
</button>
```

**Apply this rule to**: any page OR composable L2 block that uses `useQuery` with a numeric `refetchInterval` AND issues at least one `useMutation` triggered by a user-facing control on the same surface. The R51 email-outbox admin page already satisfies the pattern; R55 favorites (no refetchInterval) is out of scope. R82-iter4 (2026-05-27) extended the mechanical guard to scan `templates/L2/blocks/*.tsx` for composable polling blocks — notification-bell (read-only) and notification-list (poll + mutate) both adopted under the iter4 extension.

**When NOT to apply**: pages with `useQuery` but no `refetchInterval` (e.g. one-shot loads, manual-refetch surfaces). The freshness signal is the operator's own re-fetch, so a visible timestamp adds noise rather than safety.

A pair-with rule: when the polled data renders server-supplied error strings, apply `server-side-stored-error-sanitize` (R61) at the storage boundary so a screen-shared incident bridge cannot leak PII through the same surface this rule keeps fresh.

Reference: [TanStack Query v5 — useQuery API](https://tanstack.com/query/latest/docs/framework/react/reference/useQuery)

Reference: [WCAG 2.2 — Understanding Success Criterion 4.1.3: Status Messages](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html)
