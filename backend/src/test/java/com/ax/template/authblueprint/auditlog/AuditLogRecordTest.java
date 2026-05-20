package com.ax.template.authblueprint.auditlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.security.test.context.support.WithMockUser;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RECORD family — RECORD-001 (positive) + RECORD-002 (immutability).
 * RECORD-003 (non-blocking failure) lives in {@link AuditLogRecordNonBlockingTest}
 * to keep mock context isolation cleaner.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AuditTestFixture.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditLogRecordTest {

    @LocalServerPort int port;

    @Autowired AuditTestFixture.AuditedSampleService sampleService;
    @Autowired AuditLogRepository repository;

    @BeforeEach
    void setup() {
        AuditLogTestSupport.useRandomPort(port);
    }

    @Test
    @Tag("AUDIT_LOG")
    @Tag("AUDIT-RECORD-001")
    @WithMockUser(username = "alice-record-001", roles = {"MEMBER"})
    void record_001_aspectPersistsEntryOnAuditedMethodCall() {
        long before = repository.count();
        String result = sampleService.create("res-record-001");
        assertThat(result).isEqualTo("created:res-record-001");

        long after = repository.count();
        assertThat(after - before)
            .as("@Audited method call must persist exactly one AuditLog row")
            .isEqualTo(1);

        AuditLog latest = newestEntry();
        assertThat(latest.getAction()).isEqualTo("CREATE");
        assertThat(latest.getResourceType()).isEqualTo("sample");
        assertThat(latest.getResourceId()).isEqualTo("res-record-001");
        assertThat(latest.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(latest.getTimestamp()).isNotNull();
        assertThat(latest.getActorUserId()).isEqualTo("alice-record-001");
    }

    @Test
    @Tag("AUDIT_LOG")
    @Tag("AUDIT-RECORD-002")
    void record_002_entityIsImmutableAndRepositoryExposesNoDelete() {
        // All AuditLog @Column annotations must declare updatable=false.
        var fields = AuditLog.class.getDeclaredFields();
        long mutableCount = java.util.Arrays.stream(fields)
            .filter(f -> !java.lang.reflect.Modifier.isStatic(f.getModifiers()))
            .filter(f -> f.isAnnotationPresent(jakarta.persistence.Column.class))
            .filter(f -> f.getAnnotation(jakarta.persistence.Column.class).updatable())
            .count();
        assertThat(mutableCount)
            .as("All @Column fields on AuditLog must be updatable=false (AUDIT-RECORD-002)")
            .isZero();

        // Repository must NOT expose deleteById / deleteAll to user-facing callers.
        // The single bulk-delete path is the named query used by the retention job.
        Method[] methods = AuditLogRepository.class.getDeclaredMethods();
        boolean hasDeleteById = false;
        boolean hasDeleteAll = false;
        for (Method m : methods) {
            if (m.getName().equals("deleteById")) hasDeleteById = true;
            if (m.getName().equals("deleteAll")) hasDeleteAll = true;
        }
        assertThat(hasDeleteById)
            .as("AuditLogRepository must NOT expose deleteById")
            .isFalse();
        assertThat(hasDeleteAll)
            .as("AuditLogRepository must NOT expose deleteAll")
            .isFalse();
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private AuditLog newestEntry() {
        Page<AuditLog> page = repository.findAll(
            (Specification<AuditLog>) (root, q, cb) -> cb.isNotNull(root.get("id")),
            org.springframework.data.domain.PageRequest.of(
                0, 1,
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.DESC, "timestamp"))
        );
        return page.getContent().get(0);
    }
}
