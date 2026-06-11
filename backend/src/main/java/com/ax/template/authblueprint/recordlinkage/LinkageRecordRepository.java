package com.ax.template.authblueprint.recordlinkage;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — a record is tombstoned, never removed. */
public interface LinkageRecordRepository extends JpaRepository<LinkageRecord, UUID> {

    /** LINK-CONCURRENT-001 — records lock in ascending-id order (the deadlock guard). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM LinkageRecord r WHERE r.id = :id")
    Optional<LinkageRecord> findByIdForUpdate(@Param("id") UUID id);
}
