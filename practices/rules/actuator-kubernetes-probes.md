---
title: Expose /actuator/health/liveness + /actuator/health/readiness
impact: HIGH
impactDescription: "Without separate probes K8s can't tell a wedged pod from one still booting"
tags:
  - actuator
  - kubernetes
  - probes
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ACTUATOR-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ACTUATOR-001
upstream:
  - "https://docs.spring.io/spring-boot/reference/actuator/endpoints.html"
evidence:
  - upstream_id: spring-boot-actuator-endpoints
    section: "Spring Boot Actuator — Kubernetes Probes (liveness / readiness)"
    quote: "liveness"
  - source_type: external
    citation: "Spring Boot Reference — Kubernetes Probes"
    url: "https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes"
---

## Expose /actuator/health/liveness + /actuator/health/readiness

**Impact: HIGH — Without separate probes K8s can't tell a wedged pod from one still booting**

A single `/actuator/health` endpoint conflates two questions: "is the process alive" and "is the process ready to serve traffic". Kubernetes (and most orchestrators) needs them separately. A liveness probe failing → kill + restart the pod. A readiness probe failing → keep the pod running but stop routing traffic to it. Without dedicated endpoints the orchestrator picks one default behavior for both signals and gets at least one of them wrong (either restart-loops a slow-starting pod, or routes traffic at a pod that hasn't finished initializing).

**Incorrect — only the aggregated /actuator/health (default):**

```yaml
management:
  endpoint:
    health:
      show-details: when-authorized
  endpoints:
    web:
      exposure:
        include: health,info
```

**Correct — probes enabled + states explicitly tracked:**

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when-authorized
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
  endpoints:
    web:
      exposure:
        include: health,info,mappings
```

Verification: `./gradlew testPractices --tests "*KubernetesProbes*"` starts the app on a random port and asserts `/actuator/health/liveness` and `/actuator/health/readiness` both return 200 with UP/DOWN status.

### Liveness MUST NOT gate on a downstream dependency

Exposing the probes is only half the contract (`specs/health-check-l0.yaml#HEALTH-LIVENESS-001`). The liveness group MUST stay `livenessState`-only — never add a dependency health indicator (`db`, `redis`, `mongo`, `kafka`, …) to it. A liveness failure restarts the pod; if liveness gates on the database, a transient DB blip flips liveness DOWN, Kubernetes restarts every pod, the restarts hammer the recovering DB, and a recoverable outage becomes a self-amplifying one. Dependency checks belong in the **readiness** group (a DOWN readiness drains traffic without a restart).

**Incorrect — a dependency in the liveness group restart-loops the fleet on a DB blip:**

```yaml
management:
  endpoint:
    health:
      group:
        liveness:
          include: livenessState, db   # ← DB outage now restarts every pod
```

**Correct — liveness is process-only; dependencies gate readiness:**

```yaml
management:
  endpoint:
    health:
      group:
        readiness:
          include: readinessState, db   # ← DB outage drains traffic, no restart
```

Spring Boot's default liveness group is already `livenessState`-only, so the safe posture needs no config. `practices/evals/liveness_probe_no_downstream_guard.sh` (run-all-guards [63]) scans `application*.yml` and BLOCKS the push if a `liveness` group `include` lists any downstream indicator.

Reference: [Spring Boot — Kubernetes Probes](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes) · [Kubernetes — Liveness/Readiness/Startup Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)
