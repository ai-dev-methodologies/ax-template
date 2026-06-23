package com.ax.template.authblueprint.statemutation;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — a governed form is locked, never removed. */
public interface GovernedFormRepository extends JpaRepository<GovernedForm, UUID> {

    /** STATEMUTATION-TOCTOU-001 — the form row serializes the read-state / enforce / write sequence so the
     *  field authority is evaluated against the state that holds UNDER the lock (CWE-367). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM GovernedForm f WHERE f.id = :id")
    Optional<GovernedForm> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (HG-AGG-REPO — FormTransition owns no repository) ──

    @Query("SELECT t FROM FormTransition t WHERE t.formId = :formId ORDER BY t.seq ASC")
    List<FormTransition> findTransitions(@Param("formId") UUID formId);

    @Query("SELECT COUNT(t) FROM FormTransition t WHERE t.formId = :formId")
    long countTransitions(@Param("formId") UUID formId);
}
