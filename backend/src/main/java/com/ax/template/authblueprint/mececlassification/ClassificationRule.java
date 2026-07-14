package com.ax.template.authblueprint.mececlassification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One immutable classification rule of a {@link ClassificationScheme} (MECE-EXHAUSTIVE-002): an
 * attribute value that exactly matches {@code matchValue} resolves to {@code category}. An attribute
 * value matching NO declared rule falls through to the scheme's {@code residualCategory} — this rule
 * table is never itself the exhaustiveness guarantee; the residual bucket is. Append-only: every
 * column {@code updatable = false}, no public setter, no delete path.
 */
@AggregateMember(root = ClassificationScheme.class)
@Entity
@Table(name = "classification_rules", uniqueConstraints = {
    @UniqueConstraint(name = "uq_mece_rule_match", columnNames = {"scheme_key", "match_value"})
})
public class ClassificationRule {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "scheme_key", nullable = false, updatable = false, length = 200)
    private String schemeKey;

    @Column(name = "match_value", nullable = false, updatable = false, length = 200)
    private String matchValue;

    @Column(name = "category", nullable = false, updatable = false, length = 200)
    private String category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ClassificationRule() {}

    public ClassificationRule(UUID id, String schemeKey, String matchValue, String category, Instant createdAt) {
        this.id = id;
        this.schemeKey = schemeKey;
        this.matchValue = matchValue;
        this.category = category;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getSchemeKey() { return schemeKey; }
    public String getMatchValue() { return matchValue; }
    public String getCategory() { return category; }
    public Instant getCreatedAt() { return createdAt; }
}
