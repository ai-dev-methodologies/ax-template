package com.ax.template.authblueprint.authzparity;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The action is the only aggregate ROOT here; its signoffs, gate-satisfactions and blocked
 * attempts are {@code @AggregateMember} — read via the through-root JPQL methods below
 * (HG-AGG-REPO: members own no repository), written via {@code common/MemberWriter}. No delete
 * method is declared anywhere in this domain — an action is a permanent authorization of record.
 */
public interface AuthorizedActionRepository extends JpaRepository<AuthorizedAction, UUID> {

    /** AUTHZPARITY-CONCURRENT-001 — the row lock makes check-then-execute atomic (exactly-once). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AuthorizedAction a WHERE a.id = :id")
    Optional<AuthorizedAction> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (HG-AGG-REPO — members own no repository) ──

    @Query("SELECT s FROM ActionSignoff s WHERE s.actionId = :actionId ORDER BY s.signedAt ASC")
    List<ActionSignoff> findSignoffs(@Param("actionId") UUID actionId);

    @Query("SELECT g FROM GateSatisfaction g WHERE g.actionId = :actionId ORDER BY g.gateKey ASC")
    List<GateSatisfaction> findGates(@Param("actionId") UUID actionId);

    @Query("SELECT b FROM BlockedAttempt b WHERE b.actionId = :actionId ORDER BY b.attemptedAt ASC")
    List<BlockedAttempt> findBlockedAttempts(@Param("actionId") UUID actionId);
}
