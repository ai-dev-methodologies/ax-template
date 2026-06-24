package com.ax.template.authblueprint.commercecatalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Category aggregate root — a separate consistency boundary from {@link CatalogProduct}.
 *
 * <p>INV-6: {@link CategoryService} rejects edges that would close a cycle (ancestor walk).
 * {@code parent_id} is a self-reference by UUID only (no JPA bidirectional). Null parent_id
 * means root-level category.
 *
 * <p>Window well-formedness is backstopped by @Check mirroring the CatalogProduct date
 * constraint pattern.
 */
@AggregateRoot
@Entity
@Table(name = "catalog_categories")
@Check(constraints = "active_end_date IS NULL OR active_start_date IS NULL OR active_end_date > active_start_date")
public class Category {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "name", nullable = false, length = 400)
    private String name;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @Column(name = "active_start_date")
    private Instant activeStartDate;

    @Column(name = "active_end_date")
    private Instant activeEndDate;

    /** UUID reference to parent category. Null = root. */
    @Column(name = "parent_id")
    private UUID parentId;

    protected Category() {}

    public Category(UUID id, String name, Instant activeStartDate, Instant activeEndDate, UUID parentId) {
        this.id = id;
        this.name = name;
        this.archived = false;
        this.activeStartDate = activeStartDate;
        this.activeEndDate = activeEndDate;
        this.parentId = parentId;
    }

    /** Package-private: called by {@link CategoryService} under PESSIMISTIC_WRITE lock (INV-6). */
    void reparentTo(UUID newParentId) {
        this.parentId = newParentId;
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public String getName() { return name; }
    public boolean isArchived() { return archived; }
    public Instant getActiveStartDate() { return activeStartDate; }
    public Instant getActiveEndDate() { return activeEndDate; }
    public UUID getParentId() { return parentId; }
}
