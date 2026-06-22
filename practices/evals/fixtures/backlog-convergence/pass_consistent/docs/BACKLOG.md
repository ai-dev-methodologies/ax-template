# BACKLOG — fixture (pass_consistent)

Minimal fixture for backlog_convergence_integrity_guard.sh. Exercises range
expansion (P0-1 ~ P0-3), a range-plus-extra (P1-6~7 + P1-9), a slash list
(P1-3 / P1-4 / P1-5), and a denominator-excluded lettered sub-item (P2-1a).
Every table cell agrees with the checkbox-derived truth → guard exits 0.

## 현재 수렴률

| Tier | 전체 | closed | 수렴률 |
|---|---|---|---|
| P0 (expiry-bound) | 4 | 4 | **100%** |
| P1 (generic backlog) | 8 | 5 | 63% |
| P2 (verification escapes) | 2 | 1 | 50% |
| P3 (industry-niche) | 4 | 0 | 0% |
| **P0–P3 합계 (수렴 분모)** | **18** | **10** | **~56%** |
| P4 (trigger-bound — 분모 제외) | 99 | — | by-design |

---

## P0 — expiry-bound

- [x] **P0-1 ~ P0-3** demo range closure — closed.
- [x] **P0-4** single closure — closed.

## P1 — generic signature backlog

- [x] P1-1~2 demo trio — closed.
- [ ] P1-3 alpha / P1-4 beta / P1-5 gamma (slash list, all open)
- [x] P1-6~7 + P1-9 demo range-plus-extra — closed.

## P2 — verification escapes

- [x] P2-1 demo escape — closed.
- [ ] P2-1a residual sub-item *(P2-1 잔여, 분모 불변)* — excluded from denominator.
- [ ] P2-2 demo open escape.

## P3 — industry-niche deferrals

- [ ] P3-1 ~ P3-4 demo niche ×4
