---
title: Stored error columns MUST be PII-sanitized at storage time — render-layer scrub alone is insufficient
impact: HIGH
impactDescription: "When a service persists a sender / adapter / job error string verbatim and relies only on a render-layer scrubber to redact PII, every operator with raw DB query access reads PII the catalog promised would be redacted. The render layer is a defense, not the only defense — defense-in-depth requires server-side scrub on write."
tags:
  - pii
  - storage
  - defense-in-depth
  - error-handling
  - korean-enterprise
spec_ref: "specs/email-outbox-l0.yaml#EMAIL-SEND-002"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/emailoutbox/EmailOutboxService.java"
  pattern: "row.markFailure(EmailPiiHelper.sanitizeReason(trimmed), now, ...) — sender exception scrubbed BEFORE persist, not only at render"
upstream:
  - "https://owasp.org/www-project-application-security-verification-standard/"
  - "https://cwe.mitre.org/data/definitions/532.html"
evidence:
  - source_type: external
    citation: "OWASP ASVS V8 — Data Protection"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify the application minimizes the number of parameters in a request, such as hidden fields, AJAX variables, cookies and header values."
    quoted_at: "2026-05-26"
  - source_type: external
    citation: "CWE-532 — Insertion of Sensitive Information into Log File"
    url: "https://cwe.mitre.org/data/definitions/532.html"
    quote: "Information written to log files can be of a sensitive nature and give valuable guidance to an attacker or expose sensitive user information."
    quoted_at: "2026-05-26"
---

## Stored error columns MUST be PII-sanitized at storage time — render-layer scrub alone is insufficient

**Impact: HIGH — render-layer scrub fails for direct DB query paths**

When a service persists a thrown exception message (sender adapter failure,
job runner stack trace, webhook delivery error) verbatim into a database
column intended for operator display — `email_outbox.last_error`,
`webhook_delivery.error`, `scheduled_task_history.failure_reason`, similar —
the column itself is now a PII reservoir. Provider exceptions routinely
embed:

- the recipient email ("SMTP rejected target@example.com")
- the user's 주민등록번호 ("ID 901231-1234567 not found")
- a JWT or Bearer token ("auth header Bearer eyJ...")
- an internal hostname ("connection refused from mailer.internal")
- an IPv4 ("could not reach 10.0.5.12")

The catalog already mandates a RENDER-layer scrub via R50
`stored-server-error-sanitize-at-render-layer` so the admin UI shows
`[REDACTED]` instead of the raw fragment. But the render layer is only
one of many readers. Direct DB access — an SRE running `SELECT
last_error FROM email_outbox WHERE status='DLQ'`, a backup restoring to
a forensic lab, a fork-receiver's BI pipeline reading the table — all
bypass the UI scrub. The column is the persistence boundary; defense-in-
depth requires the scrub to apply at the WRITE path, not just at the
READ path.

Mechanically: every code path that calls `setLastError(reason)` /
`markFailure(reason, ...)` / similar MUST first pass `reason` through a
PII deny-list scrubber identical to the render-layer rule's pattern set.
The catalog ships `EmailPiiHelper.sanitizeReason()` (JVM) and
`templates/L0/fork-receiver-kit/parse-error.ts#sanitizeStoredError`
(TypeScript) as the canonical pair. Apply both — the render layer keeps
its scrub as a second line of defense for the unlikely case that a
fork-receiver introduces a new write site that forgets the storage-time
scrub.

**Incorrect — stores sender exception verbatim; relies on UI scrubber alone:**

```java
catch (EmailSendException ex) {
    String reason = ex.getMessage();   // ❌ stored verbatim — may embed PII
    if (reason != null && reason.length() > 1000) {
        reason = reason.substring(0, 1000);  // length cap but no scrub
    }
    row.markFailure(reason, now, ...);
    // → DB column email_outbox.last_error = "SMTP rejected alice@example.com: 550 ..."
    // → SRE running `SELECT last_error FROM email_outbox` reads PII directly
}
```

**Correct — scrub at storage time, render-layer keeps its scrub as second line:**

```java
catch (EmailSendException ex) {
    String raw = ex.getMessage() == null ? "unknown error" : ex.getMessage();
    String trimmed = raw.length() > 1000 ? raw.substring(0, 1000) : raw;
    String reason = EmailPiiHelper.sanitizeReason(trimmed);  // ✅ scrub BEFORE persist
    row.markFailure(reason, now, delay -> now.plusSeconds(delay));
    // → DB column email_outbox.last_error = "SMTP rejected [REDACTED]: 550 ..."
    // → operator SELECT or admin UI both see redacted form
}
```

Reference: [OWASP ASVS V8 — Data Protection](https://owasp.org/www-project-application-security-verification-standard/)
Reference: [CWE-532 — Insertion of Sensitive Information into Log File](https://cwe.mitre.org/data/definitions/532.html)

## How to apply

Audit every persisted error-string column with grep — `last_error`,
`error_message`, `failure_reason`, `stderr`, `stack_trace`. For each
write site, confirm the input flows through a sanitize helper that
redacts:

- KR RRN — `\d{6}-\d{7}`
- KR mobile — `01[016789]-?\d{3,4}-?\d{4}`
- JWT shape — `eyJ[A-Za-z0-9._-]{20,}`
- Bearer header value
- `sk-...` / `ghp_...` secret prefixes
- email address
- IPv4
- `*.internal`, `*.local` hostnames

The canonical scrubber lives in `EmailPiiHelper.sanitizeReason` (JVM) /
`templates/L0/fork-receiver-kit/parse-error.ts#sanitizeStoredError`
(TypeScript). Duplicate the deny-list per-language helper until enough
modules converge to justify a shared library.

## Anti-patterns

- "The render layer already redacts — why scrub at storage too?" — render
  layer protects UI consumers. DB queries, backups, BI exports, and
  forensic restores all bypass it.
- "Length cap is enough" — substring(0, 1000) is a size limit, not a
  content filter. A 200-char message can still embed an email.
- "We trust the sender adapter to throw clean errors" — sender adapters
  bubble up upstream library exceptions (JavaMail, AWS SES SDK, SendGrid
  client) whose error strings the catalog cannot control.
