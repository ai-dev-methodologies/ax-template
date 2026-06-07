package com.ax.template.authblueprint.transformation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransformationLegRepository extends JpaRepository<TransformationLeg, UUID> {

    /** Legs of one run, ordered by role. Bounded (Pageable) per ArchitectureUnboundedRepositoryList. */
    List<TransformationLeg> findByRunIdOrderByRoleAsc(UUID runId, Pageable pageable);
}
