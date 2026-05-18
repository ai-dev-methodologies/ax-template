/**
 * @ax-template-meta
 * template_id: backend/repositories/BaseRepository
 * layer: backend-cross-cutting
 * anchors_rule: testing-archunit-repository-shape.md (PRACTICES-TEST-004)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Defining Repository Interfaces"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html"
 *   - source_type: external
 *     citation: "Spring Data Commons Reference — CrudRepository / JpaRepository hierarchy"
 *     url: "https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Replace T with your entity type and ID with its primary key type.
 *   Domain repositories extend this interface:
 *     public interface ItemRepository extends BaseRepository<Item, Long> { ... }
 *   All *Repository interfaces must extend JpaRepository — enforced by ArchUnit rule.
 */
package com.example.app.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

/**
 * Base Spring Data JPA repository with soft-delete support.
 *
 * <p>Domain repositories extend this interface rather than {@link JpaRepository}
 * directly to gain:
 * <ul>
 *   <li>Soft-delete via {@code deleted} flag — {@link #softDelete(Object)} marks the
 *       entity deleted without removing the row
 *   <li>Active-only queries — {@link #findAllActive(Pageable)} excludes deleted rows
 *   <li>ArchUnit shape check passes — the interface extends {@link JpaRepository}
 * </ul>
 *
 * <p>Assumes entities carry a {@code deleted} boolean field and implement
 * {@code Deletable} (or use the provided {@code BaseEntity} template).
 * Adjust the JPQL queries below to match your entity's field name.
 *
 * <p>Rule reference: PRACTICES-TEST-004 (*Repository interfaces must extend JpaRepository).
 *
 * @param <T>  entity type
 * @param <ID> primary key type
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {

    /**
     * Returns all active (non-soft-deleted) entities, paginated.
     *
     * <p>For entities extending {@code BaseEntity}, the {@code @Where(clause = "deleted_at IS NULL")}
     * on the superclass already filters out soft-deleted rows from all standard JPQL queries.
     * This explicit query is provided for repositories whose entity does NOT extend BaseEntity
     * but still carries a {@code deletedAt} field.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.deletedAt IS NULL")
    Page<T> findAllActive(Pageable pageable);

    /**
     * Returns an active entity by ID. Returns empty if deleted.
     *
     * <p>For BaseEntity subclasses, {@code findById} already excludes deleted rows via
     * {@code @Where}. Use this method when the entity does not extend BaseEntity.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<T> findActiveById(ID id);

    /**
     * Soft-deletes an entity via a JPQL UPDATE.
     *
     * <p>Prefer calling {@code repository.deleteById(id)} for BaseEntity subclasses —
     * Hibernate intercepts the underlying DELETE and runs the {@code @SQLDelete} UPDATE
     * automatically. This JPQL method is provided as an explicit alternative for batch
     * or conditional soft-delete operations.
     *
     * <p>The entity row is retained; historical data and FK integrity are preserved.
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deletedAt = CURRENT_TIMESTAMP WHERE e.id = :id AND e.deletedAt IS NULL")
    int softDelete(ID id);
}
