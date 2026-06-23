package com.ax.template.authblueprint.statemutation;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * state-conditional-mutability-l0 compliance — verified against the live statemutation reference
 * workload. The invariant: the mutable field-set is a DECLARED function of state (DRAFT all,
 * SUBMITTED {reviewerNote}, APPROVED/LOCKED none); a field not in the current state's set is
 * 409 FIELD_LOCKED_IN_STATE naming the field + state; the set tightens monotonically and widens
 * only through a recorded governed re-open; an edit re-checks the state under the row lock so a
 * concurrent submit cannot let a stale-state edit through (CWE-367).
 * Spec: specs/state-conditional-mutability-l0.yaml (NIST access-control + CWE-367/362 + RFC 9457).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("STATEMUTATION")
class StateMutationComplianceTest {

    @LocalServerPort int port;
    @Autowired StateMutationService service;
    String member;

    @BeforeEach
    void setup() {
        StateMutationTestSupport.useRandomPort(port);
        member = StateMutationTestSupport.obtainToken(StateMutationTestSupport.freshEmail("sm-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String openForm(String title, String body) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"title\":\"" + title + "\",\"body\":\"" + body + "\"}")
        .when().post("/api/state-mutation/forms").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> edit(String id, String field, String value) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"field\":\"" + field + "\",\"value\":\"" + value + "\"}")
        .when().post("/api/state-mutation/forms/" + id + "/edit").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> transition(String id, String to, String reason) {
        String body = reason == null
            ? "{\"to\":\"" + to + "\"}"
            : "{\"to\":\"" + to + "\",\"reason\":\"" + reason + "\"}";
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/state-mutation/forms/" + id + "/transition").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getForm(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/state-mutation/forms/" + id).then().statusCode(200).extract();
    }

    // ── STATEMUTATION-AUTHORITY-001 — the mutable field-set is a function of state ──
    @Test @Tag("STATEMUTATION-AUTHORITY-001")
    void authority_fieldMutabilityIsAFunctionOfState() {
        String id = openForm("Original", "Body text");

        // DRAFT — all three editable
        assertThat(edit(id, "TITLE", "Edited title").statusCode()).isEqualTo(200);
        assertThat(edit(id, "BODY", "Edited body").statusCode()).isEqualTo(200);
        assertThat(edit(id, "REVIEWER_NOTE", "draft note").statusCode()).isEqualTo(200);

        // submit → SUBMITTED; only reviewerNote stays mutable
        assertThat(transition(id, "SUBMITTED", null).statusCode()).isEqualTo(200);
        assertThat(edit(id, "REVIEWER_NOTE", "reviewer annotation").statusCode()).isEqualTo(200);

        // title is frozen in SUBMITTED → 409 FIELD_LOCKED_IN_STATE naming the field + state
        ExtractableResponse<Response> locked = edit(id, "TITLE", "sneaky retitle");
        assertThat(locked.statusCode()).isEqualTo(409);
        assertThat(locked.jsonPath().getString("code")).isEqualTo("FIELD_LOCKED_IN_STATE");
        assertThat(locked.jsonPath().getString("detail")).contains("TITLE").contains("SUBMITTED");

        // body is frozen in SUBMITTED too
        assertThat(edit(id, "BODY", "sneaky rebody").statusCode()).isEqualTo(409);

        // approve → APPROVED; nothing is editable
        assertThat(transition(id, "APPROVED", null).statusCode()).isEqualTo(200);
        ExtractableResponse<Response> approvedNote = edit(id, "REVIEWER_NOTE", "post-approval edit");
        assertThat(approvedNote.statusCode()).isEqualTo(409);
        assertThat(approvedNote.jsonPath().getString("code")).isEqualTo("FIELD_LOCKED_IN_STATE");
        assertThat(approvedNote.jsonPath().getString("detail")).contains("APPROVED");
    }

    // ── STATEMUTATION-DECLARED-001 — the form reports its declared mutable-set; it matches enforcement ──
    @Test @Tag("STATEMUTATION-DECLARED-001")
    void declared_formReportsItsMutableSet_matchingEnforcement() {
        String id = openForm("Decl", "Decl body");

        List<String> draftSet = getForm(id).jsonPath().getList("mutableFields");
        assertThat(draftSet).containsExactlyInAnyOrder("TITLE", "BODY", "REVIEWER_NOTE");

        transition(id, "SUBMITTED", null);
        List<String> submittedSet = getForm(id).jsonPath().getList("mutableFields");
        assertThat(submittedSet).containsExactly("REVIEWER_NOTE");
        // the reported set IS what the edit path enforces: in-set succeeds, out-of-set 409s
        assertThat(edit(id, "REVIEWER_NOTE", "ok").statusCode()).isEqualTo(200);
        assertThat(edit(id, "TITLE", "no").statusCode()).isEqualTo(409);

        transition(id, "APPROVED", null);
        assertThat(getForm(id).jsonPath().getList("mutableFields")).isEmpty();
    }

