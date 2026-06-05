---
title: Every outbound dependency call MUST be wrapped in resilience controls — timeout, circuit breaker, bounded retry with jittered backoff, bulkhead, fallback
impact: HIGH
impactDescription: "An unprotected synchronous call to a remote dependency is a cascading-failure vector: with no timeout it blocks a request thread indefinitely when the dependency hangs; with no circuit breaker it keeps hammering a dead dependency and exhausts the thread pool; with naive immediate retries it amplifies load into a retry storm; with no bulkhead one slow dependency starves threads needed by every other call. The dependency's outage becomes your outage."
tags:
  - resilience
  - circuit-breaker
  - retry
  - timeout
  - bulkhead
  - fallback
spec_ref: "specs/resilience-l0.yaml#RESILIENCE-CIRCUITBREAKER-001"
verification:
  type: review
  source: "specs/resilience-l0.yaml#RESILIENCE-CIRCUITBREAKER-001"
  pattern: "Every synchronous outbound dependency call MUST be wrapped in resilience controls. A circuit breaker trips OPEN after consecutive failures cross a threshold (fail-fast while OPEN), and after a timeout transitions HALF_OPEN to test recovery with limited probes (RESILIENCE-CIRCUITBREAKER-001 — CLOSED/OPEN/HALF_OPEN). Retries are BOUNDED with exponential backoff plus jitter — never immediate, never unbounded — and only on transient/idempotent failures (RESILIENCE-RETRY-001). Every call sets explicit connect AND read timeouts — never the library default which may be infinite (RESILIENCE-TIMEOUT-001). Calls run inside a bulkhead (semaphore or thread-pool isolation) so one saturated dependency cannot exhaust the threads of the others (RESILIENCE-BULKHEAD-001). A breaker-open / timeout / exhausted-retry surfaces a declared fallback — cached, default, or degraded — not an unhandled exception (RESILIENCE-FALLBACK-001). A client-side rate limiter honors the server's 429 / Retry-After back-pressure (RESILIENCE-RATELIMITER-001). Reject an outbound call with no timeout, with unbounded or immediate-loop retries, or with retries layered so the effective attempt count multiplies across nested wrappers."
upstream:
  - "https://microservices.io/patterns/reliability/circuit-breaker.html"
  - "https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/"
  - "https://resilience4j.readme.io/docs/circuitbreaker"
evidence:
  - source_type: external
    citation: "Chris Richardson — Circuit Breaker pattern (microservices.io, trip on threshold)"
    url: "https://microservices.io/patterns/reliability/circuit-breaker.html"
    quote: "When the number of consecutive failures crosses a threshold, the circuit breaker trips, and for the duration of a timeout period all attempts to invoke the remote service will fail immediately."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "Chris Richardson — Circuit Breaker pattern (microservices.io, recovery probe)"
    url: "https://microservices.io/patterns/reliability/circuit-breaker.html"
    quote: "After the timeout expires the circuit breaker allows a limited number of test requests to pass through."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "Amazon Builders' Library — Timeouts, retries, and backoff with jitter (exponential backoff)"
    url: "https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/"
    quote: "The most common pattern is an _exponential backoff,_ where the wait time is increased exponentially after every attempt."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "Amazon Builders' Library — Timeouts, retries, and backoff with jitter (jitter)"
    url: "https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/"
    quote: "Our solution is jitter. Jitter adds some amount of randomness to the backoff to spread the retries around in time."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Every outbound dependency call MUST be wrapped in resilience controls

**Impact: HIGH — A synchronous call to a remote dependency (an HTTP API, a downstream service) is the single most common cascading-failure vector. With no timeout, a hung dependency holds your request thread forever; enough hung calls and your own service runs out of threads and stops serving everything. With no circuit breaker, you keep dialing a dependency that is already down — burning threads and latency on calls you know will fail. With naive immediate retries, a brief dependency blip turns into a self-inflicted retry storm that keeps it down. With no bulkhead, one slow dependency drains the thread pool that every other dependency also needs. The dependency's outage silently becomes *your* outage.**

There are six load-bearing controls — the items of `specs/resilience-l0.yaml`, all governed by this rule.

