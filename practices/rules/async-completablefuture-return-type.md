---
title: "@Async methods must return CompletableFuture, never void"
impact: HIGH
impactDescription: "void @Async swallows exceptions inside the executor and breaks await/compose at the caller"
tags:
  - async
  - spring
  - exception-handling
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ASYNC-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ASYNC-002
upstream:
  - "https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html"
  - "https://docs.spring.io/spring-framework/reference/integration/scheduling.html"
evidence:
  - upstream_id: spring-task-execution
    section: "Spring — @Async return types and exception propagation"
    quote: "Async"
  - upstream_id: spring-scheduling
    section: "Spring — @Async / CompletableFuture"
    quote: "CompletableFuture"
  - source_type: external
    citation: "Spring Framework Reference — @Async"
    url: "https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async"
---

## @Async methods must return CompletableFuture, never void

**Impact: HIGH — void @Async swallows exceptions inside the executor and breaks await/compose at the caller**

The `@Async` proxy returns immediately and dispatches the body to the task executor. A `void` return type means the caller cannot await the result and — more dangerous — cannot observe an exception thrown inside the executor; the exception is logged (sometimes) and silently lost. Returning `CompletableFuture<T>` lets the caller `.thenApply(...)` / `.get()` / `.exceptionally(...)`, lets composition work, and surfaces exceptions through the future's completion state.

**Incorrect — void return swallows exceptions:**

```java
@Service
public class ReportService {
    @Async
    public void generateReport(ReportRequest req) {   // exceptions disappear into the executor
        if (!req.valid()) throw new IllegalStateException("bad input");
        sink.write(buildReport(req));
    }
}
```

**Correct — CompletableFuture lets the caller await + observe failure:**

```java
@Service
public class ReportService {
    @Async
    public CompletableFuture<Path> generateReport(ReportRequest req) {
        if (!req.valid()) {
            return CompletableFuture.failedFuture(new IllegalStateException("bad input"));
        }
        return CompletableFuture.completedFuture(sink.write(buildReport(req)));
    }
}
```

Verification: `./gradlew testPractices --tests "*SpringAsyncReturnsCompletableFuture*"` asserts via reflection that the @Async method's return type is `CompletableFuture` and awaits a result through the proxy.

Reference: [Spring Framework — @Async](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async)
