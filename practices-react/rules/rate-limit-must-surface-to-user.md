---
title: "A client surface that receives a 429 (Too Many Requests) response must surface the rate-limit condition to the user (RateLimitBannerProvider or an equivalent visible status surface) instead of failing silently or blindly retrying"
rule_id: rate-limit-must-surface-to-user
impact: HIGH
impactDescription: "A fetch wrapper, mutation hook, or Server Action that receives a 429 and either swallows the error, shows a generic 'something went wrong' toast indistinguishable from any other failure, or immediately retries the same request without reading Retry-After leaves the user with no explanation for a stalled action and can worsen the very condition (a retry storm) the server-side limiter exists to prevent."
tags:
  - rate-limit
  - observability
  - error-handling
  - resilience
  - a11y
  - l2-blocks
applicable_to:
  - react
  - nextjs
  - vite
provenance_class: internal_design
protects_template_id: templates/L2/blocks/rate-limit-banner.tsx
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-UX-002"
verification:
  type: review
  notes: |
    Review-tier. The violating call sites are too varied (a raw `fetch` wrapper, a
    React Query/SWR `onError`, an axios response interceptor, a Server Action
    `catch` block) to detect false-positive-free with a static AST rule — flagging
    every `response.status === 429` check would require also proving what the
    surrounding branch DOES with it, which is a semantic judgment, not a shape
    match. A reviewer confirms that any client code path which inspects a 429
    status (or an equivalent thrown/caught rate-limit signal) either (a) calls
    `useRateLimitBanner().notify429(...)` with the parsed Retry-After (via
    `extractRetryAfterFrom429` / `parseRetryAfter` from
    `templates/L2/blocks/rate-limit-banner.tsx`), or (b) renders an equivalent
    visible, `role="status"`/`aria-live` surface that names the wait. Silently
    swallowing the 429, showing only a generic error toast, or retrying the same
    request without reading `Retry-After` are all violations. Nonvacuity:
    `frontend/tests/rate-limit-banner.vitest.tsx` renders the real block against
    simulated 429 responses (with and without `Retry-After`) and asserts the
    banner is what actually reaches the DOM.
evidence:
  - source_type: external
    anchors: generic_principle_only
    citation: "RFC 6585 §4 — 429 Too Many Requests: the origin server SHOULD include a Retry-After header field to indicate how long to wait before making a new request; response representations SHOULD include details explaining the condition. (Anchors that 429 is a distinct, discoverable server signal meant to be explained to the caller; requiring every client rate-limit-aware code path to render that explanation via a specific banner primitive is an ax-template layer decision.)"
    url: "https://www.rfc-editor.org/rfc/rfc6585#section-4"
    quoted_at: "2026-07-27"
  - source_type: external
    anchors: generic_principle_only
    citation: "RFC 9110 §10.2.3 — Retry-After: the Retry-After header field indicates how long the user agent ought to wait before making a follow-up request, expressed as either an HTTP-date or a non-negative decimal integer number of seconds. (Anchors only that the server communicates a wait duration in a well-defined header; a client that ignores it and retries immediately works against the server's stated intent.)"
    url: "https://www.rfc-editor.org/rfc/rfc9110.html#name-retry-after"
    quoted_at: "2026-07-27"
  - source_type: external
    anchors: generic_principle_only
    citation: "WCAG 2.2 SC 4.1.3 Status Messages (Level AA) — status messages can be programmatically determined through role or properties so they can be presented by assistive technologies without receiving focus. (Anchors only that a non-focus-stealing status announcement is the correct a11y pattern for a wait condition; requiring the specific RateLimitBannerProvider surface is an ax-template layer decision.)"
    url: "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
    quoted_at: "2026-07-27"
decided_at: "2026-07-27"
---

## A 429 must reach the user, not just the console

**Impact: HIGH — the backend rate limiter (`RateLimitFilter`) deliberately responds `429` + `Retry-After` to make a client back off; a client surface that doesn't render that condition either confuses the user (a stalled button, no feedback) or, worse, retries immediately and turns a single throttled request into a retry storm against the same limiter.** This rule is the client-side counterpart to the server-side `RATELIMIT` domain (`backend/src/main/java/com/ax/template/authblueprint/ratelimit/RateLimitFilter.java`, which sets `Retry-After` in seconds and returns `429` once `maxPerWindow` is exceeded) — `S2.OBSERVABILITY-LIMITS` needs both a server that emits the signal (BE, covered) and a client that renders it (FE, this rule).

### The violation — the 429 is caught and thrown away, or blindly retried

```typescript
// ❌ WRONG — 429 is swallowed into a generic error path; the user has no idea
// the request will ever succeed if they just wait, and the caller's retry
// logic re-fires the same request immediately, ignoring Retry-After entirely.
async function submitOrder(payload: OrderPayload) {
  const res = await fetch('/api/orders', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  if (!res.ok) {
    // VIOLATION: 429 (rate limited) and 500 (server error) render identically —
    // the user cannot tell "wait 30s" apart from "this is broken".
    toast.error('Something went wrong. Please try again.')
    return
  }
  return res.json()
}
```

### Correct — the banner surfaces the 429 with the real wait time

```typescript
// ✅ CORRECT — 429 is distinguished from other failures and routed through the
// shared RateLimitBannerProvider, which parses Retry-After (RFC 9110 §10.2.3)
// and renders a role="status" countdown instead of a generic error.
import { useRateLimitBanner, extractRetryAfterFrom429 } from 'templates/L2/blocks/rate-limit-banner'

function useSubmitOrder() {
  const { notify429 } = useRateLimitBanner()

  return async function submitOrder(payload: OrderPayload) {
    const res = await fetch('/api/orders', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    if (res.status === 429) {
      // CORRECT: distinct branch — surfaces the wait, does not retry blind.
      notify429(extractRetryAfterFrom429(res), 'order submission')
      return
    }
    if (!res.ok) {
      toast.error('Something went wrong. Please try again.')
      return
    }
    return res.json()
  }
}
```

### Why this rule exists

Without a dedicated 429 branch:
- The user sees the same "something went wrong" message for a transient, self-resolving throttle as for an actual server error — they have no reason to believe retrying in 30 seconds will work.
- A caller-side retry loop that doesn't special-case 429 re-fires the identical request on its own schedule, ignoring the exact wait time the server asked for in `Retry-After` — the retry storm the rate limiter exists to prevent.

With the banner:
- `extractRetryAfterFrom429` / `parseRetryAfter` parse the RFC 9110 §10.2.3 `Retry-After` header (both the delta-seconds and HTTP-date forms) and `notify429` renders a `role="status"`/`aria-live="polite"` countdown (WCAG 2.2 SC 4.1.3) that tells the user exactly how long to wait, then auto-dismisses.
- Because the banner state lives in a single provider (`RateLimitBannerProvider`), repeated 429s from unrelated calls update the same countdown instead of stacking duplicate warnings or spawning duplicate timers.

This rule does not mandate the exact JSX of `templates/L2/blocks/rate-limit-banner.tsx` — a fork-receiver may swap the presentation — but it does mandate that a 429 is (a) distinguished from other failure branches and (b) rendered through a visible, non-transient, assistive-tech-reachable surface rather than dropped into the same bucket as a generic error or silently retried.

Reference: [RFC 6585 §4 — 429 Too Many Requests](https://www.rfc-editor.org/rfc/rfc6585#section-4)

Reference: [RFC 9110 §10.2.3 — Retry-After](https://www.rfc-editor.org/rfc/rfc9110.html#name-retry-after)

Reference: [WCAG 2.2 SC 4.1.3 Status Messages](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html)
