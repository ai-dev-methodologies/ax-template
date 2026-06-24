package com.ax.template.authblueprint.commercecatalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * CategoryService owns category tree edits and acyclicity (INV-6).
 *
 * <p>INV-6: rejects an edge that would close a cycle by walking ancestors of the
 * requested parent. If the new child's id appears in the ancestor chain, the edge
 * is rejected 409 CATALOG_CATEGORY_CYCLE.
 */
@Service
public class CategoryService {

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    /**
     * Create a new category. If parentId is non-null, verifies the parent exists and that
     * adding the new category as a child would not create a cycle (INV-6).
     * Note: a freshly-created category cannot introduce a cycle by definition (it has no
     * existing children), but we still validate parent existence.
     */
    @Transactional
    public Category createCategory(String name, Instant activeStartDate, Instant activeEndDate,
                                   UUID parentId) {
        if (parentId != null) {
            categories.findById(parentId).orElseThrow(() -> CatalogException.notFound("Parent category"));
        }
        return categories.save(new Category(UUID.randomUUID(), name, activeStartDate, activeEndDate, parentId));
    }

    /**
     * Reparent an existing category (INV-6: rejects if new parent is in this category's subtree).
     * Acquires PESSIMISTIC_WRITE lock, walks ancestors of newParentId upward, rejects on cycle.
     */
    @Transactional
    public Category reparent(UUID categoryId, UUID newParentId) {
        Category cat = categories.findByIdForUpdate(categoryId)
            .orElseThrow(() -> CatalogException.notFound("Category"));
        if (newParentId != null) {
            categories.findById(newParentId).orElseThrow(() -> CatalogException.notFound("Parent category"));
            if (wouldCreateCycle(categoryId, newParentId)) {
                throw CatalogException.categoryCycle();
            }
        }
        cat.reparentTo(newParentId);
        return categories.saveAndFlush(cat);
    }

    @Transactional(readOnly = true)
    public Category getCategory(UUID id) {
        return categories.findById(id)
            .orElseThrow(() -> CatalogException.notFound("Category"));
    }

    /**
     * INV-6: Would adding {@code childId} as a child of {@code proposedParentId} create a cycle?
     * Walk upward from proposedParentId; if we ever reach childId, it's a cycle.
     */
    boolean wouldCreateCycle(UUID childId, UUID proposedParentId) {
        Set<UUID> visited = new HashSet<>();
        UUID current = proposedParentId;
        while (current != null) {
            if (current.equals(childId)) return true;
            if (!visited.add(current)) break; // unexpected cycle guard (db inconsistency)
            Optional<UUID> parent = categories.findParentId(current);
            current = parent.orElse(null);
        }
        return false;
    }
}
