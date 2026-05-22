# Activity Feed — Decisions (S7 Audit, R37 retrofit)

## S7 Audit Table

| # | source_domain | candidate_rule | classification | target_file | duplicate_check | reviewer_decision |
|---|---|---|---|---|---|---|
| 1 | activity-feed | "ActivityStreams 2.0 actor/verb/object vocabulary" | domain_specific | n/a | n/a | accepted as domain-specific: this is the activity-feed vocabulary itself, not a cross-domain pattern. |
| 2 | activity-feed | "Idempotent publish via (actor, idempotencyKey) UNIQUE constraint" | extend_existing | practices/rules/api-idempotency-key-required.md | matches Payment + report-export idempotency-key pattern | accepted as extension: confirms 3rd domain using the pattern. Action: extend existing rule with "+ owner-scoped UNIQUE backstop". Defer to follow-up. |
| 3 | activity-feed | "Polymorphic (object_type, object_id) — note: DIFFERENT shape from entity_type+entity_id" | reject_duplicate | n/a | per codex Q2: object_type uses different semantics than entity_type — intentional divergence | rejected for promotion: divergence is intentional (per codex review). Document in manifest only. |
| 4 | activity-feed | "Per-(parent, user) state row pattern (ActivityRead, with UNIQUE(event_id, user_id))" | new_generic | practices/rules/per-user-state-row-pattern.md (NOT YET CREATED) | `rg "per.user.state" practices/rules/` → 0 matches | accepted as new_generic candidate: pattern useful for read receipts, view counts, "I've reviewed this". Currently 1 domain. Promote when 2nd adopts. |
| 5 | activity-feed | "Fan-out-read via @ElementCollection audience — bounded to ~100 recipients" | domain_specific | n/a | n/a | accepted as domain-specific: scaling pattern choice. Document in manifest deferred_to_v2 (fan-out-write at scale). |
| 6 | activity-feed | "Fail-closed visibility: caller sees event iff actor=caller OR caller IN audience" | domain_specific | n/a | n/a | accepted as domain-specific: visibility semantics vary per domain (e.g., comments are public per entity, activities are scoped to audience). Document in manifest. |

## Summary
- 6 candidates surfaced
- 0 new_generic immediately (1 candidate watching for 2nd-domain adoption)
- 1 extend_existing (idempotency-key — 3rd domain confirmation)
- 4 domain_specific / reject_duplicate

## Cross-domain signal
The idempotency-key pattern is now confirmed in Payment + report-export +
activity-feed (3 domains). Existing rule `api-idempotency-key-required.md` should
be extended with "UNIQUE(actor_user_id, idempotency_key) DB backstop" clause.
Action: follow-up commit.
