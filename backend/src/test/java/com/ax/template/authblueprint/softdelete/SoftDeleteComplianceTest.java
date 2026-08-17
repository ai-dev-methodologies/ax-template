package com.ax.template.authblueprint.softdelete;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * soft-delete-l0 compliance — every item verified against the live reference workload. Domain
 * @Tag("SOFT_DELETE") drives ./gradlew testSoftDelete; the per-item @Tag binds the spec item to its
 * test (spec_item_verification_binding guard). Spec: specs/soft-delete-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// BEFORE_CLASS (was AFTER_CLASS): in the batched R25 per-domain run (146 @SpringBootTest contexts vs
// the default 32-entry ContextCache) this class's own context could be LRU-evicted mid-run, leaving
// @LocalServerPort pointing at a dead Tomcat → spurious failures. BEFORE_CLASS forces a fresh live
// context at class start, guaranteeing a live server for the whole class.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class SoftDeleteComplianceTest {

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry registry;

    @Autowired
    SoftDeleteService service;

    String token;

    @BeforeEach
    void setUp() {
        token = tokenFor("sd", "MEMBER");
    }

    @AfterEach
    void resetClock() {
        service.setClock(Clock.systemUTC()); // undo any window-test clock offset (shared singleton)
    }

    private String tokenFor(String prefix, String role) {
        String email = prefix + "-" + UUID.randomUUID() + "@example.com";
        given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"" + role + "\"}")
            .when().post("/api/auth/email/signup");
        return given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
            .when().post("/api/auth/email/login").then().extract().path("accessToken");
    }

    private io.restassured.specification.RequestSpecification as(String tok) {
        return given().header("Authorization", "Bearer " + tok).header("Content-Type", "application/json");
    }

    private String createAccount(String tok, String email) {
        return as(tok).body("{\"email\":\"" + email + "\",\"name\":\"n\"}")
            .post("/api/soft-delete/accounts").then().statusCode(201).extract().jsonPath().getString("id");
    }

    private static String freshEmail() {
        return "u-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    @Tag("SOFT_DELETE")
    @Tag("SOFTDELETE-MARK-001")
    void deleteTombstonesAndSecondDeleteIs404() {
        String id = createAccount(token, freshEmail());
        as(token).delete("/api/soft-delete/accounts/" + id).then().statusCode(204);   // first delete → tombstone
        as(token).get("/api/soft-delete/accounts/" + id).then().statusCode(404);       // tombstoned → invisible
        as(token).delete("/api/soft-delete/accounts/" + id).then().statusCode(404);    // idempotent observable state
    }

    @Test
    @Tag("SOFT_DELETE")
    @Tag("SOFTDELETE-QUERY-001")
    void defaultQueriesExcludeTombstonedAndAdminMayOptIn() {
        String adminToken = tokenFor("sd-admin", "ADMIN");
        String id = createAccount(adminToken, freshEmail());
        as(adminToken).delete("/api/soft-delete/accounts/" + id).then().statusCode(204);

        // default list excludes the tombstoned row
        List<String> live = as(adminToken).get("/api/soft-delete/accounts").then().statusCode(200)
            .extract().jsonPath().getList("id", String.class);
        assertThat(live).doesNotContain(id);
        // ROLE_ADMIN may opt back in with ?include_deleted=true
        List<String> withDeleted = as(adminToken).get("/api/soft-delete/accounts?include_deleted=true")
            .then().statusCode(200).extract().jsonPath().getList("id", String.class);
        assertThat(withDeleted).contains(id);

        // a NON-admin's include_deleted flag is silently ignored (filtered as if absent), never 500
        String memberId = createAccount(token, freshEmail());
        as(token).delete("/api/soft-delete/accounts/" + memberId).then().statusCode(204);
        List<String> memberWithFlag = as(token).get("/api/soft-delete/accounts?include_deleted=true")
            .then().statusCode(200).extract().jsonPath().getList("id", String.class);
        assertThat(memberWithFlag).doesNotContain(memberId);
    }

    @Test
    @Tag("SOFT_DELETE")
    @Tag("SOFTDELETE-RESTORE-001")
    void restoreWithinWindowSucceedsAndGuardsNotDeletedExpiredPurged() {
        String id = createAccount(token, freshEmail());
        // restore of a LIVE (non-tombstoned) row → 409 NOT_DELETED
        as(token).post("/api/soft-delete/accounts/" + id + "/restore").then().statusCode(409)
            .body("code", org.hamcrest.Matchers.equalTo("SOFT_DELETE_NOT_DELETED"));
        // delete then restore within the window → live again
        as(token).delete("/api/soft-delete/accounts/" + id).then().statusCode(204);
        as(token).post("/api/soft-delete/accounts/" + id + "/restore").then().statusCode(200)
            .body("deleted", org.hamcrest.Matchers.equalTo(false));
        as(token).get("/api/soft-delete/accounts/" + id).then().statusCode(200);

        // window-expired: delete, then advance the clock past the 30-day recovery window → 409
        as(token).delete("/api/soft-delete/accounts/" + id).then().statusCode(204);
        service.setClock(Clock.offset(Clock.systemUTC(), Duration.ofDays(31)));
        as(token).post("/api/soft-delete/accounts/" + id + "/restore").then().statusCode(409)
            .body("code", org.hamcrest.Matchers.equalTo("SOFT_DELETE_WINDOW_EXPIRED"));
        service.setClock(Clock.systemUTC());

        // restore of a PURGED (physically gone) row → 404
        String purgedId = createAccount(token, freshEmail());
        as(token).delete("/api/soft-delete/accounts/" + purgedId).then().statusCode(204);
        as(token).delete("/api/soft-delete/accounts/" + purgedId + "/erase").then().statusCode(204);
        as(token).post("/api/soft-delete/accounts/" + purgedId + "/restore").then().statusCode(404);
    }

    @Test
    @Tag("SOFT_DELETE")
    @Tag("SOFTDELETE-CASCADE-001")
    void deleteAndRestoreCascadeToChildren() {
        String id = createAccount(token, freshEmail());
        as(token).body("{\"text\":\"a\"}").post("/api/soft-delete/accounts/" + id + "/notes").then().statusCode(201);
        as(token).body("{\"text\":\"b\"}").post("/api/soft-delete/accounts/" + id + "/notes").then().statusCode(201);
        assertThat(liveNoteCount(id)).isEqualTo(2);
        // IDOR guard (dogfood F): a DIFFERENT owner cannot read this account's notes by id → 404
        String otherToken = tokenFor("sd-other", "MEMBER");
        as(otherToken).get("/api/soft-delete/accounts/" + id + "/notes").then().statusCode(404);

        // delete parent → children consistently tombstoned (no partial cascade)
        as(token).delete("/api/soft-delete/accounts/" + id).then().statusCode(204);
        assertThat(liveNoteCount(id)).isEqualTo(0);

        // restore parent → children tombstoned at-or-after the parent are restored symmetrically
        as(token).post("/api/soft-delete/accounts/" + id + "/restore").then().statusCode(200);
        assertThat(liveNoteCount(id)).isEqualTo(2);
    }

    @Test
    @Tag("SOFT_DELETE")
    @Tag("SOFTDELETE-PURGE-001")
    void erasureAndRetentionPurgeArePhysicalAndIdempotent() {
        // erasure: immediate physical delete, idempotent (repeat → 404), unrecoverable
        String id = createAccount(token, freshEmail());
        as(token).delete("/api/soft-delete/accounts/" + id).then().statusCode(204);
        as(token).delete("/api/soft-delete/accounts/" + id + "/erase").then().statusCode(204);
        as(token).delete("/api/soft-delete/accounts/" + id + "/erase").then().statusCode(404); // idempotent
        as(token).post("/api/soft-delete/accounts/" + id + "/restore").then().statusCode(404);  // physically gone

        // retention: a tombstoned row older than the cutoff is physically purged by the sweep
        String retained = createAccount(token, freshEmail());
        as(token).delete("/api/soft-delete/accounts/" + retained).then().statusCode(204);
        int purged = service.purgeExpired(java.time.Instant.now().plus(Duration.ofDays(1)));
        assertThat(purged).isGreaterThanOrEqualTo(1);
        as(token).post("/api/soft-delete/accounts/" + retained + "/restore").then().statusCode(404);
    }

    @Test
    @Tag("SOFT_DELETE")
    @Tag("SOFTDELETE-UNIQUE-001")
    void uniqueAmongLiveRowsOnlyAndTombstonedValueIsFreed() {
        String email = freshEmail();
        String first = createAccount(token, email);
        // a second LIVE row with the same natural key → 409
        as(token).body("{\"email\":\"" + email + "\",\"name\":\"dup\"}")
            .post("/api/soft-delete/accounts").then().statusCode(409)
            .body("code", org.hamcrest.Matchers.equalTo("SOFT_DELETE_UNIQUE_CONFLICT"));
        // tombstone the first → the value is freed: re-creating the same email now succeeds
        as(token).delete("/api/soft-delete/accounts/" + first).then().statusCode(204);
        as(token).body("{\"email\":\"" + email + "\",\"name\":\"reuse\"}")
            .post("/api/soft-delete/accounts").then().statusCode(201);
    }

    @Test
    @Tag("SOFT_DELETE")
    @Tag("SOFTDELETE-OBSERVABILITY-001")
    void exposesBoundedLabelMeters() {
        String id = createAccount(token, freshEmail());
        as(token).delete("/api/soft-delete/accounts/" + id).then().statusCode(204);            // soft_delete_total
        as(token).post("/api/soft-delete/accounts/" + id + "/restore").then().statusCode(200); // restore restored
        String live = createAccount(token, freshEmail());
        as(token).post("/api/soft-delete/accounts/" + live + "/restore").then().statusCode(409); // restore not_deleted
        as(token).delete("/api/soft-delete/accounts/" + live).then().statusCode(204);
        as(token).delete("/api/soft-delete/accounts/" + live + "/erase").then().statusCode(204); // purge erasure_request

        assertThat(registry.find(SoftDeleteMetrics.DELETED).counter()).isNotNull();
        assertThat(registry.find(SoftDeleteMetrics.RESTORED).counter()).isNotNull();
        assertThat(registry.find(SoftDeleteMetrics.PURGED).counter()).isNotNull();

        Set<String> allowed = Set.of(SoftDeleteMetrics.TAG_TENANT, SoftDeleteMetrics.TAG_ENTITY,
                SoftDeleteMetrics.TAG_OUTCOME, SoftDeleteMetrics.TAG_REASON);
        Set<String> outcomes = Set.of("restored", "window_expired", "not_deleted");
        Set<String> reasons = Set.of("retention", "erasure_request");
        for (String name : List.of(SoftDeleteMetrics.DELETED, SoftDeleteMetrics.RESTORED, SoftDeleteMetrics.PURGED)) {
            for (Meter m : registry.find(name).meters()) {
                for (io.micrometer.core.instrument.Tag t : m.getId().getTags()) {
                    assertThat(allowed).as("meter %s tag key %s bounded", name, t.getKey()).contains(t.getKey());
                    if (t.getKey().equals(SoftDeleteMetrics.TAG_ENTITY)) {
                        assertThat(t.getValue()).isEqualTo(SoftDeleteMetrics.ENTITY);
                    } else if (t.getKey().equals(SoftDeleteMetrics.TAG_OUTCOME)) {
                        assertThat(outcomes).contains(t.getValue());
                    } else if (t.getKey().equals(SoftDeleteMetrics.TAG_REASON)) {
                        assertThat(reasons).contains(t.getValue());
                    }
                }
            }
        }
    }

    private int liveNoteCount(String accountId) {
        return as(token).get("/api/soft-delete/accounts/" + accountId + "/notes")
            .then().statusCode(200).extract().jsonPath().getList("id").size();
    }
}
