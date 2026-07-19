package com.ax.template.authblueprint.auditlog;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S2.AUDIT-PII.XB closure — cross-feature {@link AuditLogDto} write-port boundary.
 * <p>
 * Trace: AUDIT-PII-001. Plain unit test (no {@code @SpringBootTest} context needed):
 * {@link AuditLogService#record(AuditLogDto)} is the published port other features use
 * to write an audit entry (DDD decomposition spec §6 / {@code HG-FEAT-ISOLATION}) — unlike
 * the {@code @Audited} aspect path ({@link AuditLoggingAspect}), which already redacts
 * {@code actorIp} through {@link AuditLogPiiRedactor} before calling
 * {@link AuditLogService#record(AuditLog)}, this DTO-based port built the entity directly
 * from the caller-supplied raw {@code actorIp} with no redaction call at all — any other
 * domain writing audit entries through the published port stored (and then served back via
 * {@link AuditLogResponse#from}) an unmasked IP address.
 * <p>
 * RED-on-revert: reverting the {@code piiRedactor.redactIp(dto.actorIp())} call in
 * {@link AuditLogService#record(AuditLogDto)} back to the raw {@code dto.actorIp()} makes
 * this test fail (captured entity {@code actorIp} would be the raw, unmasked value).
 */
class AuditLogDtoPiiTest {

    @Test
    @Tag("AUDIT_LOG")
    @Tag("AUDIT-PII-001")
    void piiCrossBoundary_dtoWritePortRedactsActorIpLikeTheAuditedAspectDoes() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditLogProperties properties = new AuditLogProperties();
        properties.getPii().setStoreFull(false);
        AuditLogPiiRedactor redactor = new AuditLogPiiRedactor(properties);
        AuditLogService service = new AuditLogService(repository, redactor);

        when(repository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLogDto dto = AuditLogDto.builder()
            .id(UUID.randomUUID())
            .actorUserId("other-domain-caller")
            .actorIp("203.0.113.42")
            .action("CROSS_FEATURE_WRITE")
            .resourceType("payment")
            .resourceId("pay-1")
            .build();

        service.record(dto);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getActorIp())
            .as("AuditLogService.record(AuditLogDto) must redact actorIp through the SAME "
                + "AuditLogPiiRedactor the @Audited aspect uses (S2.AUDIT-PII.XB) — the "
                + "published cross-feature write port must not leak a raw IP into storage, "
                + "which AuditLogResponse.from() would then serve unmasked to the FE")
            .isEqualTo("203.0.113.xxx");
    }
}
