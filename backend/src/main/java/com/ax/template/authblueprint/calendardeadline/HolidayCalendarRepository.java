package com.ax.template.authblueprint.calendardeadline;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared — a holiday calendar is versioned, never removed. */
public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendar, UUID> {

    /** CALDLINE-CALVER-001 — serialize the read-set / write-new-version republish on the row. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM HolidayCalendar c WHERE c.id = :id")
    Optional<HolidayCalendar> findByIdForUpdate(@Param("id") UUID id);
}
