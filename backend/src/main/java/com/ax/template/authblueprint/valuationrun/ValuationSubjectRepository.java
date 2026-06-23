package com.ax.template.authblueprint.valuationrun;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared — a valuation subject is never removed. */
public interface ValuationSubjectRepository extends JpaRepository<ValuationSubject, UUID> {

    /** VALRUN-CONCURRENT-001 — the subject row serializes the read-version / write-next-version advance. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ValuationSubject s WHERE s.id = :id")
    Optional<ValuationSubject> findByIdForUpdate(@Param("id") UUID id);
}
