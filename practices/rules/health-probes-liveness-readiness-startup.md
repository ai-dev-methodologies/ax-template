---
title: A service MUST expose distinct liveness, readiness, and startup health endpoints — and fail readiness before shutdown drain
impact: HIGH
impactDescription: "Collapsing liveness and readiness into one endpoint corrupts orchestration: if the single check fails when a downstream dependency is briefly down, the orchestrator RESTARTS a perfectly healthy process (liveness semantics) instead of just removing it from rotation (readiness semantics) — turning a transient dependency blip into a restart storm. And a service that does not fail readiness before it stops draining sheds in-flight requests on every deploy."
tags:
  - health-check
  - liveness
  - readiness
  - kubernetes
  - graceful-shutdown
  - observability
spec_ref: "specs/health-check-l0.yaml#HEALTH-LIVENESS-001"
verification:
  type: review
  source: "specs/health-check-l0.yaml#HEALTH-LIVENESS-001"
  pattern: "A service MUST expose DISTINCT health endpoints with distinct semantics. Liveness reports only whether the process is alive and should be restarted if it is wedged — it MUST NOT check downstream dependencies (a dependency outage must not trigger a restart) (HEALTH-LIVENESS-001). Readiness reports whether the instance can serve traffic right now, checking critical dependencies, so the orchestrator removes a not-ready instance from rotation without restarting it (HEALTH-READINESS-001). A startup probe gives a slow-starting container grace before liveness begins, so a long boot is not mistaken for a hang (HEALTH-STARTUP-001). Downstream dependency health is surfaced with a degraded/down distinction, and a non-critical dependency being down MUST NOT fail liveness (HEALTH-DEPENDENCY-001). The health response SHOULD use the application/health+json media type with a top-level status (HEALTH-FORMAT-001). On shutdown, readiness MUST flip to fail BEFORE the server stops accepting and begins draining, so in-flight requests complete and the orchestrator stops routing new ones first (HEALTH-GRACEFUL-001). Reject a single combined health endpoint used for both liveness and readiness, a liveness check that calls a database/remote dependency, or a shutdown that drains before failing readiness."
upstream:
  - "https://kubernetes.io/docs/concepts/configuration/liveness-readiness-startup-probes/"
  - "https://datatracker.ietf.org/doc/html/draft-inadarei-api-health-check"
evidence:
  - source_type: external
    citation: "Kubernetes Documentation — Liveness, Readiness, and Startup Probes (liveness restart)"
    url: "https://kubernetes.io/docs/concepts/configuration/liveness-readiness-startup-probes/"
    quote: "If a container fails its liveness probe more times than the configured tolerance, the kubelet restarts that container."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "Kubernetes Documentation — Liveness, Readiness, and Startup Probes (readiness removes from endpoints)"
    url: "https://kubernetes.io/docs/concepts/configuration/liveness-readiness-startup-probes/"
    quote: "If the readiness probe returns a failed state, the EndpointSlice controller removes the Pod's IP address from the EndpointSlices of all Services that match the Pod."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "Kubernetes Documentation — Liveness, Readiness, and Startup Probes (startup for slow starters)"
    url: "https://kubernetes.io/docs/concepts/configuration/liveness-readiness-startup-probes/"
    quote: "Startup probes are useful for Pods that have containers that take a long time to come into service."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## A service MUST expose distinct liveness, readiness, and startup health endpoints — and fail readiness before shutdown drain

**Impact: HIGH — Liveness and readiness answer two different questions and drive two different orchestrator actions. Liveness: "is this process wedged — should it be restarted?" Per Kubernetes, *if a container fails its liveness probe more times than the configured tolerance, the kubelet restarts that container*. Readiness: "can this instance serve traffic right now?" — *if the readiness probe returns a failed state, the EndpointSlice controller removes the Pod's IP address from the EndpointSlices of all Services that match the Pod* (removed from rotation, NOT restarted). Collapse them into one endpoint that checks a database, and the moment that database blips the orchestrator restarts every healthy instance — a transient dependency outage becomes a self-inflicted restart storm. The two MUST be distinct.**

