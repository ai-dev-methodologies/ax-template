package com.ax.template.authblueprint.copresence;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * negative-copresence-gate-l0 compliance — verified against the live copresence reference workload. The
 * invariant: an introducing write is evaluated against the subject's ACTIVE member set (set-intersection
 * on a normalized concept), graded ABSOLUTE (hard-stop) / RELATIVE (overridable with a reason), fails
 * closed on an unassessable concept, and re-reads the set under the subject lock. Spec:
 * specs/negative-copresence-gate-l0.yaml (Saltzer & Schroeder fail-safe defaults).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("COPRESENCE")
class CopresenceComplianceTest {

    @LocalServerPort int port;
    String admin;
    String member;

    @BeforeEach
    void setup() {
        CopresenceTestSupport.useRandomPort(port);
        admin = CopresenceTestSupport.obtainToken(CopresenceTestSupport.freshEmail("cop-admin"), "ADMIN");
        member = CopresenceTestSupport.obtainToken(CopresenceTestSupport.freshEmail("cop-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    // The KB is global (shared across test methods on one context); seeding is idempotent — 201 first,
    // 409 (already seeded) on later tests. Both mean "the KB has this entry".
    private void registerConcept(String concept) {
        int code = given().header("Authorization", "Bearer " + admin).header("Content-Type", "application/json")
            .body("{\"concept\":\"" + concept + "\"}")
        .when().post("/api/copresence/kb/concepts").thenReturn().statusCode();
        assertThat(code).isIn(201, 409);
    }

    private void addConflict(String a, String b, String severity) {
        int code = given().header("Authorization", "Bearer " + admin).header("Content-Type", "application/json")
            .body("{\"conceptA\":\"" + a + "\",\"conceptB\":\"" + b + "\",\"severity\":\"" + severity + "\",\"reason\":\"kb\"}")
        .when().post("/api/copresence/kb/conflicts").thenReturn().statusCode();
        assertThat(code).isIn(201, 409);
    }

    private String createSubject(String key) {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"subjectKey\":\"" + key + "\"}")
        .when().post("/api/copresence/subjects").then().statusCode(201);
        return key;
    }

    private ExtractableResponse<Response> addMember(String subjectKey, String concept, String overrideField) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"concept\":\"" + concept + "\",\"label\":\"" + concept + " label\"" + overrideField + "}")
        .when().post("/api/copresence/subjects/" + subjectKey + "/members").thenReturn().then().extract();
    }

