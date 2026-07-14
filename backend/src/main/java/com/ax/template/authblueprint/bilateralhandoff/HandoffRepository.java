package com.ax.template.authblueprint.bilateralhandoff;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** NO delete method — a handoff is completed or voided, never removed. */
public interface HandoffRepository extends JpaRepository<Handoff, UUID> {

    /** BHO-ATOMIC-001 — the row lock serializing the two independent confirmations onto one
     *  atomic completion check (CWE-362). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Handoff h WHERE h.id = :id")
    Optional<Handoff> findByIdForUpdate(@Param("id") UUID id);
}
