package com.ax.template.authblueprint.auditlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * AUDIT-RECORD-003 — audit persistence failure is non-blocking.
 * <p>
 * Mocks {@link AuditLogService} to throw on every {@code record()} call; the
 * caller's business method must still complete.
 */
@SpringBootTest
@Import(AuditTestFixture.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditLogRecordNonBlockingTest {

    @Autowired AuditTestFixture.AuditedSampleService sampleService;

    @MockBean AuditLogService auditLogService;

    @BeforeEach
    void setup() {
        doThrow(new DataAccessResourceFailureException("simulated DB outage"))
            .when(auditLogService).record(any(AuditLog.class));
    }

    @Test
    @Tag("AUDIT_LOG")
    @Tag("AUDIT-RECORD-003")
    @WithMockUser(username = "bob-record-003", roles = {"MEMBER"})
    void record_003_persistFailureDoesNotBlockBusinessMethod() {
        String result = sampleService.create("res-record-003");

        assertThat(result)
            .as("Business method must complete even if audit-log save fails (AUDIT-RECORD-003)")
            .isEqualTo("created:res-record-003");
    }
}
