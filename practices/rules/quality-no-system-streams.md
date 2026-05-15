---
title: Production code must not write to System.out / System.err
impact: MEDIUM
impactDescription: "Standard streams bypass the logger — no MDC, no structured fields, no appender routing"
tags:
  - quality
  - logging
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-QUALITY-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-QUALITY-003
upstream:
  - "https://www.archunit.org/userguide/html/000_Index.html"
evidence:
  - upstream_id: archunit-userguide
    section: "ArchUnit — GeneralCodingRules"
    quote: "ArchRule"
  - source_type: external
    citation: "ArchUnit pre-canned rule — NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS"
    url: "https://www.archunit.org/userguide/html/000_Index.html#_some_general_coding_rules"
---

## Production code must not write to System.out / System.err

**Impact: MEDIUM — Standard streams bypass the logger — no MDC, no structured fields, no appender routing**

`System.out.println(...)` writes to stdout directly. That bypass means:
- the line carries no `MDC.trace_id` (PRACTICES-OBS-002),
- the line is not a structured event (PRACTICES-OBS-001 — no key-value pairs),
- the line is not routed through the configured appender (no JSON shape, no log-server fan-out),
- and worse, secret values that *would* have been redacted by PRACTICES-OBS-003's `PiiRedactor` are emitted raw.

`System.out` is a debugging crutch that survives into production. The mechanical remedy is an ArchUnit rule that flags every access. New code uses an SLF4J `Logger`; old code gets migrated.

**Incorrect — debug print via standard streams:**

```java
@Service
public class TokenIssuer {
    public String issue(String email) {
        String t = mintToken();
        System.out.println("[AUTH-TOKEN] type=ISSUE email=" + email + " token=" + t);
        return t;
    }
}
```

**Correct — structured logger:**

```java
@Service
public class TokenIssuer {
    private static final Logger log = LoggerFactory.getLogger(TokenIssuer.class);

    public String issue(String email) {
        String t = mintToken();
        log.atInfo()
           .addKeyValue("event", "auth-token-issued")
           .addKeyValue("email", PiiRedactor.redact(email))
           .log();
        return t;
    }
}
```

Verification: `./gradlew testPractices --tests "*NoSystemStreams*"` runs ArchUnit's pre-canned `NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS` rule over the practices/ subtree.

Reference: [ArchUnit — GeneralCodingRules](https://www.archunit.org/userguide/html/000_Index.html#_some_general_coding_rules)
