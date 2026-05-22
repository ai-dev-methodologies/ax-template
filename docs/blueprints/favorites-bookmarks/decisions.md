# Favorites/Bookmarks — Decisions (S7 Audit, R37 retrofit)

## S7 Audit Table

| # | source_domain | candidate_rule | classification | target_file | duplicate_check | reviewer_decision |
|---|---|---|---|---|---|---|
| 1 | favorites-bookmarks | "Polymorphic (entity_type, entity_id) pair with UNIQUE constraint backing idempotent toggle" | reject_duplicate | n/a | `rg "polymorphic" practices/rules/` → no exact match; pattern in tag-categorization R32, comment-thread R36, file-storage already | rejected: same pattern as R32 candidate #1 — promote when architectural resolution of (entity_type vs object_type) divergence completes per codex Q2. |
| 2 | favorites-bookmarks | "Caller derived from Authentication.getName() — no userId path/query parameter" | new_generic | practices/rules/caller-authentication-only-no-userid-param.md (NOT YET CREATED) | `rg "Authentication.getName" practices/rules/` → 0 matches | accepted as new_generic candidate: structural defense against cross-user enumeration. Pattern in 5+ domains (file-storage, api-key management, favorites, activity-feed, comment-thread). Action: extract in next iteration. **Meets ≥2 domain threshold immediately.** |
| 3 | favorites-bookmarks | "HTTP DELETE idempotency: 204 even when target row absent (RFC 9110 §9.3.5)" | new_generic | practices/rules/http-delete-idempotency-rfc9110.md (NOT YET CREATED) | `rg "delete.idempot" practices/rules/` → 0 matches | accepted as new_generic candidate: tag-categorization, favorites, comment-thread, session-management all follow this. Action: extract in follow-up. |
| 4 | favorites-bookmarks | "Per-user quota soft-cap with HTTP 400 + specific error code (FAVORITES_QUOTA_EXCEEDED)" | extend_existing | practices/rules/api-quota-soft-cap.md (NOT YET) | session-management has similar pattern (max-active auto-revoke) but different semantics (revoke oldest vs reject new) | accepted as domain-specific: quota semantics differ (favorites rejects, sessions evicts). Document in manifest only. |

## Summary
- 4 candidates surfaced
- 2 strong new_generic candidates (caller-from-Authentication, HTTP DELETE idempotency) — IMMEDIATELY promotable, action in follow-up commit
- 1 reject_duplicate (same as R32)
- 1 domain_specific

## Cross-domain signal
The "caller from Authentication only" pattern is now confirmed in 5+ domains. This
is the strongest immediately-promotable rule from the R29-R36 cohort.

## Real bug retention rationale
R34 dogfood found 0 real bugs because the entity (Favorite) is structurally
minimal (5 columns) and the service is 4 methods. Honest assessment: simple
domain × design borrowed from R30 (api-key idempotent pattern) = nothing to find.
Not theater — but also doesn't prove dogfood loop quality. Next dogfood with a
more complex domain will be the real test.
