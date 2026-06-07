package com.ax.template.authblueprint.costshare;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccumulatorRepository extends JpaRepository<Accumulator, UUID> {

    Optional<Accumulator> findByScopeKey(String scopeKey);

    boolean existsByScopeKey(String scopeKey);

    /**
     * ACC-ATOMIC-001 / WF-LOCK-001 — pessimistic row lock so concurrent consumes against the SAME
     * accumulator serialize (each racing draw is served a deterministic partial `applied`; the
     * partials sum to exactly the headroom; `used` never passes `limit`). The service acquires these
     * locks in deterministic scope-key order across tiers to avoid a lock-order deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Accumulator a WHERE a.scopeKey = :scopeKey")
    Optional<Accumulator> findByScopeKeyForUpdate(@Param("scopeKey") String scopeKey);
}
