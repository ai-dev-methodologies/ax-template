package com.ax.template.authblueprint.accessgrant;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — a grant is revoked, never removed. */
public interface AccessGrantRepository extends JpaRepository<AccessGrant, UUID> {

    /** AGRANT-REVOKE-001 — the grant row serializes a concurrent revoke so exactly one (actor, instant) is recorded. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM AccessGrant g WHERE g.id = :id")
    Optional<AccessGrant> findByIdForUpdate(@Param("id") UUID id);
}
