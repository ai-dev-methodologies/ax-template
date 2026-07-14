package com.ax.template.authblueprint.facetcount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * facet-count-l0 demonstrating resource: every row is scoped to its creating caller
 * ({@link #ownerId}) — the facet endpoint's caller-visibility boundary is "my own rows", so
 * two different callers computing facets over the SAME table get DIFFERENT counts
 * (FACET-COUNT-001). {@code category} and {@code status} are the only facetable fields
 * ({@link FacetFieldAllowlist}); {@code ownerId} itself is deliberately NOT facetable — the
 * keystone target proving a scope column cannot be probed via the aggregate path.
 *
 * <p>Identity columns are immutable ({@code updatable=false}); the entity is its own sole
 * mutator (no public setters); {@code @Version} guards concurrent writes.
 */
@AggregateRoot
@Entity
@Table(name = "facetable_items")
public class FacetableItem {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The caller-visibility scope boundary — never facetable itself (FACET-ALLOWLIST-002). */
    @Column(name = "owner_id", nullable = false, updatable = false, length = 200)
    private String ownerId;

    @Column(name = "category", nullable = false, updatable = false, length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ItemStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FacetableItem() {}

    public FacetableItem(UUID id, String ownerId, String category, ItemStatus status, Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.category = category;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getCategory() { return category; }
    public ItemStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
