# Coverage Map — generated report

> GENERATED FILE — do not hand-edit. Regenerate with:
> `bash practices/consumer-proof/engine/coverage-report.sh --write`

- generated_at (UTC): 2026-07-27T10:02:13Z
- head_sha: 60a23bb7d432e7640b319482b5a13a80f8de6e05

## C_total = 0.7786

- C_S1 (capability, 65 cells) = 0.7615
- C_S2 (invariant, 31 cells)  = 0.9205
- C_S3 (composition, 11 cells) = 0.5455

## Status counts per tier

| Tier | covered | partial | gap | not-applicable |
|---|---|---|---|---|
| S1 | 44 | 11 | 10 | 10 |
| S2 | 26 | 5 | 0 | 5 |
| S3 | 1 | 10 | 0 | 0 |

## Top uncovered cells (ranked by w·(1−score))

| rank | cell | tier | weight | status | value |
|---|---|---|---|---|---|
| 1 | S2.AUTHZ.XB | S2 | 2 | partial | 1.00 |
| 2 | S2.IDEMPOTENCY-CONCURRENCY.XB | S2 | 2 | partial | 1.00 |
| 3 | S1.activity-feed.XB | S1 | 1 | gap | 1.00 |
| 4 | S1.api-key.XB | S1 | 1 | gap | 1.00 |
| 5 | S1.comment-thread.XB | S1 | 1 | gap | 1.00 |
| 6 | S1.data-subject-rights.XB | S1 | 1 | gap | 1.00 |
| 7 | S1.email-outbox.XB | S1 | 1 | gap | 1.00 |
| 8 | S1.favorites-bookmarks.XB | S1 | 1 | gap | 1.00 |
| 9 | S1.file-storage.XB | S1 | 1 | gap | 1.00 |
| 10 | S1.scheduled-task.XB | S1 | 1 | gap | 1.00 |
| 11 | S1.session-management.XB | S1 | 1 | gap | 1.00 |
| 12 | S1.tag-categorization.XB | S1 | 1 | gap | 1.00 |
| 13 | S3.api-gateway-relay | S3 | 2 | partial | 1.00 |
| 14 | S3.b2b-admin | S3 | 2 | partial | 1.00 |
| 15 | S3.booking | S3 | 2 | partial | 1.00 |

