---
title: Redact PII (including PAN) before it enters a log statement
impact: HIGH
impactDescription: "Application logs are indexed and retained; raw PII or PAN is a compliance + breach-radius hazard"
tags:
  - observability
  - security
  - pii
  - pci-dss
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-OBS-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-OBS-003
upstream:
  - "https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html"
  - "https://www.pcisecuritystandards.org/document_library/?category=saqs"
evidence:
  - upstream_id: owasp-logging-cheatsheet
    section: "OWASP Logging Cheat Sheet — Data to exclude"
    quote: "exclude"
  - upstream_id: pci-dss-saq-a
    section: "Requirement 3.4 — PAN rendered unreadable"
    quote: "PAN is rendered unreadable anywhere it is stored"
  - source_type: external
    citation: "OWASP Logging Cheat Sheet — Data to exclude"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html#data-to-exclude"
  - source_type: external
    citation: "PCI-DSS v4.0 Requirement 3.4 — PAN rendered unreadable"
    url: "https://www.pcisecuritystandards.org/document_library/"
---

## Redact PII (including PAN) before it enters a log statement

**Impact: HIGH — Application logs are indexed and retained; raw PII or PAN is a compliance + breach-radius hazard**

Logs flow through aggregators, SIEMs, retention buckets, backups, and developer terminals. Anything written to a log statement is — practically — broadcast to a wider audience than the original request handler ever was. Per the OWASP Logging Cheat Sheet, the safe default is to redact PII (email, phone, SSN, payment data, session tokens) *at the source*: before the string is handed to `log.info(...)`. Sanitising downstream (log scrubbers) is best-effort and routinely bypassed by new fields.

For payment-handling code the bar is stricter: PCI-DSS Requirement 3.4 mandates that the Primary Account Number (PAN — the 13-19 digit card number) be rendered unreadable wherever it is stored, **including in application logs**. The token-vs-PAN distinction matters: an opaque provider-issued token is safe to log, but the raw PAN, CVV, expiration date, and any combination of those is Sensitive Authentication Data (SAD) that must never appear in plaintext. Use `@JsonIgnore` on PAN-bearing fields plus an explicit `toString()` override that returns `[REDACTED]`.

**Incorrect — raw user data in a log message:**

```java
String email = user.getEmail();
String phone = user.getPhone();
String pan = paymentMethod.getPan();   // 16-digit card number
log.info("password reset for user " + email + " phone " + phone + " card " + pan);
```

**Correct — redactor at the boundary:**

```java
log.info("password reset for user {}", PiiRedactor.redact(user.identifier()));
// or, prefer structured fields with a known-safe id:
log.atInfo().addKeyValue("user_id", user.id()).setMessage("password reset").log();
```

**Correct — PAN-bearing field with @JsonIgnore + toString override:**

```java
public final class PaymentMethodToken {
    @JsonIgnore
    private final String rawPan;   // never serialized to JSON, never logged

    public PaymentMethodToken(String rawPan) { this.rawPan = rawPan; }

    @Override
    public String toString() {
        return "[REDACTED]";   // log.info("token={}", token) → "token=[REDACTED]"
    }
}
```

Verification: `./gradlew testPractices --tests "*NoPiiInLogs*"` exercises the `PiiRedactor` over emails / phones / SSNs / 16-digit card numbers and asserts the original strings are gone, the redaction markers are present, and clean strings pass through unchanged. PAN coverage is additionally asserted by the Payment blueprint's `PanRedactionTest` (`./gradlew testPayment --tests "*PanRedaction*"`).

Reference: [OWASP Logging Cheat Sheet — Data to exclude](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html#data-to-exclude)

Reference: [PCI Security Standards Council — Document Library (PCI-DSS v4.0 Requirement 3.4)](https://www.pcisecuritystandards.org/document_library/)
