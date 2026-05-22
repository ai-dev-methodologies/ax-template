# Tag Categorization Blueprint — Progress (R32 retrofit)

> Retrofit ledger. R32 base + R32-iter1 + R37 retrofit closure.

## Phases

### Phase 0 — Plan (RETROFIT)
- Authored after-the-fact at R37 retrofit. Future domains MUST do this at S1.

### Phase 1 — Spec Trio (R32 base `eeabe7a`, 2026-05-21)
- ✅ `specs/tag-categorization-l0.yaml` — 12 spec items / 4 families
- ✅ `contracts/tag-categorization-openapi.yaml` — 8 endpoints
- ✅ `blueprints/tag-categorization-manifest.yaml` — slug policy + ROLE_ADMIN + deferred_to_v2

### Phase 2 — TDD (R32 base)
- **GAP**: TDD RED→GREEN not separately committed. Spec + impl + tests in single commit.
- Tests authored alongside impl, not before. After-fact test pattern (METHODOLOGY violation
  acknowledged in retrofit decisions.md).

### Phase 3 — Implementation (R32 base)
- ✅ Tag entity (slug + parentTagId immutable)
- ✅ TagAttachment polymorphic edge
- ✅ TagSlugger (NFKD + ASCII + Korean fallback)
- ✅ TagService + TagController + 8 endpoints
- ✅ V019 migration with UNIQUE + FK CASCADE

### Phase 4 — Verification (R32 base)
- ✅ `./gradlew testTagCategorization` → 19/19 PASS
- ✅ `verify-completion.sh` → exit 0

### Phase 5 — Dogfood (R32-iter1 `1f035bc`, 2026-05-21)
- P1 김지훈 (fintech 보험상품 카테고리):
  - 한글 slug fallback → manifest 명시 ✓
  - Tag 사용량 통계 → v2 deferred ✓
  - 동시 attach race → `DataIntegrityViolationException` catch ✓
- P2 이서연 (B2B SaaS multi-tenant):
  - Multi-tenant → manifest not_for ✓
  - Tag 검색 → v2 deferred ✓
  - Tag rename UX → 의도된 design ✓
- **Real bugs found**: 0
- **Methodology gaps found**: 1 (VIOLATION proof test 부재)

### Phase 6 — Iter1 closure (R32-iter1)
- ✅ TagViolationProofTest (8 structural assertions)
- ✅ blueprints/tag-categorization-manifest.yaml — deferred_to_v2 section added

### Phase 7 — S7 Generalization audit (R37 retrofit)
- See decisions.md

## Outstanding

- No outstanding items as of R37 retrofit closure.
