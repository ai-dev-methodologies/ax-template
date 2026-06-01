---
title: Ordered sibling collections MUST persist an explicit position and renumber atomically under serialization
impact: HIGH
impactDescription: "A reorder that sorts on created_at or renumbers row-by-row with no parent @Version / row lock lets two concurrent reorders of the same parent silently clobber each other (CWE-362), producing a torn list with gaps or duplicate positions"
tags:
  - ordering
  - concurrency
  - jpa
  - optimistic-locking
  - race-condition
  - collection
spec_ref: "specs/ordered-collection-l0.yaml#ORDER-REORDER-ATOMIC-001"
verification:
  type: review
  source: "specs/ordered-collection-l0.yaml#ORDER-REORDER-ATOMIC-001"
  pattern: "A user-orderable sibling collection (a) sorts on an explicit non-null `position` column (never created_at/insertion order), and (b) renumbers all affected siblings in ONE @Transactional method whose concurrency is serialized by an optimistic `@Version` on the PARENT (or a SELECT ... FOR UPDATE on the parent row, or a positional/fractional sort_key that rewrites only the moved row). Reject: read-all-then-write-all reorder with no parent version/lock; per-item separate UPDATE round-trips; ORDER BY created_at as the ordering source."
upstream:
  - "https://www.postgresql.org/docs/current/explicit-locking.html"
  - "https://www.figma.com/blog/realtime-editing-of-ordered-sequences/"
  - "https://cwe.mitre.org/data/definitions/362.html"
  - "https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1"
evidence:
  - source_type: external
    citation: "PostgreSQL 18 Documentation — 13.3.2 Row-Level Locks (FOR UPDATE)"
    url: "https://www.postgresql.org/docs/current/explicit-locking.html"
    quote: "FOR UPDATE causes the rows retrieved by the SELECT statement to be locked as though for update. This prevents them from being locked, modified or deleted by other transactions until the current transaction ends. That is, other transactions that attempt UPDATE, DELETE, SELECT FOR UPDATE, SELECT FOR NO KEY UPDATE, SELECT FOR SHARE or SELECT FOR KEY SHARE of these rows will be blocked until the current transaction ends; conversely, SELECT FOR UPDATE will wait for a concurrent transaction that has run any of those commands on the same row, and will then lock and return the updated row (or no row, if the row was deleted)."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Figma Engineering Blog — Realtime editing of ordered sequences (fractional indexing)"
    url: "https://www.figma.com/blog/realtime-editing-of-ordered-sequences/"
    quote: "Every object has a real number as an index and the order of the children for an element of the tree is determined by sorting all children by their index."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "CWE-362 — Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition')"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
---

## Ordered sibling collections MUST persist an explicit position and renumber atomically under serialization

**Impact: HIGH — a reorder with no persisted position and no parent-level serialization is a silent data-corruption race**

When a feature lets a user drag siblings into a deliberate order — LMS lessons under a module, kanban cards in a column, playlist tracks, form-builder fields, an approval line — two mistakes compound into a HIGH-severity bug. First, ordering on `created_at` (or insertion order) cannot represent a move: dragging lesson C above lesson A changes neither row's `created_at`, so the new order is unrepresentable and the list re-sorts wrong on the next read. Second, renumbering the siblings with a plain read-all-then-write-all sequence — read the current positions, compute new ones in app code, write them back — is a textbook CWE-362 race: two instructors reordering the same module at once each compute from the same stale snapshot, and the second commit silently overwrites the first. The result is a torn list with gaps or two siblings both claiming position 3. No compiler and no single-threaded test catches this; only an explicit position column plus parent-level serialization closes it.

