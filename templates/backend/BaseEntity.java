/**
 * @ax-template-meta
 * template_id: backend/BaseEntity
 * layer: backend-cross-cutting
 * domain: common
 * anchors_rule: soft-delete-only-on-base-entity.md (PRACTICES-PERS-005)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Hibernate ORM 6 User Guide — @SQLDelete overrides the DELETE SQL so the delete path runs an UPDATE instead, enabling soft-delete without application-level interception"
 *     url: "https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#soft-delete"
 *   - source_type: external
 *     citation: "Hibernate ORM 6 User Guide — @Where(clause) adds a predicate to every JPQL/Criteria query for the annotated entity; use deleted_at IS NULL to exclude soft-deleted rows automatically"
 *     url: "https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#mapping-where"
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — @EnableJpaAuditing + @EntityListeners(AuditingEntityListener.class) populate @CreatedDate and @LastModifiedDate on persist/merge"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/auditing.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   All domain entities that require audit + soft-delete MUST extend BaseEntity.
 *   Add @SQLDelete(sql="UPDATE <table> SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
 *   to each concrete @Entity subclass — Hibernate requires the annotation on the
 *   concrete class, not the @MappedSuperclass.
 *   Add JpaAuditConfig (@EnableJpaAuditing) to your Spring Boot configuration.
 *   Run the Flyway migration V...add_soft_delete_columns.sql to add deleted_at columns.
 *   NOTE: @SoftDelete (Hibernate 6.4+) is boolean-only and incompatible with TIMESTAMP
 *   columns — use @SQLDelete + @Where instead.
 */
package com.example.app.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base JPA superclass for all domain entities that require audit fields, optimistic
 * locking, and timestamp-based soft-delete.
 *
 * <h3>Soft-delete contract</h3>
 * <ul>
 *   <li>Rows are never hard-deleted via JPA {@code EntityManager.remove()} or
 *       repository {@code deleteById()}; instead, Hibernate intercepts every
 *       {@code DELETE} statement and replaces it with:
 *       <pre>UPDATE &lt;table&gt; SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?</pre>
 *   <li>The per-subclass {@code @SQLDelete} annotation carries the concrete
 *       table name (Hibernate cannot resolve it from the {@code @MappedSuperclass}).
 *   <li>{@code @Where(clause = "deleted_at IS NULL")} is applied here to exclude
 *       soft-deleted rows from all standard JPQL/Criteria queries transparently.
 *   <li>For queries that must include deleted rows (e.g. admin audit trail),
 *       use native SQL or call {@code em.createNativeQuery(...)}.
 * </ul>
 *
 * <h3>Optimistic locking</h3>
 * {@code @Version} on {@code version} causes Hibernate to append
 * {@code AND version = ?} to every UPDATE. A concurrent write that started with a
 * stale version throws {@code ObjectOptimisticLockingFailureException}.
 *
 * <h3>Audit fields</h3>
 * {@code createdAt} and {@code updatedAt} are populated automatically by
 * {@link AuditingEntityListener}. Requires {@code @EnableJpaAuditing} in a
 * {@code @Configuration} class (see {@code JpaAuditConfig}).
 *
 * <h3>Per-subclass requirement</h3>
 * Every concrete {@code @Entity} subclass MUST carry:
 * <pre>
 * {@literal @}SQLDelete(sql = "UPDATE my_table SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
 * </pre>
 * This is enforced at runtime by {@code BaseEntitySoftDeleteArchTest}.
 *
 * @see com.example.app.common.JpaAuditConfig
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Where(clause = "deleted_at IS NULL")
public abstract class BaseEntity {

    /** Primary key — UUID assigned by the database (no application-level UUID generation). */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Timestamp of initial persist. Never updated after creation. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Timestamp of last merge. Updated automatically by AuditingEntityListener. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Optimistic locking counter. Hibernate increments on every UPDATE and
     * checks for staleness before flushing. Concurrent writes with the same
     * version value result in {@code ObjectOptimisticLockingFailureException}.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Soft-delete timestamp. {@code NULL} = active row; non-null = deleted row.
     * Set by the {@code @SQLDelete} SQL on each concrete subclass — never set
     * directly by application code. Read via {@link #isDeleted()}.
     *
     * <p>Excluded from all standard JPQL queries by {@code @Where(clause = "deleted_at IS NULL")}.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    // ─── constructors ──────────────────────────────────────────────────────────

    protected BaseEntity() {
        // JPA — subclasses use protected no-arg constructors
    }

    // ─── accessors ─────────────────────────────────────────────────────────────

    public UUID getId()            { return id; }
    public Instant getCreatedAt()  { return createdAt; }
    public Instant getUpdatedAt()  { return updatedAt; }
    public Long getVersion()       { return version; }
    public Instant getDeletedAt()  { return deletedAt; }

    /**
     * Returns {@code true} if this entity has been soft-deleted.
     *
     * <p>In practice, active entities are never returned by standard JPQL queries
     * because of {@code @Where}; this method is useful in integration tests and
     * admin-path native queries that need to inspect the deleted state.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