    // ── STATEMUTATION-MONOTONE-001 — illegal edges 409; a recorded re-open widens; LOCKED terminal ──
    @Test @Tag("STATEMUTATION-MONOTONE-001")
    void monotone_illegalEdgesRejected_reopenIsRecorded_lockedTerminal() {
        String id = openForm("Mono", "Mono body");

        // skip DRAFT→APPROVED is illegal
        ExtractableResponse<Response> skip = transition(id, "APPROVED", null);
        assertThat(skip.statusCode()).isEqualTo(409);
        assertThat(skip.jsonPath().getString("code")).isEqualTo("ILLEGAL_FORM_TRANSITION");

        transition(id, "SUBMITTED", null);

        // a re-open (widening) with no reason → 422
        ExtractableResponse<Response> noReason = transition(id, "DRAFT", null);
        assertThat(noReason.statusCode()).isEqualTo(422);
        assertThat(noReason.jsonPath().getString("code")).isEqualTo("REOPEN_REASON_REQUIRED");

        // a re-open WITH a reason → 200, back to DRAFT, title editable again, recorded REOPEN
        ExtractableResponse<Response> reopened = transition(id, "DRAFT", "needs correction");
        assertThat(reopened.statusCode()).isEqualTo(200);
        assertThat(reopened.jsonPath().getString("state")).isEqualTo("DRAFT");
        assertThat(edit(id, "TITLE", "corrected title").statusCode()).isEqualTo(200);   // widened back

        var transitions = given().header("Authorization", "Bearer " + member)
            .when().get("/api/state-mutation/forms/" + id + "/transitions")
            .then().statusCode(200).extract().jsonPath();
        assertThat(transitions.getList("kind")).containsExactly("FORWARD", "REOPEN");
        // the re-open carries its recorded reason
        assertThat(transitions.getList("reason")).contains("needs correction");

        // walk to LOCKED and prove it is terminal — a re-open of LOCKED is 409
        transition(id, "SUBMITTED", null);
        transition(id, "APPROVED", null);
        assertThat(transition(id, "LOCKED", null).statusCode()).isEqualTo(200);
        ExtractableResponse<Response> reopenLocked = transition(id, "DRAFT", "too late");
        assertThat(reopenLocked.statusCode()).isEqualTo(409);
        assertThat(reopenLocked.jsonPath().getString("code")).isEqualTo("ILLEGAL_FORM_TRANSITION");
    }

    // ── STATEMUTATION-TOCTOU-001 — deterministic: an edit after a concurrent freeze is 409 ──
    @Test @Tag("STATEMUTATION-TOCTOU-001")
    void toctou_editAgainstStaleStateIsRejected() {
        String id = openForm("Toctou", "Toctou body");
        // the caller "observed" DRAFT and intends to edit the title; meanwhile a submit lands first
        transition(id, "SUBMITTED", null);
        // the title-edit composed against the observed DRAFT is now re-checked against SUBMITTED → 409
        ExtractableResponse<Response> stale = edit(id, "TITLE", "edit against stale DRAFT");
        assertThat(stale.statusCode()).isEqualTo(409);
        assertThat(stale.jsonPath().getString("code")).isEqualTo("FIELD_LOCKED_IN_STATE");
    }

    // ── STATEMUTATION-TOCTOU-001 — keystone: a concurrent submit and title-edits never let a frozen write through ──
    @Test @Tag("STATEMUTATION-TOCTOU-001")
    void toctou_concurrentSubmitAndEdits_frozenFieldNeverWrittenAfterFreeze() throws Exception {
        String id = openForm("Race", "Race body");
        UUID formId = UUID.fromString(id);

        int editors = 8;
        ExecutorService pool = Executors.newFixedThreadPool(editors + 1);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<String> editOutcomes = new ConcurrentLinkedQueue<>();

        // one submitter freezes the title; editors all try to edit the title concurrently
        pool.submit(() -> {
            start.await();
            service.transition(formId, FormState.SUBMITTED, null, "submitter");
            return null;
        });
        for (int i = 0; i < editors; i++) {
            final int n = i;
            pool.submit(() -> {
                start.await();
                try {
                    service.editField(formId, FormField.TITLE, "title-by-" + n);
                    editOutcomes.add("OK");
                } catch (StateMutationException ex) {
                    editOutcomes.add(ex.code());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        // the form ends SUBMITTED (the submit cannot be lost)
        assertThat(getForm(id).jsonPath().getString("state")).isEqualTo("SUBMITTED");

        // CWE-367 invariant (deterministic, race-interleaving-independent): every editor either
        // ran legitimately while DRAFT still held (OK, serialized by the lock) or was cleanly
        // rejected with the declared FIELD_LOCKED_IN_STATE — NEVER a stale-state lost-update and
        // NEVER a different error. (We do NOT assert HOW MANY lost the race: if the submit commits
        // last, all 8 edits legitimately land during DRAFT — correct behaviour, not a violation.
        // Asserting ">=1 rejected" was a flaky race-outcome assumption and is removed.)
        for (String outcome : editOutcomes) {
            assertThat(outcome).isIn("OK", "FIELD_LOCKED_IN_STATE");
        }
        // deterministic proof the freeze actually blocks: the form is now SUBMITTED, so a fresh
        // edit of the frozen TITLE MUST be rejected — no timing dependence.
        assertThatThrownBy(() -> service.editField(formId, FormField.TITLE, "after-freeze"))
            .isInstanceOf(StateMutationException.class)
            .extracting(e -> ((StateMutationException) e).code())
            .isEqualTo("FIELD_LOCKED_IN_STATE");
    }

    // ── IDOR / 404 — an unknown form is 404, never a leak ──
    @Test @Tag("STATEMUTATION-AUTHORITY-001")
    void unknownForm_is404() {
        ExtractableResponse<Response> missing = given().header("Authorization", "Bearer " + member)
            .when().get("/api/state-mutation/forms/" + UUID.randomUUID()).thenReturn().then().extract();
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(missing.jsonPath().getString("code")).isEqualTo("RESOURCE_NOT_FOUND");
    }
}
