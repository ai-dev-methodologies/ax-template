# Favorites/Bookmarks Blueprint — Plan (R34 retrofit)

> Retrofit at R37. Future domains MUST author at S1.

## Domain
Per-user single-state toggle on any polymorphic entity reference — like / save / bookmark / star.

## Scope
**In v1**: idempotent add/delete + global count behind JWT + quota cap + Authentication-only caller scoping.

**Out (`not_for`)**: heavy social graph, multi-state reactions, public bookmark sharing, multi-tenant, entity-existence validation.

**Deferred v2**: popularity index, list pagination, bulk check, list sort options, public count.

## Standards
- Twitter favorites API (single-state toggle, idempotent endpoints)
- WordPress meta subsystems (object_type + object_id polymorphism)
- RFC 9110 §9.3.5 — DELETE idempotency

## Acceptance gates
1. `./gradlew testFavorites` exits 0 (17 tests = 11 compliance + 1 quota + 5 violation proof)
2. verify-completion exit 0
3. Dogfood (R34-iter1): 0 real bugs, 1 methodology gap (VIOLATION proof retrofit) — acceptable per codex Q1 calibration only because newer pattern; tolerance is 4 consecutive zero-bug rounds, R34 is 2nd zero-bug
4. S7 audit produces decisions.md
