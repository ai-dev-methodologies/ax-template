package com.ax.template.authblueprint.calendardeadline;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * business-day-deadline-arithmetic-l0 compliance — verified against the live calendardeadline
 * reference workload. The invariant: a statutory deadline computed by CALENDAR-vs-BUSINESS-day
 * arithmetic that RECORDS its full reconstructible basis (start/N/mode/calendar id+version/raw/
 * roll/adjusted), skips weekends + the holiday set in BUSINESS mode, applies + records a roll
 * convention off a non-business day, recomputes 'overdue' on read, and pins a VERSIONED holiday
 * calendar so a later edit cannot silently move an already-computed deadline.
 * Spec: specs/business-day-deadline-arithmetic-l0.yaml (FRCP Rule 6(a) + Following date-roll).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("CALENDARDEADLINE")
class CalendarDeadlineComplianceTest {

    @LocalServerPort int port;
    String member;

    // Mon 2026-06-01 is a deterministic anchor (independent of the wall clock).
    private static final String MON = "2026-06-01";   // Monday
    private static final String WED = "2026-06-03";   // Wednesday (an intervening business day)

    @BeforeEach
    void setup() {
        member = CalendarDeadlineTestSupport.obtainToken(
            CalendarDeadlineTestSupport.freshEmail("cal-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private ExtractableResponse<Response> createCalendar(String name, String holidaysJsonArray) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"calendarName\":\"" + name + "\",\"holidays\":" + holidaysJsonArray + "}")
        .when().post("/api/calendar-deadline/calendars").thenReturn().then().statusCode(201).extract();
    }

    private ExtractableResponse<Response> editCalendar(String id, String holidaysJsonArray) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"holidays\":" + holidaysJsonArray + "}")
        .when().put("/api/calendar-deadline/calendars/" + id).thenReturn().then().extract();
    }

    private ExtractableResponse<Response> compute(String ref, String startDate, int n, String mode,
                                                  String calendarId, String roll) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"obligationRef\":\"" + ref + "\",\"startDate\":\"" + startDate + "\","
                + "\"periodCount\":" + n + ",\"mode\":\"" + mode + "\","
                + "\"holidayCalendarId\":\"" + calendarId + "\",\"rollConvention\":\"" + roll + "\"}")
        .when().post("/api/calendar-deadline/deadlines").thenReturn().then().statusCode(201).extract();
    }

    private ExtractableResponse<Response> getDeadline(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/calendar-deadline/deadlines/" + id).then().statusCode(200).extract();
    }

    private String emptyCalendar() {
        return createCalendar("empty-" + System.nanoTime(), "[]").path("id");
    }

    // ── CALDLINE-BASIS-001 — the computed deadline records its full reconstructible basis ──
    @Test @Tag("CALDLINE-BASIS-001")
    void compute_recordsFullReconstructibleBasis() {
        String calId = emptyCalendar();
        ExtractableResponse<Response> r = compute("FILE-1", MON, 5, "CALENDAR", calId, "FOLLOWING");
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat(r.jsonPath().getString("startDate")).isEqualTo(MON);
        assertThat(r.jsonPath().getInt("periodCount")).isEqualTo(5);
        assertThat(r.jsonPath().getString("mode")).isEqualTo("CALENDAR");
        assertThat(r.jsonPath().getString("holidayCalendarId")).isEqualTo(calId);
        assertThat(r.jsonPath().getLong("holidayCalendarVersion")).isEqualTo(0L);
        assertThat(r.jsonPath().getString("rawDeadline")).as("raw basis recorded").isNotBlank();
        assertThat(r.jsonPath().getString("rollConvention")).isEqualTo("FOLLOWING");
        assertThat(r.jsonPath().getString("adjustedDeadline")).as("adjusted basis recorded").isNotBlank();
    }

    // ── CALDLINE-BUSINESS-001 — BUSINESS skips weekends + holidays; CALENDAR counts every day ──
    @Test @Tag("CALDLINE-BUSINESS-001")
    void businessMode_skipsWeekendsAndHolidays_calendarCountsEveryDay() {
        String calId = emptyCalendar();

        // CALENDAR add 5 from Mon 2026-06-01 → raw Sat 2026-06-06 (every day counted)
        ExtractableResponse<Response> cal = compute("CAL-5", MON, 5, "CALENDAR", calId, "NONE");
        assertThat(cal.jsonPath().getString("rawDeadline")).isEqualTo("2026-06-06");

        // BUSINESS add 5 from Mon 2026-06-01 → Tue2,Wed3,Thu4,Fri5,(skip Sat6/Sun7) Mon8
        ExtractableResponse<Response> biz = compute("BIZ-5", MON, 5, "BUSINESS", calId, "NONE");
        assertThat(biz.jsonPath().getString("rawDeadline")).isEqualTo("2026-06-08");
        // a weekend intervened → BUSINESS raw is strictly later than CALENDAR raw
        assertThat(LocalDate.parse(biz.jsonPath().getString("rawDeadline")))
            .as("intervening weekend pushes the BUSINESS raw later")
            .isAfter(LocalDate.parse(cal.jsonPath().getString("rawDeadline")));

        // add a holiday inside the window (Wed 2026-06-03) → BUSINESS skips it → one further day
        String calWithHoliday = createCalendar("hol-" + System.nanoTime(), "[\"" + WED + "\"]").path("id");
        ExtractableResponse<Response> bizHol = compute("BIZ-HOL", MON, 5, "BUSINESS", calWithHoliday, "NONE");
        // Tue2,(skip Wed3 holiday)Thu4,Fri5,(skip wknd)Mon8,Tue9
        assertThat(bizHol.jsonPath().getString("rawDeadline")).isEqualTo("2026-06-09");
        assertThat(LocalDate.parse(bizHol.jsonPath().getString("rawDeadline")))
            .as("a holiday inside the window pushes the BUSINESS raw one further day")
            .isAfter(LocalDate.parse(biz.jsonPath().getString("rawDeadline")));
    }

    // ── CALDLINE-ROLL-001 — a roll convention is applied + recorded off a non-business day ──
    @Test @Tag("CALDLINE-ROLL-001")
    void roll_followingRollsForwardOffNonBusinessDay_noneLeavesRaw_businessDayIsNoop() {
        String calId = emptyCalendar();

        // raw lands on Sat 2026-06-06; FOLLOWING rolls to next business day Mon 2026-06-08
        ExtractableResponse<Response> following = compute("ROLL-F", MON, 5, "CALENDAR", calId, "FOLLOWING");
        assertThat(following.jsonPath().getString("rawDeadline")).isEqualTo("2026-06-06");
        assertThat(following.jsonPath().getString("adjustedDeadline")).isEqualTo("2026-06-08");

        // same raw with NONE → adjusted == raw
        ExtractableResponse<Response> none = compute("ROLL-N", MON, 5, "CALENDAR", calId, "NONE");
        assertThat(none.jsonPath().getString("adjustedDeadline"))
            .isEqualTo(none.jsonPath().getString("rawDeadline")).isEqualTo("2026-06-06");

        // a raw already on a business day with FOLLOWING → no-op (BUSINESS raw Mon 2026-06-08)
        ExtractableResponse<Response> noop = compute("ROLL-NOOP", MON, 5, "BUSINESS", calId, "FOLLOWING");
        assertThat(noop.jsonPath().getString("rawDeadline")).isEqualTo("2026-06-08");
        assertThat(noop.jsonPath().getString("adjustedDeadline")).isEqualTo("2026-06-08");
    }

    // ── CALDLINE-OVERDUE-001 — overdue is recomputed on read against the as-of instant ──
    @Test @Tag("CALDLINE-OVERDUE-001")
    void overdue_isRecomputedOnRead_notStored() {
        String calId = emptyCalendar();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // a deadline computed deep in the past (start 100 days ago, +0 days) → overdue on read
        ExtractableResponse<Response> past = compute("OD-PAST", today.minusDays(100).toString(), 0,
            "CALENDAR", calId, "NONE");
        assertThat(getDeadline(past.path("id")).jsonPath().getBoolean("overdue"))
            .as("a past adjusted deadline is overdue when recomputed").isTrue();

        // a deadline computed into the future (start today, +60 calendar days) → not overdue
        ExtractableResponse<Response> future = compute("OD-FUT", today.toString(), 60,
            "CALENDAR", calId, "NONE");
        assertThat(getDeadline(future.path("id")).jsonPath().getBoolean("overdue"))
            .as("a future adjusted deadline is not overdue").isFalse();
    }

    // ── CALDLINE-CALVER-001 — keystone: editing the calendar publishes a NEW version and does
    //    NOT move an already-computed deadline ──
    @Test @Tag("CALDLINE-CALVER-001")
    void holidayCalendar_isVersioned_editDoesNotMoveAlreadyComputedDeadline() {
        // v0: empty calendar
        ExtractableResponse<Response> cal = createCalendar("ver-" + System.nanoTime(), "[]");
        assertThat(cal.statusCode()).isEqualTo(201);
        String calId = cal.path("id");
        assertThat(cal.jsonPath().getLong("version")).isEqualTo(0L);

        // compute against v0: BUSINESS +5 from Mon → raw Mon 2026-06-08, adjusted Mon 2026-06-08
        ExtractableResponse<Response> d1 = compute("VER-1", MON, 5, "BUSINESS", calId, "FOLLOWING");
        assertThat(d1.jsonPath().getLong("holidayCalendarVersion")).isEqualTo(0L);
        assertThat(d1.jsonPath().getString("adjustedDeadline")).isEqualTo("2026-06-08");
        String d1Id = d1.path("id");

        // edit the calendar: add Mon 2026-06-08 as a holiday → publishes a NEW version (>= 1)
        ExtractableResponse<Response> edited = editCalendar(calId, "[\"2026-06-08\"]");
        assertThat(edited.statusCode()).isEqualTo(200);
        long v2 = edited.jsonPath().getLong("version");
        assertThat(v2).as("editing the calendar publishes a new version").isGreaterThanOrEqualTo(1L);

        // the ALREADY-COMPUTED deadline is unchanged — still v0, still adjusted Mon 2026-06-08
        ExtractableResponse<Response> d1Again = getDeadline(d1Id);
        assertThat(d1Again.jsonPath().getLong("holidayCalendarVersion"))
            .as("the computed deadline keeps the version it pinned").isEqualTo(0L);
        assertThat(d1Again.jsonPath().getString("adjustedDeadline"))
            .as("a later calendar edit does not move an already-computed deadline").isEqualTo("2026-06-08");

        // a FRESH computation now records the new version and may roll differently:
        // with 2026-06-08 a holiday, BUSINESS +5 → Tue2,Wed3,Thu4,Fri5,(skip wknd, skip Mon8)Tue9
        ExtractableResponse<Response> d2 = compute("VER-2", MON, 5, "BUSINESS", calId, "FOLLOWING");
        assertThat(d2.jsonPath().getLong("holidayCalendarVersion")).isEqualTo(v2);
        assertThat(d2.jsonPath().getString("adjustedDeadline"))
            .as("a fresh computation against the new version rolls past the new holiday").isEqualTo("2026-06-09");
    }
}
