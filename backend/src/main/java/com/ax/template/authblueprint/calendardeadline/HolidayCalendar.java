package com.ax.template.authblueprint.calendardeadline;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * business-day-deadline-arithmetic-l0 root: a VERSIONED holiday calendar (CALDLINE-CALVER-001).
 * The holiday set is an {@link ElementCollection} of {@link LocalDate} owned by this root (no
 * member repository). Editing the set publishes a NEW DOMAIN version — {@code publishedVersion}
 * is incremented EXPLICITLY by {@link #republishWith} (it is the domain-meaningful, reproducible
 * version a deadline pins, deliberately distinct from the JPA {@code @Version} optimistic-lock
 * counter so it does not depend on Hibernate collection dirty-checking). A published version is
 * never mutated in place: a {@link CalendarDeadline} computed against version K keeps K even after
 * the calendar is later edited. A CalendarDeadline references the calendar by identity (calendar id
 * + the long version it saw), never an object pointer (HG-AGG-REF).
 */
@AggregateRoot
@Entity
@Table(name = "holiday_calendars")
@Check(constraints = "published_version >= 0")
public class HolidayCalendar {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "calendar_name", nullable = false, updatable = false, length = 100)
    private String calendarName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "holiday_calendar_dates",
        joinColumns = @JoinColumn(name = "calendar_id"))
    @Column(name = "holiday_date", nullable = false)
    private Set<LocalDate> holidays;

    /** Monotonic DOMAIN publish version — incremented on every edit; the recorded input a deadline pins. */
    @Column(name = "published_version", nullable = false)
    private long publishedVersion;

    /** JPA optimistic-lock counter — concurrency control, NOT the domain publish version. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected HolidayCalendar() {}

    public HolidayCalendar(UUID id, String calendarName, Set<LocalDate> holidays, Instant createdAt) {
        this.id = id;
        this.calendarName = calendarName;
        this.holidays = new LinkedHashSet<>(holidays);
        this.publishedVersion = 0L;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook (CALDLINE-CALVER-001) — replace the holiday set and publish a NEW version
     *  by incrementing {@code publishedVersion}. A deadline already computed keeps the version it
     *  pinned — this never re-rolls it. */
    void republishWith(Set<LocalDate> nextHolidays) {
        this.holidays = new LinkedHashSet<>(nextHolidays);
        this.publishedVersion = this.publishedVersion + 1L;
    }

    public UUID getId() { return id; }
    public String getCalendarName() { return calendarName; }
    /** Defensive copy — the live set is never exposed (the entity is its own sole mutator). */
    public Set<LocalDate> getHolidays() { return Set.copyOf(holidays); }
    /** The DOMAIN publish version a deadline pins (CALDLINE-CALVER-001). */
    public long getPublishedVersion() { return publishedVersion; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
