package com.ax.template.authblueprint.tagcategorization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * TagAttachment — a polymorphic edge between a {@link Tag} and any
 * domain entity identified by the ({@code entityType}, {@code entityId}) pair.
 *
 * <p>Trace:
 * <ul>
 *   <li>TAG-ATTACH-001 — uniqueness on (tagId, entityType, entityId) makes the
 *       attach call idempotent at the DB layer</li>
 *   <li>TAG-CRUD-004 — FK to {@code tags(id)} ON DELETE CASCADE so deleting a
 *       tag never strands attachments</li>
 *   <li>TAG-ATTACH-003 — repository finder joins on Tag + filters on the pair</li>
 * </ul>
 */
@AggregateRoot
@Entity
@Table(
    name = "tag_attachments",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_tag_attachments_tag_entity",
        columnNames = {"tag_id", "entity_type", "entity_id"}
    ),
    indexes = {
        @Index(name = "ix_tag_attachments_entity",
               columnList = "entity_type,entity_id"),
        @Index(name = "ix_tag_attachments_tag", columnList = "tag_id")
    }
)
public class TagAttachment {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tag_id", nullable = false, updatable = false)
    private UUID tagId;

    @Column(name = "entity_type", nullable = false, updatable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false, length = 255)
    private String entityId;

    @Column(name = "attached_at", nullable = false, updatable = false)
    private Instant attachedAt;

    @Column(name = "attached_by_user_id", length = 255, updatable = false)
    private String attachedByUserId;

    protected TagAttachment() {}

    private TagAttachment(Builder b) {
        this.id = (b.id != null) ? b.id : UUID.randomUUID();
        this.tagId = b.tagId;
        this.entityType = b.entityType;
        this.entityId = b.entityId;
        this.attachedAt = (b.attachedAt != null) ? b.attachedAt : Instant.now();
        this.attachedByUserId = b.attachedByUserId;
    }

    public UUID getId() { return id; }
    public UUID getTagId() { return tagId; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public Instant getAttachedAt() { return attachedAt; }
    public String getAttachedByUserId() { return attachedByUserId; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID tagId;
        private String entityType;
        private String entityId;
        private Instant attachedAt;
        private String attachedByUserId;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder tagId(UUID v) { this.tagId = v; return this; }
        public Builder entityType(String v) { this.entityType = v; return this; }
        public Builder entityId(String v) { this.entityId = v; return this; }
        public Builder attachedAt(Instant v) { this.attachedAt = v; return this; }
        public Builder attachedByUserId(String v) { this.attachedByUserId = v; return this; }

        public TagAttachment build() { return new TagAttachment(this); }
    }
}
