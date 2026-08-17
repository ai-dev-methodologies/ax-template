package com.ax.template.authblueprint.auditlog;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * LIST family (2 items).
 * <ul>
 *   <li>AUDIT-LIST-001 — pagination + total counts</li>
 *   <li>AUDIT-LIST-002 — multi-filter combination (AND)</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditLogListTest {

    @LocalServerPort int port;
    @Autowired AuditLogRepository repository;

    private String token;
    private String actorA;
    private String actorB;

    @BeforeEach
    void setup() {
        token = AuditLogTestSupport.obtainToken(
            AuditLogTestSupport.freshEmail("list"), "ADMIN");

        actorA = "list-actor-A-" + UUID.randomUUID();
        actorB = "list-actor-B-" + UUID.randomUUID();
    }

    @Test
    @Tag("AUDIT_LOG")
    @Tag("AUDIT-LIST-001")
    void list_001_paginatedListWithTotal() {
        // Seed 25 entries — all with the same actor so the filter below can
        // exercise total-page math.
        String tag = "list-001-" + UUID.randomUUID();
        for (int i = 0; i < 25; i++) {
            repository.save(AuditLog.builder()
                .id(UUID.randomUUID())
                .actorUserId(tag)
                .action("CREATE")
                .resourceType("sample")
                .resourceId("r" + i)
                .outcome(AuditOutcome.SUCCESS)
                .timestamp(Instant.now().minusSeconds(i))
                .build());
        }

        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .queryParam("actorId", tag)
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when().get("/api/audit-logs")
        .then().statusCode(200)
            .body("content.size()", equalTo(10))
            .body("totalElements", equalTo(25))
            .body("totalPages", equalTo(3))
            .body("page", equalTo(0))
            .body("size", equalTo(10));
    }

    @Test
    @Tag("AUDIT_LOG")
    @Tag("AUDIT-LIST-002")
    void list_002_filterByActorAndDateRange() {
        Instant now = Instant.now();
        repository.save(AuditLog.builder()
            .id(UUID.randomUUID())
            .actorUserId(actorA)
            .action("UPDATE")
            .resourceType("sample")
            .resourceId("a1")
            .outcome(AuditOutcome.SUCCESS)
            .timestamp(now.minus(1, ChronoUnit.MINUTES))
            .build());

        repository.save(AuditLog.builder()
            .id(UUID.randomUUID())
            .actorUserId(actorB)
            .action("UPDATE")
            .resourceType("sample")
            .resourceId("b1")
            .outcome(AuditOutcome.SUCCESS)
            .timestamp(now.minus(1, ChronoUnit.MINUTES))
            .build());

        // Filter by actorA — must return only the actorA row.
        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .queryParam("actorId", actorA)
        .when().get("/api/audit-logs")
        .then().statusCode(200)
            .body("content.size()", equalTo(1))
            .body("content[0].actorId", equalTo(actorA));

        // Filter by date range that excludes both seeded rows.
        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .queryParam("actorId", actorA)
            .queryParam("from", now.plus(1, ChronoUnit.DAYS).toString())
        .when().get("/api/audit-logs")
        .then().statusCode(200)
            .body("totalElements", equalTo(0));

        // Filter combination: actorA AND outcome=SUCCESS must match the seed.
        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .queryParam("actorId", actorA)
            .queryParam("outcome", "SUCCESS")
        .when().get("/api/audit-logs")
        .then().statusCode(200)
            .body("totalElements", greaterThanOrEqualTo(1));
    }
}
