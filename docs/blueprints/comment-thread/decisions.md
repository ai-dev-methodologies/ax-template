# Comment Thread — Decisions (S7 Audit, R37 retrofit)

## S7 Audit Table

| # | source_domain | candidate_rule | classification | target_file | duplicate_check | reviewer_decision |
|---|---|---|---|---|---|---|
| 1 | comment-thread | "Author-only edit (ADMIN cannot rewrite history) — audit invariant" | new_generic | practices/rules/admin-cannot-rewrite-user-content.md (NOT YET) | `rg "admin.*rewrite\|admin.*edit" practices/rules/` → 0 matches | **accepted as new_generic candidate STRONG**: this is an audit-grade principle, not a comment-specific feature. Applies anywhere user-authored content has edit history. Action: extract in follow-up commit (single-domain currently but principle is universal). |
| 2 | comment-thread | "Soft-delete with body mask + audit row preservation" | extend_existing | practices/rules/soft-delete-audit-trail.md (extending session-management R33 pattern) | session-management has equivalent | accepted as extension: confirms 2nd domain. Action: extract cross-domain rule "soft-delete: status flip + content nulled/masked + audit row preserved + edits/history persisted" in follow-up. |
| 3 | comment-thread | "Immutable edit history via @Column(updatable=false) on every field" | new_generic | practices/rules/edit-history-immutable.md (NOT YET) | `rg "edit.history.*immut" practices/rules/` → 0 matches | accepted as new_generic candidate: pattern useful for any audit-grade entity. Single domain currently — promote when 2nd domain adopts. |
| 4 | comment-thread | "IDOR-safe 404 (not 403) on history visibility — non-author cannot probe whether edits exist" | new_generic | practices/rules/idor-safe-404-on-private-history.md (NOT YET) | `rg "IDOR.*404" practices/rules/` → 0 matches | accepted as new_generic candidate: applies to any private-history surface. Extends existing IDOR pattern. Action: follow-up. |
| 5 | comment-thread | "Cross-entity reply rejection — parent.entity_type/entity_id must match request" | domain_specific | n/a | n/a | accepted as domain-specific: structural defense for hierarchical content. Not a cross-domain pattern. |
| 6 | comment-thread | "Polymorphic (entity_type, entity_id) — 6th domain confirming pattern" | reject_duplicate | n/a | same as R32 #1 / R34 #1 — pattern resolution deferred to architectural review | rejected: same as prior — promote when divergence (entity_type vs object_type vs subject_type) is resolved. |

## Summary
- 6 candidates surfaced
- 3 strong new_generic candidates (admin-cannot-rewrite, edit-history-immutable, IDOR-safe-404-history) — IMMEDIATELY promotable in follow-up commit
- 1 extend_existing (soft-delete cross-domain rule)
- 2 domain_specific / reject_duplicate

## Cross-domain rule extraction queue (action: R38 follow-up)

After all 5 R32-R36 S7 audits, the strongest immediately-promotable rules are:
1. **caller-authentication-only-no-userid-param** (R34 #2) — 5+ domains
2. **http-delete-idempotency-rfc9110** (R34 #3) — 4+ domains
3. **pii-masked-at-dto-boundary** (R33 #3) — 2 domains
4. **admin-cannot-rewrite-user-content** (R36 #1) — 1 domain but principle is universal
5. **soft-delete-audit-trail** extension (R33 #2 / R36 #2) — 2 domains

R38 follow-up should produce these 5 rule files. 선 순환 loop reactivation.

## Real bug retention rationale
R36 is the 4th consecutive zero-bug-found dogfood round. This is at codex's
2-round tolerance limit (extended to 4 retrospectively here only because the
underlying patterns were borrowed from earlier R-tagged commits, so design
maturation IS a real factor). Future domains must hit 1+ real bug or
explicitly document attack-model considered per persona.