    private int activeCount(String subjectKey) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/copresence/subjects/" + subjectKey + "/members?size=50")
            .then().statusCode(200).extract().jsonPath().getInt("data.size()");
    }

    private void seedKb() {
        registerConcept("drugX");
        registerConcept("drugY");
        registerConcept("allergenZ");
        addConflict("drugX", "drugY", "ABSOLUTE");
        addConflict("drugX", "allergenZ", "RELATIVE");
    }

    // ── GATE-SET-EVAL-001 / GATE-GRADED-001 — set-evaluated, graded verdict ──
    @Test @Tag("GATE-SET-EVAL-001") @Tag("GATE-GRADED-001")
    void candidateEvaluatedAgainstActiveSet_gradedVerdict() {
        seedKb();
        String s = createSubject("subj-" + UUID.randomUUID());
        // first member: no conflict (empty set) → admitted
        assertThat(addMember(s, "drugX", "").statusCode()).isEqualTo(201);
        // ABSOLUTE conflict with the active drugX → hard-stop 422
        ExtractableResponse<Response> abs = addMember(s, "drugY", "");
        assertThat(abs.statusCode()).isEqualTo(422);
        assertThat(abs.path("code").toString()).isEqualTo("COPRESENCE_ABSOLUTE");
        // RELATIVE conflict with the active drugX, no override → 422
        ExtractableResponse<Response> rel = addMember(s, "allergenZ", "");
        assertThat(rel.statusCode()).isEqualTo(422);
        assertThat(rel.path("code").toString()).isEqualTo("COPRESENCE_RELATIVE");
        assertThat(activeCount(s)).as("only drugX admitted").isEqualTo(1);
    }

    // ── GATE-SET-EVAL-001 / GATE-GRADED-001 — KB integrity: unordered pair uniqueness, referential
    //    integrity (no orphaned rule), and duplicate-therapy graded RELATIVE ──
    @Test @Tag("GATE-SET-EVAL-001") @Tag("GATE-GRADED-001")
    void kbIntegrity_unorderedUniqueness_referential_duplicateTherapy() {
        seedKb();   // drugX×drugY ABSOLUTE stored canonically
        // (A) the reverse-order pair is the SAME unordered pair → 409 (no contradictory dual rows)
        int rev = given().header("Authorization", "Bearer " + admin).header("Content-Type", "application/json")
            .body("{\"conceptA\":\"drugY\",\"conceptB\":\"drugX\",\"severity\":\"RELATIVE\",\"reason\":\"reverse\"}")
        .when().post("/api/copresence/kb/conflicts").thenReturn().statusCode();
        assertThat(rev).as("reverse-order is the same unordered pair").isEqualTo(409);

        // (B) a conflict rule referencing an UNREGISTERED concept is rejected (no silent fail-OPEN)
        ExtractableResponse<Response> orphan = given().header("Authorization", "Bearer " + admin)
            .header("Content-Type", "application/json")
            .body("{\"conceptA\":\"drugX\",\"conceptB\":\"ghost-" + UUID.randomUUID() + "\",\"severity\":\"ABSOLUTE\",\"reason\":\"typo\"}")
        .when().post("/api/copresence/kb/conflicts").thenReturn().then().extract();
        assertThat(orphan.statusCode()).isEqualTo(422);
        assertThat(orphan.path("code").toString()).isEqualTo("COPRESENCE_UNKNOWN_CONCEPT");

        // (C) duplicate-therapy: re-adding the same active concept is a RELATIVE finding (overridable)
        String s = createSubject("subj-" + UUID.randomUUID());
        assertThat(addMember(s, "drugX", "").statusCode()).isEqualTo(201);
        ExtractableResponse<Response> dup = addMember(s, "drugX", "");
        assertThat(dup.statusCode()).isEqualTo(422);
        assertThat(dup.path("code").toString()).isEqualTo("COPRESENCE_RELATIVE");
        assertThat(addMember(s, "drugX", ",\"overrideReason\":\"intentional-restart\"").statusCode()).isEqualTo(201);
    }

    // ── GATE-FAILCLOSED-001 — an unassessable (unmapped) concept blocks, never silent-allows ──
    @Test @Tag("GATE-FAILCLOSED-001")
    void unmappedConcept_failsClosed() {
        seedKb();
        String s = createSubject("subj-" + UUID.randomUUID());
        ExtractableResponse<Response> unk = addMember(s, "unknownDrug", "");
        assertThat(unk.statusCode()).isEqualTo(422);
        assertThat(unk.path("code").toString()).isEqualTo("COPRESENCE_UNASSESSABLE");
        assertThat(activeCount(s)).as("nothing admitted for an unassessable concept").isEqualTo(0);
    }

    // ── GATE-OVERRIDE-001 — RELATIVE overridable with a reason; ABSOLUTE has NO override path ──
    @Test @Tag("GATE-OVERRIDE-001")
    void relativeOverridableWithReason_absoluteNever() {
        seedKb();
        String s = createSubject("subj-" + UUID.randomUUID());
        assertThat(addMember(s, "drugX", "").statusCode()).isEqualTo(201);

        // RELATIVE + non-blank override → admitted, overridden findings recorded by reference
        ExtractableResponse<Response> ov = addMember(s, "allergenZ", ",\"overrideReason\":\"clinical-judgment\"");
        assertThat(ov.statusCode()).isEqualTo(201);
        assertThat(ov.path("overriddenFindings").toString()).contains("RELATIVE").contains("drugX");
        assertThat(ov.path("overrideReason").toString()).isEqualTo("clinical-judgment");

        // ABSOLUTE + any override reason → STILL 422 (no override path)
        ExtractableResponse<Response> absOv = addMember(s, "drugY", ",\"overrideReason\":\"i-really-mean-it\"");
        assertThat(absOv.statusCode()).isEqualTo(422);
        assertThat(absOv.path("code").toString()).isEqualTo("COPRESENCE_ABSOLUTE");
    }

    // ── GATE-CONCURRENT-001 (keystone) — two concurrent mutually-ABSOLUTE adds → at most one admitted ──
    @Test @Tag("GATE-CONCURRENT-001")
    void concurrentMutuallyConflictingAdds_neverBoth() throws InterruptedException {
        seedKb();
        String s = createSubject("subj-" + UUID.randomUUID());   // drugX × drugY = ABSOLUTE

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        try {
            for (String drug : new String[]{"drugX", "drugY"}) {
                pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    codes.add(addMember(s, drug, "").statusCode());
                    return null;
                });
            }
            ready.await();
            go.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
        // the subject-lock serializes: one add commits, the other sees it and is hard-stopped
        assertThat(codes.stream().filter(c -> c == 201).count()).as("at most one admitted").isEqualTo(1);
        assertThat(activeCount(s)).as("never both mutually-ABSOLUTE members").isEqualTo(1);
    }

    // ── RBAC — KB mutation is ADMIN-only; the gate is authenticated ──
    @Test
    void rbac_kbIsAdminOnly_gateAuthenticated() {
        // a MEMBER cannot poison the safety knowledge base
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"concept\":\"sneaky\"}")
        .when().post("/api/copresence/kb/concepts").then().statusCode(403);
        // unauthenticated cannot use the gate
        given().header("Content-Type", "application/json").body("{\"concept\":\"x\",\"label\":\"x\"}")
        .when().post("/api/copresence/subjects/anything/members").then().statusCode(401);
    }
}
