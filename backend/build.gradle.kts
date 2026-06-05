plugins {
    java
    id("org.springframework.boot") version "3.2.12"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.ax.template"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

springBoot {
    buildInfo()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    // H2 is on the implementation classpath (not runtimeOnly) so the Payment blueprint
    // can register an H2 Java-trigger that enforces append-only semantics on
    // payment_events (PAYMENT-RECON-001). Production Postgres replaces this with a
    // CREATE TRIGGER ... EXECUTE FUNCTION raise_immutable migration.
    //
    // Pinned to 2.3.232+ for PostgreSQL-compatible JSON operator support (->>).
    // Required by PaymentReconciliationTest's payload->>'amount' query.
    implementation("com.h2database:h2:2.3.232")

    // CSV import — PRACTICES-INTEG-002 (chunked-import-required-when-rowcount-gt-1000)
    implementation("com.opencsv:opencsv:5.9")

    // XLSX generation — report-export domain (EXPORT-FORMAT-001 / EXPORT-INJECT-002).
    // SXSSF streaming workbook keeps memory bounded for large exports (manifest:
    // blueprints/report-export-manifest.yaml#xlsx.window_size).
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<Test>("testCrud") {
    useJUnitPlatform {
        includeTags("CRUD")
    }
}

tasks.register<Test>("testCaching") {
    useJUnitPlatform {
        includeTags("CACHING")
    }
}

tasks.register<Test>("testPagination") {
    useJUnitPlatform {
        includeTags("PAGINATION")
    }
}

tasks.register<Test>("testProblemDetails") {
    useJUnitPlatform {
        includeTags("PROBLEM_DETAILS")
    }
}

tasks.register<Test>("testRequestValidation") {
    useJUnitPlatform {
        includeTags("REQUEST_VALIDATION")
    }
}

tasks.register<Test>("testIdempotency") {
    useJUnitPlatform {
        includeTags("IDEMPOTENCY")
    }
}

tasks.register<Test>("testOptimisticLocking") {
    useJUnitPlatform {
        includeTags("OPTLOCK")
    }
}

tasks.register<Test>("testSecrets") {
    useJUnitPlatform {
        includeTags("SECRETS")
    }
}

tasks.register<Test>("testSoftDelete") {
    useJUnitPlatform {
        includeTags("SOFT_DELETE")
    }
}

tasks.register<Test>("testAsvs") {
    useJUnitPlatform {
        includeTags("ASVS")
    }
}

tasks.register<Test>("testPractices") {
    useJUnitPlatform {
        includeTags("PRACTICES")
    }
}

tasks.register<Test>("testRateLimit") {
    useJUnitPlatform {
        includeTags("RATELIMIT")
    }
}

tasks.register<Test>("testPayment") {
    useJUnitPlatform {
        includeTags("PAYMENT")
    }
    description = "Run Payment blueprint compliance tests (29 items / 9 families)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testNotification") {
    useJUnitPlatform {
        includeTags("NOTIFICATION")
    }
    description = "Run Notification domain compliance tests (11 items / 5 families)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testPortability") {
    useJUnitPlatform {
        includeTags("PORTABILITY")
    }
    // Fixtures must be built first: practices/evals/portability/run.sh --full
    // If a fixture's classes dir is missing, the test is skipped via JUnit Assumptions
    // (not failed) — see PortabilityFixtures.importFixture().
}

tasks.register<Test>("testIntegration") {
    useJUnitPlatform {
        includeTags("INTEGRATION")
    }
    description = "Run SP24 integration tests (WebhookReceiverIT, CsvImportChunkedIT)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testSearch") {
    useJUnitPlatform {
        includeTags("SEARCH")
    }
    description = "Run SP26 search domain compliance tests (SEARCH-AUTHZ, SEARCH-QUERY, SEARCH-INDEX, SEARCH-BACKEND)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testAuditLog") {
    useJUnitPlatform {
        includeTags("AUDIT_LOG")
    }
    description = "Run audit-log domain compliance tests (R14: 11 items / 5 families — RECORD, LIST, RETENTION, EXPORT, PII)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testFileStorage") {
    useJUnitPlatform {
        includeTags("FILE_STORAGE")
    }
    description = "Run file-storage domain compliance tests (SP18: upload/list/download/delete + presigned URL)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testFeatureFlags") {
    useJUnitPlatform {
        includeTags("FEATURE_FLAGS")
    }
    description = "Run feature-flags domain compliance tests (SP28: runtime toggles, admin CRUD)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testBilling") {
    useJUnitPlatform {
        includeTags("BILLING")
    }
    description = "Run billing domain compliance tests (SP30: subscription lifecycle, plan management, invoice listing)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testIdentityVerification") {
    useJUnitPlatform {
        includeTags("IDENTITY_VERIFICATION")
    }
    description = "Run identity-verification domain compliance tests (SP31: KISA 본인인증 CI/DI token storage)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testScheduledTask") {
    useJUnitPlatform {
        includeTags("SCHEDULED_TASK")
    }
    description = "Run scheduled-task domain compliance tests (R18: 5 items / 4 families — REGISTER, LOCK, EXECUTE, IDEMPOTENCY)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testWebhook") {
    useJUnitPlatform {
        includeTags("WEBHOOK")
    }
    description = "Run webhook domain compliance tests (R19: 10 items / 5 families — EMIT, SIGN, RETRY, DEAD-LETTER, CIRCUIT-BREAKER, IDEMPOTENCY)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testEcommerce") {
    useJUnitPlatform {
        includeTags("ECOMMERCE")
    }
    description = "Run e-commerce capstone end-to-end tests (R23: recipes/e-commerce/RECIPE.md — composes crud + payment + notification + audit-log + search)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testReportExport") {
    useJUnitPlatform {
        includeTags("REPORT_EXPORT")
    }
    description = "Run report-export domain compliance tests (R29: 11 items / 4 families — AUTHZ, LIFECYCLE, INJECT (CWE-1236), FORMAT)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testApiKey") {
    useJUnitPlatform {
        includeTags("API_KEY")
    }
    description = "Run api-key domain compliance tests (R30: 12 items / 4 families — AUTHN, STORAGE, LIFECYCLE, AUTHZ)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testApprovalWorkflow") {
    useJUnitPlatform {
        includeTags("WORKFLOW")
    }
    description = "Run approval-workflow domain compliance tests (R31: 12 items / 4 families — LIFECYCLE, AUTHZ, STEP, QUERY)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testTagCategorization") {
    useJUnitPlatform {
        includeTags("TAGGING")
    }
    description = "Run tag-categorization domain compliance tests (R32: 12 items / 4 families — CRUD, ATTACHMENT, HIERARCHY, AUTHZ)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testSessionManagement") {
    useJUnitPlatform {
        includeTags("SESSION")
    }
    description = "Run session-management domain compliance tests (R33: 12 items / 4 families — LIFECYCLE, REVOCATION, INTROSPECTION, AUTHZ)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testFavorites") {
    useJUnitPlatform {
        includeTags("FAVORITES")
    }
    description = "Run favorites-bookmarks domain compliance tests (R34: 12 items / 4 families — CRUD, QUERY, AUTHZ, VALIDATION)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testActivityFeed") {
    useJUnitPlatform {
        includeTags("ACTIVITY")
    }
    description = "Run activity-feed domain compliance tests (R35: 12 items / 4 families — PUBLISH, READ, MARK, AUTHZ)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testCommentThread") {
    useJUnitPlatform {
        includeTags("COMMENT")
    }
    description = "Run comment-thread domain compliance tests (R36: 12 items / 4 families — CRUD, THREAD, AUTHZ, HISTORY)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testEmailOutbox") {
    useJUnitPlatform {
        includeTags("EMAIL")
    }
    description = "Run email-outbox domain compliance tests (R51: 8 items / 4 families — QUEUE, SEND, RETRY, TEMPLATE, ADMIN)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testDsr") {
    useJUnitPlatform {
        includeTags("DSR")
    }
    description = "Run data-subject-rights domain compliance tests (IMW6: 7 items / 7 families — ACCESS, RECTIFY, ERASURE, PORTABILITY, RESTRICT, SLA, OBSERVABILITY)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testCommonAdvice") {
    useJUnitPlatform {
        includeTags("COMMON_ADVICE")
    }
    description = "Run shared RFC 9457 fallback advice tests (IMW1-B: common.GlobalProblemDetailAdvice — @Valid/415/405/malformed → problem+json)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testCommonPrimitives") {
    useJUnitPlatform {
        includeTags(
            "COMMON_BREAK_GLASS", "COMMON_BULK_RESULT", "COMMON_CALLER_SCOPE",
            "COMMON_CONSENT", "COMMON_IDEMPOTENCY", "COMMON_PAGE_ENVELOPE",
            "COMMON_PARTICIPANT_SCOPE", "OBSERVABILITY"
        )
    }
    description = "Run the cross-cutting backend tests not owned by a single domain vertical: common/* primitives (BreakGlass, BulkResult, CallerScope, Consent, Idempotency, PageEnvelope, ParticipantScope) + the MDC correlation-id observability IT. Closes the @Tag→per-domain-task hard-gate escape (2026-06-01 audit)."
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testI18n") {
    useJUnitPlatform {
        includeTags("I18N")
    }
    description = "Run i18n-policy domain compliance tests (backend_only: 5 testable items / 4 families — LOCALE-NEG, MESSAGE-SOURCE, TIMEZONE, FORMATTING)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testRealtime") {
    useJUnitPlatform {
        includeTags("REALTIME")
    }
    description = "Run realtime-policy domain compliance tests (backend_only, SSE via MVC SseEmitter: 6 testable items / 5 families — CHANNEL-AUTH x2, FANOUT, BACKPRESSURE, RECONNECT, OBSERVABILITY; PROTOCOL is review-only)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testWebhookSigning") {
    useJUnitPlatform {
        includeTags("WEBHOOK_SIGNING")
    }
    description = "Run webhook-signing-l0 compliance tests (INBOUND HMAC-SHA256 signature verification: 7 items / 7 families — HMAC, TIMESTAMP, REPLAY, SECRET, HEADER, VERIFY, OBSERVABILITY)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Exec>("specRefGuard") {
    workingDir = rootDir.parentFile
    commandLine = listOf("bash", "practices/evals/spec_ref_guard.sh")
}

tasks.register<Exec>("evalPractices") {
    dependsOn("specRefGuard")
    workingDir = rootDir.parentFile
    commandLine = listOf("bash", "practices/evals/run.sh")
}
