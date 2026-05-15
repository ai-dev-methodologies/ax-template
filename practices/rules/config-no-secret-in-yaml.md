---
title: Never hardcode secrets in application.yml; use ${ENV[:default]}
impact: HIGH
impactDescription: "A hardcoded secret in yaml is one git-log search away from credential leak"
tags:
  - config
  - security
  - secrets
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CONFIG-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CONFIG-002
upstream:
  - "https://docs.spring.io/spring-boot/reference/features/external-config.html"
evidence:
  - upstream_id: spring-boot-external-config
    section: "Spring Boot — Externalized Configuration (env-var placeholders)"
    quote: "@Value"
  - source_type: external
    citation: "OWASP Cheat Sheet — Secrets Management"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html"
---

## Never hardcode secrets in application.yml; use ${ENV[:default]}

**Impact: HIGH — A hardcoded secret in yaml is one git-log search away from credential leak**

`client-secret: hunter2` in `application.yml` is a credential leak the moment the file lands in git. Public mirrors get scraped within minutes, internal forks land in archived repos forever, and the credential has to be rotated everywhere it was used — across all environments, all consumers — before the leak is contained. The mechanical remedy is universal: route every sensitive key through an environment variable placeholder. Default-only values (`dummy-foo` / `changeme`) are acceptable in committed config so a clone runs out of the box; the real secret is supplied at runtime through the env var.

**Incorrect — literal secret in committed yaml:**

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: real-client-id-1234
            client-secret: real-secret-abcdef          # leaked the moment it lands in git
```

**Correct — env-var placeholder with safe default:**

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:dummy-google-id}
            client-secret: ${GOOGLE_CLIENT_SECRET:dummy-google-secret}
```

Verification: `./gradlew testPractices --tests "*NoSecretInYaml*"` scans `application.yml` line by line, applies a sensitive-key regex (`client-secret`, `api-key`, `access-token`, `jwt-secret`, `encryption-key`, `webhook-secret`), and rejects any line whose value is a non-empty literal that does not start with `${`.

Reference: [OWASP Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
