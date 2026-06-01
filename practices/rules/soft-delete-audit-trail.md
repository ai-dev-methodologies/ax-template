---
title: Soft-delete via status flip preserves audit trail — hard-delete forbidden when audit matters
impact: HIGH
impactDescription: "Hard-delete on audit-grade entities loses the why/when/who; the right-to-erasure mandate is satisfied by body redaction, not row removal"
tags:
  - audit
  - soft-delete
  - gdpr
  - data-retention
spec_ref: "specs/comment-thread-l0.yaml#COMMENT-CRUD-003"
verification:
  gradle_task: testCommentThread
  tag: COMMENT-CRUD-003
upstream:
  - "https://gdpr-info.eu/art-17-gdpr/"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "GDPR Article 17 — Right to erasure ('right to be forgotten')"
    url: "https://gdpr-info.eu/art-17-gdpr/"
    quote: "The data subject shall have the right to obtain from the controller the erasure of personal data concerning him or her without undue delay. ... [The controller shall] take account of available technology and the cost of implementation."
    quoted_at: "2026-05-22"
---

## Soft-delete via status flip preserves audit trail — hard-delete forbidden when audit matters

**Impact: HIGH — Hard-delete on audit-grade entities loses the who/when/why**

The right-to-erasure mandate (GDPR Article 17) does NOT require row removal. It requires that personal data "concerning the data subject" be erased. The catalog soft-delete pattern satisfies this by clearing the data (body → NULL, DTO mask `[deleted]`) while preserving the audit metadata (`deletedAt`, `deletedByUserId`, the original `createdAt`, and any edit history). The personal data is gone; the act of deletion is recorded.

Hard-delete loses what compliance needs (who deleted what when) and what threading needs (a reply's parent still must resolve). Audit-grade entities — comments, sessions, file uploads, approval requests, payment events, audit logs themselves — should never hard-delete in their domain code. The catalog pattern (R33 session-management, R36 comment-thread) uses status flip + content nulling.

Catalog evidence:
- **R33 session-management (SESS-LIFECYCLE-003)**: logout flips `status` ACTIVE → REVOKED, sets `revokedAt`, stamps `revokedByUserId`. The row stays for historical session audit.
- **R36 comment-thread (COMMENT-CRUD-003)**: delete flips `status` ACTIVE → DELETED, clears `body` to NULL, stamps `deletedAt` + `deletedByUserId`. The DTO masks the missing body as `'[deleted]'`. Replies remain readable; thread structure is preserved.

**Incorrect — hard-delete loses audit metadata:**

```java
@DeleteMapping("/api/comments/{id}")
public ResponseEntity<Void> delete(@PathVariable UUID id) {
    repo.deleteById(id);                              // row gone forever
    return ResponseEntity.noContent().build();
}
```

Replies to this comment now reference a missing parent. The audit log cannot answer "who deleted comment X". GDPR erasure is over-satisfied — the metadata about *the act of erasure* is lost too.

**Correct — soft-delete with status flip + content clearing:**

```java
@DeleteMapping("/api/comments/{id}")
public ResponseEntity<Void> delete(Authentication auth, @PathVariable UUID id) {
    Comment c = repo.findById(id).orElseThrow(() -> new CommentNotFoundException(id));
    // Authorization check elided — author OR admin per admin-cannot-rewrite-user-content rule
    c.softDelete(auth.getName(), Instant.now(clock));  // status flip + body→NULL + deletedAt + deletedByUserId
    repo.save(c);
    return ResponseEntity.noContent().build();
}

// Entity:
void softDelete(String actorUserId, Instant now) {
    if (this.status == CommentStatus.DELETED) return;  // idempotent
    this.status = CommentStatus.DELETED;
    this.body = null;                                  // personal data cleared
    this.deletedAt = now;
    this.deletedByUserId = actorUserId;
}

// DTO masks for read side:
public static CommentResponse from(Comment c) {
    String visibleBody = (c.getStatus() == CommentStatus.DELETED || c.getBody() == null)
        ? "[deleted]"
        : c.getBody();
    // … rest of mapping
}
```

The body is gone (GDPR erasure satisfied); the audit (deletedAt + deletedByUserId) survives; replies still resolve their parent. Edit history rows (`CommentEdit`) are preserved across the delete so a moderator can still reconstruct what the comment said and when it was edited — without the body itself.

**Apply this pattern when**: the entity participates in an audit trail or a thread / chain / graph where its absence would break adjacent rows. Apply hard-delete only for entities with no audit value (transient session caches, ephemeral computation outputs).

**Anti-cascade**: soft-delete should NOT cascade to dependent rows. Comment's replies remain ACTIVE even when the parent comment is DELETED. Session's `ActivityRead` rows remain. The user can still see *that* something happened; the body is what's gone.

Reference: [GDPR Article 17 — Right to erasure](https://gdpr-info.eu/art-17-gdpr/)

Reference: [OWASP ASVS V8.3.5 — Data retention sanitization](https://owasp.org/www-project-application-security-verification-standard/)
