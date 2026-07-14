package com.ax.template.authblueprint.appealindependence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppealDecisionRepository extends JpaRepository<AppealDecision, UUID> {

    List<AppealDecision> findByChainRootIdOrderByLevelAsc(UUID chainRootId);

    /** APPEAL-CHAIN-001 — one appeal per decision level. */
    Optional<AppealDecision> findByParentDecisionId(UUID parentDecisionId);
}
