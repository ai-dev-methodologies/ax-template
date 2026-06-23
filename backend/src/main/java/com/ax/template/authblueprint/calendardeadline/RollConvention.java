package com.ax.template.authblueprint.calendardeadline;

/**
 * business-day-deadline-arithmetic-l0 roll convention (CALDLINE-ROLL-001). FOLLOWING rolls a raw
 * deadline that lands on a non-business day forward to the next business day (the date-rolling
 * "Following" convention; FRCP Rule 6(a)(1)(C)); NONE leaves the adjusted deadline equal to the
 * raw deadline. The applied convention is part of the recorded basis (CALDLINE-BASIS-001).
 */
public enum RollConvention {
    FOLLOWING,
    NONE
}
