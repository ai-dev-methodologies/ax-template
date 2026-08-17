package com.ax.template.authblueprint.appealindependence;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * appeal-decider-independence-l0 compliance — RestAssured black-box against the live
 * appealindependence reference workload. Spec: specs/appeal-decider-independence-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("APPEALINDEPENDENCE")
class AppealComplianceTest {

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbcTemplate;


    private ExtractableResponse<Response> fileOriginal(String token, String outcome) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"outcome\":\"" + outcome + "\"}")
        .when().post("/api/appeals").thenReturn().then().statusCode(201).extract();
    }

    private ExtractableResponse<Response> fileAppeal(String token, UUID parentId, String outcome) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"outcome\":\"" + outcome + "\"}")
        .when().post("/api/appeals/" + parentId + "/appeal").thenReturn().then().extract();
    }

    // ── APPEAL-DISTINCT-001 — appeal decider must differ from the original's decider ──
    @Test
    @Tag("APPEAL-DISTINCT-001")
    void appeal_rejectsSameDeciderAsOriginal_dbCheckBackstopsIt() {
        String alice = AppealTestSupport.obtainToken(AppealTestSupport.freshEmail("ai-alice"), "MEMBER");
        String bob = AppealTestSupport.obtainToken(AppealTestSupport.freshEmail("ai-bob"), "MEMBER");

        ExtractableResponse<Response> original = fileOriginal(alice, "DENIED");
        assertThat(original.statusCode()).isEqualTo(201);
        UUID originalId = UUID.fromString(original.jsonPath().getString("id"));
        String aliceDeciderId = original.jsonPath().getString("decidedBy");

        // alice (same decider) attempts the appeal → 422, rejected BEFORE persistence.
        ExtractableResponse<Response> selfAppeal = fileAppeal(alice, originalId, "OVERTURN");
        assertThat(selfAppeal.statusCode()).isEqualTo(422);
        assertThat(selfAppeal.jsonPath().getString("code")).isEqualTo("APPEAL_DECIDER_NOT_INDEPENDENT");

        // bob (independent) may appeal.
        ExtractableResponse<Response> bobAppeal = fileAppeal(bob, originalId, "OVERTURN");
        assertThat(bobAppeal.statusCode()).isEqualTo(201);
        assertThat(bobAppeal.jsonPath().getInt("level")).isEqualTo(1);

        // Native-write proof: an INSERT that sets decided_by == appealed_decider_by is rejected
        // by the DB @Check, not only by the service.
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO appeal_decisions (id, parent_decision_id, chain_root_id, level, kind,"
                    + " decided_by, appealed_decider_by, outcome, decided_at)"
                    + " VALUES (?, ?, ?, 99, 'APPEAL', ?, ?, 'OVERTURN', CURRENT_TIMESTAMP)",
                UUID.randomUUID(), originalId, originalId, aliceDeciderId, aliceDeciderId))
            .as("a code path that forgets the independence gate must fail at the DB")
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    // ── APPEAL-CHAIN-001 — 3-level chain; each level distinct from ALL prior deciders; 1 appeal/level ──
    @Test
    @Tag("APPEAL-CHAIN-001")
    void appeal_threeLevelChain_rejectsReusedDeciderAndSecondAppealOnSameLevel() {
        String alice = AppealTestSupport.obtainToken(AppealTestSupport.freshEmail("ai2-alice"), "MEMBER");
        String bob = AppealTestSupport.obtainToken(AppealTestSupport.freshEmail("ai2-bob"), "MEMBER");
        String carol = AppealTestSupport.obtainToken(AppealTestSupport.freshEmail("ai2-carol"), "MEMBER");

        UUID level0 = UUID.fromString(fileOriginal(alice, "DENIED").jsonPath().getString("id"));
        ExtractableResponse<Response> level1 = fileAppeal(bob, level0, "UPHOLD");
        assertThat(level1.statusCode()).isEqualTo(201);
        UUID level1Id = UUID.fromString(level1.jsonPath().getString("id"));

        // a SECOND appeal against level0 (already appealed) → 409, one appeal per level.
        ExtractableResponse<Response> duplicateAppeal = fileAppeal(carol, level0, "OVERTURN");
        assertThat(duplicateAppeal.statusCode()).isEqualTo(409);
        assertThat(duplicateAppeal.jsonPath().getString("code")).isEqualTo("APPEAL_ALREADY_FILED");

        ExtractableResponse<Response> level2 = fileAppeal(carol, level1Id, "OVERTURN");
        assertThat(level2.statusCode()).isEqualTo(201);
        UUID level2Id = UUID.fromString(level2.jsonPath().getString("id"));

        // alice already decided at level 0 — not carol's immediate parent, but still in the
        // chain — a level-3 appeal by alice against carol's decision MUST be rejected.
        ExtractableResponse<Response> aliceAgain = fileAppeal(alice, level2Id, "UPHOLD");
        assertThat(aliceAgain.statusCode()).isEqualTo(422);
        assertThat(aliceAgain.jsonPath().getString("code")).isEqualTo("APPEAL_DECIDER_NOT_INDEPENDENT");

        // a genuinely new decider may still extend the chain.
        String dave = AppealTestSupport.obtainToken(AppealTestSupport.freshEmail("ai2-dave"), "MEMBER");
        ExtractableResponse<Response> level3 = fileAppeal(dave, level2Id, "UPHOLD");
        assertThat(level3.statusCode()).isEqualTo(201);
        assertThat(level3.jsonPath().getInt("level")).isEqualTo(3);
    }

    // ── APPEAL-OUTCOME-001 — appeal outcome is append-only; original row never mutated ──
    @Test
    @Tag("APPEAL-OUTCOME-001")
    void appealOutcome_isAppendOnly_originalRowNeverMutated() {
        String alice = AppealTestSupport.obtainToken(AppealTestSupport.freshEmail("ai3-alice"), "MEMBER");
        String bob = AppealTestSupport.obtainToken(AppealTestSupport.freshEmail("ai3-bob"), "MEMBER");

        ExtractableResponse<Response> original = fileOriginal(alice, "DENIED");
        UUID originalId = UUID.fromString(original.jsonPath().getString("id"));
        UUID chainRootId = UUID.fromString(original.jsonPath().getString("chainRootId"));

        assertThat(fileAppeal(bob, originalId, "OVERTURN").statusCode()).isEqualTo(201);

        // the ORIGINAL row still reports its own outcome/decider — never rewritten.
        ExtractableResponse<Response> replay = given().header("Authorization", "Bearer " + alice)
        .when().get("/api/appeals/" + originalId).then().statusCode(200).extract();
        assertThat(replay.jsonPath().getString("outcome")).isEqualTo("DENIED");
        assertThat(replay.jsonPath().getInt("level")).isEqualTo(0);
        assertThat(replay.jsonPath().getString("kind")).isEqualTo("ORIGINAL");

        // the chain shows both rows, in level order — a client derives "current" from the LAST row.
        ExtractableResponse<Response> chain = given().header("Authorization", "Bearer " + alice)
        .when().get("/api/appeals/chain/" + chainRootId).then().statusCode(200).extract();
        assertThat(chain.jsonPath().getList("level", Integer.class)).containsExactly(0, 1);
        assertThat(chain.jsonPath().getList("outcome", String.class)).containsExactly("DENIED", "OVERTURN");
    }
}
