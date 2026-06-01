---
title: AUDIT log lines MUST hash PII identifiers — never write raw email / phone / RRN to log aggregators
impact: HIGH
impactDescription: "Operator log aggregators (ELK, Splunk, CloudWatch, Datadog) typically have looser access controls than the primary DB and longer retention. Writing raw PII to audit log lines silently expands the PII surface area to every engineer who can query the log."
tags:
  - pii
  - audit
  - logging
  - korean-enterprise
  - 개인정보보호법
spec_ref: "specs/email-outbox-l0.yaml#EMAIL-ADMIN-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/emailoutbox/EmailOutboxService.java"
  pattern: "AUDIT.info(\"verb=ADMIN_RETRY id={} recipientHash={}\", id, EmailPiiHelper.recipientHash(row.getRecipient()))"
upstream:
  - "https://owasp.org/www-project-application-security-verification-standard/"
  - "https://www.rfc-editor.org/rfc/rfc6234"
evidence:
  - source_type: external
    citation: "OWASP ASVS V7 — Error Handling and Logging"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify that the application does not log credentials, payment details, or other sensitive data."
    quoted_at: "2026-05-26"
  - source_type: external
    citation: "RFC 6234 — US Secure Hash Algorithms (SHA-256 deterministic hash for correlation tokens)"
    url: "https://www.rfc-editor.org/rfc/rfc6234"
    quote: "Any change to a message in transit will, with very high probability, result in a different message digest."
    quoted_at: "2026-05-26"
---

## AUDIT log lines MUST hash PII identifiers — never write raw email / phone / RRN to log aggregators

**Impact: HIGH — log aggregators silently expand the PII surface area**

When a service writes an AUDIT log line carrying a user identifier — email,
phone number, 주민등록번호, or any value the privacy regime classifies as PII —
that identifier flows to every downstream log aggregator: ELK, Splunk,
CloudWatch, Datadog, Loki. The aggregator's access control is typically
WIDER than the primary database (every on-call engineer needs to query
logs), the retention is LONGER (logs are kept for 30/90/180 days for
incident forensics), and the geographic scope is BROADER (logs replicate
across regions while the DB may not). Storing the raw value once at the
service is multiplied N-fold by the log fan-out.

The catalog rule is binary: AUDIT log lines MUST hash any PII identifier
before emitting the log statement. The hash is a stable correlation token
— same input deterministically produces the same hash, so an SRE
investigating "why did this row keep failing" can trace the row across
log lines without ever seeing the raw email. Use a truncated SHA-256
(16 hex chars is sufficient for ops correlation; the collision risk for
typical org sizes is on the order of 10^-19).

The full identifier remains in the primary DB column where the access
control is tighter — admins who legitimately need it can query the row
by id. Operators reading the log get the hash; they never see the raw.

**Incorrect — writes raw recipient email to the operator log aggregator:**

```java
@Transactional
public EmailOutbox adminRetry(UUID id) {
    EmailOutbox row = outboxRepository.findById(id).orElseThrow(...);
    row.resetForRetry();
    AUDIT.info("verb=ADMIN_RETRY id={} recipient={}", id, row.getRecipient());
    return outboxRepository.save(row);
}
```

**Correct — hash the identifier before logging:**

```java
static String recipientHash(String email) {
    if (email == null || email.isBlank()) return "(none)";
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] digest = md.digest(email.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest).substring(0, 16);
}

AUDIT.info("verb=ADMIN_RETRY id={} recipientHash={}", id, recipientHash(row.getRecipient()));
// → "verb=ADMIN_RETRY id=abc... recipientHash=4f3a9b7e8c1d2f0a"  ✅ stable correlation token, no PII
```

Reference: [OWASP ASVS V7 — Error Handling and Logging](https://owasp.org/www-project-application-security-verification-standard/)
Reference: [RFC 6234 — US Secure Hash Algorithms (SHA-256)](https://www.rfc-editor.org/rfc/rfc6234)

## How to apply

Before adding `log.info(...)` / `AUDIT.info(...)` / metric labels, check every
positional argument:

```text
for each interpolated value in the log statement:
  if value is a recipient email / phone / RRN / 주민등록번호 / national ID:
    REWRITE: pass through recipientHash() / phoneHash() / similar
  if value is an entity id (UUID, integer PK):
    OK — entity ids are not PII
  if value is a request id / correlation id:
    OK
```

The catalog ships `EmailPiiHelper.recipientHash()` in the email-outbox L4
as the canonical example. Other L4s that touch PII in their audit lines
should follow the same pattern; the helper is small (one method, no
dependencies) so duplicating it per L4 is fine until enough L4s converge
to justify lifting to L0/fork-receiver-kit (the R53 pattern).

## Anti-patterns

- "I'll just log `userId` — that's not PII" — userId is fine IF the rest of
  the table doesn't carry the email next to it. If the audit table joins
  to users by id, the operator can `JOIN users` and recover the email,
  defeating the purpose. Hashing keeps log queries opaque to mass PII
  recovery.
- "We have log access controls — only on-call sees it" — log access
  controls drift; team membership changes; SOC-2 audits routinely find
  ex-employees retaining log access for weeks. Hashing is structural,
  not policy.
- "Hashing makes debugging harder" — same input → same hash, so an SRE
  filters logs by `recipientHash=<value>` after retrieving the value
  from the DB once. The investigation pattern is one extra SQL query.
