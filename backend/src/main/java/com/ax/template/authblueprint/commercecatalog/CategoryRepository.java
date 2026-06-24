package com.ax.template.authblueprint.commercecatalog;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Root repository for {@link Category} — the separate tree-editing consistency boundary.
 * Cycle detection (INV-6) is performed in {@link CategoryService} using ancestor-walk queries.
 */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Category c WHERE c.id = :id")
    Optional<Category> findByIdForUpdate(@Param("id") UUID id);

    /** Load all children of a given parent for ancestor-walk cycle detection (INV-6). */
    @Query("SELECT c FROM Category c WHERE c.parentId = :parentId")
    List<Category> findByParentId(@Param("parentId") UUID parentId);

    /** Load all categories that have a given parent — used for ancestor-walk upward traversal. */
    @Query("SELECT c.parentId FROM Category c WHERE c.id = :id")
    Optional<UUID> findParentId(@Param("id") UUID id);
}