The fix has two halves, both required. (1) Persist an explicit `position` column and make `ORDER BY position` the sole sort key. (2) Make the reorder one atomic `@Transactional` method whose concurrency is serialized by an optimistic `@Version` on the **parent** row (the loser's flush raises `OptimisticLockException` → map to 409, retry), OR by a `SELECT ... FOR UPDATE` on the parent row, OR by a positional / fractional sort_key strategy where a single move rewrites only the moved row's key and never renumbers siblings at all. Back the column with a `UNIQUE(parent_id, position)` constraint so a buggy renumber fails loudly instead of persisting a duplicate.

**Incorrect — ordering on created_at; read-all-then-write-all renumber with no parent version or lock (CWE-362 race; two concurrent reorders clobber each other):**

```java
// Reads order from created_at — a deliberate move is unrepresentable
List<Lesson> lessons =
    lessonRepository.findByModuleIdOrderByCreatedAtAsc(moduleId);

@Transactional
public void reorder(Long moduleId, List<Long> newOrder) {
    // No @Version on Module, no FOR UPDATE: two callers read the same
    // snapshot and both write — the second commit silently wins.
    int pos = 0;
    for (Long lessonId : newOrder) {
        Lesson l = lessonRepository.findById(lessonId).orElseThrow();
        l.setPosition(pos++);          // per-item, observable mid-shift
        lessonRepository.save(l);      // separate round-trips → torn list
    }
}
```

**Correct — explicit position; single-transaction renumber serialized by an optimistic @Version on the PARENT; conflict → 409 retry; UNIQUE(parent, position) backstop:**

```java
@Entity
class Module {
    @Id Long id;
    @Version Long version;             // serializes concurrent reorders of THIS module
}

@Entity
@Table(uniqueConstraints =
    @UniqueConstraint(columnNames = {"module_id", "position"}))   // structural backstop
class Lesson {
    @Id Long id;
    @Column(nullable = false) Integer position;   // ORDER BY position is the sole sort key
}

@Transactional
public void reorder(Long moduleId, List<Long> newOrder) {
    Module module = moduleRepository.findById(moduleId).orElseThrow();
    // Touching module.version makes the persistence provider assert the
    // parent row is unchanged at flush; a concurrent reorder bumps it and
    // the loser's flush raises OptimisticLockException.
    module.touch();
    List<Lesson> lessons = lessonRepository.findByModuleId(moduleId);
    Map<Long, Integer> rank = indexOf(newOrder);   // intended final order
    lessons.forEach(l -> l.setPosition(rank.get(l.getId())));
    // One transaction → UNIQUE(module_id, position) is evaluated at commit;
    // the whole renumber is all-or-nothing.
}
// OptimisticLockException → @ExceptionHandler → 409 urn:problem:reorder-conflict
// (ORDER-CONFLICT-001): client re-GETs the fresh order and retries (budget 3).
```

The positional/fractional alternative is equally valid and touches only one row per move: give each sibling a sparse sort_key and, to move an item, generate a new key strictly between its two new neighbors — no sibling renumber, no cross-row race (this is the Figma fractional-indexing technique). Either path satisfies `ORDER-REORDER-ATOMIC-001`; what the rule rejects is created_at ordering and an unserialized read-all-then-write-all renumber.

Verification: review-tier (no `@Tag` test ships in the catalog reference workload for this generic pattern; a fork-receiver realizing the spec adds a concurrent-reorder RestAssured harness per `ORDER-REORDER-ATOMIC-001`). The reviewer confirms the orderable collection sorts on an explicit `position` column and that the reorder method serializes concurrency via a parent `@Version`, a parent-row `SELECT ... FOR UPDATE`, or a positional/fractional sort_key — and rejects any `ORDER BY created_at` ordering source or unserialized per-item renumber.

Reference: [PostgreSQL — Explicit Locking, Row-Level Locks (FOR UPDATE)](https://www.postgresql.org/docs/current/explicit-locking.html)

Reference: [Figma Engineering — Realtime editing of ordered sequences (fractional indexing)](https://www.figma.com/blog/realtime-editing-of-ordered-sequences/)

Reference: [CWE-362 — Race Condition](https://cwe.mitre.org/data/definitions/362.html)
