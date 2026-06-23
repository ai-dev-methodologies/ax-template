---
title: Reading a governed sensitive field is itself an AUDITED event — every service read that returns the raw @SensitiveField value MUST append an immutable access-log row (who / when / what / why) in the SAME transaction and BEFORE the value is returned; the default projection masks the value and the raw value is reached ONLY via the audited, purpose-stated reveal path, whose append-only trail is admin-queryable
impact: HIGH
impactDescription: "A sensitive-data store that returns a raw governed identifier / card number / PII value WITHOUT recording the access cannot answer the after-the-fact 'who saw what, when, and why' that NIST SP 800-53 AU-3 demands of an audit record — a breach or insider-misuse investigation has no trail. The catalog already audits PHI reads, but only off the clinical @Phi tag; any non-health sensitive field (a PII vault, a payment-card store, a KYC identifier) carried the SAME obligation with NO generic primitive, so each fork-receiver hand-rolled (or silently omitted) the read-audit. A reveal-without-record, or a default projection that leaks the raw value, defeats least privilege (AC-6)"
tags:
  - audit
  - security
  - pii
  - governance
  - state-machine
spec_ref: "specs/sensitive-read-audit-l0.yaml#SENSITIVE-READ-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/sensitiveaccess/SensitiveAccessService.java + backend/src/main/java/com/ax/template/authblueprint/sensitiveaccess/SensitiveRecord.java + backend/src/main/java/com/ax/template/authblueprint/sensitiveaccess/SensitiveAccessLog.java + backend/src/main/java/com/ax/template/authblueprint/sensitiveaccess/SensitiveField.java"
  pattern: "A SensitiveRecord carries a @SensitiveField-tagged raw value; the service reveal method validates a non-blank purpose then appends an immutable SensitiveAccessLog row (accessor=Authentication principal, occurredAt from the injected Clock, recordRef, fieldName, purpose) via common/MemberWriter.persistAndFlush in the SAME @Transactional method BEFORE returning the raw value, so a reveal-without-record rolls back; the default projection returns only maskedValue() and writes NO access-log row; a blank purpose is 422 SENSITIVE_PURPOSE_REQUIRED; the access trail is admin-queryable (ROLE_ADMIN), append-only, with NO delete path on the record repository; the @SensitiveField marker is the domain-agnostic generalization of common/@Phi"
upstream:
  - "https://csf.tools/reference/nist-sp-800-53/r5/au/au-3/"
  - "https://csf.tools/reference/nist-sp-800-53/r5/au/au-2/"
  - "https://csf.tools/reference/nist-sp-800-53/r5/ac/ac-6/"
evidence:
  - source_type: external
    citation: "NIST SP 800-53 Rev 5, AU-3 Content of Audit Records (csf.tools mirror) — what an audit record must establish for every access to a sensitive datum"
    url: "https://csf.tools/reference/nist-sp-800-53/r5/au/au-3/"
    quote: "Ensure that audit records contain information that establishes the following: a. What type of event occurred; b. When the event occurred; c. Where the event occurred; d. Source of the event; e. Outcome of the event; and f. Identity of any individuals, subjects, or objects/entities associated with the event."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "NIST SP 800-53 Rev 5, AU-2 Event Logging (csf.tools mirror) — the rationale (WHY) requirement the recorded purpose generalizes"
    url: "https://csf.tools/reference/nist-sp-800-53/r5/au/au-2/"
    quote: "Provide a rationale for why the event types selected for logging are deemed to be adequate to support after-the-fact investigations of incidents"
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "NIST SP 800-53 Rev 5, AC-6 Least Privilege (csf.tools mirror) — the masked default projection + audited raw-value access"
    url: "https://csf.tools/reference/nist-sp-800-53/r5/ac/ac-6/"
    quote: "Employ the principle of least privilege, allowing only authorized accesses for users (or processes acting on behalf of users) that are necessary to accomplish assigned organizational tasks."
    quoted_at: "2026-06-23"
---

## Reading a sensitive field is an audited event — record the access before you return the value, mask by default, require a purpose

**Impact: HIGH — a raw sensitive value returned with no recorded access has no AU-3 trail (who/when/what/why); a default projection that leaks the raw value defeats AC-6 least privilege.**

A *sensitive-data read-audit* primitive generalizes the catalog's clinical audit-on-read. NIST SP 800-53 AU-3 requires that audit records establish *"a. What type of event occurred; b. When the event occurred; … f. Identity of any individuals, subjects, or objects/entities associated with the event"* — so **reading a governed sensitive value is itself an event that must be recorded**. The catalog already enforced this for PHI (`common/@Phi` + `audit_on_read_guard`), but only for health data; a PII vault, a payment-card store, or a KYC identifier store carries the identical obligation with no health field in sight. The generalization is a domain-agnostic marker, `@SensitiveField`, plus a service contract:

