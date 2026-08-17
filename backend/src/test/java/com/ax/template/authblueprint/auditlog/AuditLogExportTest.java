package com.ax.template.authblueprint.auditlog;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.oneOf;

/**
 * EXPORT family (2 items).
 * <ul>
 *   <li>AUDIT-EXPORT-001 — POST /api/audit-logs/export returns 202 + job ID;
 *       GET /api/audit-logs/export/{jobId} returns one of PENDING|PROCESSING|COMPLETED|FAILED</li>
 *   <li>AUDIT-EXPORT-002 — non-ADMIN/AUDITOR caller rejected with 403</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditLogExportTest {

    @LocalServerPort int port;


    @Test
    @Tag("AUDIT_LOG")
    @Tag("AUDIT-EXPORT-001")
    void export_001_acceptedAndJobIdReturnedAndStatusPollable() {
        String adminToken = AuditLogTestSupport.obtainToken(
            AuditLogTestSupport.freshEmail("export-001"), "ADMIN");

        String jobId =
            given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("{\"format\":\"CSV\"}")
            .when().post("/api/audit-logs/export")
            .then().statusCode(202)
                .body("jobId", notNullValue())
                .body("status", equalTo("PENDING"))
                .extract().path("jobId");

        given()
            .header("Authorization", "Bearer " + adminToken)
            .accept(ContentType.JSON)
        .when().get("/api/audit-logs/export/" + jobId)
        .then().statusCode(200)
            .body("jobId", equalTo(jobId))
            .body("status", oneOf("PENDING", "PROCESSING", "COMPLETED", "FAILED"));
    }

    @Test
    @Tag("AUDIT_LOG")
    @Tag("AUDIT-EXPORT-002")
    void export_002_nonAdminNonAuditorRejectedWith403() {
        // Plain MEMBER must be rejected with 403.
        String memberToken = AuditLogTestSupport.obtainToken(
            AuditLogTestSupport.freshEmail("export-002-member"), "MEMBER");

        given()
            .header("Authorization", "Bearer " + memberToken)
            .contentType(ContentType.JSON)
            .body("{\"format\":\"CSV\"}")
        .when().post("/api/audit-logs/export")
        .then().statusCode(403);

        // AUDITOR must be allowed (202).
        String auditorToken = AuditLogTestSupport.obtainToken(
            AuditLogTestSupport.freshEmail("export-002-auditor"), "AUDITOR");

        given()
            .header("Authorization", "Bearer " + auditorToken)
            .contentType(ContentType.JSON)
            .body("{\"format\":\"JSON\"}")
        .when().post("/api/audit-logs/export")
        .then().statusCode(202);
    }

    /**
     * AUDIT-EXPORT-002 (P1-66) — the GET status/poll surface is an "Export request" too and MUST be
     * role-gated. Before the fix it carried no @PreAuthorize, so any authenticated principal holding
     * a jobId could read a completed ADMIN/AUDITOR export's downloadUrl + recordCount (IDOR). A
     * ROLE_MEMBER polling a real ADMIN-enqueued job must now be rejected with 403 — never 200.
     */
    @Test
    @Tag("AUDIT_LOG")
    @Tag("AUDIT-EXPORT-002")
    void export_002_getStatusByNonAdminNonAuditorRejectedWith403() {
        // ADMIN enqueues a real export job.
        String adminToken = AuditLogTestSupport.obtainToken(
            AuditLogTestSupport.freshEmail("export-002-get-admin"), "ADMIN");
        String jobId =
            given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("{\"format\":\"CSV\"}")
            .when().post("/api/audit-logs/export")
            .then().statusCode(202)
                .extract().path("jobId");

        // A ROLE_MEMBER attempting to read that job's status is rejected with 403 (IDOR sealed).
        String memberToken = AuditLogTestSupport.obtainToken(
            AuditLogTestSupport.freshEmail("export-002-get-member"), "MEMBER");
        given()
            .header("Authorization", "Bearer " + memberToken)
            .accept(ContentType.JSON)
        .when().get("/api/audit-logs/export/" + jobId)
        .then().statusCode(403);

        // AUDITOR may still read it (legitimately admin-global resource; not owner-scoped).
        String auditorToken = AuditLogTestSupport.obtainToken(
            AuditLogTestSupport.freshEmail("export-002-get-auditor"), "AUDITOR");
        given()
            .header("Authorization", "Bearer " + auditorToken)
            .accept(ContentType.JSON)
        .when().get("/api/audit-logs/export/" + jobId)
        .then().statusCode(200)
            .body("jobId", equalTo(jobId));
    }
}
