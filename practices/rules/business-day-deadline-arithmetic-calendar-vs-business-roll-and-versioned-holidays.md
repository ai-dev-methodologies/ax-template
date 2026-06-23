---
title: A statutory/regulatory deadline computed by CALENDAR-vs-BUSINESS-day arithmetic must RECORD its full reconstructible basis (start date, N, mode, holiday-calendar id + version, raw date, roll convention, adjusted date — never a bare stored date), skip weekends + the configured holiday set in BUSINESS mode (CALENDAR counts every day), apply and RECORD a roll convention off a non-business day, RECOMPUTE 'overdue' on read (never a stored boolean), and pin the holiday calendar as a VERSIONED input so a later edit does not silently move an already-computed deadline
impact: HIGH
impactDescription: "A deadline stored as a bare date with no recorded inputs cannot be reconstructed or audited when a filing is challenged (the FRCP time-computation rule presumes the triggering day, the intervening days, and the holiday set are all known); business-day arithmetic that does not skip weekends/holidays or a roll convention that is not recorded produces a wrong statutory deadline a court or regulator rejects; a stored 'overdue' boolean drifts out of band; and a holiday calendar edited in place silently moves every past computed deadline, breaking reproducibility of an already-served filing date"
tags:
  - state-machine
  - audit
  - governance
  - billing
spec_ref: "specs/business-day-deadline-arithmetic-l0.yaml#CALDLINE-BASIS-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/calendardeadline/DeadlineArithmetic.java + backend/src/main/java/com/ax/template/authblueprint/calendardeadline/CalendarDeadlineService.java + backend/src/main/java/com/ax/template/authblueprint/calendardeadline/CalendarDeadline.java + backend/src/main/java/com/ax/template/authblueprint/calendardeadline/HolidayCalendar.java"
  pattern: "DeadlineArithmetic.rawDeadline adds N in CALENDAR mode (every day) or BUSINESS mode (skipping Saturdays/Sundays + the holiday set, the day-0 start excluded per FRCP Rule 6(a)(1)(A)); DeadlineArithmetic.roll applies FOLLOWING (forward to the next business day) or NONE; CalendarDeadlineService.compute reads the HolidayCalendar's holiday set + its published version, computes raw + adjusted, and persists a CalendarDeadline recording the full basis (start/periodCount/mode/holidayCalendarId/holidayCalendarVersion/raw/rollConvention/adjusted) with every basis column @Column(updatable=false); CalendarDeadline.isOverdueAt RECOMPUTES overdue from the recorded adjusted deadline and the as-of instant (no stored boolean); HolidayCalendar.republishWith increments publishedVersion (a deadline keeps the version it pinned, so an edit never moves it); NO delete path exists on either root"
upstream:
  - "https://www.law.cornell.edu/rules/frcp/rule_6"
  - "https://en.wikipedia.org/wiki/Date_rolling"
evidence:
  - source_type: external
    citation: "Federal Rules of Civil Procedure, Rule 6(a)(1)(A)-(C) (Cornell LII) — the legal time-computation rule the calendar/business-day arithmetic generalizes: exclude the triggering day, count every intervening day including weekends and holidays, and roll the last day forward off a Saturday, Sunday, or legal holiday"
    url: "https://www.law.cornell.edu/rules/frcp/rule_6"
    quote: "(A) exclude the day of the event that triggers the period; (B) count every day, including intermediate Saturdays, Sundays, and legal holidays; and (C) include the last day of the period, but if the last day is a Saturday, Sunday, or legal holiday, the period continues to run until the end of the next day that is not a Saturday, Sunday, or legal holiday."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "Date rolling — Wikipedia (the 'Following' business-day date-rolling convention the FOLLOWING roll generalizes): a non-business-day settlement date is rolled to the next business day"
    url: "https://en.wikipedia.org/wiki/Date_rolling"
    quote: "the payment date is rolled to the next business day."
    quoted_at: "2026-06-23"
---

## A computed statutory deadline is reconstructible-by-basis, business-day-aware, roll-recorded, recompute-overdue, and pinned to a versioned holiday calendar — not a bare stored date

**Impact: HIGH — a bare stored deadline cannot be reconstructed or audited; business-day arithmetic that ignores weekends/holidays or an unrecorded roll produces a wrong statutory date; a stored 'overdue' boolean drifts; an in-place holiday-calendar edit silently moves every past computed deadline.**

A *statutory/regulatory deadline* (a court filing deadline, a tax-filing window, a statutory cure or notice period) is computed by adding N units to a start date where the unit is EITHER calendar-days OR business-days. The discipline is legal: FRCP Rule 6(a) requires you to *"exclude the day of the event that triggers the period … count every day, including intermediate Saturdays, Sundays, and legal holidays … include the last day of the period, but if the last day is a Saturday, Sunday, or legal holiday, the period continues to run until the … next day that is not a Saturday, Sunday, or legal holiday."* The catalog modelled grounded multi-axis obligation deadlines (`deadline-obligation`) and irreversible threshold terminals (`threshold-terminal`) but had no primitive for the calendar-vs-business-day arithmetic a regulated deadline is actually computed by:

