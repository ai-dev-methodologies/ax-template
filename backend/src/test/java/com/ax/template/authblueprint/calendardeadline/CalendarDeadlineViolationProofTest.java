package com.ax.template.authblueprint.calendardeadline;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for business-day-deadline-arithmetic-l0. Structural assertions a deliberate break
 * cannot pass silently: the deadline's basis columns are immutable, there is NO stored 'overdue'
 * boolean (it is recomputed), the case carries @Version + the @Check backstops, NO delete path
 * exists anywhere in the domain, mutators are package-sealed, the arithmetic is deterministic
 * (CALENDAR counts every day / BUSINESS skips weekends + holidays / FOLLOWING rolls forward), and
 * the migration carries the same backstops.
 */
@Tag("CALENDARDEADLINE")
class CalendarDeadlineViolationProofTest {

    // ── CALDLINE-BASIS-001 — the full basis is recorded + immutable; no public setter ──
    @Test @Tag("CALDLINE-BASIS-001")
    void violation_basisRecorded_immutable_noPublicSetter() throws Exception {
        for (Method m : CalendarDeadline.class.getMethods()) {
            assertThat(m.getName()).as("CalendarDeadline must have no public setter").doesNotStartWith("set");
        }
        // every basis column is present and immutable
        for (String f : new String[]{"id", "obligationRef", "startDate", "periodCount", "mode",
                "holidayCalendarId", "holidayCalendarVersion", "rawDeadline", "rollConvention",
                "adjustedDeadline", "createdAt"}) {
            Column col = CalendarDeadline.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("CalendarDeadline." + f + " (basis) must be immutable").isFalse();
        }
        assertThat(CalendarDeadline.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── CALDLINE-OVERDUE-001 — overdue is RECOMPUTED, never a stored boolean field ──
    @Test @Tag("CALDLINE-OVERDUE-001")
    void violation_noStoredOverdueBoolean_predicateRecomputed() {
        for (Field f : CalendarDeadline.class.getDeclaredFields()) {
            String lower = f.getName().toLowerCase();
            boolean isStoredFlag = (f.getType() == boolean.class || f.getType() == Boolean.class)
                && (lower.contains("overdue") || lower.contains("islate") || lower.contains("late")
                    || lower.contains("expired"));
            assertThat(isStoredFlag)
                .as("overdue must be recomputed — no stored boolean flag like " + f.getName()).isFalse();
        }
        // the recompute method exists and is derived from the recorded adjusted deadline
        boolean hasRecompute = java.util.Arrays.stream(CalendarDeadline.class.getDeclaredMethods())
            .anyMatch(m -> m.getName().equals("isOverdueAt"));
        assertThat(hasRecompute).as("CalendarDeadline.isOverdueAt must recompute the predicate").isTrue();
    }

    // ── CALDLINE-BUSINESS/ROLL-001 — deterministic calendar/business arithmetic + Following roll ──
    @Test @Tag("CALDLINE-BUSINESS-001") @Tag("CALDLINE-ROLL-001")
    void violation_arithmeticIsDeterministic() {
        Set<LocalDate> none = Set.of();
        LocalDate mon = LocalDate.parse("2026-06-01");   // Monday

        // CALENDAR counts every day: +5 → Sat 2026-06-06
        assertThat(DeadlineArithmetic.rawDeadline(mon, 5, DeadlineMode.CALENDAR, none))
            .isEqualTo(LocalDate.parse("2026-06-06"));
        // BUSINESS skips the weekend: +5 → Mon 2026-06-08
        assertThat(DeadlineArithmetic.rawDeadline(mon, 5, DeadlineMode.BUSINESS, none))
            .isEqualTo(LocalDate.parse("2026-06-08"));
        // a holiday inside the window pushes BUSINESS one further day
        assertThat(DeadlineArithmetic.rawDeadline(mon, 5, DeadlineMode.BUSINESS,
                Set.of(LocalDate.parse("2026-06-03"))))
            .isEqualTo(LocalDate.parse("2026-06-09"));

        // FOLLOWING rolls a Saturday raw forward to the next business day (Mon)
        assertThat(DeadlineArithmetic.roll(LocalDate.parse("2026-06-06"), RollConvention.FOLLOWING, none))
            .isEqualTo(LocalDate.parse("2026-06-08"));
        // NONE leaves the raw unchanged
        assertThat(DeadlineArithmetic.roll(LocalDate.parse("2026-06-06"), RollConvention.NONE, none))
            .isEqualTo(LocalDate.parse("2026-06-06"));
        // FOLLOWING on a business day is a no-op
        assertThat(DeadlineArithmetic.roll(LocalDate.parse("2026-06-08"), RollConvention.FOLLOWING, none))
            .isEqualTo(LocalDate.parse("2026-06-08"));
    }

    // ── CALDLINE-CALVER-001 — the holiday calendar versions explicitly; mutators sealed; @Version ──
    @Test @Tag("CALDLINE-CALVER-001")
    void violation_calendarVersionsExplicitly_mutatorsSealed() throws Exception {
        for (Method m : HolidayCalendar.class.getMethods()) {
            assertThat(m.getName()).as("HolidayCalendar must have no public setter").doesNotStartWith("set");
        }
        // the sole publish hook is package-private
        Method republish = java.util.Arrays.stream(HolidayCalendar.class.getDeclaredMethods())
            .filter(x -> x.getName().equals("republishWith")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(republish.getModifiers()))
            .as("HolidayCalendar.republishWith must be package-private").isFalse();
        // the published version + the JPA optimistic-lock @Version both exist and are distinct
        assertThat(HolidayCalendar.class.getDeclaredField("publishedVersion").getType()).isEqualTo(long.class);
        assertThat(HolidayCalendar.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
        Column immutableName = HolidayCalendar.class.getDeclaredField("calendarName").getAnnotation(Column.class);
        assertThat(immutableName.updatable()).as("calendarName must be immutable").isFalse();
    }

    // ── NO delete path; the @Check backstops on the deadline row ──
    @Test @Tag("CALDLINE-BASIS-001") @Tag("CALDLINE-CALVER-001")
    void violation_noDeletePath_checkBackstops() throws Exception {
        for (Class<?> repo : new Class<?>[]{CalendarDeadlineRepository.class, HolidayCalendarRepository.class}) {
            for (Method m : repo.getDeclaredMethods()) {
                assertThat(m.getName()).as(repo.getSimpleName() + " declares no delete").doesNotContain("delete");
            }
        }
        for (String src : new String[]{"CalendarDeadlineService", "CalendarDeadlineController"}) {
            String text = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(
                System.getProperty("user.dir"), "src", "main", "java", "com", "ax", "template",
                "authblueprint", "calendardeadline", src + ".java")), StandardCharsets.UTF_8);
            assertThat(text).as(src + " must contain no delete call — deadlines/calendars are kept, never removed")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
        Check check = CalendarDeadline.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("period_count >= 0 AND holiday_calendar_version >= 0");
        assertThat(c).contains("roll_convention <> 'NONE' OR adjusted_deadline = raw_deadline");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("CALDLINE-BASIS-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V065__create_calendardeadline.sql")) {
            assertThat(in).as("V065__create_calendardeadline.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("period_count >= 0 AND holiday_calendar_version >= 0");
            assertThat(sql).contains("roll_convention <> 'NONE' OR adjusted_deadline = raw_deadline");
            assertThat(sql).contains("holiday_calendar_dates");
            assertThat(sql).contains("published_version");
            // NO stored overdue/is_late column in the migration
            assertThat(sql).doesNotContain("is_late").doesNotContain("overdue BOOLEAN");
        }
    }
}
