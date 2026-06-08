package com.ax.template.authblueprint.copresence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * negative-copresence-gate-l0 knowledge-base entry — an UNORDERED conflicting concept pair with a
 * graded severity (the pair {A,B} conflicts whether the candidate is A-with-B-present or vice versa).
 * The gate looks up (candidate.concept, existing.concept) in either order. Immutable KB row.
 */
@AggregateRoot
@Entity
@Table(name = "copresence_conflict_rules",
    uniqueConstraints = @UniqueConstraint(name = "uq_copresence_conflict_pair",
        columnNames = {"concept_a", "concept_b"}))
@Check(constraints = "concept_a <> concept_b")
public class ConflictRule {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "concept_a", nullable = false, updatable = false, length = 200)
    private String conceptA;

    @Column(name = "concept_b", nullable = false, updatable = false, length = 200)
    private String conceptB;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, updatable = false, length = 20)
    private ConflictSeverity severity;

    @Column(name = "reason", nullable = false, updatable = false, length = 500)
    private String reason;

    protected ConflictRule() {}

    public ConflictRule(UUID id, String conceptA, String conceptB, ConflictSeverity severity, String reason) {
        this.id = id;
        this.conceptA = conceptA;
        this.conceptB = conceptB;
        this.severity = severity;
        this.reason = reason;
    }

    public UUID getId() { return id; }
    public String getConceptA() { return conceptA; }
    public String getConceptB() { return conceptB; }
    public ConflictSeverity getSeverity() { return severity; }
    public String getReason() { return reason; }
}
