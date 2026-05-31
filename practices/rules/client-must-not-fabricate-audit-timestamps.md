---
title: Client must NOT fabricate audit timestamps — server is the source of truth
impact: HIGH
impactDescription: "A UI that shows a wall-clock time the server did not record creates an audit-truth mismatch that erodes incident-timeline reconstruction"
tags:
  - audit
  - forensic
  - timestamp
  - optimistic-update
spec_ref: "specs/activity-feed-l0.yaml#ACT-READ-001"
verification:
  type: review
  source: "templates/L4/activity-feed/app/(activities)/page.tsx"
  pattern: "pendingReadIds Set<string> in component state — cache only ever carries backend's readAt or null; no `new Date().toISOString()` written into cache"
upstream:
  - "https://gdpr-info.eu/art-5-gdpr/"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "GDPR Article 5(1)(d) — Personal data shall be accurate"
    url: "https://gdpr-info.eu/art-5-gdpr/"
    quote: "Personal data shall be: accurate and, where necessary, kept up to date; every reasonable step must be taken to ensure that personal data that are inaccurate, having regard to the purposes for which they are processed, are erased or rectified without delay (accuracy)."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "OWASP ASVS V8 — Data Protection (logging accuracy + integrity)"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify that authentication and session events are logged including admin login, user login, password change, and other security-relevant events."
    quoted_at: "2026-05-25"
---

## Client must NOT fabricate audit timestamps — server is the source of truth

**Impact: HIGH — a UI that lies about *when* eats into incident timeline integrity**

When the client optimistically updates state for a mutation that has an audit timestamp (`readAt`, `actedAt`, `deletedAt`, `revokedAt`, `acknowledgedAt`), it is tempting to immediately set `field = new Date().toISOString()` so the row reflects the action without waiting for the server response. **Do not do this.** The server's actual timestamp will differ — by the network round-trip, by clock skew if the client's wall clock is off, by intentional client tampering. The audit log holds the server's timestamp; the screenshot the user takes shows the client's. When those diverge in an incident-response review, the system's trust posture collapses: *the UI showed me one time, the log shows another, which is the real evidence?*

Two narrow classes of timestamp can be client-rendered safely:
1. **"as-of" relative labels with no absolute claim** — e.g. `"just now"`, `"a moment ago"` rendered from a `pending: boolean` flag. These do not claim to be the audit time.
2. **Display-only formatting of a server-returned timestamp** — once the server has responded with the canonical value, the client may format it (`new Date(serverIso).toLocaleString(...)`).

**The forbidden pattern: write `new Date()` into the cache as if it were the server's authoritative timestamp.**

**Incorrect — fabricated optimistic timestamp:**

```ts
// ❌ Fabricated readAt — client clock, not server truth.
const read = useMutation({
  mutationFn: markRead,
  onSuccess: (_void, id) => {
    qc.setQueryData(['activity-feed'], (old) => ({
      ...old,
      items: old.items.map((e) =>
        e.id === id ? { ...e, readAt: new Date().toISOString() } : e,
      ),
    }))
  },
})

// Renderer displays a time the server may never have stored:
<span>read {timeAgo(event.readAt, now)}</span>
```

**Correct — typed pending set in component state, cache only ever holds backend truth:**

```ts
const [pendingReadIds, setPendingReadIds] = React.useState<Set<string>>(() => new Set())

const read = useMutation({
  mutationFn: markRead,             // returns the backend's authoritative readAt
  onMutate: (id) => {
    setPendingReadIds((prev) => new Set(prev).add(id))
  },
  onSettled: (_data, _err, id) => {
    setPendingReadIds((prev) => {
      const next = new Set(prev)
      next.delete(id)
      return next
    })
  },
  onSuccess: () => {
    // Family-key invalidate so the next refetch carries the server's readAt.
    qc.invalidateQueries({ queryKey: ['activity-feed'] })
  },
})

// Renderer: "marking read…" while pending, then the server's real timestamp:
const isPendingRead = isUnread && pendingReadIds.has(event.id)
{isPendingRead
  ? '· marking read…'
  : event.readAt && `· read ${timeAgo(event.readAt, now)}`}
```

Apply this rule to any timestamp field that ends up in an audit query or a compliance export: `readAt`, `actedAt`, `submittedAt`, `cancelledAt`, `revokedAt`, `approvedAt`, `rejectedAt`, `acknowledgedAt`, `verifiedAt`. The bias toward "show something instantly" is real and important — solve it with a pending sentinel and a `"…ing"` label, not with `new Date()`.

Reference: [GDPR Article 5 — Lawfulness, fairness, accuracy](https://gdpr-info.eu/art-5-gdpr/)

Reference: [OWASP ASVS V8 — Data Protection & Logging](https://owasp.org/www-project-application-security-verification-standard/)
