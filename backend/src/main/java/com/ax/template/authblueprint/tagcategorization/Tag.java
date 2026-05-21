package com.ax.template.authblueprint.tagcategorization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Tag — an admin-curated taxonomy entry. {@code slug} is the stable URL-safe identifier
 * derived from the name; {@code parentTagId} is the parent in the hierarchy (null = top-level).
 *
 * <p>Trace:
 * <ul>
 *   <li>TAG-CRUD-001 — slug auto-generated from name, unique, immutable</li>
 *   <li>TAG-CRUD-003 — slug + parentTagId are JPA @Column(updatable=false)</li>
 *   <li>TAG-HIER-001 — parentTagId immutable closes the cycle-introduction window</li>
 * </ul>
 */
@Entity
@Table(
    name = "tags",
    uniqueConstraints = @UniqueConstraint(name = "uq_tags_slug", columnNames = "slug"),
    indexes = {
        @Index(name = "ix_tags_parent", columnList = "parent_tag_id"),
        @Index(name = "ix_tags_name", columnList = "name")
    }
)
public class Tag {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "slug", nullable = false, updatable = false, length = 64)
    private String slug;

    @Column(name = "parent_tag_id", updatable = false)
    private UUID parentTagId;

    @Column(name = "color", length = 16)
    private String color;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by_user_id", length = 255, updatable = false)
    private String createdByUserId;

    protected Tag() {}

    private Tag(Builder b) {
        this.id = (b.id != null) ? b.id : UUID.randomUUID();
        this.name = b.name;
        this.slug = b.slug;
        this.parentTagId = b.parentTagId;
        this.color = b.color;
        this.createdAt = (b.createdAt != null) ? b.createdAt : Instant.now();
        this.createdByUserId = b.createdByUserId;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public UUID getParentTagId() { return parentTagId; }
    public String getColor() { return color; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedByUserId() { return createdByUserId; }

    // Package-private — service is the only mutator. Only name + color are writable
    // (TAG-CRUD-003: slug + parentTagId + createdAt + createdByUserId are immutable).
    void setName(String name) { this.name = name; }
    void setColor(String color) { this.color = color; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String name;
        private String slug;
        private UUID parentTagId;
        private String color;
        private Instant createdAt;
        private String createdByUserId;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder slug(String v) { this.slug = v; return this; }
        public Builder parentTagId(UUID v) { this.parentTagId = v; return this; }
        public Builder color(String v) { this.color = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }
        public Builder createdByUserId(String v) { this.createdByUserId = v; return this; }

        public Tag build() { return new Tag(this); }
    }
}
