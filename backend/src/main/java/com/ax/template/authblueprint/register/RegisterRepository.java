package com.ax.template.authblueprint.register;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RegisterRepository extends JpaRepository<Register, UUID> {

    Optional<Register> findByScopeKey(String scopeKey);

    boolean existsByScopeKey(String scopeKey);

    /** REG-CONCURRENT-001 — lock the register so concurrent appends serialize: each delta is computed
     *  against the freshly-committed anchor, never a stale one (no double-count / lost read). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Register r WHERE r.scopeKey = :scopeKey")
    Optional<Register> findByScopeKeyForUpdate(@Param("scopeKey") String scopeKey);
}
