package com.ax.template.authblueprint.mececlassification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * mece-classification-l0 root: the classification IDENTITY for one (schemeKey, itemRef) pair
 * (MECE-EXCLUSIVE-001 — {@code uq(scheme_key, item_ref)} makes a second identity for the same pair
 * unrepresentable, even under a concurrent race). This row carries NO category column — the current
 * category is always DERIVED from the latest {@link ClassificationMove} (MECE-RECLASS-003); storing
 * a separately-mutable "current category" field here would be exactly the drift risk that
 * derive-on-read avoids. A second (scheme, item) is a 409 at the SERVICE layer (reclassify exists for
 * category changes); this row itself is otherwise immutable and carries no public setter.
 */
@AggregateRoot
@Entity
@Table(name = "item_classifications", uniqueConstraints = {
    @UniqueConstraint(name = "uq_mece_scheme_item", columnNames = {"scheme_key", "item_ref"})
})
public class ItemClassification {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "scheme_key", nullable = false, updatable = false, length = 200)
    private String schemeKey;

    @Column(name = "item_ref", nullable = false, updatable = false, length = 200)
    private String itemRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ItemClassification() {}

    public ItemClassification(UUID id, String schemeKey, String itemRef, Instant createdAt) {
        this.id = id;
        this.schemeKey = schemeKey;
        this.itemRef = itemRef;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getSchemeKey() { return schemeKey; }
    public String getItemRef() { return itemRef; }
    public Instant getCreatedAt() { return createdAt; }
}
