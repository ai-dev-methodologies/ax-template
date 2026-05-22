# Comment Thread Blueprint — Plan (R36 retrofit)

> Retrofit at R37. Future domains MUST author at S1.

## Domain
Entity-agnostic threaded comments with reply hierarchy, soft-delete, immutable edit history.

## Scope
**In v1**: CRUD on comment + reply via parentCommentId + cross-entity reply rejection
+ soft-delete with body mask `[deleted]` + immutable CommentEdit history + author-only edit
(admin CANNOT rewrite) + author-or-admin delete + IDOR-safe 404 on history visibility.

**Out (`not_for`)**: anonymous comments, realtime collab, massive scale (>1M/entity),
public moderation queue, multi-tenant.

**Deferred v2**: max_depth enforcement, moderation queue compose with approval-workflow,
mention resolution, pagination, vote aggregation compose with favorites, redacted-for-others view.

## Standards
- Disqus/Reddit/Discourse threaded pattern (parentId + flat list + client tree rebuild)
- GDPR Article 17 (right to erasure satisfied by soft-delete + body redaction)
- RFC 9110 §9.3.5 — DELETE idempotency

## Acceptance gates
1. `./gradlew testCommentThread` exits 0 (18 tests = 12 compliance + 6 violation proof)
2. verify-completion exit 0
3. Dogfood (R36-iter1): 0 real bugs (4th consecutive zero-bug — at codex tolerance limit)
4. S7 audit per decisions.md
