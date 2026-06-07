package com.ax.template.authblueprint.copresence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KnownConceptRepository extends JpaRepository<KnownConcept, UUID> {

    /** GATE-FAILCLOSED-001 — is the candidate's concept assessable by the KB? Absent => fail closed. */
    boolean existsByConcept(String concept);
}
