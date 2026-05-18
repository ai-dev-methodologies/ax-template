---
title: "Soft-delete must be implemented via @SQLDelete on BaseEntity subclasses, never via application-level flag fields"
impact: HIGH
impactDescription: "Boolean deleted=true flags are invisible to @Where filters, produce schema drift, and allow JPA hard-deletes to silently bypass the soft-delete contract. Timestamp-based @SQLDelete + @Where guarantees every ORM DELETE becomes an UPDATE with no application code changes."
tags:
  - persistence
  - soft-delete
  - hibernate
  - base-entity
  - data-integrity
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-PERS-005"
verification:
  gradle_task: testPractices
  tag: PRACTICES-PERS-005
failing_fixture_path: "practices/evals/fixtures/soft_delete/fail_boolean_flag"
passing_fixture_path: "practices/evals/fixtures/soft_delete/pass"
protects_template_ids:
  - "templates/backend/BaseEntity.java"
  - "templates/backend/notification/Notification.java"
  - "templates/backend/notification/NotificationPreferences.java"
  - "templates/backend/audit-log/AuditLog.java"
  - "templates/backend/file-storage/StoredFile.java"
  - "templates/backend/email-outbox/EmailOutbox.java"
  - "templates/backend/email-outbox/EmailTemplate.java"
  - "templates/backend/scheduled-task/ScheduledTask.java"
  - "templates/backend/scheduled-task/JobHistory.java"
upstream:
  - "https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#soft-delete"
  - "https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#mapping-where"
evidence:
  - source_type: external
    citation: "Hibernate ORM 6.4 User Guide — @SQLDelete: Customizes the SQL DELETE statement; when set to an UPDATE, every call to EntityManager.remove() or repository deleteById() runs the UPDATE instead, enabling transparent soft-delete without application-layer interception."
    url: "https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#soft-delete"
  - source_type: external
    citation: "Hibernate ORM 6.4 User Guide — @Where(clause): Adds a predicate appended to every JPQL/Criteria query for the annotated entity or collection. Use @Where(clause = 'deleted_at IS NULL') on the @MappedSuperclass so all standard queries automatically exclude soft-deleted rows."
    url: "https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#mapping-where"
  - source_type: external
    citation: "Hibernate ORM 6.4 Release Notes — @SoftDelete introduced in 6.4 but is boolean-only (BIT/BOOLEAN column). For TIMESTAMP-based soft-delete columns (deleted_at TIMESTAMP NULL), use @SQLDelete + @Where(clause = 'deleted_at IS NULL') instead."
    url: "https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#soft-delete"
---

## Soft-delete must be implemented via @SQLDelete on BaseEntity subclasses

**Impact: HIGH — Boolean deleted=true flags are invisible to @Where filters, produce schema drift, and allow JPA hard-deletes to silently bypass the soft-delete contract.**

Soft-delete is a data-retention pattern: rows are never physically removed; instead, a marker signals "this row is logically gone." There are two implementation paths:

1. **Boolean flag** (`deleted BOOLEAN DEFAULT FALSE`) — application code sets `entity.setDeleted(true)` and every query must manually add `WHERE deleted = false`.
2. **Timestamp column** (`deleted_at TIMESTAMP NULL`) — Hibernate's `@SQLDelete` converts every ORM-triggered `DELETE` into `UPDATE <table> SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?`, and `@Where(clause = "deleted_at IS NULL")` on the `@MappedSuperclass` excludes soft-deleted rows from all JPQL/Criteria queries automatically.

The boolean flag approach has three critical failure modes:
- **Inconsistent enforcement:** a single repository query that forgets `AND deleted = false` leaks deleted data.
- **No audit timestamp:** you cannot determine *when* a record was deleted without a separate audit column.
- **Hard-delete bypass:** `entityManager.remove()` and `repository.deleteById()` physically delete the row unless every code path is reviewed and individually guarded.

With `@SQLDelete` + `@Where`, hard-delete bypass is structurally impossible: Hibernate rewrites the DELETE at the JDBC level before it reaches the database. No application code can accidentally bypass this.

**Incorrect — boolean flag soft-delete:**

```java
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // BAD: application must remember to filter WHERE deleted = false in every query
    @Column(nullable = false)
    private boolean deleted = false;

    // No audit trail of when deletion happened
}
```

**Correct — timestamp-based @SQLDelete + BaseEntity:**

```java
// BaseEntity (shared superclass — @MappedSuperclass):
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Where(clause = "deleted_at IS NULL")      // applied to ALL subclass queries automatically
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "deleted_at")            // NULL = active, non-null = deleted
    private Instant deletedAt;
}

// Concrete entity — MUST carry @SQLDelete:
@Entity
@Table(name = "notifications")
@SQLDelete(sql = "UPDATE notifications SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Notification extends BaseEntity {
    // domain fields only — audit + soft-delete inherited from BaseEntity
    @Column(name = "title", nullable = false, length = 255)
    private String title;
}
```

**What @SQLDelete does:**
When Spring Data calls `notificationRepository.deleteById(id)` or JPA calls `entityManager.remove(entity)`, Hibernate intercepts the DELETE and executes:
```sql
-- Intercepted by @SQLDelete — never reaches the database as DELETE:
UPDATE notifications SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?
```

**What @Where does:**
Every standard JPQL or Criteria query on `Notification` automatically has `AND deleted_at IS NULL` appended:
```sql
-- Standard findById — @Where appended automatically:
SELECT n.* FROM notifications n WHERE n.id = ? AND n.deleted_at IS NULL

-- findAll — @Where appended automatically:
SELECT n.* FROM notifications n WHERE n.deleted_at IS NULL
```

**Database migration requirement:**
All 8 entities that extend `BaseEntity` require a `deleted_at TIMESTAMP NULL` column and a partial index for query performance:
```sql
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;
CREATE INDEX IF NOT EXISTS idx_notifications_not_deleted
    ON notifications (id) WHERE deleted_at IS NULL;
```

See `templates/backend/data/migrations/V202605181200__add_soft_delete_columns.sql` for the full migration covering all 8 entities.

Verification: `./gradlew testPractices --tests "*BaseEntitySoftDelete*"` asserts that every `@Entity` in the base template package that extends `BaseEntity` also carries `@SQLDelete`.

Reference: [Hibernate ORM 6.4 — @SQLDelete](https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#soft-delete) | [Hibernate ORM 6.4 — @Where](https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#mapping-where)
