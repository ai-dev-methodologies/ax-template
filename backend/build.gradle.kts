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

tasks.register<Exec>("specRefGuard") {
    workingDir = rootDir.parentFile
    commandLine = listOf("bash", "practices/evals/spec_ref_guard.sh")
}

tasks.register<Exec>("evalPractices") {
    dependsOn("specRefGuard")
    workingDir = rootDir.parentFile
    commandLine = listOf("bash", "practices/evals/run.sh")
}
