package com.ax.template.authblueprint.recordlinkage;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchProposalRepository extends JpaRepository<MatchProposal, UUID> {

    /** LINK-CONCURRENT-001 — the proposal row makes the decide-once 409 deterministic. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM MatchProposal p WHERE p.id = :id")
    Optional<MatchProposal> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (HG-AGG-REPO — SurvivorshipDecision owns no repository) ──

    @Query("SELECT d FROM SurvivorshipDecision d WHERE d.proposalId = :proposalId ORDER BY d.fieldName ASC")
    List<SurvivorshipDecision> findDecisions(@Param("proposalId") UUID proposalId);
}
