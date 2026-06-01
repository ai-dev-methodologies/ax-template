---
title: Published content editions are immutable copy-on-write snapshots — never edit a live edition in place
impact: HIGH
impactDescription: "Editing a published course/terms/pricing/API-spec row in place silently rewrites what every already-pinned consumer sees — the lost-history anomaly: there is no way to prove what a user actually agreed to, enrolled in, or was charged"
tags:
  - content-versioning
  - copy-on-write
  - immutability
  - jpa
  - append-only
  - event-sourcing
spec_ref: "specs/content-versioning-l0.yaml#PUBLISH-SNAPSHOT-001"
verification:
  type: review
  source: "specs/content-versioning-l0.yaml#PUBLISH-SNAPSHOT-001 (PublishedEdition entity contract) + #PUBLISH-TRANSITION-001 (sole-mutator publish)"
  pattern: "Published-edition entity maps every content column @Column(updatable=false); publish INSERTs a new edition (next version_no) and supersedes the prior one without rewriting it; the working draft is a separate mutable row; no code path UPDATEs a published edition's content columns"
upstream:
  - "https://semver.org/"
  - "https://martinfowler.com/eaaDev/EventSourcing.html"
  - "https://www.postgresql.org/docs/current/mvcc-intro.html"
evidence:
  - source_type: external
    citation: "Semantic Versioning 2.0.0 — Specification §3 (released-version immutability)"
    url: "https://semver.org/"
    quote: "Once a versioned package has been released, the contents of that version MUST NOT be modified. Any modifications MUST be released as a new version."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Martin Fowler — Event Sourcing (martinfowler.com)"
    url: "https://martinfowler.com/eaaDev/EventSourcing.html"
    quote: "Capture all changes to an application state as a sequence of events."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "PostgreSQL Documentation — Introduction to MVCC (Multiversion Concurrency Control)"
    url: "https://www.postgresql.org/docs/current/mvcc-intro.html"
    quote: "This means that each SQL statement sees a snapshot of data (a database version) as it was some time ago, regardless of the current state of the underlying data."
    quoted_at: "2026-06-01"
---

## Published content editions are immutable copy-on-write snapshots — never edit a live edition in place

**Impact: HIGH — editing a published edition in place silently rewrites what every pinned consumer sees, and destroys the proof of what they actually agreed to**

A fork-receiver building an LMS course, a terms-of-service page, a pricing plan, or a published API spec reaches a moment where someone edits *live* content. The naive model stores one mutable row per logical item and `UPDATE`s it on every edit. That row is also what enrolled students, accepted-terms users, and active subscribers are reading. So a single edit retroactively rewrites history for everyone: the student who enrolled in "Course v1" is now silently looking at v3, the user who clicked "I accept" on the old terms is now legally pinned to terms they never saw, and the customer on a grandfathered price sees the new price. There is no row that still says what v1 *was* — the history is gone, and with it any ability to answer "what did this consumer actually agree to?".

The correct model is copy-on-write editions. Publishing INSERTs a **new** immutable edition row holding a frozen copy of the content; it never mutates a previously published edition. Every content-bearing column on the published-edition entity is mapped `@Column(updatable=false)`, so the persistence provider physically cannot emit an `UPDATE` against a published row — the table is append-only. Authoring happens on a **separate** mutable draft row that no consumer reads. A publish is a single atomic transition (one sole-mutator service, `@Transactional`) that freezes the draft into a new edition, assigns the next `version_no`, and supersedes the prior current edition by flipping a pointer — *without deleting the prior edition*. Consumers pin to the edition id current at join time, so a new publish is invisible to them until they explicitly re-accept. This is the same discipline SemVer mandates for released packages and the same append-forward posture as event sourcing: you never rewrite the past, you publish a new version.

**Incorrect — one mutable row; editing it rewrites every pinned consumer's view and erases history:**

```java
@Entity
public class Course {
    @Id @GeneratedValue private Long id;
    private String title;          // mutable — students read THIS row
    @Lob private String body;      // editing body rewrites it for everyone
    private BigDecimal price;
}

@Service
class CourseService {
    @Transactional
    public void editCourse(Long id, CourseEdit e) {
        Course c = repo.findById(id).orElseThrow();
        c.setTitle(e.title());     // ❌ in-place UPDATE on the live row
        c.setBody(e.body());       // ❌ every enrolled student now sees the new body
        c.setPrice(e.price());     // ❌ grandfathered subscribers silently re-priced
        // v1 is gone: no row remembers what the student actually enrolled in
    }
}
```

**Correct — immutable published edition (copy-on-write) + separate mutable draft + sole-mutator publish:**

```java
@Entity
public class PublishedCourseEdition {
    @Id @GeneratedValue private Long id;
    @Column(updatable = false) private Long courseId;
    @Column(updatable = false) private int versionNo;        // write-once, monotonic
    @Column(updatable = false) private String title;         // frozen at publish
    @Lob @Column(updatable = false) private String body;     // frozen at publish
    @Column(updatable = false) private BigDecimal price;     // frozen at publish
    @Column(updatable = false) private Instant publishedAt;
    private boolean current;   // the ONLY mutable column: the supersede pointer
}

@Entity
public class CourseDraft {            // separate mutable row — no consumer reads this
    @Id @GeneratedValue private Long id;
    private Long courseId;
    private String title;
    @Lob private String body;
    private BigDecimal price;
}

@Service
class CoursePublishService {          // SOLE mutator of published editions
    @Transactional
    public PublishedCourseEdition publish(Long courseId) {
        CourseDraft draft = drafts.findByCourseId(courseId).orElseThrow();
        int next = editions.maxVersionNo(courseId) + 1;        // prior_max + 1
        editions.findCurrent(courseId).ifPresent(prev -> prev.setCurrent(false)); // supersede, keep
        PublishedCourseEdition fresh = PublishedCourseEdition.freeze(draft, next); // INSERT, never UPDATE
        fresh.setCurrent(true);
        return editions.save(fresh);   // prior edition row retained, byte-identical
    }
}
// A consumer stores editionId at enrollment and is served THAT edition forever
// until an explicit, audited re-acceptance moves them forward.
```

The `@Column(updatable=false)` mapping is what makes this mechanical rather than aspirational: the provider omits those columns from every generated `UPDATE`, so even a stray `edition.setTitle(...)` is a silent no-op at flush time rather than a history-destroying write. Pair this rule with `optimistic-locking-l0` (apply `@Version` + If-Match to the *draft* row, where concurrent authors actually race) — published editions need no optimistic lock because they are never updated. Reserve all mutation for the draft; treat every published edition as append-only.

Verification: review-tier — `specs/content-versioning-l0.yaml#PUBLISH-SNAPSHOT-001` defines the immutable-edition contract and `#PUBLISH-TRANSITION-001` the sole-mutator publish. A fork-receiver realizing this domain asserts the mapping in a ViolationProofTest (reflect over the `PublishedCourseEdition` fields; assert every content column carries `@Column(updatable=false)`) and asserts publish-twice yields two distinct rows with the first edition's content unchanged. No `@Tag` test ships in ax-template for this generic pattern yet, so this rule is honestly verification:review, not gradle+tag.

Reference: [Semantic Versioning 2.0.0 — §3 released-version immutability](https://semver.org/)

Reference: [Martin Fowler — Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)

Reference: [PostgreSQL — Introduction to MVCC](https://www.postgresql.org/docs/current/mvcc-intro.html)
