package com.ax.template.authblueprint.inputplausibility;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — readings and rejected attempts are append-only. */
public interface PlausibilityChannelRepository extends JpaRepository<PlausibilityChannel, UUID> {

    /** PLAUSIBILITY-CONCURRENT-001 — the channel row serializes the read-prior / append-reading sequence. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM PlausibilityChannel c WHERE c.id = :id")
    Optional<PlausibilityChannel> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (HG-AGG-REPO — readings/attempts own no repository) ──

    @Query("SELECT r FROM PlausibilityReading r WHERE r.channelId = :channelId ORDER BY r.occurredAt ASC, r.id ASC")
    List<PlausibilityReading> findReadings(@Param("channelId") UUID channelId);

    @Query("SELECT a FROM RejectedAttempt a WHERE a.channelId = :channelId ORDER BY a.occurredAt ASC, a.id ASC")
    List<RejectedAttempt> findRejectedAttempts(@Param("channelId") UUID channelId);
}
