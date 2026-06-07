package com.ax.template.authblueprint.governedrecord;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GovernedDatumRepository extends JpaRepository<GovernedDatum, UUID> {

    boolean existsByName(String name);

    /** ACR-APPEND-ONLY-001 — lock the datum so the per-field change sequence is allocated serially
     *  and the oldValue read is exactly the pre-edit value (ACR-PREIMAGE-001). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM GovernedDatum d WHERE d.id = :id")
    Optional<GovernedDatum> findByIdForUpdate(@Param("id") UUID id);
}
