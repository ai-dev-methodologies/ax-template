# Activity Feed Blueprint — Plan (R35 retrofit)

> Retrofit at R37. Future domains MUST author at S1.

## Domain
ActivityStreams 2.0-style (actor, verb, object, audience) event publishing +
fan-out-read inbox + per-(event, user) read state.

## Scope
**In v1**: publish (idempotent via actor+idempotencyKey) / list (paginated, unreadOnly filter) /
get / mark-read / mark-all-read + Authentication-only caller + audience defaults to actor.

**Out (`not_for`)**: fan-out-write at million-follower scale, audit-of-record (use audit-log),
realtime push, multi-tenant, verb taxonomy enforcement.

**Deferred v2**: fan-out write for scale, realtime push, verb taxonomy, follower graph,
soft-delete-or-redaction.

## Standards
- ActivityStreams 2.0 vocabulary (actor/verb/object/target)
- Mastodon timelines API
- Facebook News Feed paper (Hoffman 2010) — fan-out trade-offs

## Acceptance gates
1. `./gradlew testActivityFeed` exits 0 (18 tests = 12 compliance + 6 violation proof)
2. verify-completion exit 0
3. Dogfood (R35-iter1): 0 real bugs (3rd consecutive zero-bug round, approaching codex 4-round limit)
4. S7 audit per decisions.md
