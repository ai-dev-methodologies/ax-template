# Tag Categorization Blueprint — Plan (R32 retrofit)

> **Retrofit notice**: This document was authored after the domain was implemented
> (R32 base commit `eeabe7a`, 2026-05-21). The catalog (METHODOLOGY.md Appendix C
> S1-S4) requires plan/progress/decisions BEFORE implementation; for R32 these
> were skipped. The dogfood loop (R32-iter1, commit `1f035bc`) surfaced the
> methodology gap as part of the integrated review at HEAD `4ba07d0`. This
> retrofit closes the gap by documenting decisions as they were actually made.
> Future domains MUST author this file at S1 (before code).

## Domain

Entity-agnostic tag system — admin-curated taxonomy + per-user attachment to any
polymorphic (entityType, entityId) reference.

## Scope

**In scope (v1)**:
- Tag entity with hierarchical parent/child relationship
- Slug auto-generation (NFKD + ASCII filter + Korean fallback)
- TagAttachment polymorphic edge with UNIQUE(tag_id, entity_type, entity_id)
- Idempotent attach/detach per HTTP RFC 9110 §9.3.5
- ROLE_ADMIN gate on tag definition mutations
- Cascade delete attachments when tag is deleted

**Out of scope (manifest `not_for`)**:
- User-generated tags / folksonomy
- High-volume tagging (millions of attachments)
- Many-to-many tag-of-tag graph relationships
- Multi-tenant tenant scoping
- Soft delete / archival

**Deferred to v2** (manifest `deferred_to_v2`):
- Tag search by name substring
- Tag usage statistics counter
- List pagination beyond 100 children
- Korean slug transliteration SPI

## External standards anchored

- WordPress taxonomy schema (term_taxonomy + term_relationships) — slug + parent
  + relationships model
- Strapi categories API — slug + parent + locale convention
- RFC 9110 §9.3.5 — HTTP DELETE idempotency contract
- Korean enterprise CMS 분류 체계 — admin-curated taxonomy + user attachment

## Acceptance gates

1. `./gradlew testTagCategorization` exits 0 with 12 spec items GREEN
2. `bash practices/scripts/verify-completion.sh` exits 0
3. dogfood loop (P1 김지훈 fintech, P2 이서연 B2B SaaS) finds either real bugs OR
   produces an explicit "attack model considered" paragraph per persona
4. S7 generalization audit produces decisions.md with classification per
   proposed rule

## Risk register

| Risk | Likelihood | Mitigation |
|---|---|---|
| Korean slug collision (multiple non-ASCII tags) | Medium | `tag-<uuid8>` fallback yields unique slugs per row |
| Parent cycle creation via re-parenting | Low | parentTagId `@Column(updatable=false)` — structural prevention |
| Phantom attachments after tag delete | Low | FK ON DELETE CASCADE |
| Cross-user IDOR via shared taxonomy | n/a | Tag definitions are global (admin-curated); not user-scoped |

## Dependencies

- Auth domain — JWT + ROLE_ADMIN authority claim
- Spring Security — `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` on mutations
