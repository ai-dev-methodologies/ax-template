# R7 SP41 — Scheduler L4 external evidence snapshot

**Fetched:** 2026-05-20
**Purpose:** anchor TD-2026-05-20-020 (scheduler L4 catalog row completion) with
≥1 external verbatim. Two quotes captured below; both URLs returned HTTP 200 OK on
2026-05-20 and the quoted substrings appear verbatim in the rendered page text.

These quotes anchor `templates/L4/scheduled-task/README.md` and the ADR; they are
not registered in `practices/upstream/_MANIFEST.yaml` (this file is a per-ADR
evidence ledger, not a `.snapshot.md` time-decay-guarded snapshot).

---

## Quote 1 — Spring Framework Reference §Scheduling

- **URL:** https://docs.spring.io/spring-framework/reference/integration/scheduling.html
- **Fetched at:** 2026-05-20
- **HTTP status:** 200 OK
- **Verbatim quote:**

> In addition to the TaskExecutor abstraction, Spring has a TaskScheduler SPI with a variety of methods for scheduling tasks to run at some point in the future.

- **Relevance:** confirms Spring framework ships a first-class scheduling SPI
  (TaskScheduler). The scheduled-task L4 domain wraps this primitive (or an
  equivalent like Quartz / ShedLock) with the REGISTER / LOCK / EXECUTE /
  IDEMPOTENCY families specified in `specs/scheduled-task-l0.yaml`.

---

## Quote 2 — Quartz Scheduler 2.3.0 Tutorial — Lesson 1

- **URL:** https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/tutorial-lesson-01.html
- **Fetched at:** 2026-05-20
- **HTTP status:** 200 OK
- **Verbatim quote:**

> Triggers do not fire (jobs do not execute) until the scheduler has been started

- **Relevance:** establishes that an independent, widely-deployed scheduling
  primitive (Quartz) has the same lifecycle semantics the scheduled-task L4
  domain encodes — registration of trigger-bearing jobs that fire only after
  the scheduler has been started. SCHED-REGISTER-001 + SCHED-EXECUTE-001 reflect
  this lifecycle.

---

## Notes

- These two quotes satisfy the R7 SP41 evidence-density floor (≥1 external
  verbatim per deliverable; 2 captured here for buffer).
- The PRD §4.4 evidence ledger documents the full fetch result table for R7.
- Korean engineering blog evidence: 5 hosts attempted (toss.tech, d2.naver.com,
  tech.kakao.com, techblog.woowahan.com, engineering.linecorp.com/ko); zero
  scheduler-specific Korean verbatim was extractable without fabrication.
  This is an explicit zero-Korean-verbatim cycle exception (PRD §4.4, M1
  closure). R8 evidence refresh re-attempts these hosts plus 카카오 / 라인
  / 토스 if any scheduler-relevant Korean post is published.
