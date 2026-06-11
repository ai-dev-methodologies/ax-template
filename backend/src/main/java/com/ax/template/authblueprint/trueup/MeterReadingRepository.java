package com.ax.template.authblueprint.trueup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {

    /** The current truth for one slot — writes reach it only under the period's row lock. */
    @Query("SELECT r FROM MeterReading r WHERE r.periodId = :periodId AND r.slotIndex = :slotIndex"
        + " AND r.status = com.ax.template.authblueprint.trueup.ReadingStatus.ACTIVE")
    Optional<MeterReading> findActive(@Param("periodId") UUID periodId, @Param("slotIndex") int slotIndex);

    @Query("SELECT r FROM MeterReading r WHERE r.periodId = :periodId"
        + " AND r.status = com.ax.template.authblueprint.trueup.ReadingStatus.ACTIVE"
        + " ORDER BY r.slotIndex ASC")
    List<MeterReading> findActiveByPeriod(@Param("periodId") UUID periodId);

    /** Full supersession trail — every version of every slot, in slot/version order. */
    List<MeterReading> findByPeriodIdOrderBySlotIndexAscSlotVersionAsc(UUID periodId);
}
