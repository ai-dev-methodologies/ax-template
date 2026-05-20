package com.ax.template.authblueprint.auditlog;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test-only fixture wiring an {@code @Audited} service so the aspect can be
 * exercised by integration tests without depending on a real domain service.
 *
 * <p>Imported into individual audit-log tests via {@code @Import(AuditTestFixture.class)}.
 */
@TestConfiguration
public class AuditTestFixture {

    @Bean
    public AuditedSampleService auditedSampleService() {
        return new AuditedSampleService();
    }

    /** Trivial @Audited service used to exercise the aspect. */
    public static class AuditedSampleService {

        @Audited(action = "CREATE", resourceType = "sample")
        public String create(@ResourceId String id) {
            return "created:" + id;
        }

        @Audited(action = "DELETE", resourceType = "sample")
        public void boom(@ResourceId String id) {
            throw new IllegalStateException("intentional failure for AUDIT-RECORD-001 negative path");
        }
    }
}
