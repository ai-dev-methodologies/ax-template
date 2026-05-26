package com.ax.template.authblueprint.favoritesbookmarks;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Favorite — per-user single-state marker on a polymorphic entity reference.
 *
 * <p>Trace:
 * <ul>
 *   <li>FAV-CRUD-001 — UNIQUE(user_id, entity_type, entity_id) backs idempotent add</li>
 *   <li>FAV-AUTHZ-002 / 003 — user_id is the only scoping key</li>
 * </ul>
 */
@Entity
@Table(
    name = "favorites",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_favorites_user_entity",
        columnNames = {"user_id", "entity_type", "entity_id"}
    ),
    indexes = {
        @Index(name = "ix_favorites_user_favorited", columnList = "user_id,favorited_at"),
        @Index(name = "ix_favorites_entity", columnList = "entity_type,entity_id")
    }
)
public class Favorite {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false, length = 255)
    private String userId;

    @Column(name = "entity_type", nullable = false, updatable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false, length = 255)
    private String entityId;

    /**
     * R78 iter1 F4 (HIGH) — user-owned free-text annotation on a favorite.
     * Caller-scoped read (FAV-AUTHZ-002) keeps direct exposure private to
     * the owner, but the value is stored VERBATIM and treated as PII
     * territory at every downstream surface:
     *
     * <ul>
     *   <li>Database backups / replicas — note travels with the row.</li>
     *   <li>BI / analytics extracts — fork-receiver-owned scrubbing.</li>
     *   <li>Audit-log emission (R61 / R67) — fork-receivers wiring audit
     *       logs on favorites MUST NOT log the raw note. Use
     *       {@code AuditPiiHelper.piiHash} for correlation when needed.</li>
     *   <li>Cross-user features (e.g. "shared favorites", "collaborative
     *       lists") — note becomes a phishing surface; require redaction
     *       before fan-out.</li>
     * </ul>
     *
     * <p>A user can paste 주민등록번호 / email / JWT / Bearer fragments
     * here; the catalog deliberately does NOT redact at storage time
     * because that would break the legitimate private-annotation UX.
     * Compliance posture (개인정보보호법 §24) is delegated to the
     * fork-receiver's privacy program.
     */
    @Column(name = "note", length = 256)
    private String note;

    @Column(name = "favorited_at", nullable = false, updatable = false)
    private Instant favoritedAt;

    protected Favorite() {}

    private Favorite(Builder b) {
        this.id = (b.id != null) ? b.id : UUID.randomUUID();
        this.userId = b.userId;
        this.entityType = b.entityType;
        this.entityId = b.entityId;
        this.note = b.note;
        this.favoritedAt = (b.favoritedAt != null) ? b.favoritedAt : Instant.now();
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getNote() { return note; }
    public Instant getFavoritedAt() { return favoritedAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String userId;
        private String entityType;
        private String entityId;
        private String note;
        private Instant favoritedAt;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder userId(String v) { this.userId = v; return this; }
        public Builder entityType(String v) { this.entityType = v; return this; }
        public Builder entityId(String v) { this.entityId = v; return this; }
        public Builder note(String v) { this.note = v; return this; }
        public Builder favoritedAt(Instant v) { this.favoritedAt = v; return this; }

        public Favorite build() { return new Favorite(this); }
    }
}
