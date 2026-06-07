package com.ax.template.authblueprint.dispatch;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    /**
     * OFFER-FSM-001 — pessimistic row lock so concurrent PENDING-offer creation for the SAME request
     * is serialized (the at-most-one-PENDING invariant holds in application logic, independent of the
     * partial unique index that a Flyway fork-receiver adds as the DB backstop). Closes the
     * check-then-act double-PENDING race under Read Committed.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ServiceRequest r WHERE r.id = :id")
    Optional<ServiceRequest> findByIdForUpdate(@Param("id") UUID id);

    /**
     * EXCL-CLAIM-001 — the request side of the two-sided claim, as ONE atomic status-guarded
     * conditional UPDATE. affected-rows == 1 ⇒ this caller won the request; affected-rows == 0 ⇒ the
     * request was no longer OFFERED (cancelled, or already assigned → JOB_ALREADY_TAKEN). Acquired
     * BEFORE the provider claim (deterministic request-then-provider order, EXCL-PAIR-002).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ServiceRequest r SET r.status = :assigned, r.assignedProviderId = :pid, "
        + "r.version = r.version + 1 WHERE r.id = :id AND r.status = :offered")
    int claim(@Param("id") UUID id,
              @Param("pid") UUID pid,
              @Param("offered") ServiceRequestStatus offered,
              @Param("assigned") ServiceRequestStatus assigned);
}