There are six load-bearing requirements — the items of `specs/health-check-l0.yaml`, all governed by this rule.

**1. Liveness — process alive only (HEALTH-LIVENESS-001).** The liveness endpoint reports ONLY whether the process is alive and able to make progress (not deadlocked). It MUST NOT call downstream dependencies — a dependency being down is not a reason to restart this process, and doing so causes restart storms.

**2. Readiness — can serve now (HEALTH-READINESS-001).** The readiness endpoint checks the critical dependencies this instance needs to serve a request (DB pool, required downstreams). A failed readiness removes the instance from rotation without a restart, so it recovers when the dependency does.

**3. Startup — slow-boot grace (HEALTH-STARTUP-001).** A startup probe protects a slow-starting container: per k8s, *startup probes are useful for Pods that have containers that take a long time to come into service* — liveness only begins after startup succeeds, so a long boot is never mistaken for a hang and killed mid-start.

**4. Dependency health + degraded status (HEALTH-DEPENDENCY-001).** The health detail distinguishes a hard `down` from a `degraded` (a non-critical dependency unavailable). A non-critical dependency being down feeds `degraded`, never a liveness failure.

**5. health+json format (HEALTH-FORMAT-001).** The health response SHOULD use the `application/health+json` media type with a top-level `status` (`pass`/`warn`/`fail`) and per-check detail, so any monitor parses it uniformly.

**6. Graceful shutdown — readiness fails first (HEALTH-GRACEFUL-001).** On SIGTERM, readiness MUST flip to fail FIRST (so the orchestrator stops routing new requests), THEN the server stops accepting and drains in-flight requests to completion, THEN exits. Draining before failing readiness sheds live requests on every deploy.

**Incorrect — one combined endpoint that checks the database; a DB blip restarts every healthy instance:**

```java
@GetMapping("/health")                 // VIOLATION: used as BOTH liveness and readiness
public ResponseEntity<?> health() {
    jdbc.execute("SELECT 1");          // VIOLATION: liveness now depends on the DB → DB down ⇒ kubelet restarts the process
    return ResponseEntity.ok("UP");
}
```

**Correct — distinct probes; liveness is dependency-free; readiness checks deps; readiness fails before drain:**

```java
// Spring Boot Actuator: management.endpoint.health.probes.enabled=true exposes
//   /actuator/health/liveness   — group: livenessState only (NO dependency checks)  (HEALTH-LIVENESS-001)
//   /actuator/health/readiness  — group: readinessState + db + critical downstreams (HEALTH-READINESS-001)
// startupProbe points at readiness with a generous failureThreshold (HEALTH-STARTUP-001)

@Component
class ReadinessShutdownListener {
    @EventListener
    void onShutdown(ContextClosedEvent e) {
        availability.publish(ReadinessState.REFUSING_TRAFFIC); // fail readiness FIRST (HEALTH-GRACEFUL-001)
        // server.shutdown=graceful then drains in-flight requests before exit
    }
}
```

Verification: review-tier. Probe semantics are an orchestration-contract property with no compile-time signal — a combined endpoint compiles and looks healthy until a dependency blips in production and triggers restarts. Verify by review against `specs/health-check-l0.yaml`: liveness and readiness are distinct endpoints; liveness makes no dependency calls; readiness checks critical dependencies; a startup probe grants slow-boot grace; dependency health distinguishes degraded vs down; the response uses health+json; shutdown fails readiness before draining. When a fork-receiver wires a real IT asserting liveness stays UP while a downstream is forced down (and readiness goes DOWN), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [Kubernetes — Liveness, Readiness and Startup Probes](https://kubernetes.io/docs/concepts/configuration/liveness-readiness-startup-probes/)