```text
record(ref, field, raw, owner):  store the @SensitiveField datum; owner = Authentication principal
get(id):                         the MASKED projection (last-4) — NO access-log row (a mask read is not a sensitive read)
reveal(id, accessor, purpose):   validate non-blank purpose (else 422); append an IMMUTABLE access-log row
                                 {accessor, occurredAt(Clock), recordRef, fieldName, purpose} in the SAME tx,
                                 BEFORE returning the raw value — record-before-return is the keystone
accessLog(id):                   admin-only (ROLE_ADMIN) append-only trail; NO delete path anywhere
```

**1. The reveal records the access before it returns the value (SENSITIVE-READ-001).** The write and the read are one `@Transactional` unit; the log row is flushed before the raw value leaves the method, so if the record write fails the reveal rolls back — a reveal-without-record is unrepresentable. The row carries the AU-3 who/when/what plus the AU-2 why (purpose).

**2. The default projection masks; only the reveal exposes raw (SENSITIVE-MASK-001 / AC-6).** A bare `GET` returns `maskedValue()` and writes no audit row; the raw value is reachable only through the audited reveal path. That is least privilege made structural.

**3. A reveal requires a stated purpose (SENSITIVE-PURPOSE-001).** A blank/whitespace purpose is `422 SENSITIVE_PURPOSE_REQUIRED` with no row written and no value returned — the trail always answers *why*, not only *who/when*.

**Incorrect — returns the raw value with no recorded access, no mask, no purpose:**

```java
// <!-- catalog-example-ok: SensitiveVaultRepository (illustrative anti-pattern, not a shipped symbol) -->
@GetMapping("/api/vault/{id}/value")
public String readValue(@PathVariable UUID id) {
    SensitiveRecord r = repo.findById(id).orElseThrow();   // ❌ no purpose, no audit row
    return r.getRawValue();                                // ❌ raw value leaves with NO access record (AU-3 violated)
}                                                          // ❌ default path leaks raw — AC-6 least privilege defeated
```

**Correct — record the access (who/when/what/why) in the same tx, then return the raw value:**

```java
@Transactional
public String reveal(UUID recordId, String accessor, String purpose) {
    if (purpose == null || purpose.isBlank()) {
        throw SensitiveAccessException.purposeRequired();         // ✅ 422 — no row, no value
    }
    SensitiveRecord r = records.findById(recordId).orElseThrow(SensitiveAccessException::notFound);
    Instant now = Instant.now(clock);
    // ✅ RECORD BEFORE RETURN — the access row is written + flushed before the raw value leaves.
    members.persistAndFlush(new SensitiveAccessLog(UUID.randomUUID(), r.getId(), r.getRecordRef(),
        r.getFieldName(), accessor, purpose.strip(), now));       // ✅ AU-3 who/when/what + AU-2 why
    return r.getRawValue();
}
```

The reveal and the access record share one transaction, so the value never escapes unrecorded. The default `get()` returns `SensitiveRecord.maskedValue()` only — the raw value is reached solely through `reveal`. `SensitiveAccessLog` columns are `@Column(updatable=false)` and the `SensitiveRecordRepository` declares NO delete method, so the trail is append-only; `accessLog` is `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`. `SensitiveAccessLog` rows are `@AggregateMember` of `SensitiveRecord` — root-JPQL reads, `common/MemberWriter` writes. The `@SensitiveField` marker is the generic generalization of `common/@Phi`: the same audit-on-read obligation attaches to any tagged field, not only PHI.

Verification: review-tier — confirm the reveal writes the access-log row via `MemberWriter.persistAndFlush` BEFORE returning the raw value (same `@Transactional`), the default projection returns only the masked value with no audit row, a blank purpose is 422, the access-log query is ROLE_ADMIN and append-only, and no delete path exists. The behavioural proof a fork-receiver keeps green: reveal N times → exactly N append-only access-log rows recording accessor + occurredAt + fieldName + purpose.

Reference: [NIST SP 800-53 Rev 5 AU-3 (Content of Audit Records)](https://csf.tools/reference/nist-sp-800-53/r5/au/au-3/)

Reference: [NIST SP 800-53 Rev 5 AU-2 (Event Logging)](https://csf.tools/reference/nist-sp-800-53/r5/au/au-2/)

Reference: [NIST SP 800-53 Rev 5 AC-6 (Least Privilege)](https://csf.tools/reference/nist-sp-800-53/r5/ac/ac-6/)
