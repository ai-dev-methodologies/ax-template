package com.ax.template.authblueprint.mececlassification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One immutable move record of an {@link ItemClassification} (MECE-RECLASS-003): the FROM category
 * (null for the first move — the initial assignment), the TO category, the acting principal, a
 * reason, and a timestamp. The current category is ALWAYS derived by reading the LATEST move for a
 * classification — there is no separate mutable "current" field anywhere in this domain. Append-only:
 * every column {@code updatable = false}, no public setter, no delete path.
 */
@AggregateMember(root = ItemClassification.class)
@Entity
@Table(name = "classification_moves")
public class ClassificationMove {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "classification_id", nullable = false, updatable = false)
    private UUID classificationId;

    @Column(name = "from_category", updatable = false, length = 200)
    private String fromCategory;

    @Column(name = "to_category", nullable = false, updatable = false, length = 200)
    private String toCategory;

    @Column(name = "actor", nullable = false, updatable = false, length = 200)
    private String actor;

    @Column(name = "reason", nullable = false, updatable = false, length = 500)
    private String reason;

    @Column(name = "moved_at", nullable = false, updatable = false)
    private Instant movedAt;

    protected ClassificationMove() {}

    public ClassificationMove(UUID id, UUID classificationId, String fromCategory, String toCategory,
                              String actor, String reason, Instant movedAt) {
        this.id = id;
        this.classificationId = classificationId;
        this.fromCategory = fromCategory;
        this.toCategory = toCategory;
        this.actor = actor;
        this.reason = reason;
        this.movedAt = movedAt;
    }

    public UUID getId() { return id; }
    public UUID getClassificationId() { return classificationId; }
    public String getFromCategory() { return fromCategory; }
    public String getToCategory() { return toCategory; }
    public String getActor() { return actor; }
    public String getReason() { return reason; }
    public Instant getMovedAt() { return movedAt; }
}
