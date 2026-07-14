package com.ax.template.authblueprint.mececlassification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * mece-classification-l0 root: one immutable scheme declaration, keyed by {@code schemeKey}. The
 * {@code residualCategory} is MANDATORY at config time (MECE-EXHAUSTIVE-002) — a scheme with no
 * residual bucket cannot be collectively exhaustive by construction. Every column is
 * {@code updatable = false}; there is no public setter — a scheme, once declared, is fixed (adding
 * rules is a separate, additive member write; changing the residual bucket means declaring a new
 * scheme).
 */
@AggregateRoot
@Entity
@Table(name = "classification_schemes", uniqueConstraints = {
    @UniqueConstraint(name = "uq_mece_scheme_key", columnNames = {"scheme_key"})
})
@Check(constraints = "LENGTH(residual_category) > 0")
public class ClassificationScheme {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "scheme_key", nullable = false, updatable = false, length = 200)
    private String schemeKey;

    /** MECE-EXHAUSTIVE-002 — mandatory non-blank residual bucket; makes classification never fail open. */
    @Column(name = "residual_category", nullable = false, updatable = false, length = 200)
    private String residualCategory;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ClassificationScheme() {}

    public ClassificationScheme(UUID id, String schemeKey, String residualCategory, Instant createdAt) {
        this.id = id;
        this.schemeKey = schemeKey;
        this.residualCategory = residualCategory;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getSchemeKey() { return schemeKey; }
    public String getResidualCategory() { return residualCategory; }
    public Instant getCreatedAt() { return createdAt; }
}
