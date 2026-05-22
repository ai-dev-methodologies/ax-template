# Tag Categorization — Decisions (S7 Generalization Audit)

> METHODOLOGY.md Appendix C S7 retrofit (R37 closure). Required schema per
> ralplan codex critic iter-1 acceptance criteria.

## S7 Generalization Audit Table

Schema: `source_domain | candidate_rule | classification | target_file | duplicate_check_evidence | reviewer_decision`

| # | source_domain | candidate_rule | classification | target_file | duplicate_check (grep against practices/rules/) | reviewer_decision |
|---|---|---|---|---|---|---|
| 1 | tag-categorization | "Polymorphic entity_type/entity_id pair MUST use VARCHAR(64)/VARCHAR(255) and UNIQUE constraint backing idempotent attach" | reject_duplicate | n/a | `rg "polymorphic.*entity" practices/rules/` → no exact match BUT pattern is used in 3+ other domains (file-storage, favorites, comment-thread, activity-feed) — see decision below | rejected: deferred to architectural review at R37 (NOT a generic rule yet; pattern divergence between entity_type+entity_id vs object_type+object_id is intentional per codex review Q2). Will revisit when ≥5 domains use exact same triple-column shape. |
| 2 | tag-categorization | "Slug field MUST be @Column(updatable=false) — slug is a stable URL identifier and rename produces history loss" | new_generic | practices/rules/slug-immutable-after-creation.md (NOT YET CREATED — see decision below) | `rg "slug.*immutable" practices/rules/` → 0 matches | rejected: pattern appears in only 1 domain so far (tag-categorization). Promote to new_generic when ≥2 domains adopt slug-as-URL-identifier. |
| 3 | tag-categorization | "Cycle defense via parentTagId @Column(updatable=false) — re-parenting structurally impossible at JPA layer" | domain_specific | n/a | `rg "parent.*updatable.*false" practices/rules/` → 0 matches | accepted as domain-specific: this is a hierarchical-taxonomy concern, not general. Approval-workflow (R31) has a different parent semantics (approval steps). Document in blueprint manifest only. |
| 4 | tag-categorization | "Idempotent attach pattern: findByXxx → existing|insert, UNIQUE constraint backstop" | extend_existing | practices/rules/api-idempotency-key-required.md (from R29 report-export) | `rg "idempotent" practices/rules/` → matches api-idempotency-key-required.md (Payment R-various) | accepted as extension: pattern adds to existing idempotency rule with "UNIQUE constraint as DB-level backstop for race conditions" clause. Defer to follow-up commit (not blocking R37 closure). |

## Generalization summary

- 4 candidate rules surfaced
- 0 new_generic (none yet meet the ≥2-domain threshold)
- 1 extend_existing (api-idempotency-key-required, deferred to follow-up)
- 1 domain_specific (cycle defense — manifest only)
- 2 reject_duplicate / deferred (polymorphic pattern + slug-immutable — promote when more domains adopt)

## Anti-pattern observation

The post-hoc nature of this S7 audit highlights a methodology drift: when S7 is
skipped during initial domain implementation, candidate generalization signals
are lost to time. The R37 retrofit is recoverable but partial — patterns that
existed only in author's working memory at R32 commit time may not surface here.

## Future actions

- Promote "Polymorphic entity_type/entity_id with UNIQUE + FK CASCADE" to
  new_generic when 5+ domains adopt the exact triple-column shape. Currently:
  file-storage, tag-categorization, favorites, comment-thread, activity-feed
  (already 5) — but pattern divergence (object_type vs entity_type) needs
  resolution first per codex Q2.
- Watch for slug-immutable adoption in another domain to promote rule #2.
