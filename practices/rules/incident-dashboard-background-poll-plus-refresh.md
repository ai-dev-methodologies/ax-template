---
title: Incident dashboards MUST poll in background AND expose a manual Refresh control with "last updated" timestamp
impact: MEDIUM
impactDescription: "TanStack Query default pauses polling when a tab is backgrounded — SRE second-monitor incident views silently stale, causing mis-assessed urgency during pager response"
tags:
  - incident-response
  - sre
  - tanstack-query
  - background-poll
  - data-freshness
spec_ref: "specs/scheduled-task-l0.yaml#SCHED-EXECUTE-001"
verification:
  source: "templates/L4/webhook/app/(admin)/webhooks/deliveries/page.tsx, templates/L4/scheduled-task/app/(admin)/scheduled-tasks/[id]/page.tsx"
  pattern: "useQuery with refetchInterval + refetchIntervalInBackground:true + visible dataUpdatedAt timestamp + manual Refresh button"
upstream:
  - "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
  - "https://developer.mozilla.org/en-US/docs/Web/API/Page_Visibility_API"
evidence:
  - source_type: external
    citation: "TanStack Query v5 — useQuery options (refetchIntervalInBackground)"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
    quote: "refetchIntervalInBackground: boolean — If set to true, queries that are set to continuously refetch with a refetchInterval will continue to refetch while their tab is in the background."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "MDN Web Docs — Page Visibility API"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/Page_Visibility_API"
    quote: "When the user navigates to a different tab or minimizes the browser containing the tab with the page, the API sends a visibilitychange event to listeners. ... browsers tend to throttle setTimeout and setInterval calls when the page is hidden."
    quoted_at: "2026-05-25"
---

## Incident dashboards MUST poll in background AND expose a manual Refresh control with "last updated" timestamp

**Impact: MEDIUM — a stale dashboard during pager response leads to wrong urgency assessment**

Webhook deliveries, scheduled-task history, activity-feed inbox, approval-workflow inbox, file-storage virus-scan queue, billing-event ledger — all incident-bearing surfaces share the same usage pattern: an SRE / on-call leaves the dashboard open on a secondary monitor during pager rotation. When the tab goes to background (browser switches to another window, screen locks, OS suspends inactive tabs), TanStack Query's default behavior pauses `refetchInterval` polling. When the SRE switches back, the data shows the state from when the tab was last focused — not the current state.

Mis-assessed urgency follows. An SRE sees DEAD_LETTER count at 3 (stale), responds at low urgency, while the live count is at 47. The SRE files a low-priority ticket; the actual incident is severe.

The fix has two parts:
1. **Poll continues in background** — `refetchIntervalInBackground: true` overrides the default-pause behavior. SRE on second monitor or pager-rotating across multiple incident dashboards sees fresh data without needing to refocus each tab.
2. **Visible "last updated" timestamp + manual Refresh button** — even with (1), network blips, server-side rate-limiting, or query-error retries can leave the data older than the polling cadence. A "Updated 14:32:18" indicator next to a Refresh button lets the SRE confirm staleness explicitly and force a fresh fetch when the auto-poll lags.

**Incorrect — default polling pauses in background; no staleness indicator:**

```tsx
const { data, error, isLoading } = useQuery({
  queryKey: ['webhook-deliveries'],
  queryFn: fetchDeliveries,
  refetchInterval: 10_000,
})
```

The SRE puts this on a second monitor at 14:00. At 14:15 they switch back. The data they see is from 14:01 (the moment of last focus before the browser backgrounded the tab). The DEAD_LETTER count looks normal — but the live count is much worse.

**Correct — background polling continues + Refresh + staleness timestamp:**

```tsx
const { data, error, isLoading, dataUpdatedAt, refetch } = useQuery({
  queryKey: ['webhook-deliveries'],
  queryFn: fetchDeliveries,
  refetchInterval: 10_000,
  refetchIntervalInBackground: true,
})

// In the header, alongside filters:
<span className="text-xs text-muted-foreground" aria-live="polite">
  {dataUpdatedAt ? `Updated ${new Date(dataUpdatedAt).toLocaleTimeString()}` : ''}
</span>
<button
  type="button"
  className="rounded border px-2 py-1 text-xs hover:bg-muted"
  onClick={() => refetch()}
>
  Refresh
</button>
```

**Apply this rule to**: any frontend surface that satisfies all three:
- Status data transitions during expected lifecycle (PENDING → IN_FLIGHT → SUCCEEDED / FAILED, ENABLED → DISABLED, queued → dispatched → ack'd)
- Used during incident response (failure triage, manual intervention, postmortem)
- Likely viewed on a second monitor or in a browser tab the operator does not actively focus on every minute

**When NOT to apply**: user-driven CRUD surfaces (a comment thread, a tag library, a favorite list) where the operator's own action is what triggers the next render and staleness does not change incident outcome.

A pair-with rule: when the dashboard surfaces server-supplied error strings (`lastError`, `errorMessage`), apply `stored-server-error-sanitize-at-render-layer` so a screen-shared incident bridge does not leak PII / internal hostnames via the same surface this rule keeps fresh.

Reference: [TanStack Query v5 — useQuery API](https://tanstack.com/query/latest/docs/framework/react/reference/useQuery)

Reference: [MDN — Page Visibility API](https://developer.mozilla.org/en-US/docs/Web/API/Page_Visibility_API)
