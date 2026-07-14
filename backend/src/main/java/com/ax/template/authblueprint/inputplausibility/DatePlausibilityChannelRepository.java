package com.ax.template.authblueprint.inputplausibility;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — readings and rejected attempts are append-only. */
public interface DatePlausibilityChannelRepository extends JpaRepository<DatePlausibilityChannel, UUID> {

    // ── through-root member reads (HG-AGG-REPO — readings/attempts own no repository) ──

    @Query("SELECT r FROM DatePlausibilityReading r WHERE r.channelId = :channelId ORDER BY r.occurredAt ASC, r.id ASC")
    List<DatePlausibilityReading> findReadings(@Param("channelId") UUID channelId);

    @Query("SELECT a FROM DateRejectedAttempt a WHERE a.channelId = :channelId ORDER BY a.occurredAt ASC, a.id ASC")
    List<DateRejectedAttempt> findRejectedAttempts(@Param("channelId") UUID channelId);
}