```text
rawDeadline(start, N, mode, holidays):  CALENDAR → start + N days (every day counted);
                                        BUSINESS → advance N days skipping Sat/Sun + holidays
                                        (day-0 start excluded; FRCP 6(a)(1)(A))
roll(raw, convention, holidays):        FOLLOWING → next business day off a non-business day;
                                        NONE → raw unchanged
compute:                                persist start/N/mode/calendar id + pinned version/raw/
                                        roll/adjusted — the FULL reconstructible basis
overdue:                                RECOMPUTED: nowUTC > adjustedDeadline — never stored
calendar:                               VERSIONED — republish increments publishedVersion; a
                                        deadline keeps the version it pinned (no silent move)
```

**1. The deadline records its full reconstructible basis (CALDLINE-BASIS-001).** Every input — start date, N, mode, holiday-calendar id, the pinned version, the raw date, the roll convention, and the adjusted date — is persisted with `@Column(updatable=false)`, so the date is reconstructible and a recompute is a NEW row, never an overwrite.

**2. Business-day arithmetic skips weekends + holidays; CALENDAR counts every day (CALDLINE-BUSINESS-001).** The two modes diverge whenever a weekend or holiday intervenes.

**3. A roll convention is applied and recorded off a non-business day (CALDLINE-ROLL-001).** FOLLOWING rolls forward to the next business day; both raw and adjusted are recorded.

**4. Overdue is recomputed, the calendar is versioned (CALDLINE-OVERDUE/CALVER-001).** Overdue is re-derived on read from the recorded adjusted deadline and the injected Clock; the holiday calendar is a versioned input so a later edit publishes a new version and cannot move an already-computed deadline.

**Incorrect — a bare stored date, naive calendar add, a stored overdue flag, an in-place calendar edit:**

```java
public LocalDate deadline(LocalDate start, int n) {
    LocalDate d = start.plusDays(n);                 // ❌ no business-day skip, no holiday set, no roll
    repo.save(new Deadline(d, d.isBefore(today)));    // ❌ bare date + stored 'overdue' boolean (drifts)
    calendar.getHolidays().add(newHoliday);           // ❌ in-place edit silently moves every past deadline
    return d;                                         // ❌ no recorded basis — unreconstructible
}
```

**Correct — calendar/business arithmetic with a recorded roll, a recomputed overdue, a pinned versioned calendar:**

```java
@Transactional
public CalendarDeadline compute(String obligationRef, LocalDate startDate, int periodCount,
                                DeadlineMode mode, UUID calendarId, RollConvention rollConvention) {
    if (periodCount < 0) throw CalendarDeadlineException.invalidPeriod();
    HolidayCalendar calendar = calendars.findById(calendarId)
        .orElseThrow(CalendarDeadlineException::calendarNotFound);
    Set<LocalDate> holidays = calendar.getHolidays();
    long pinnedVersion = calendar.getPublishedVersion();        // ✅ the version in force, pinned

    LocalDate raw = DeadlineArithmetic.rawDeadline(startDate, periodCount, mode, holidays);  // ✅ skips wknd/holiday in BUSINESS
    LocalDate adjusted = DeadlineArithmetic.roll(raw, rollConvention, holidays);             // ✅ FOLLOWING/NONE, recorded

    CalendarDeadline d = new CalendarDeadline(UUID.randomUUID(), obligationRef, startDate,
        periodCount, mode, calendarId, pinnedVersion, raw, rollConvention, adjusted,
        Instant.now(clock));                                    // ✅ FULL reconstructible basis, immutable
    return deadlines.save(d);
}

// overdue is RECOMPUTED, never stored:
public boolean isOverdueAt(Instant asOf) {
    return asOf.atZone(ZoneOffset.UTC).toLocalDate().isAfter(adjustedDeadline);   // ✅ re-derived from the basis
}

// the calendar versions explicitly — an edit cannot move an already-computed deadline:
void republishWith(Set<LocalDate> nextHolidays) {
    this.holidays = new LinkedHashSet<>(nextHolidays);
    this.publishedVersion = this.publishedVersion + 1L;          // ✅ NEW version; the old deadline keeps its pin
}
```

`DeadlineArithmetic` is a pure `LocalDate` function (no clock, no persistence) so the computation is reproducible from the recorded basis. `CalendarDeadline`'s basis columns are immutable; `isOverdueAt` re-derives the predicate; `HolidayCalendar.republishWith` increments `publishedVersion` so a deadline computed against version K keeps K. No delete path exists on either root.

Verification: review-tier — confirm the arithmetic skips weekends + the holiday set in BUSINESS mode and counts every day in CALENDAR mode, the roll convention is applied + recorded, the full basis columns are `@Column(updatable=false)`, overdue is recomputed (no stored boolean), and the holiday calendar publishes a new version on edit so a past computed deadline is never moved. The behavioural proof a fork-receiver keeps green: a deadline computed against calendar v0 is unchanged after the calendar is edited to v1.

Reference: [FRCP Rule 6(a) — Computing Time](https://www.law.cornell.edu/rules/frcp/rule_6)

Reference: [Date rolling — the Following business-day convention](https://en.wikipedia.org/wiki/Date_rolling)
