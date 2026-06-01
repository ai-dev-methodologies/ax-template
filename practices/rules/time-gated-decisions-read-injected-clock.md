---
title: Time-gated decisions must read an injected Clock and compare a server-stored instant — never a client timestamp
impact: HIGH
impactDescription: "Untestable Instant.now() hides expiry/deadline bugs; trusting a client-supplied timestamp lets the caller forge their way past any window or cutoff (authorization bypass)"
tags:
  - time
  - clock
  - authz
  - security
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TIME-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/sessionmanagement/SessionService.java"
  pattern: "Time-gated path injects `private final Clock clock`, computes `Instant now = Instant.now(clock)`, and decides against a server-stored instant (`request.expiresAt().isAfter(now)`); no `Instant.now()` without a clock argument and no comparison against a request-body/header timestamp appears on any expiry/deadline/window path"
upstream:
  - "https://cwe.mitre.org/data/definitions/367.html"
  - "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html"
  - "https://github.com/OWASP/ASVS/blob/v4.0.3/4.0/en/0x19-V11-BusLogic.md"
  - "https://cwe.mitre.org/data/definitions/639.html"
evidence:
  - source_type: external
    citation: "CWE-367: Time-of-check Time-of-use (TOCTOU) Race Condition — MITRE Common Weakness Enumeration"
    url: "https://cwe.mitre.org/data/definitions/367.html"
    quote: "The product checks the state of a resource before using that resource, but the resource's state can change between the check and the use in a way that invalidates the results of the check."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "java.time.Clock — Java SE 21 API documentation (Oracle)"
    url: "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html"
    quote: "Best practice for applications is to pass a Clock into any method that requires the current instant and time-zone. A dependency injection framework is one way to achieve this:"
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "OWASP Application Security Verification Standard 4.0.3 — V11.1.6 Business Logic Security"
    url: "https://github.com/OWASP/ASVS/blob/v4.0.3/4.0/en/0x19-V11-BusLogic.md"
    quote: "Verify that the application does not suffer from \"Time Of Check to Time Of Use\" (TOCTOU) issues or other race conditions for sensitive operations."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "CWE-639: Authorization Bypass Through User-Controlled Key — MITRE Common Weakness Enumeration"
    url: "https://cwe.mitre.org/data/definitions/639.html"
    quote: "The system's authorization functionality does not prevent one user from gaining access to another user's data or record by modifying the key value identifying the data."
    quoted_at: "2026-06-01"
---

## Time-gated decisions must read an injected Clock and compare a server-stored instant — never a client timestamp

**Impact: HIGH — Untestable `Instant.now()` hides expiry/deadline bugs; trusting a client-supplied timestamp lets the caller forge their way past any window or cutoff (authorization bypass)**

A *time-gated decision* is any authorization or lifecycle outcome whose result depends on "what time is it now" relative to a moment the server owns: a token TTL, a session `expiresAt`, a coupon/enrolment window, a submission deadline, a scheduled cutoff, a grace period, a retry-backoff gate, or a server-written `createdAt` / `revokedAt` / soft-delete `deleted_at` audit timestamp. Two defects recur across these paths, and one rule closes both.

**Defect 1 — `Instant.now()` with no clock is untestable.** `Instant.now()`, `System.currentTimeMillis()`, `LocalDate.now()`, and `new Date()` read the JVM wall clock at the exact instant the line executes. A test cannot pin them, cannot advance them past a deadline, and cannot assert that the decision *flips* when the deadline is crossed. So the most security-relevant branch — the one that fires only after a window closes — is never exercised, and a wrong comparison (`isBefore` vs `isAfter`, `<=` vs `<`) ships GREEN. The Java platform's own answer is to inject a `java.time.Clock`: *"Best practice for applications is to pass a Clock into any method that requires the current instant and time-zone. A dependency injection framework is one way to achieve this."* With an injected clock a test substitutes `Clock.fixed(...)`, advances it one tick past a stored expiry, and proves the predicate changes.

**Defect 2 — trusting a client timestamp is an authorization bypass.** If the server decides "is the window still open?" by reading a timestamp the *client* put in the request body, a query parameter, or a header, the caller simply sends a timestamp inside the window and walks past the cutoff. That is CWE-639 generalised from a user-controlled *key* to a user-controlled *time value*: "The system's authorization functionality does not prevent one user from gaining access … by modifying the [client-supplied] value." The window decision must compare the **server's clock** to an instant the **server stored** (a persisted `expiresAt` column, a computed `start + ttl`), and must ignore any time the client claims.

