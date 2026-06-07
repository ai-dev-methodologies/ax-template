package com.ax.template.authblueprint.copresence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * negative-copresence-gate-l0 knowledge-base vocabulary — the set of concepts the KB can ASSESS. A
 * candidate whose concept is absent here is UNASSESSABLE and the gate fails closed (GATE-FAILCLOSED-001,
 * Saltzer fail-safe defaults), rather than silently allowing an unrecognized concept past the check.
 */
@Entity
@Table(name = "copresence_known_concepts")
public class KnownConcept {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "concept", nullable = false, updatable = false, length = 200, unique = true)
    private String concept;

    protected KnownConcept() {}

    public KnownConcept(UUID id, String concept) {
        this.id = id;
        this.concept = concept;
    }

    public UUID getId() { return id; }
    public String getConcept() { return concept; }
}
