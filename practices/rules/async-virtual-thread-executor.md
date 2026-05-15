---
title: Use JDK 21 virtual threads for blocking-IO workloads
impact: HIGH
impactDescription: "Cheap threads remove the headcount-limited platform-thread bottleneck for blocking I/O"
tags:
  - async
  - concurrency
  - jdk21
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ASYNC-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ASYNC-001
upstream:
  - "https://openjdk.org/jeps/444"
  - "https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html"
evidence:
  - upstream_id: jep-444-virtual-threads
    section: "JEP 444 — Virtual Threads (Final)"
    quote: "Thread.ofVirtual"
  - upstream_id: spring-task-execution
    section: "Spring Boot — Task Execution with virtual threads"
    quote: "virtual"
  - source_type: external
    citation: "JEP 444 — Virtual Threads (Final), Java 21"
    url: "https://openjdk.org/jeps/444"
---

## Use JDK 21 virtual threads for blocking-IO workloads

**Impact: HIGH — Cheap threads remove the headcount-limited platform-thread bottleneck for blocking I/O**

Platform threads (the default since Java 1.0) are 1:1 with OS threads — expensive to allocate, scarce on a process. A connection-pool-style design caps concurrency at a few hundred. Virtual threads (JEP 444, Java 21) are M:N over a small carrier pool: an idle virtual thread parked on a `socket.read()` consumes ~kilobytes, not megabytes. Workloads that spend most of their wall-clock blocked on I/O (HTTP outbound, JDBC, message-queue consumers) should switch to `Executors.newVirtualThreadPerTaskExecutor()`. CPU-bound work stays on platform threads — virtual threads do not make the CPU faster.

**Incorrect — capped platform-thread pool for an outbound-call workload:**

```java
ExecutorService pool = Executors.newFixedThreadPool(50);   // 50 concurrent ceiling
for (var batch : batches) {
    pool.submit(() -> http.fetch(batch.url()));            // 99% wall-clock waiting on socket
}
```

**Correct — virtual threads scale to the workload, not the pool size:**

```java
try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
    for (var batch : batches) {
        pool.submit(() -> http.fetch(batch.url()));
    }
}   // close blocks until all virtual threads finish
```

Verification: `./gradlew testPractices --tests "*VirtualThreadExecutor*"` submits a task to the executor and asserts `Thread.currentThread().isVirtual()` is `true` inside the body.

Reference: [JEP 444 — Virtual Threads](https://openjdk.org/jeps/444) · [Spring Boot — Task Execution](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html)
