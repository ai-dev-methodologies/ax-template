# Favorites/Bookmarks Blueprint — Progress (R34 retrofit)

- Phase 0 Plan: RETROFIT R37
- Phase 1 Spec Trio: R34 base `848cb39` (12 items / 4 families)
- Phase 2 TDD: GAP — single commit, no separate RED
- Phase 3 Impl: Favorite entity + FavoriteService + FavoriteController + 5 endpoints + UNIQUE(user_id, entity_type, entity_id) + quota enforcement
- Phase 4 Verify: 17/17 PASS
- Phase 5 Dogfood (R34-iter1):
  - P1: phantom favorite (manifest ✓), top-N popular (deferred ✓), bulk check (deferred ✓), race (DataIntegrityViolation ✓)
  - P2: multi-tenant (not_for ✓), API key 통합 (R30 SecurityConfig ✓), 자동 정리 (out of scope)
  - **Real bugs**: 0
  - **Methodology gaps**: 1 (VIOLATION proof 부재 → FavoriteViolationProofTest 추가)
- Phase 6 Iter1: FavoriteViolationProofTest (5 structural assertions)
- Phase 7 S7: see decisions.md
