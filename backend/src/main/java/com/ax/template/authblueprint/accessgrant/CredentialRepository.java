package com.ax.template.authblueprint.accessgrant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — a credential is append-only. */
public interface CredentialRepository extends JpaRepository<Credential, UUID> {

    /**
     * AGRANT-ELIGIBILITY-001 — every credential a subject holds. Bounded by the subjectId
     * predicate (one subject's small credential set, not a full-table scan), so it is a
     * findBy<predicate> finder, not findAll* (ArchitectureUnboundedRepositoryListTest scope).
     * The eligibility verdict (which classes are valid at now) is recomputed in the service.
     */
    @Query("SELECT c FROM Credential c WHERE c.subjectId = :subjectId")
    List<Credential> findBySubjectId(@Param("subjectId") String subjectId);
}