**Defect 3 — expiry is a predicate, not a stored boolean.** Persisting `boolean expired` (or `boolean windowOpen`) snapshots a decision that immediately goes stale: the row says `expired = false` forever until something re-writes it, so a resource silently stays valid past its deadline (a TOCTOU gap — *"the resource's state can change between the check and the use"*, CWE-367). Store the *instant* (`expiresAt`) and evaluate `now(clock).isAfter(expiresAt)` at read time. The truth is recomputed on every access from the live clock, so there is no stale snapshot to exploit.

**Incorrect — wall-clock `now()` (untestable) + trusting the client's timestamp (forgeable) + a stale boolean:**

```java
@Service
public class EnrollmentService {
    // ❌ no injected Clock — every decision below is unpinnable in a test

    public void enroll(EnrollRequest req) {
        // ❌ DEFECT 2: the cutoff is read from the REQUEST BODY — the caller
        //    sends submittedAt = course.openUntil.minusSeconds(1) and is always "on time"
        if (req.getSubmittedAt().isBefore(course.getDeadline())) {
            seat.setExpired(false);           // ❌ DEFECT 3: stored boolean snapshot, never recomputed
            seatRepository.save(seat);
            grant(req);
        }
    }

    public boolean isStillValid(Seat seat) {
        // ❌ DEFECT 1: Instant.now() — a test cannot advance time past the deadline,
        //    so the "expired" branch is never proven to fire
        return Instant.now().isBefore(seat.getDeadline());
    }
}
```

**Correct — injected `Clock`, decision compares server clock to a server-stored instant, expiry as a live predicate:**

```java
@Service
public class EnrollmentService {
    private final Clock clock;                 // ✅ dependency-injected (Spring @Bean Clock.systemUTC())

    public EnrollmentService(Clock clock, SeatRepository seatRepository) {
        this.clock = clock;
    }

    public void enroll(EnrollRequest req) {
        Instant now = Instant.now(clock);      // ✅ DEFECT 1 closed: pinnable in tests
        // ✅ DEFECT 2 closed: compare the SERVER clock to the SERVER-STORED deadline;
        //    req carries no usable time value — anything the client claims is ignored
        if (now.isAfter(course.getDeadline())) {
            throw new DeadlinePassedException("enrolment window closed at " + course.getDeadline());
        }
        grant(req);
    }

    public boolean isStillValid(Seat seat) {
        // ✅ DEFECT 3 closed: expiry is a predicate recomputed from the live clock,
        //    not a stored boolean that goes stale
        return Instant.now(clock).isBefore(seat.getDeadline());
    }
}
```

This is exactly the shape the reference workload already runs: `SessionService.register()` injects `private final Clock clock`, computes `Instant now = Instant.now(clock)`, and gates on `request.expiresAt().isAfter(now)` — server clock versus server-stored instant, with no client-supplied time anywhere on the path. The same discipline covers `DsrSlaSweeper` (30-day SLA), `ApiKeyService` rotation, `EmailOutbox` exponential backoff, and the `@SQLDelete ... SET deleted_at = CURRENT_TIMESTAMP` soft-delete column — all server-clock-owned. A client timestamp, where one is genuinely needed (e.g. an event's *display* time), is data to store, never an input to an authorization predicate.

Verification: review-tier. There is no single static `@Tag` test that owns this runtime property, so a reviewer (or a fork-receiver's grep gate) confirms on every time-gated path: (1) the bean holds an injected `Clock` and reads `Instant.now(clock)` — never a bare `Instant.now()` / `System.currentTimeMillis()` / `new Date()`; (2) the deadline/window comparison is against a server-stored instant, with no request-body/query/header timestamp feeding the predicate; (3) expiry is evaluated as a live `now(clock).isAfter(stored)` predicate, not persisted as a boolean. The canonical per-path proof a fork-receiver writes is a `Clock.fixed(...)` unit test that advances one tick past the stored deadline and asserts the decision flips from allow to deny.

Reference: [CWE-367: Time-of-check Time-of-use (TOCTOU) Race Condition](https://cwe.mitre.org/data/definitions/367.html)

Reference: [java.time.Clock — Java SE 21 API documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html)

Reference: [OWASP ASVS 4.0.3 — V11.1.6 Business Logic (TOCTOU / race conditions)](https://github.com/OWASP/ASVS/blob/v4.0.3/4.0/en/0x19-V11-BusLogic.md)

Reference: [CWE-639: Authorization Bypass Through User-Controlled Key](https://cwe.mitre.org/data/definitions/639.html)
