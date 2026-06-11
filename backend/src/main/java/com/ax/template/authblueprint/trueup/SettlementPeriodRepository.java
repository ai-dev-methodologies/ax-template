package com.ax.template.authblueprint.trueup;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SettlementPeriodRepository extends JpaRepository<SettlementPeriod, UUID> {

    /** TUP-CONCURRENT-001 — the period row is the serialization point for every write. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SettlementPeriod p WHERE p.id = :id")
    Optional<SettlementPeriod> findByIdForUpdate(@Param("id") UUID id);
}
