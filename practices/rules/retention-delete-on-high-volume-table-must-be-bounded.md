---
title: Scheduled retention/purge on a high-volume table MUST drop partitions or batch the DELETE — never one unbounded DELETE ... WHERE created_at < cutoff
impact: HIGH
impactDescription: "A single unbounded age-based DELETE against an append-only / high-write table scans and locks an open-ended row set, bloats the table with dead tuples, holds one long transaction, and times out under statement_timeout — the retention sweep that was meant to reclaim space instead stalls writes and accumulates lock contention at scale."
tags:
  - persistence
  - performance
  - retention
spec_ref: "specs/scheduled-task-l0.yaml#SCHED-RETENTION-001"
verification:
  type: review
  source: "specs/scheduled-task-l0.yaml#SCHED-RETENTION-001"
  pattern: "The scheduled retention job removes the aged range with DROP TABLE / DETACH PARTITION on a RANGE-partitioned-by-created_at table, OR a `DELETE ... LIMIT N` loop committing each batch — and contains no single open-ended `DELETE ... WHERE created_at < cutoff` against a high-write-volume table."
upstream:
  - "https://www.postgresql.org/docs/current/ddl-partitioning.html"
  - "https://docs.gitlab.com/development/database/iterating_tables_in_batches/"
evidence:
  - source_type: external
    citation: "PostgreSQL 16 Documentation — 5.12.1 Overview (Table Partitioning), benefits of partitioning for bulk loads and deletes"
    url: "https://www.postgresql.org/docs/current/ddl-partitioning.html"
    quote: "Dropping an individual partition using DROP TABLE, or doing ALTER TABLE DETACH PARTITION, is far faster than a bulk operation. These commands also entirely avoid the VACUUM overhead caused by a bulk DELETE."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "PostgreSQL 16 Documentation — 5.12.6 Best Practices for Declarative Partitioning (design so data removed at once sits in one partition for fast DETACH)"
    url: "https://www.postgresql.org/docs/current/ddl-partitioning.html"
    quote: "An entire partition can be detached fairly quickly, so it may be beneficial to design the partition strategy in such a way that all data to be removed at once is located in a single partition."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "GitLab Development Documentation — Iterating tables in batches, the 'Slow iteration' subsection (under 'Improve filtering with each_batch'): PK-index iteration is safe from statement timeouts"
    url: "https://docs.gitlab.com/development/database/iterating_tables_in_batches/"
    quote: "The iteration uses the primary key index (on the id column) which makes it safe from statement timeouts."
    quoted_at: "2026-06-01"
---

## Scheduled retention/purge on a high-volume table MUST drop partitions or batch the DELETE — never one unbounded DELETE ... WHERE created_at < cutoff

**Impact: HIGH — one open-ended age-based DELETE locks and bloats a hot table and times out at scale**

This is the DELETE-path mirror of the chunked-import rule. Imports bound the *write* work set with per-chunk transactions; retention sweeps must bound the *delete* work set the same way. The dangerous shape is a scheduled job that reclaims space with a single statement:

```sql
DELETE FROM tracking_event WHERE created_at < now() - interval '90 days';
```

Against an append-only / high-write-volume table (tracking events, telemetry, audit rows, notifications, webhook deliveries) this one statement:

1. **Scans and locks an open-ended row set** — the predicate matches everything older than the cutoff, which on a hot table can be tens of millions of rows in a single pass.
2. **Holds one long transaction** — every matched row's lock and undo/WAL is held until the whole statement commits, blocking autovacuum and starving concurrent writers.
3. **Bloats the table** — a bulk `DELETE` leaves dead tuples that VACUUM must later reclaim; the cleanup debt can exceed the space the delete freed.
4. **Times out** — under a configured `statement_timeout` the sweep is killed mid-flight, having done work but committed nothing, and retries from the same unbounded cutoff next cycle.

Bound the blast radius with one of two patterns.

**Incorrect — one unbounded age-based DELETE against a high-volume table:**

```java
@Scheduled(cron = "0 0 3 * * *")
@Transactional                                   // VIOLATION: whole purge in one transaction
public void purgeOldEvents() {
    Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
    // VIOLATION: single open-ended DELETE — scans/locks every aged row at once
    jdbc.update("DELETE FROM tracking_event WHERE created_at < ?", Timestamp.from(cutoff));
}
```

**Correct (a) — drop whole time-range PARTITIONS (preferred for range-partitioned tables):**

```sql
-- tracking_event is RANGE-partitioned by created_at, one partition per month:
--   CREATE TABLE tracking_event_2026_02 PARTITION OF tracking_event
--     FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
-- Retention = drop the whole expired partition. Metadata-only, no row scan, no VACUUM debt.
ALTER TABLE tracking_event DETACH PARTITION tracking_event_2026_02;
DROP TABLE tracking_event_2026_02;
```

**Correct (b) — bounded batch DELETE in a loop (fallback for non-partitioned tables):**

```java
public static final int BATCH_SIZE = 5_000;
public static final long BATCH_PAUSE_MS = 200L;

// RetentionSweeper — no @Transactional on the loop. The per-batch transaction lives on a
// SEPARATE bean (BatchDeleter) so Spring's proxy is actually crossed; a self-call to a
// @Transactional method in THIS bean would bypass the proxy and run in JDBC autocommit,
// making the "each batch is its own transaction" promise false.
private final BatchDeleter batchDeleter;

@Scheduled(cron = "0 0 3 * * *")
public void purgeOldEvents() throws InterruptedException {      // no outer @Transactional
    Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
    int deleted;
    do {
        deleted = batchDeleter.deleteBatch(cutoff);             // cross-bean call → real per-batch tx
        if (deleted > 0) Thread.sleep(BATCH_PAUSE_MS);          // let autovacuum / replicas catch up
    } while (deleted == BATCH_SIZE);
}

@Component
class BatchDeleter {
    @Transactional                                              // honored — call crosses the proxy boundary
    public int deleteBatch(Instant cutoff) {
        // LIMIT bounds the locked row set per transaction; commit between batches caps lock + bloat
        return jdbc.update("""
            DELETE FROM tracking_event
             WHERE id IN (SELECT id FROM tracking_event
                           WHERE created_at < ? ORDER BY id LIMIT ?)
            """, Timestamp.from(cutoff), BATCH_SIZE);
    }
}
```

Prefer (a) when the table is RANGE-partitioned by `created_at` — dropping a partition is a catalog operation that scans no rows and incurs no VACUUM overhead. Use (b) when the table is not partitioned: a `LIMIT`-bounded loop with a commit and a short sleep between batches keeps each transaction's lock footprint and dead-tuple churn small enough to stay within `statement_timeout` and out of the way of concurrent writers. Either way, the invariant holds: a retention sweep on a high-volume table never issues one unbounded `DELETE ... WHERE created_at < cutoff`.

Verification: review the scheduled retention job + table DDL — confirm a `DROP`/`DETACH PARTITION` path on a range-partitioned-by-`created_at` table, or a `DELETE ... LIMIT N` batch loop committing each iteration; flag any single open-ended `DELETE ... WHERE created_at < cutoff` against an append-only / high-write table (see `specs/scheduled-task-l0.yaml#SCHED-RETENTION-001`).

Reference: [PostgreSQL — Table Partitioning: Bulk loads and deletes via dropping partitions](https://www.postgresql.org/docs/current/ddl-partitioning.html)

Reference: [GitLab — Iterating tables in batches (EachBatch): bounded iteration stays safe from statement timeouts](https://docs.gitlab.com/development/database/iterating_tables_in_batches/)