**1. Circuit breaker: CLOSED / OPEN / HALF_OPEN (RESILIENCE-CIRCUITBREAKER-001).** Wrap the call in a breaker. Per the pattern: *when the number of consecutive failures crosses a threshold, the circuit breaker trips, and for the duration of a timeout period all attempts to invoke the remote service will fail immediately* — that is the OPEN state, failing fast instead of piling threads onto a dead dependency. Then *after the timeout expires the circuit breaker allows a limited number of test requests to pass through* — the HALF_OPEN probe; success closes it, failure re-opens it.

**2. Bounded retry, exponential backoff + jitter (RESILIENCE-RETRY-001).** Retries are bounded (a small max attempts), applied ONLY to transient failures on idempotent operations, and spaced by *exponential backoff* — per AWS, *the most common pattern is an exponential backoff, where the wait time is increased exponentially after every attempt* — with *jitter*: *jitter adds some amount of randomness to the backoff to spread the retries around in time*, so a fleet of clients does not retry in lockstep. Never an immediate retry loop, never unbounded.

**3. Explicit connect + read timeouts (RESILIENCE-TIMEOUT-001).** Every outbound call sets BOTH a connect timeout and a read timeout explicitly — never the client library default, which on several clients is *infinite*. A call with no read timeout is the no.1 thread-exhaustion cause.

**4. Bulkhead isolation (RESILIENCE-BULKHEAD-001).** Calls to a dependency run inside a bulkhead — a bounded semaphore or a dedicated thread pool — so saturation of one dependency consumes only its own quota and cannot starve the threads other dependencies (and the rest of the app) rely on.

**5. Declared fallback (RESILIENCE-FALLBACK-001).** When the breaker is OPEN, a call times out, or retries are exhausted, the caller returns a *declared* fallback — a cached value, a safe default, or a degraded response — not an unhandled exception bubbling to the user. The fallback is part of the contract, chosen deliberately per call site.

**6. Client-side rate limiter, honor 429 / Retry-After (RESILIENCE-RATELIMITER-001).** The client self-limits its outbound rate and, when the server responds 429, honors `Retry-After` rather than retrying immediately — cooperating with the dependency's back-pressure instead of fighting it.

**Incorrect — no timeout, unbounded immediate retry, no breaker; a dependency blip becomes thread exhaustion + a retry storm:**

```java
while (true) {                                  // VIOLATION: unbounded retry loop
    try {
        return restTemplate.getForObject(url, Quote.class);  // VIOLATION: no connect/read timeout (default may be infinite)
    } catch (RestClientException e) {
        // VIOLATION: immediate retry, no backoff/jitter, no circuit breaker, no fallback
    }
}
```

**Correct — timeout + breaker + bounded jittered-backoff retry + bulkhead + fallback:**

```java
// Resilience4j composition (decorators applied outermost→innermost):
@CircuitBreaker(name = "quotes", fallbackMethod = "cachedQuote")  // OPEN/HALF_OPEN + fallback (1,5)
@Bulkhead(name = "quotes")                                        // isolation (4)
@Retry(name = "quotes")                                           // bounded, expo backoff + jitter (2)
public Quote getQuote(String sym) {
    return webClient.get().uri(url, sym)
        .retrieve().bodyToMono(Quote.class)
        .timeout(Duration.ofSeconds(2))          // explicit read timeout (3); connect timeout on the client
        .block();
}
Quote cachedQuote(String sym, Throwable t) {     // declared fallback (5)
    return quoteCache.lastKnown(sym).orElse(Quote.unavailable(sym));
}
// resilience4j retry config: maxAttempts=3, intervalFunction=ofExponentialRandomBackoff(...)  (expo + jitter)
// a 429 from the server is honored via Retry-After, not retried immediately (6).
```

Verification: review-tier. Resilience is a failure-mode property with no compile-time signal — an unprotected call compiles and works perfectly until the dependency degrades. Verify by review against `specs/resilience-l0.yaml`: every outbound call has explicit connect+read timeouts; a circuit breaker with CLOSED/OPEN/HALF_OPEN; bounded retries with exponential backoff + jitter on transient/idempotent failures only; bulkhead isolation; a declared fallback on open/timeout/exhausted; a client rate limiter honoring 429/Retry-After. Confirm retries are not nested-multiplied across layers. When a fork-receiver wires a real IT (stub a hanging/failing dependency; assert fail-fast + fallback + bounded attempts), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [Chris Richardson — Circuit Breaker pattern (trip on threshold, probe after timeout)](https://microservices.io/patterns/reliability/circuit-breaker.html)

Reference: [Amazon Builders' Library — Timeouts, retries, and backoff with jitter](https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/)
