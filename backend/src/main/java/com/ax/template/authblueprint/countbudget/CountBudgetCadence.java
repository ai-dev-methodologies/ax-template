package com.ax.template.authblueprint.countbudget;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * periodic-count-budget-l0 recurring cadence (PCB-RESET-001). {@link #periodKeyFor} is a PURE function of
 * {@code (this, instant)} — the calendar boundary, never a prior period's completion. All arithmetic is
 * done in UTC so the boundary is TZ-independent (mirrors the netmetering/threshold reference workloads'
 * explicit-instant discipline).
 */
public enum CountBudgetCadence {
    DAILY,
    WEEKLY,
    MONTHLY;

    /** Deterministic period key for this cadence at the given instant (UTC). Never completion-triggered. */
    String periodKeyFor(Instant instant) {
        ZonedDateTime z = instant.atZone(ZoneOffset.UTC);
        return switch (this) {
            case DAILY -> z.toLocalDate().toString();                                   // yyyy-MM-dd
            case WEEKLY -> {
                WeekFields iso = WeekFields.ISO;
                int week = z.get(iso.weekOfWeekBasedYear());
                int year = z.get(iso.weekBasedYear());
                yield String.format(Locale.ROOT, "%04d-W%02d", year, week);              // yyyy-Www (ISO week)
            }
            case MONTHLY -> String.format(Locale.ROOT, "%04d-%02d", z.getYear(), z.getMonthValue());  // yyyy-MM
        };
    }
}
