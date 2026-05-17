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
     * Returns all non-deleted (active) entities, paginated.
     *
     * <p>Replace {@code e.deleted = false} with the actual field name if your
     * entity uses a different convention (e.g. {@code e.deletedAt IS NULL}).
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.deleted = false")
    Page<T> findAllActive(Pageable pageable);

    /**
     * Returns an active entity by ID. Returns empty if deleted.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.deleted = false")
    Optional<T> findActiveById(ID id);

    /**
     * Soft-deletes an entity by marking {@code deleted = true}.
     *
     * <p>The entity row is retained; historical data and FK integrity are preserved.
     * Hard-delete via {@link #deleteById(Object)} is still available for administrative use.
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deleted = true WHERE e.id = :id")
    void softDelete(ID id);
}
