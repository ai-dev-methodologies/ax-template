package com.ax.template.authblueprint.auditlog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S2.AUDIT-PII.XB — FE&lt;-&gt;BE contract parity for the redacted {@link AuditLogResponse}
 * JSON shape. {@link AuditLogDtoPiiTest} (same package) already pins the redaction CALL on
 * the DTO write-port; this test pins the exact SERIALIZED SHAPE the FE actually receives.
 * {@code frontend/tests/audit-log-redaction-render.vitest.tsx} renders the L4 audit-log
 * detail view off the SAME committed golden fixture
 * ({@code frontend/tests/_fixtures/audit-log-response.golden.json}) this test builds through
 * the real redaction path ({@link AuditLogPiiRedactor#redactIp}) and serializes — one golden,
 * two independent consumers (this Jackson test + the FE render test). A drift in either the
 * record's field names, the redactor's masking format, or the FE parser trips exactly one of
 * the two, never silently.
 *
 * <p>Plain Jackson unit test — no {@code @SpringBootTest}, zero ContextCache pressure. Mirrors
 * {@link AuditLogDtoPiiTest}'s construction style ({@code new AuditLogProperties()} /
 * {@code new AuditLogPiiRedactor(properties)}, no Spring container needed).
 *
 * <p>RED-on-revert: if {@link AuditLogPiiRedactor#redactIp} stops masking the last IPv4 octet
 * (or a future write path forgets to call it before persisting {@code actorIp}), the produced
 * JSON tree diverges from the golden (raw IP instead of masked) and the structural-equality
 * assertion below fails.
 */
@Tag("AUDIT_LOG")
@Tag("AUDIT-PII-001")
class AuditLogResponseRedactionGoldenTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String RAW_IP = "203.0.113.42";
    private static final String MASKED_IP = "203.0.113.xxx";

    private static Path goldenPath() {
        return Path.of(System.getProperty("user.dir"), "..", "frontend", "tests",
            "_fixtures", "audit-log-response.golden.json");
    }

    private static JsonNode goldenTree() throws IOException {
        return MAPPER.readTree(Files.readString(goldenPath()));
    }

    /**
     * Builds the response exactly as the real write+read path produces it: the entity's
     * {@code actorIp} is set to the REDACTED value — mirroring
     * {@link AuditLogService#record(AuditLogDto)}, which stores
     * {@code piiRedactor.redactIp(dto.actorIp())} rather than the raw IP — and then
     * {@link AuditLogResponse#from(AuditLog)} reads it back out, exactly as
     * {@link AuditLogService#list} does for every row it serves to the FE.
     */
    private static AuditLogResponse buildResponseThroughRedactionPath() {
        AuditLogProperties properties = new AuditLogProperties();
        properties.getPii().setStoreFull(false);
        AuditLogPiiRedactor redactor = new AuditLogPiiRedactor(properties);

        AuditLog entity = AuditLog.builder()
            .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
            .actorUserId("user-42")
            .actorIp(redactor.redactIp(RAW_IP))
            .action("LOGIN")
            .resourceType("session")
            .resourceId("sess-99")
            .outcome(AuditOutcome.SUCCESS)
            .timestamp(Instant.parse("2026-01-01T00:00:00Z"))
            .correlationId("corr-123")
            .userAgent("Mozilla/5.0 TestAgent/1.0")
            .build();

        return AuditLogResponse.from(entity);
    }

    @Test
    void auditLogResponse_serializesFieldWiseEqualToTheRedactedGoldenFixture() throws IOException {
        AuditLogResponse response = buildResponseThroughRedactionPath();
        JsonNode actual = MAPPER.readTree(MAPPER.writeValueAsString(response));
        JsonNode expected = goldenTree();

        assertThat(actual)
            .as("AuditLogResponse JSON must match the FE-shared redaction golden field-for-field — "
                + "a masked-IP regression (e.g. reverting/skipping redactIp on a write path) "
                + "changes actorIp here and trips this")
            .isEqualTo(expected);
    }

    @Test
    void goldenFixture_carriesTheMaskedIpAndNeverTheRawOne() throws IOException {
        JsonNode golden = goldenTree();
        assertThat(golden.get("actorIp").asString())
            .as("golden fixture actorIp must be the masked form")
            .isEqualTo(MASKED_IP);
        assertThat(golden.toString())
            .as("golden fixture must not contain the raw IP anywhere")
            .doesNotContain(RAW_IP);
    }

    @Test
    void redactionPath_neverProducesTheRawIpRegardlessOfFixtureDrift() {
        // Non-vacuity guard independent of the checked-in fixture file: even if the golden
        // were hand-edited to something else, the ACTUAL redaction path must never emit RAW_IP.
        AuditLogResponse response = buildResponseThroughRedactionPath();
        assertThat(response.actorIp())
            .as("redaction path must mask the raw IP, never pass it through")
            .isEqualTo(MASKED_IP)
            .isNotEqualTo(RAW_IP);
    }
}
