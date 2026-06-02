---
title: Server-supplied stored error strings MUST pass a PII / secret deny-list at the render layer
impact: HIGH
impactDescription: "errorMessage / lastError fields persisted on entities and rendered to admin views leak PII / internal hostnames / credentials via screen-share + screenshot; render-layer deny-list is defense-in-depth even when backend sanitization is the canonical fix"
tags:
  - pii
  - error-handling
  - defense-in-depth
  - admin-surface
  - screen-share-leak
spec_ref: "specs/webhook-l0.yaml#WEBHOOK-DEAD-LETTER-002"
verification:
  type: review
  source: "templates/L4/webhook/app/(admin)/webhooks/deliveries/page.tsx, templates/L0/fork-receiver-kit/parse-error.ts (sanitizeStoredError helper)"
  pattern: "sanitize helper applied to any server-stored error field (lastError on Delivery, errorMessage on JobHistory) before inline render; regex deny-list includes email / Bearer / JWT / IPv4 / .internal/.local / Korean RRN / Korean mobile / PEM headers / GitHub PAT"
upstream:
  - "https://cwe.mitre.org/data/definitions/209.html"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "CWE-209 — Generation of Error Message Containing Sensitive Information"
    url: "https://cwe.mitre.org/data/definitions/209.html"
    quote: "The product generates an error message that includes sensitive information about its environment, users, or associated data."
    quoted_at: "2026-05-25"
---

## Server-supplied stored error strings MUST pass a PII / secret deny-list at the render layer

**Impact: HIGH — incident-bridge screen-share is a regular leak vector for raw server errors**

There are two classes of server-supplied error string a frontend renders:

1. **Transient (fetch-time)** — the `error.message` returned by a failed mutation or query. Rule `error-message-not-in-native-title-attribute` (R47) covers this: errors render in `role='alert'` aria-live spans, not in native `title` tooltips, and `parse-error.ts` already has a PII deny-list at the fetch boundary.

2. **Stored (persisted on an entity)** — `lastError` on a webhook delivery row, `errorMessage` on a scheduled-task `JobHistory` row, `lastFailureReason` on a billing event, `verificationError` on a KYC attempt. These are server-side strings written into the DB at the moment a job / delivery / verification failed, then surfaced as part of the entity's DTO on every GET.

The second class is the dangerous one. Because the error is *stored*, the same bytes are read back every time an admin page renders. An SRE screen-sharing the deliveries page during an incident bridge replays the leak every time the page repaints. Slack screenshots, recorded incident calls, post-incident video reviews — all replay.

`parseError`'s deny-list covers transient errors. Stored errors need the same deny-list applied at the render boundary, as defense-in-depth even when the backend should be sanitizing on write (the catalog tracks "backend DTO sanitization" as the canonical fix per domain; this rule is the layer the frontend owns regardless).

**Incorrect — stored errorMessage rendered raw:**

```tsx
{d.lastError && (
  <div className="rounded border border-red-200 bg-red-50/50 px-2 py-1 text-xs">
    last error: <code>{d.lastError}</code>
  </div>
)}
```

A stack-trace excerpt, a backend Bearer token leaked into the message, an internal hostname (`db-prod.internal`), a Korean RRN that crept into a logging line — all appear inline. JSX escapes HTML but does NOT sanitize content patterns.

**Correct — sanitize helper applied at render:**

```ts
// app/parse-error.ts (per-domain or in fork-receiver-kit)
const STORED_ERROR_MAX = 200
export function sanitizeStoredError(raw: string | null): string {
  if (!raw) return ''
  const looksSensitive =
    /@[\w.-]+\.[A-Za-z]{2,}/.test(raw) ||
    /\b(?:sk-|pk-|Bearer\s+|jdbc:|-----BEGIN |ghp_|ghs_)/i.test(raw) ||
    /\b\d{1,3}(?:\.\d{1,3}){3}\b/.test(raw) ||
    /\.internal\b|\.local\b/.test(raw) ||
    /\d{6}-\d{7}/.test(raw) ||                    // Korean RRN
    /01[016789]-?\d{3,4}-?\d{4}/.test(raw) ||     // Korean mobile
    /eyJ[A-Za-z0-9._-]{20,}/.test(raw)             // JWT
  if (looksSensitive) return '[redacted — see server logs]'
  return raw.length <= STORED_ERROR_MAX ? raw : `${raw.slice(0, STORED_ERROR_MAX)}… [truncated]`
}
```

```tsx
{d.lastError && (
  <div className="rounded border border-red-200 bg-red-50/50 px-2 py-1 text-xs">
    last error: <code>{sanitizeStoredError(d.lastError)}</code>
  </div>
)}
```

**Deny-list locale**: the catalog ships Korean enterprise patterns (RRN `XXXXXX-XXXXXXX`, mobile `010-XXXX-XXXX` and other carrier prefixes) by default. Fork-receivers operating in other locales extend the deny-list with locale-specific PII shapes (US SSN, EU national IDs, JP MyNumber) — this is a domain-level extension point, not a one-size-fits-all global rule.

**When to apply**: any entity DTO field that carries server-side error text accessible to admin / SRE views — `lastError`, `errorMessage`, `failureReason`, `verificationError`, `auditNote`, `lastFailureDetail`. Apply at every render site of the field, not just the most-trafficked one (different pages render the same field).

**When NOT to apply**: short structured error codes (`ERR_TIMEOUT`, `RATE_LIMITED`) without free-form server prose. The deny-list's job is to catch free-form text; a structured enum is already safe.

Reference: [CWE-209 — Information Exposure Through Error Messages](https://cwe.mitre.org/data/definitions/209.html)

Reference: [OWASP ASVS V14.3 — Unintended Security Disclosure](https://owasp.org/www-project-application-security-verification-standard/)
