package com.ax.template.authblueprint.rangeownership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — assignments/events are append-only. */
public interface IdentifierAssignmentRepository extends JpaRepository<IdentifierAssignment, UUID> {

    Optional<IdentifierAssignment> findByIdentifierValue(long identifierValue);

    // ── through-root member reads (HG-AGG-REPO — events own no repository) ──

    @Query("SELECT e FROM OwnershipEvent e WHERE e.assignmentId = :assignmentId ORDER BY e.occurredAt ASC, e.id ASC")
    List<OwnershipEvent> findEvents(@Param("assignmentId") UUID assignmentId);

    /** RNG-PORT-003 — the current owner is derive-on-read: the toOwner of the LATEST event. */
    @Query("SELECT e FROM OwnershipEvent e WHERE e.assignmentId = :assignmentId ORDER BY e.occurredAt DESC, e.id DESC")
    List<OwnershipEvent> findLatestEventFirst(@Param("assignmentId") UUID assignmentId);
}
