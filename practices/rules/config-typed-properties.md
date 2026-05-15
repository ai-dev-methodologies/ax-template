---
title: Bind config through @ConfigurationProperties records, not @Value
impact: HIGH
impactDescription: "One typed contract beats scattered untyped string injections"
tags:
  - config
  - configuration-properties
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CONFIG-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CONFIG-001
upstream:
  - "https://docs.spring.io/spring-boot/reference/features/external-config.html"
evidence:
  - upstream_id: spring-boot-external-config
    section: "Spring Boot — Type-safe Configuration Properties"
    quote: "@ConfigurationProperties"
  - source_type: external
    citation: "Spring Boot Reference — Type-safe Configuration Properties"
    url: "https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties"
---

## Bind config through @ConfigurationProperties records, not @Value

**Impact: HIGH — One typed contract beats scattered untyped string injections**

`@Value("${smtp.host}")` injects one config key at one field. Multiply that by twenty fields and the contract for what `smtp.*` looks like lives nowhere — each field is its own undocumented binding. `@ConfigurationProperties` records collect the whole namespace into one immutable type. Spring Boot validates the binding at startup, the IDE refactors every callsite atomically when a field is renamed, and tests instantiate the record with plain `new`.

**Incorrect — scattered @Value injection:**

```java
@Service
public class SmtpSender {
    @Value("${smtp.host}") private String host;
    @Value("${smtp.port:587}") private int port;
    @Value("${smtp.username}") private String username;
    // ... no single source of truth, mutable fields, untyped binding
}
```

**Correct — @ConfigurationProperties record:**

```java
@ConfigurationProperties("smtp")
public record SmtpProperties(String host, int port, String username) {
    public SmtpProperties {
        if (port <= 0) port = 587;
    }
}

@Service
public class SmtpSender {
    private final SmtpProperties props;
    public SmtpSender(SmtpProperties props) { this.props = props; }
}
```

Verification: `./gradlew testPractices --tests "*TypedProperties*"` runs an ArchUnit rule that rejects any `@Value`-annotated field in the practices/ subtree, plus a reflective check that the `PracticesAppProperties` fixture is a record with an explicit `@ConfigurationProperties` namespace.

Reference: [Spring Boot — Type-safe Configuration Properties](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties)
