# Coverage Map — generated report

> GENERATED FILE — do not hand-edit. Regenerate with:
> `bash practices/consumer-proof/engine/coverage-report.sh --write`

- generated_at (UTC): 2026-07-19T17:20:02Z
- head_sha: bdcb0703f343c7dbde298d7d24b1a3e6d3b6bc53

## C_total = 0.7328

- C_S1 (capability, 65 cells) = 0.7538
- C_S2 (invariant, 31 cells)  = 0.7955
- C_S3 (composition, 11 cells) = 0.5455

## Status counts per tier

| Tier | covered | partial | gap | not-applicable |
|---|---|---|---|---|
| S1 | 44 | 10 | 11 | 10 |
| S2 | 19 | 11 | 1 | 5 |
| S3 | 1 | 10 | 0 | 0 |

## Top uncovered cells (ranked by w·(1−score))

| rank | cell | tier | weight | status | value |
|---|---|---|---|---|---|
| 1 | S2.AUDIT-PII.XB | S2 | 2 | partial | 1.00 |
| 2 | S2.AUTHZ.FE | S2 | 2 | partial | 1.00 |
| 3 | S2.AUTHZ.XB | S2 | 2 | partial | 1.00 |
| 4 | S2.IDEMPOTENCY-CONCURRENCY.XB | S2 | 2 | partial | 1.00 |
| 5 | S2.MONEY-QUANTITY.XB | S2 | 2 | partial | 1.00 |
| 6 | S2.QUERY-BOUNDS.XB | S2 | 1 | gap | 1.00 |
| 7 | S1.activity-feed.XB | S1 | 1 | gap | 1.00 |
| 8 | S1.api-key.XB | S1 | 1 | gap | 1.00 |
| 9 | S1.approval-workflow.XB | S1 | 1 | gap | 1.00 |
| 10 | S1.comment-thread.XB | S1 | 1 | gap | 1.00 |
| 11 | S1.data-subject-rights.XB | S1 | 1 | gap | 1.00 |
| 12 | S1.email-outbox.XB | S1 | 1 | gap | 1.00 |
| 13 | S1.favorites-bookmarks.XB | S1 | 1 | gap | 1.00 |
| 14 | S1.file-storage.XB | S1 | 1 | gap | 1.00 |
| 15 | S1.scheduled-task.XB | S1 | 1 | gap | 1.00 |

