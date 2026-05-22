# Session Management — Decisions (S7 Generalization Audit, R37 retrofit)

## S7 Audit Table

| # | source_domain | candidate_rule | classification | target_file | duplicate_check | reviewer_decision |
|---|---|---|---|---|---|---|
| 1 | session-management | "Fail-closed SPI default: unknown identifier / expired / revoked all return 'invalid'" | new_generic | practices/rules/spi-fail-closed-by-default.md (NOT YET CREATED) | `rg "fail.closed" practices/rules/` → 0 matches | rejected for promotion now: pattern exists in 1 domain (SessionRevocationCheck). Promote when 2nd domain adopts the fail-closed SPI shape. Candidate next domains: api-key revocation check, feature-flag eval. |
| 2 | session-management | "Audit-grade row preservation: status flip not hard-delete on logout/revoke" | extend_existing | practices/rules/soft-delete-audit-trail.md (NOT YET — extends comment-thread R36 soft-delete pattern) | `rg "soft.delete" practices/rules/` → 0 matches | accepted as extend_existing pending: comment-thread (R36) also uses soft-delete with audit preservation. Pattern is real but needs cross-domain rule extraction in follow-up commit. |
| 3 | session-management | "Raw PII (IP/UA) stored on entity but @JsonIgnore — masked/summarized in DTO" | new_generic | practices/rules/pii-masked-at-dto-boundary.md (NOT YET CREATED) | `rg "JsonIgnore" practices/rules/` → 0 matches | accepted as new_generic candidate: this is a privacy-posture pattern reusable across domains. Promote in follow-up when 2nd domain demonstrates the pattern (api-key already has @JsonIgnore on storage key — qualifies). Action: create rule in next iteration. |
| 4 | session-management | "Per-user record max cap with auto-revoke-oldest on overflow (graceful instead of reject)" | domain_specific | n/a | n/a | accepted as domain-specific: this is a session lifecycle policy choice (vs api-key which rejects with TOO_MANY_KEYS). Different domains have different cap semantics — document in manifest only. |
| 5 | session-management | "Clock-based expiration as derived predicate, not stored status" | new_generic | practices/rules/expiration-as-clock-predicate.md (NOT YET CREATED) | `rg "clock.based.expir" practices/rules/` → 0 matches | rejected for promotion now: pattern in only 1 domain. Promote when api-key adopts equivalent clock-based expiresAt check (currently api-key uses isActive(now) — already qualifies). Action: revisit at follow-up. |

## Summary
- 5 candidates surfaced
- 2 new_generic candidates ready for promotion in follow-up (PII masking, clock-based expiration) — but defer to avoid blocking R37
- 1 extend_existing (soft-delete audit trail, with comment-thread)
- 2 domain_specific / deferred

## Cross-domain pattern signals (architect Q1)
- PII masking pattern (R30 api-key + R33 session-management) — now 2 domains
- Soft-delete audit pattern (R33 session-management + R36 comment-thread) — now 2 domains
- Both meet promotion threshold. Action: extract in next iteration commit (R38+).

## Real bug retention rationale
R33-iter1 found 2 real bugs through persona simulation. Recording them here
preserves the why-this-bug-existed reasoning: manifest declared
max_active_sessions_per_user without enforcement code = documentation-code drift,
and accepting any expiresAt without future-check = born-revoked sessions confuse
both UI and SPI. Both were honest design oversights, not edge cases.
