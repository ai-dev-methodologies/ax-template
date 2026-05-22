---
title: ROLE_ADMIN may MODERATE (delete) but MUST NOT rewrite user-authored content
impact: HIGH
impactDescription: "Admin-edit-of-user-content destroys audit trail trust — the strongest counter-evidence against the platform's good faith"
tags:
  - authz
  - audit
  - moderation
  - trust
spec_ref: "specs/comment-thread-l0.yaml#COMMENT-AUTHZ-002"
verification:
  gradle_task: testCommentThread
  tag: COMMENT-AUTHZ-002
upstream:
  - "https://gdpr-info.eu/art-5-gdpr/"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "GDPR Article 5(1)(a) — Lawfulness, fairness and transparency"
    url: "https://gdpr-info.eu/art-5-gdpr/"
    quote: "Personal data shall be processed lawfully, fairly and in a transparent manner in relation to the data subject."
    quoted_at: "2026-05-22"
  - source_type: external
    citation: "OWASP ASVS V8.3.4 — Verify that sensitive personal information is subject to data retention classification"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify that sensitive personal information is subject to data retention classification, such that old or out of date data is deleted automatically, on a schedule, or as the situation requires."
    quoted_at: "2026-05-22"
---

## ROLE_ADMIN may MODERATE (delete) but MUST NOT rewrite user-authored content

**Impact: HIGH — Admin-edit-of-user-content destroys audit trust**

The split between moderation (delete) and rewriting (edit) is the hinge of any audit-grade content system. A platform whose admins can silently rewrite user posts cannot credibly claim to preserve the user's voice. The user has no way to prove the published text was their own. This is also the precise failure mode that destroys long-lived comment systems: a single staff "fix" of someone's typo erodes the contract that the published text is the user's own words.

The catalog rule: `ROLE_ADMIN` is permitted on DELETE (moderation outcome) but rejected on PUT/edit. The author is the only principal allowed to mutate text. The author can edit their own text; the audit trail (CommentEdit row) captures the pre-image. If the platform needs the offending text removed for legal reasons, the path is delete (status flip to DELETED, body masked) — not rewrite.

Catalog evidence (R36 comment-thread, COMMENT-AUTHZ-002): `CommentService.edit()` checks `comment.getAuthorUserId().equals(auth.getName())` regardless of authority. Admin attempts return 403 EDIT_FORBIDDEN. The dedicated admin endpoint (under `/api/admin/comments`) accepts only DELETE, not PUT.

**Incorrect — admin can edit any user's content:**

```java
@PutMapping("/api/comments/{id}")
public CommentResponse edit(Authentication auth, @PathVariable UUID id, @RequestBody UpdateRequest body) {
    Comment c = repo.findById(id).orElseThrow();
    // Anti-pattern: admin override allowed
    if (!c.getAuthorUserId().equals(auth.getName()) && !isAdmin(auth)) {
        throw new AccessDeniedException();
    }
    c.editBody(body.body(), Instant.now());
    return CommentResponse.from(c);
}
```

A leaked admin token now silently rewrites any comment. Even with no leak: this code asks the platform's staff to be trusted with rewriting any user's words. The audit invariant is broken by design.

**Correct — author-only edit, admin can only moderate (delete):**

```java
@PutMapping("/api/comments/{id}")
public CommentResponse edit(Authentication auth, @PathVariable UUID id, @RequestBody UpdateRequest body) {
    Comment c = repo.findById(id).orElseThrow(() -> new CommentNotFoundException(id));
    if (!c.getAuthorUserId().equals(auth.getName())) {
        // No `|| isAdmin(auth)` here — even admin cannot rewrite
        throw new EditForbiddenException("only the author may edit comment " + id);
    }
    // captures pre-image to CommentEdit BEFORE mutating
    editHistory.save(new CommentEdit(c.getId(), Instant.now(), auth.getName(), c.getBody()));
    c.editBody(body.body(), Instant.now());
    return CommentResponse.from(c);
}

@DeleteMapping("/api/admin/comments/{id}")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public ResponseEntity<Void> moderate(Authentication auth, @PathVariable UUID id) {
    service.softDelete(id, auth.getName());           // status flip + body→NULL + audit row
    return ResponseEntity.noContent().build();
}
```

The admin path is delete-only. The author path is the only edit path. The audit trail (`actedByUserId` + `CommentEdit`) is honest because the operation it records is the only operation that ever happened.

**This rule applies anywhere user-authored text is subject to revision history**: comments, reviews, posts, change logs, journal entries, ticket descriptions. It does NOT apply to admin-curated metadata (tag names, category labels, feature-flag descriptions) where the admin IS the authoring party.

Reference: [GDPR Article 5 — Lawfulness, fairness and transparency](https://gdpr-info.eu/art-5-gdpr/)

Reference: [OWASP ASVS V8 — Data Protection](https://owasp.org/www-project-application-security-verification-standard/)
