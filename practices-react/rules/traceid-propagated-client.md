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
