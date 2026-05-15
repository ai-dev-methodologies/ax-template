---
title: Redact PII before it enters a log statement
impact: HIGH
impactDescription: "Application logs are indexed and retained; raw PII is a compliance + breach-radius hazard"
tags:
  - observability
  - security
  - pii
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-OBS-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-OBS-003
upstream:
  - "https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html"
evidence:
  - upstream_id: owasp-logging-cheatsheet
    section: "OWASP Logging Cheat Sheet — Data to exclude"
    quote: "exclude"
  - source_type: external
    citation: "OWASP Logging Cheat Sheet — Data to exclude"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html#data-to-exclude"
---

## Redact PII before it enters a log statement

**Impact: HIGH — Application logs are indexed and retained; raw PII is a compliance + breach-radius hazard**

Logs flow through aggregators, SIEMs, retention buckets, backups, and developer terminals. Anything written to a log statement is — practically — broadcast to a wider audience than the original request handler ever was. Per the OWASP Logging Cheat Sheet, the safe default is to redact PII (email, phone, SSN, payment data, session tokens) *at the source*: before the string is handed to `log.info(...)`. Sanitising downstream (log scrubbers) is best-effort and routinely bypassed by new fields.

**Incorrect — raw user data in a log message:**

```java
String email = user.getEmail();
String phone = user.getPhone();
log.info("password reset for user " + email + " phone " + phone);
```

**Correct — redactor at the boundary:**

```java
log.info("password reset for user {}", PiiRedactor.redact(user.identifier()));
// or, prefer structured fields with a known-safe id:
log.atInfo().addKeyValue("user_id", user.id()).setMessage("password reset").log();
```

Verification: `./gradlew testPractices --tests "*NoPiiInLogs*"` exercises the `PiiRedactor` over emails / phones / SSNs and asserts the original strings are gone, the redaction markers are present, and clean strings pass through unchanged.

Reference: [OWASP Logging Cheat Sheet — Data to exclude](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html#data-to-exclude)
