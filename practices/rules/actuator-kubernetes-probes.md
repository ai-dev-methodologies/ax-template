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

Reference: [Spring Boot — Kubernetes Probes](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes)
