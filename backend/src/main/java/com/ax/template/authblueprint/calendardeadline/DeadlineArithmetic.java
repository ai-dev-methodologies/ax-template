package com.ax.template.authblueprint.calendardeadline;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * business-day-deadline-arithmetic-l0 pure calendar/business-day arithmetic (CALDLINE-BUSINESS-001
 * + CALDLINE-ROLL-001). A side-effect-free {@link LocalDate} function — no entity, no clock, no
 * persistence — so the computation is reproducible from its recorded basis. Generalizes the legal
 * time-computation rule (FRCP Rule 6(a)(1): the day-0 triggering event is EXCLUDED — counting
 * starts the day after the start date — every intervening day is examined, and the last day rolls
 * forward off a Saturday/Sunday/holiday).
 */
final class DeadlineArithmetic {

    private DeadlineArithmetic() {}

    static boolean isWeekend(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    /** A business day is any day that is not a weekend and not in the holiday set. */
    static boolean isBusinessDay(LocalDate d, Set<LocalDate> holidays) {
        return !isWeekend(d) && !holidays.contains(d);
    }

    /**
     * CALDLINE-BUSINESS-001 — the RAW deadline (before any roll). CALENDAR adds {@code n} calendar
     * days counting every day; BUSINESS advances {@code n} business days, skipping weekends and the
     * holiday set (a skipped day does not consume a unit). FRCP Rule 6(a)(1)(A): the start date
     * (the day-0 event) is excluded — counting begins the day after.
     */
    static LocalDate rawDeadline(LocalDate startDate, int n, DeadlineMode mode, Set<LocalDate> holidays) {
        if (mode == DeadlineMode.CALENDAR) {
            return startDate.plusDays(n);
        }
        LocalDate cursor = startDate;          // day-0 excluded; the first business day counted is after it
        int counted = 0;
        while (counted < n) {
            cursor = cursor.plusDays(1);
            if (isBusinessDay(cursor, holidays)) {
                counted++;
            }
        }
        return cursor;
    }

    /**
     * CALDLINE-ROLL-001 — apply the roll convention. FOLLOWING rolls a raw date that lands on a
     * non-business day forward to the next business day (FRCP Rule 6(a)(1)(C): "the period
     * continues to run until the next day that is not a Saturday, Sunday, or legal holiday"); NONE
     * leaves the date unchanged. FOLLOWING on a date that is already a business day is a no-op.
     */
    static LocalDate roll(LocalDate raw, RollConvention convention, Set<LocalDate> holidays) {
        if (convention == RollConvention.NONE) {
            return raw;
        }
        LocalDate cursor = raw;
        while (!isBusinessDay(cursor, holidays)) {
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }
}
