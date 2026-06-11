package com.ax.template.authblueprint.obligation;

/**
 * deadline-obligation-l0 axis kinds (OBL-AXIS-001). CALENDAR = anchor + interval-days (a fixed
 * candidate). USAGE = limit units + used units + a declared units-per-day consumption rate
 * (the candidate re-derives whenever usage advances). The EARLIEST candidate governs.
 */
public enum AxisKind {
    CALENDAR,
    USAGE
}
