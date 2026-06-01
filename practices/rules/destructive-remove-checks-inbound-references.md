---
title: Destructive remove of a structural entity MUST count live inbound references first — never silently orphan dependents
impact: HIGH
impactDescription: "Hard-deleting (or cascade-tombstoning) a category/tag/parent node that other aggregates still point at leaves dangling foreign keys — dependents become unresolvable, list views 500 on the missing join, and audit reads lose the node entirely"
tags:
  - referential-integrity
  - destructive-action
  - foreign-key
  - soft-delete
  - data-integrity
spec_ref: "specs/soft-delete-l0.yaml#SOFTDELETE-REFERENTIAL-001"
verification:
  type: review
  source: "templates/L4/tag-categorization, templates/L4/comment-thread, backend/src/main/java/com/ax/template/authblueprint/common/ResourceNotFoundException.java"
  pattern: "Service hard-delete / cascade-tombstone path counts live inbound references from OTHER aggregates (repository.countLiveReferencing(id)) inside the SAME @Transactional, then either throws a 409 referential-conflict ProblemDetail carrying dependent_count OR retires the node (deleted_at tombstone) instead of issuing a physical DELETE; never an unconditional repo.deleteById(id)"
upstream:
  - "https://www.postgresql.org/docs/current/ddl-constraints.html"
  - "https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html"
evidence:
  - source_type: external
    citation: "PostgreSQL Documentation — 5.4 Constraints, Foreign Keys (ON DELETE RESTRICT)"
    url: "https://www.postgresql.org/docs/current/ddl-constraints.html"
    quote: "RESTRICT is a stricter setting than NO ACTION. It prevents deletion of a referenced row. RESTRICT does not allow the check to be deferred until later in the transaction."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Jakarta Persistence 3.1 Specification — §2.10 Entity Relationships"
    url: "https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html"
    quote: "Note that it is the application that bears responsibility for maintaining the consistency of runtime relationships—for example, for insuring that the 'one' and the 'many' sides of a bidirectional relationship are consistent with one another when the application updates the relationship at runtime."
    quoted_at: "2026-06-01"
---

## Destructive remove of a structural entity MUST count live inbound references first — never silently orphan dependents

**Impact: HIGH — a delete that ignores inbound references trades one click for a swarm of dangling foreign keys**

A *structural* entity is one that other aggregates point at: a category that products are filed under, a tag that attachments reference (`entity_type`/`entity_id` pair), a parent node with children, a team that memberships belong to. When the service hard-deletes (physical `DELETE`) or cascade-tombstones such a node without first asking "does anything still point at me?", the dependents are left referencing a row that no longer resolves. The product list 500s on the missing category join. The tagged-items query returns rows whose tag id resolves to nothing. An audit read of "what was this attachment filed under" returns a hole. The relational invariant the database would have enforced with `ON DELETE RESTRICT` — *prevent deletion of a referenced row* — is silently skipped because the code reached for `repo.deleteById(id)` directly.

The rule: before any destructive remove of a structural entity, **count the live inbound references from other aggregates inside the same transaction**, then branch:

- **(a) Refuse** — non-zero dependents → throw a `409 Conflict` RFC 9457 ProblemDetail of `type=urn:problem:referential-conflict` carrying a `dependent_count` member (and, bounded, a `dependent_resource` naming the blocking aggregate type). The caller detaches or re-files the dependents, then retries.
- **(b) Retire instead of remove** — convert the operation to a tombstone (`deleted_at`, per `SOFTDELETE-MARK-001`) so the node stays resolvable for historical reads of the dependents. Default-excluding queries hide it from live pickers; the dependents' joins still resolve.

Never the third option: silently orphan. Jakarta Persistence is explicit that the database does not do this for you at the object layer — *the application bears responsibility for maintaining the consistency of runtime relationships.* This is the application-layer realization of `ON DELETE RESTRICT`, and it is required precisely for the stores that have **no** real DB foreign key — polymorphic attachment tables keyed by `entity_type`/`entity_id`, cross-aggregate references the schema never declared as FKs.

**Incorrect — unconditional hard-delete; dependents left dangling:**

```java
@Transactional
public void deleteTag(UUID tagId) {
    tagRepository.deleteById(tagId);          // ❌ TagAttachment rows still point at tagId
}                                              //    tagged-items query now resolves to nothing
```

**Correct — count live inbound references in-transaction, then refuse (a) or retire (b):**

```java
@Transactional
public void deleteTag(UUID tagId) {
    Tag tag = tagRepository.findById(tagId)
        .orElseThrow(() -> new ResourceNotFoundException("tag", tagId));

    // read-then-delete in ONE transaction: a concurrent attach cannot slip
    // through the check-to-delete window
    long dependents = attachmentRepository.countByTagIdAndDeletedAtIsNull(tagId);
    if (dependents > 0) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "Tag is still referenced by live attachments; detach them or retire the tag.");
        pd.setType(URI.create("urn:problem:referential-conflict"));
        pd.setProperty("dependent_count", dependents);
        pd.setProperty("dependent_resource", "tag_attachment");
        throw new ErrorResponseException(HttpStatus.CONFLICT, pd, null);   // (a) 409 RESTRICT semantics
        // — OR — (b) tag.retire(now); keep the node resolvable for historical reads
    }
    tagRepository.deleteById(tagId);          // ✅ provably no dependents remain
}
```

**When to apply**: any hard-delete or hierarchy-cascade of an entity that other aggregates reference (category, tag, parent node, team, lookup row), *especially* when the reference is a polymorphic `entity_type`/`entity_id` pair with no declared FK. **When NOT to apply**: leaf entities nothing points at (a draft, an ephemeral session row, a self-contained note) — there is no inbound edge to orphan. Pair with `destructive-action-confirm-with-side-effects.md` (the UI confirm) and `soft-delete-audit-trail.md` (the retire mechanism behind option b).

Verification: review-tier. The destructive path of each structural-entity service is read for an in-transaction `count*Referencing` (or equivalent) guard ahead of any physical `deleteById`, branching to a 409 `referential-conflict` ProblemDetail with `dependent_count` or to a tombstone — and the absence of a bare unconditional `repo.deleteById(structuralId)`. No `@Tag` test asserts this cross-aggregate runtime property generically, so it is verified by structured review against the `spec_ref` invariant rather than a `./gradlew` task.

Reference: [PostgreSQL — 5.4 Constraints, Foreign Keys (ON DELETE RESTRICT)](https://www.postgresql.org/docs/current/ddl-constraints.html)

Reference: [Jakarta Persistence 3.1 — §2.10 Entity Relationships](https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html)
