package com.ax.template.authblueprint.geofence;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * geofence-transition-l0 compliance — verified against the live geofence reference workload. The
 * invariant: a raw observation is CONFIRMED only after sustaining, unreversed, for the minimum
 * dwell duration; rapid flapping within the dwell window commits ZERO transitions; a confirmed
 * transition is immutable and dual-timestamped. Spec: specs/geofence-transition-l0.yaml.
 *
 * <p>EVENT-TIME driven: every observedAt is an explicit fixed Instant passed by the test — no
 * wall-clock dependency anywhere in the assertions (fully deterministic, replayable).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("GEOFENCE")
class GeofenceComplianceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final long DWELL = 60L; // must match GeofenceTrackerService.DEFAULT_DWELL_SECONDS

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        GeofenceTestSupport.useRandomPort(port);
        member = GeofenceTestSupport.obtainToken(GeofenceTestSupport.freshEmail("geofence-member"), "MEMBER");
    }

    private String registerTracker(String subjectId, String zoneId) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"subjectId\":\"" + subjectId + "\",\"zoneId\":\"" + zoneId + "\"}")
        .when().post("/api/geofence/trackers").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> observe(String trackerId, String rawState, Instant observedAt) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"rawState\":\"" + rawState + "\",\"observedAt\":\"" + observedAt + "\"}")
        .when().post("/api/geofence/trackers/" + trackerId + "/observations").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getTracker(String trackerId) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/geofence/trackers/" + trackerId).then().statusCode(200).extract();
    }

    private ExtractableResponse<Response> getTransitions(String trackerId) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/geofence/trackers/" + trackerId + "/transitions").then().statusCode(200).extract();
    }

    // ── GEOFENCE-DWELL-001 — sustained ENTER, unreversed, confirms once the dwell elapses ──
    @Test @Tag("GEOFENCE-DWELL-001")
    void sustainedEnter_confirmsOnlyAfterDwellElapses_noReversal() {
        String trackerId = registerTracker(subject("DWELL"), "zone-a");
        assertThat(getTracker(trackerId).jsonPath().getString("confirmedState")).isEqualTo("OUTSIDE");

        observe(trackerId, "INSIDE", T0);                                      // first raw signal — pending starts
        assertThat(getTracker(trackerId).jsonPath().getString("confirmedState")).isEqualTo("OUTSIDE");
        assertThat(getTransitions(trackerId).jsonPath().getList("id")).isEmpty();

        observe(trackerId, "INSIDE", T0.plusSeconds(30));                      // still within dwell — no-op
        assertThat(getTracker(trackerId).jsonPath().getString("confirmedState")).isEqualTo("OUTSIDE");
        assertThat(getTransitions(trackerId).jsonPath().getList("id")).isEmpty();

        Instant confirmAt = T0.plusSeconds(DWELL + 1);
        observe(trackerId, "INSIDE", confirmAt);                               // dwell elapsed — CONFIRMED
        assertThat(getTracker(trackerId).jsonPath().getString("confirmedState")).isEqualTo("INSIDE");

        var transitions = getTransitions(trackerId).jsonPath();
        assertThat(transitions.getList("id")).hasSize(1);
        assertThat(transitions.getString("[0].direction")).isEqualTo("ENTER");
        assertThat(Instant.parse(transitions.getString("[0].observedAt"))).isEqualTo(T0);
        assertThat(Instant.parse(transitions.getString("[0].confirmedAt"))).isEqualTo(confirmAt);
    }

    // ── GEOFENCE-FLAP-SUPPRESS-001 — rapid flapping within the dwell window commits ZERO transitions ──
    @Test @Tag("GEOFENCE-FLAP-SUPPRESS-001")
    void rapidFlapping_withinDwellWindow_commitsZeroTransitions() {
        String trackerId = registerTracker(subject("FLAP"), "zone-a");

        observe(trackerId, "INSIDE", T0);                          // pending ENTER starts
        observe(trackerId, "OUTSIDE", T0.plusSeconds(30));          // matches confirmed OUTSIDE — pending cancelled
        observe(trackerId, "INSIDE", T0.plusSeconds(31));           // fresh pending ENTER (own new window)
        observe(trackerId, "OUTSIDE", T0.plusSeconds(59));          // cancelled again — never sustained 60s unreversed

        assertThat(getTracker(trackerId).jsonPath().getString("confirmedState"))
            .as("the tracker never left its starting state").isEqualTo("OUTSIDE");
        assertThat(getTransitions(trackerId).jsonPath().getList("id"))
            .as("flapping commits zero transitions").isEmpty();
    }

    // ── GEOFENCE-FLAP-SUPPRESS-001 — a cancelled pending window MUST NOT leak into a later one.
    //    The prior flap test alone cannot distinguish a true cancel from a no-op that merely
    //    forgets to clear: in that test's own sequence, the dwell threshold is never approached
    //    either way, so a mutant that deletes GeofenceTracker#clearPending() still passes it. This
    //    case forces the distinction — a LATER same-direction observation lands PAST the dwell
    //    threshold measured from the FIRST (should-be-cancelled) pendingSince, so a surviving
    //    stale pendingSince would wrongly confirm here. ──
    @Test @Tag("GEOFENCE-FLAP-SUPPRESS-001")
    void cancelledPending_doesNotLeakIntoALaterObservation_dwellRestartsFromScratch() {
        String trackerId = registerTracker(subject("CANCELRESET"), "zone-a");

        observe(trackerId, "INSIDE", T0);                          // pending ENTER starts at t=0
        observe(trackerId, "OUTSIDE", T0.plusSeconds(30));         // matches confirmed OUTSIDE — pending MUST cancel
        // T0+61 is < 60s past THIS observation's own start, but 61s past the t=0 pendingSince a
        // clearPending() bug would leave behind — the exact gap the prior flap test cannot see.
        observe(trackerId, "INSIDE", T0.plusSeconds(61));

        assertThat(getTracker(trackerId).jsonPath().getString("confirmedState"))
            .as("the dwell has NOT elapsed on the fresh window — must still be OUTSIDE")
            .isEqualTo("OUTSIDE");
        assertThat(getTransitions(trackerId).jsonPath().getList("id"))
            .as("no transition committed — a clearPending() regression would wrongly confirm here")
            .isEmpty();
    }

    // ── GEOFENCE-CONFIRM-001 — the committed record is immutable and dual-timestamped ──
    @Test @Tag("GEOFENCE-CONFIRM-001")
    void confirmedTransition_isDualTimestamped_confirmedNotBeforeObserved() {
        String trackerId = registerTracker(subject("CONFIRM"), "zone-b");
        observe(trackerId, "INSIDE", T0);
        Instant confirmAt = T0.plusSeconds(DWELL + 5);
        observe(trackerId, "INSIDE", confirmAt);

        var t = getTransitions(trackerId).jsonPath();
        assertThat(t.getList("id")).hasSize(1);
        assertThat(t.getString("[0].zoneId")).isEqualTo("zone-b");
        Instant observedAt = Instant.parse(t.getString("[0].observedAt"));
        Instant confirmedAt = Instant.parse(t.getString("[0].confirmedAt"));
        assertThat(confirmedAt).as("confirmed-at is not before observed-at").isAfterOrEqualTo(observedAt);
        assertThat(observedAt).isEqualTo(T0);
        assertThat(confirmedAt).isEqualTo(confirmAt);
    }

    private static String subject(String tag) {
        return "SUBJ-" + tag + "-" + UUID.randomUUID();
    }
}
