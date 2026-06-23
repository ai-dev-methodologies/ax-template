package com.ax.template.authblueprint.calendardeadline;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * business-day-deadline-arithmetic-l0 sole orchestrator. A deadline is computed by adding N units
 * to a start date in CALENDAR or BUSINESS mode against a PINNED holiday-calendar version, then
 * rolled per the recorded convention; the FULL basis is persisted so the deadline is
 * reconstructible (CALDLINE-BASIS-001). The holiday calendar is a versioned input
 * (CALDLINE-CALVER-001): editing it publishes a NEW version under the calendar's PESSIMISTIC_WRITE
 * row lock and never moves an already-computed deadline (which keeps the version it pinned). The
 * arithmetic is delegated to the pure {@link DeadlineArithmetic} helper; overdue is recomputed on
 * read (CALDLINE-OVERDUE-001), never stored. Each @Transactional method mutates ONE aggregate.
 */
@Service
public class CalendarDeadlineService {

    private final HolidayCalendarRepository calendars;
    private final CalendarDeadlineRepository deadlines;
    private final CalendarDeadlineMetrics metrics;
    private final Clock clock;

    public CalendarDeadlineService(HolidayCalendarRepository calendars,
                                   CalendarDeadlineRepository deadlines,
                                   CalendarDeadlineMetrics metrics, Clock clock) {
        this.calendars = calendars;
        this.deadlines = deadlines;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** Create a new versioned holiday calendar (version 0). Mutates the calendar aggregate only. */
    @Transactional
    public HolidayCalendar createCalendar(String calendarName, Set<LocalDate> holidays) {
        HolidayCalendar c = new HolidayCalendar(UUID.randomUUID(), calendarName, holidays,
            Instant.now(clock));
        HolidayCalendar saved = calendars.save(c);
        metrics.record("create_calendar", "ok");
        return saved;
    }

    /** CALDLINE-CALVER-001 — replace the holiday set; the @Version bump publishes a NEW version.
     *  Already-computed deadlines keep the version they pinned — this never re-rolls them. */
    @Transactional
    public HolidayCalendar editCalendar(UUID calendarId, Set<LocalDate> nextHolidays) {
        HolidayCalendar c = calendars.findByIdForUpdate(calendarId)
            .orElseThrow(CalendarDeadlineException::calendarNotFound);
        c.republishWith(nextHolidays);
        metrics.record("edit_calendar", "ok");
        return c;
    }

    /**
     * CALDLINE-BASIS/BUSINESS/ROLL-001 — compute a deadline against the calendar's CURRENT version,
     * recording the full reconstructible basis. Creating the CalendarDeadline is the only aggregate
     * mutated; the calendar is read (for its holiday set + version) but not written.
     */
    @Transactional
    public CalendarDeadline compute(String obligationRef, LocalDate startDate, int periodCount,
                                    DeadlineMode mode, UUID calendarId, RollConvention rollConvention) {
        if (periodCount < 0) {
            metrics.record("compute", "invalid");
            throw CalendarDeadlineException.invalidPeriod();
        }
        HolidayCalendar calendar = calendars.findById(calendarId)
            .orElseThrow(CalendarDeadlineException::calendarNotFound);
        Set<LocalDate> holidays = calendar.getHolidays();
        long pinnedVersion = calendar.getPublishedVersion();     // the domain version in force at compute time

        LocalDate raw = DeadlineArithmetic.rawDeadline(startDate, periodCount, mode, holidays);
        LocalDate adjusted = DeadlineArithmetic.roll(raw, rollConvention, holidays);

        CalendarDeadline d = new CalendarDeadline(UUID.randomUUID(), obligationRef, startDate,
            periodCount, mode, calendarId, pinnedVersion, raw, rollConvention, adjusted,
            Instant.now(clock));
        CalendarDeadline saved = deadlines.save(d);
        metrics.record("compute", "ok");
        return saved;
    }

    @Transactional(readOnly = true)
    public CalendarDeadline get(UUID deadlineId) {
        return deadlines.findById(deadlineId)
            .orElseThrow(CalendarDeadlineException::deadlineNotFound);
    }

    @Transactional(readOnly = true)
    public HolidayCalendar getCalendar(UUID calendarId) {
        return calendars.findById(calendarId)
            .orElseThrow(CalendarDeadlineException::calendarNotFound);
    }

    /** CALDLINE-OVERDUE-001 — the recomputed predicate at the current as-of instant. */
    public boolean isOverdue(CalendarDeadline d) {
        return d.isOverdueAt(Instant.now(clock));
    }
}
