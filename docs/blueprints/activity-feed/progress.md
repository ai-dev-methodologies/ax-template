# Activity Feed Blueprint — Progress (R35 retrofit)

- Phase 0 Plan: RETROFIT R37
- Phase 1 Spec Trio: R35 base `bc5a343` (12 items / 4 families)
- Phase 2 TDD: GAP — single commit
- Phase 3 Impl: ActivityEvent + ActivityRead + ActivityService (idempotent publish, fan-out-read, mark-read) + 5 endpoints + UNIQUE constraints
- Phase 4 Verify: 18/18 PASS
- Phase 5 Dogfood (R35-iter1):
  - P1: audience cap (@Size(max=100) ✓), idempotent publish (UNIQUE ✓)
  - P2: multi-tenant (not_for ✓), realtime push (deferred ✓), verb taxonomy (deferred ✓)
  - **Real bugs**: 0
  - **Methodology gaps**: 1 (VIOLATION proof)
- Phase 6 Iter1: ActivityViolationProofTest (6 structural assertions)
- Phase 7 S7: see decisions.md
