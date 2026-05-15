---
title: Use fixedDelay for variable-duration tasks; reserve fixedRate for instant heartbeats
impact: MEDIUM
impactDescription: "fixedRate on slow tasks piles invocations and exhausts the scheduler pool"
tags:
  - async
  - scheduled
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ASYNC-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ASYNC-003
upstream:
  - "https://docs.spring.io/spring-framework/reference/integration/scheduling.html"
evidence:
  - upstream_id: spring-scheduling
    section: "Spring — @Scheduled (fixedDelay vs fixedRate)"
    quote: "fixedDelay"
  - source_type: external
    citation: "Spring Framework Reference — @Scheduled attributes"
    url: "https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-scheduled"
---

## Use fixedDelay for variable-duration tasks; reserve fixedRate for instant heartbeats

**Impact: MEDIUM — fixedRate on slow tasks piles invocations and exhausts the scheduler pool**

`@Scheduled(fixedDelay = N)` waits N ms AFTER the previous run finishes. A cleanup that occasionally takes 5 minutes simply waits another N ms before the next run — it cannot stack. `@Scheduled(fixedRate = N)` invokes every N ms from the previous invocation's START. If a run takes longer than N ms, the next one starts immediately and invocations queue up; under load the scheduler pool exhausts and other scheduled work starves. Use `fixedRate` only for tasks that finish in noticeably less than N ms (cheap heartbeats, cadence-sensitive metrics).

**Incorrect — fixedRate on a long-running cleanup:**

```java
@Scheduled(fixedRate = 60_000L)        // 60s cadence — if cleanup takes 90s, runs stack
public void cleanup() {
    purgeOldRecords();                 // sometimes 5 minutes
}
```

**Correct — fixedDelay on a long-running cleanup:**

```java
@Scheduled(fixedDelay = 60_000L)       // 60s gap AFTER the previous run finishes
public void cleanup() {
    purgeOldRecords();
}
```

Verification: `./gradlew testPractices --tests "*ScheduledFixedDelay*"` asserts via reflection that the cleanup fixture has `fixedDelay > 0` and `fixedRate ≤ 0`, while the heartbeat fixture has the inverse.

Reference: [Spring Framework — @Scheduled attributes](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-scheduled)
