---
title: Configuration MUST be typed, validated, and fail-fast at startup — separated from code, immutable after boot
impact: HIGH
impactDescription: "Config read ad-hoc as untyped strings with silent defaults fails LATE and in production: a missing or malformed value surfaces as a NullPointerException or wrong behavior on the first request that touches it — hours after a clean-looking boot — instead of refusing to start. Typed binding + declarative constraints + fail-fast turns a latent misconfiguration into an immediate, obvious startup failure."
tags:
  - configuration
  - twelve-factor
  - bean-validation
  - fail-fast
  - secrets
  - startup
spec_ref: "specs/config-validation-l0.yaml#CONFIG-SCHEMA-001"
verification:
  type: review
  source: "specs/config-validation-l0.yaml#CONFIG-SCHEMA-001"
  pattern: "Configuration MUST be bound to a typed object with declarative constraints (@ConfigurationProperties + Jakarta Bean Validation @Validated), not read as scattered untyped string lookups (CONFIG-SCHEMA-001). Validation MUST run at application startup and a constraint violation MUST FAIL the boot (fail-fast), never defer the error to first use (CONFIG-FAILFAST-001). Required keys MUST have NO silent default — a missing required value fails startup, it does not fall back to an empty/zero/placeholder (CONFIG-REQUIRED-001). Secret values MUST be separated from ordinary config (env/secret-manager, not committed config files) and never logged (CONFIG-SECRET-SEPARATION-001). Environment differences MUST be expressed through explicit profiles, not code branches on a hostname (CONFIG-PROFILE-001). Config MUST be immutable after startup — bound once, no mutable global setters rewriting it at runtime (CONFIG-IMMUTABLE-001). Reject ad-hoc System.getenv/Environment.getProperty scattered through business code, a required key with a hardcoded fallback, secrets in application.yml, and runtime mutation of bound config."
upstream:
  - "https://12factor.net/config"
  - "https://docs.spring.io/spring-boot/reference/features/external-config.html"
evidence:
  - source_type: external
    citation: "The Twelve-Factor App — III. Config (definition)"
    url: "https://12factor.net/config"
    quote: "An app's config is everything that is likely to vary between deploys (staging, production, developer environments, etc)."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "The Twelve-Factor App — III. Config (separation from code)"
    url: "https://12factor.net/config"
    quote: "Config varies substantially across deploys, code does not."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "The Twelve-Factor App — III. Config (store in environment)"
    url: "https://12factor.net/config"
    quote: "The twelve-factor app stores config in environment variables (often shortened to env vars or env)."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Configuration MUST be typed, validated, and fail-fast at startup

**Impact: HIGH — Configuration is, per the Twelve-Factor App, *everything that is likely to vary between deploys (staging, production, developer environments, etc)* — and *config varies substantially across deploys, code does not*. The failure mode of doing it badly is always the same: a value is read ad-hoc as an untyped string with a silent default, the app boots clean, and then the first request that touches a missing or malformed value blows up with a NullPointerException or — worse — behaves wrongly with a placeholder default. The error surfaces hours after deploy, far from its cause. The fix is to bind config to a typed object, validate it declaratively, and **fail the boot** if it is wrong.**

There are six load-bearing requirements — the items of `specs/config-validation-l0.yaml`, all governed by this rule.

**1. Typed binding + declarative constraints (CONFIG-SCHEMA-001).** Config is bound to a typed `@ConfigurationProperties` record/class carrying Jakarta Bean Validation constraints (`@NotBlank`, `@Min`, `@Positive`, `@Pattern`), enabled with `@Validated` — not read as scattered `getProperty("...")` string lookups whose type and presence are unchecked.

**2. Fail-fast at startup (CONFIG-FAILFAST-001).** Validation runs at boot; a constraint violation aborts startup with a clear message naming the offending key. The application never starts in a misconfigured state and defers the error to first use.

**3. Required keys have no silent default (CONFIG-REQUIRED-001).** A required value has NO fallback — its absence fails startup. Hardcoding an empty string, `0`, or a placeholder for a required key hides the misconfiguration and ships it to production.

**4. Secret separation (CONFIG-SECRET-SEPARATION-001).** Secrets live in the environment or a secret manager, separate from ordinary config — per Twelve-Factor, *the twelve-factor app stores config in environment variables*. Secrets are never committed in `application.yml` and never logged (composes the no-PII/secrets-in-logs discipline).

**5. Explicit profiles (CONFIG-PROFILE-001).** Per-environment differences are expressed through explicit profiles (`spring.profiles.active`), not `if (hostname.contains("prod"))` branches scattered in code.

**6. Immutable after startup (CONFIG-IMMUTABLE-001).** Config is bound once and immutable thereafter — a `record` or final fields, no mutable global setter rewriting it at runtime, so behavior cannot silently change mid-process.

**Incorrect — untyped lookups with silent defaults; a missing required key fails late as an NPE:**

```java
@Service
class PaymentClient {
    String key = System.getenv("PAYMENT_API_KEY");          // VIOLATION: untyped, unchecked
    int timeout = Integer.parseInt(                          // VIOLATION: NumberFormatException at first use
        Optional.ofNullable(System.getenv("PAY_TIMEOUT")).orElse("0")); // VIOLATION: silent default 0 for a required key
    // boots clean; the first charge() NPEs on a null key in production.
}
```

**Correct — typed @ConfigurationProperties with constraints; validated at boot; required keys fail fast:**

```java
@ConfigurationProperties("payment")
@Validated
public record PaymentConfig(
    @NotBlank String apiKey,                 // required, no default → boot fails if absent (CONFIG-REQUIRED-001)
    @Positive int timeoutSeconds) {}         // typed + constrained (CONFIG-SCHEMA-001)
// @Validated → a violation aborts startup with the bad key named (CONFIG-FAILFAST-001).
// apiKey comes from the environment / secret manager, never application.yml (CONFIG-SECRET-SEPARATION-001).
// The record is immutable after binding (CONFIG-IMMUTABLE-001); environments differ by profile (CONFIG-PROFILE-001).
```

Verification: review-tier. Config validation is a startup-contract property — untyped ad-hoc reads compile and boot fine, failing only when a specific deploy is misconfigured. Verify by review against `specs/config-validation-l0.yaml`: config is bound to typed `@ConfigurationProperties` with Bean Validation constraints and `@Validated`; a bad value fails the boot; required keys have no fallback; secrets are environment/secret-manager sourced and unlogged; environments differ by profile; bound config is immutable. When a fork-receiver wires a real test (context fails to start when a required key is absent/invalid), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [The Twelve-Factor App — III. Config](https://12factor.net/config)

Reference: [Spring Boot — Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)
