package com.ax.template.authblueprint.mandate;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * mandate-fanout-l0 MANDATE-DEEMED-001 + the deemed-vs-explicit keystone (MANDATE-CONCURRENT-001).
 * Runs with mandate.deemed-window-days = -1 so a freshly issued task's deemed deadline is one day
 * in the PAST — immediately overdue — letting the deterministic synchronous worker
 * ({@link MandateService#resolveDeemed}) resolve it without moving the injected Clock. A separate
 * context (its own @TestPropertySource) keeps the negative window isolated from
 * {@link MandateComplianceTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = "mandate.deemed-window-days=-1")
@Tag("MANDATE")
class MandateDeemedSweepTest {

    @LocalServerPort int port;
    @Autowired MandateService service;
    String member;

    @BeforeEach
    void setup() {
        MandateTestSupport.useRandomPort(port);
        member = MandateTestSupport.obtainToken(MandateTestSupport.freshEmail("mandate-deemed"), "MEMBER");
    }

    private String issue(String directive, int taskCount) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"directive\":\"" + directive + "\",\"taskCount\":" + taskCount + ",\"checkKeys\":[]}")
        .when().post("/api/mandate/mandates").then().statusCode(201).extract().path("id");
    }

    private List<String> taskIds(String mandateId) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/mandate/mandates/" + mandateId + "/tasks")
            .then().statusCode(200).extract().jsonPath().getList("id");
    }

    private ExtractableResponse<Response> task(String mandateId, int idx) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/mandate/mandates/" + mandateId + "/tasks").then().statusCode(200).extract();
    }

    // ── MANDATE-DEEMED-001 — an overdue unanswered task is resolved to DEEMED (SYSTEM/DEEMED), once ──
    @Test @Tag("MANDATE-DEEMED-001")
    void deemed_overdueUnansweredTask_resolvedToDeemed_exactlyOnce() {
        String id = issue("Will lapse to deemed", 2);
        List<String> tasks = taskIds(id);

        // the worker resolves an overdue PENDING task to DEEMED with resolver SYSTEM + reason DEEMED
        assertThat(service.resolveDeemed(UUID.fromString(tasks.get(0)))).isTrue();

        ExtractableResponse<Response> after = task(id, 0);
        assertThat(after.jsonPath().getString("[0].state")).isEqualTo("DEEMED");
        assertThat(after.jsonPath().getString("[0].resolvedBy")).isEqualTo("SYSTEM");
        assertThat(after.jsonPath().getString("[0].resolveReason")).isEqualTo("DEEMED");

        // a second pass over the now-terminal task is a no-op (exactly-once)
        assertThat(service.resolveDeemed(UUID.fromString(tasks.get(0)))).isFalse();
        assertThat(task(id, 0).jsonPath().getString("[0].state")).isEqualTo("DEEMED");

        // deemed children count toward the conserved completion recall
        assertThat(service.resolveDeemed(UUID.fromString(tasks.get(1)))).isTrue();
        assertThat(given().header("Authorization", "Bearer " + member)
            .when().get("/api/mandate/mandates/" + id).then().statusCode(200)
            .extract().jsonPath().getBoolean("complete")).isTrue();
    }

    // ── MANDATE-DEEMED-001 — an explicitly resolved task is NEVER re-resolved to DEEMED ──
    @Test @Tag("MANDATE-DEEMED-001")
    void deemed_explicitlyResolvedTask_isNeverDeemed() {
        String id = issue("Explicit beats deemed", 1);
        UUID taskId = UUID.fromString(taskIds(id).get(0));

        // an explicit complete wins first
        service.completeTask(taskId, MandateTaskState.DONE, "alice");
        // the deemed worker observes it already terminal → no-op, keeps the explicit resolver
        assertThat(service.resolveDeemed(taskId)).isFalse();

        ExtractableResponse<Response> t = task(id, 0);
        assertThat(t.jsonPath().getString("[0].state")).isEqualTo("DONE");
        assertThat(t.jsonPath().getString("[0].resolveReason")).isEqualTo("EXPLICIT");
        assertThat(t.jsonPath().getString("[0].resolvedBy")).isEqualTo("alice");
    }

    // ── MANDATE-CONCURRENT-001 — keystone: explicit complete vs deemed sweep → terminal exactly once ──
    @Test @Tag("MANDATE-CONCURRENT-001")
    void concurrent_explicitCompleteVsDeemedSweep_terminalExactlyOnce() throws Exception {
        String id = issue("Deemed-vs-explicit race", 1);
        UUID taskId = UUID.fromString(taskIds(id).get(0));

        int explicitThreads = 4;
        int deemedThreads = 4;
        int total = explicitThreads + deemedThreads;
        ExecutorService pool = Executors.newFixedThreadPool(total);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger explicitWins = new AtomicInteger();
        AtomicInteger deemedWins = new AtomicInteger();
        ConcurrentLinkedQueue<Integer> explicitCodes = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < explicitThreads; i++) {
            pool.submit(() -> {
                start.await();
                try {
                    service.completeTask(taskId, MandateTaskState.DONE, "racer");
                    explicitWins.incrementAndGet();
                    explicitCodes.add(200);
                } catch (MandateException ex) {
                    explicitCodes.add(ex.status().value());
                }
                return null;
            });
        }
        for (int i = 0; i < deemedThreads; i++) {
            pool.submit(() -> {
                start.await();
                if (service.resolveDeemed(taskId)) {
                    deemedWins.incrementAndGet();
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        // EXACTLY ONE terminal resolution total across both paths — no double-terminal (CWE-362)
        assertThat(explicitWins.get() + deemedWins.get())
            .as("MANDATE-CONCURRENT-001 — exactly one terminal resolution across explicit + deemed").isEqualTo(1);
        // the losing explicit threads got 409 each (the deemed losers are silent no-ops)
        assertThat(explicitCodes.stream().filter(c -> c == 200).count()).isEqualTo(explicitWins.get());
        assertThat(explicitCodes.stream().filter(c -> c == 409).count())
            .isEqualTo(explicitThreads - explicitWins.get());

        // the task is terminal with exactly one resolver/reason consistent with the winner
        ExtractableResponse<Response> t = task(id, 0);
        String state = t.jsonPath().getString("[0].state");
        String reason = t.jsonPath().getString("[0].resolveReason");
        assertThat(state).isIn("DONE", "DEEMED");
        if (state.equals("DONE")) {
            assertThat(reason).isEqualTo("EXPLICIT");
        } else {
            assertThat(reason).isEqualTo("DEEMED");
            assertThat(t.jsonPath().getString("[0].resolvedBy")).isEqualTo("SYSTEM");
        }
    }
}
