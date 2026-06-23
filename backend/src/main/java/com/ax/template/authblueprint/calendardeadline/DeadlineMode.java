package com.ax.template.authblueprint.calendardeadline;

/**
 * business-day-deadline-arithmetic-l0 counting mode (CALDLINE-BUSINESS-001). CALENDAR counts
 * every day (weekends + holidays included); BUSINESS skips Saturdays, Sundays, and the holiday
 * set in force. The chosen mode is part of the recorded basis (CALDLINE-BASIS-001).
 */
public enum DeadlineMode {
    CALENDAR,
    BUSINESS
}
