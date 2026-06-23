package com.ax.template.authblueprint.calendardeadline;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * business-day-deadline-arithmetic-l0 root: one statutory/regulatory deadline computed by
 * calendar-vs-business-day arithmetic. The row records the FULL reconstructible basis
 * (CALDLINE-BASIS-001): the start date, the period count N, the {@link DeadlineMode}, the holiday
 * calendar's id AND the version in force at compute time (CALDLINE-CALVER-001), the raw computed
 * date, the applied {@link RollConvention}, and the rolled/adjusted deadline. ALL basis columns
 * are immutable ({@code updatable=false}) — a recompute against a different calendar or N produces
 * a NEW deadline. There is NO stored 'overdue' boolean: {@link #isOverdueAt} RECOMPUTES the
 * predicate from the recorded adjusted deadline and the as-of instant (CALDLINE-OVERDUE-001).
 */
@AggregateRoot
@Entity
@Table(name = "calendar_deadlines")
@Check(constraints =
    "period_count >= 0 AND holiday_calendar_version >= 0"
    + " AND (roll_convention <> 'NONE' OR adjusted_deadline = raw_deadline)")
public class CalendarDeadline {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The obligation's external reference — opaque, recorded verbatim. */
    @Column(name = "obligation_ref", nullable = false, updatable = false, length = 200)
    private String obligationRef;

    @Column(name = "start_date", nullable = false, updatable = false)
    private LocalDate startDate;

    @Column(name = "period_count", nullable = false, updatable = false)
    private int periodCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, updatable = false, length = 20)
    private DeadlineMode mode;

    @Column(name = "holiday_calendar_id", nullable = false, updatable = false)
    private UUID holidayCalendarId;

    /** The holiday-calendar version pinned at compute time — a later calendar edit cannot move this. */
    @Column(name = "holiday_calendar_version", nullable = false, updatable = false)
    private long holidayCalendarVersion;

    @Column(name = "raw_deadline", nullable = false, updatable = false)
    private LocalDate rawDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "roll_convention", nullable = false, updatable = false, length = 20)
    private RollConvention rollConvention;

    @Column(name = "adjusted_deadline", nullable = false, updatable = false)
    private LocalDate adjustedDeadline;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CalendarDeadline() {}

    public CalendarDeadline(UUID id, String obligationRef, LocalDate startDate, int periodCount,
                            DeadlineMode mode, UUID holidayCalendarId, long holidayCalendarVersion,
                            LocalDate rawDeadline, RollConvention rollConvention,
                            LocalDate adjustedDeadline, Instant createdAt) {
        this.id = id;
        this.obligationRef = obligationRef;
        this.startDate = startDate;
        this.periodCount = periodCount;
        this.mode = mode;
        this.holidayCalendarId = holidayCalendarId;
        this.holidayCalendarVersion = holidayCalendarVersion;
        this.rawDeadline = rawDeadline;
        this.rollConvention = rollConvention;
        this.adjustedDeadline = adjustedDeadline;
        this.createdAt = createdAt;
    }

    /**
     * CALDLINE-OVERDUE-001 — overdue is RECOMPUTED, never stored: true iff the as-of date (UTC) is
     * strictly after the recorded adjusted deadline. A deadline whose adjusted date is today or in
     * the future is not overdue.
     */
    public boolean isOverdueAt(Instant asOf) {
        LocalDate asOfDate = asOf.atZone(ZoneOffset.UTC).toLocalDate();
        return asOfDate.isAfter(adjustedDeadline);
    }

    public UUID getId() { return id; }
    public String getObligationRef() { return obligationRef; }
    public LocalDate getStartDate() { return startDate; }
    public int getPeriodCount() { return periodCount; }
    public DeadlineMode getMode() { return mode; }
    public UUID getHolidayCalendarId() { return holidayCalendarId; }
    public long getHolidayCalendarVersion() { return holidayCalendarVersion; }
    public LocalDate getRawDeadline() { return rawDeadline; }
    public RollConvention getRollConvention() { return rollConvention; }
    public LocalDate getAdjustedDeadline() { return adjustedDeadline; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
