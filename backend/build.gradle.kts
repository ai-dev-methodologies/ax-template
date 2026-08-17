plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    // PIT mutation testing — backs the non-vacuity / hollow-test enforcement spine
    // (METHODOLOGY.md "Non-Vacuity / Hollow-Test Enforcement"). Scoped, parameterized
    // runs are driven by practices/evals/vacuity_class_proof_guard.sh.
    //
    // BACKLOG P2-88 — 1.15.0 was the HARD BLOCKER on promoting this build to Gradle 9: it
    // reads the `ReportingExtension.baseDir` property, which Gradle 9 removed, so the build
    // died at plugin-APPLY time ("Could not get unknown property 'baseDir'") before any task
    // could run. Measured on the 8.14.5 wrapper with `--warning-mode all`, that deprecation
    // was the ONLY Gradle-9 incompatibility this whole build script had.
    // 1.19.0 (2026-03-29) is the fix — its release notes list "Initial support for Gradle 9"
    // and it emits ZERO deprecation warnings on 8.14.5. It is published to the Gradle Plugin
    // Portal only (Maven Central still stops at 1.15.0), which the `plugins {}` block
    // resolves by default.
    id("info.solidsoft.pitest") version "1.19.0"
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
    // Jackson 3 (tools.jackson) — Boot 4's default. P1-63 closed the deprecated
    // spring-boot-jackson2 bridge that the initial SB4 migration used for behavior
    // preservation: all SPI subclasses (payment/MoneyDeserializer, requestvalidation/
    // StrictNumericDeserializer, secretsmanagement/SecretValueSerializer) and every
    // ObjectMapper site now target tools.jackson.*, so the plain starter suffices.
    // java.time support is built into Jackson 3 databind (no separate jsr310 module).
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    // RestTemplateBuilder moved out of spring-boot-starter-web into its own module in Boot 4
    // (org.springframework.boot.restclient.RestTemplateBuilder, see webhook/WebhookHttpClient.java).
    implementation("org.springframework.boot:spring-boot-restclient")
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
    implementation("org.apache.poi:poi-ooxml:5.5.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    // Boot 4 split @AutoConfigureMockMvc / @DataJpaTest out of spring-boot-test-autoconfigure
    // into dedicated per-technology modules — no longer transitively pulled by
    // spring-boot-starter-test. Bare (non-starter) artifacts avoid pulling in
    // spring-boot-starter-jackson-test, which would reintroduce the Jackson 2/3 split-brain
    // this dependencies block otherwise avoids (see spring-boot-jackson2 above).
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-data-jpa-test")
    // rest-assured is no longer version-managed by the Spring Boot 4.1 BOM
    // (P0-27 Fix 1) — pin explicitly. rest-assured 6.0.1 (2026-07-10) ships native
    // Jackson 3 (tools.jackson) + Groovy 5 + Spring 7 support, which retired both the
    // test-scope Jackson-2 client shim and the Groovy 4.0.22 dependencySubstitution block
    // the initial SB4 migration needed for 5.x. The CLIENT-side `.body(pojo/map)` serializer
    // now sees Jackson 3 directly, so the whole application + test classpath is Jackson-3-only.
    testImplementation("io.rest-assured:rest-assured:6.0.1")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")

    // PIT JUnit 5 support. Must be on the test classpath (not only PIT's tool classpath) so
    // the mutation minion can DISCOVER + RUN the Jupiter tests when measuring coverage —
    // without it pitest reports "Ran 0 tests" / NO_COVERAGE. The solidsoft plugin auto-detects
    // it here and forwards it to PIT. Pinned to 1.2.1 (compatible with the PIT 1.15.2 pinned below).
    testImplementation("org.pitest:pitest-junit5-plugin:1.2.1")

    // BACKLOG P2-88 — the junit-platform-launcher the PIT minion loads MUST be the same
    // JUnit Platform generation as the Jupiter engine on the test runtime, or the minion dies
    // instantly ("Coverage generation minion exited abnormally! (UNKNOWN_ERROR)") right after
    // "Sent tests to minion", with no other diagnostic.
    // gradle-pitest-plugin 1.15.0 added the launcher itself (`addJUnitPlatformLauncher`) and
    // happened to resolve the Boot-4.1-managed 6.0.3. 1.19.0 changed that resolution to a
    // DETACHED configuration (upstream #390) which does NOT see this project's Spring Boot BOM
    // and resolved 1.10.2 instead — against a junit-jupiter-engine 6.0.3 runtime. That version
    // split was measured directly by diffing PIT's own `classPathElements` between the two
    // plugin versions: the ONLY difference in 181 entries was
    // junit-platform-launcher 6.0.3 (1.15.0, works) vs 1.10.2 (1.19.0, minion dies).
    // Fix: turn the plugin's auto-add OFF (see `addJUnitPlatformLauncher` in the pitest block)
    // and declare the launcher here, where the Boot BOM version-manages it — so it can never
    // drift from the engine again.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    // BACKLOG P2-86 — Gradle 9 latent vacuous-green. Every `register<Test>(...)` task below
    // (116 of them) relies on the `java` plugin's convention wiring of testClassesDirs/classpath
    // onto Test tasks. That convention wiring holds on Gradle 8.x (the current wrapper — see
    // gradle/wrapper/gradle-wrapper.properties) but is NOT applied to manually `register`-ed
    // Test tasks on Gradle 9 (confirmed downstream, GH #78): without an explicit assignment,
    // every `test{Domain}` task silently discovers ZERO test classes and reports
    // "BUILD SUCCESSFUL" — exactly the vacuous-green failure mode this catalog's guards exist
    // to prevent, except here it would be in our OWN harness. Assign explicitly so the wiring
    // does not depend on convention-application timing/version at all, on 8.x or 9.x alike.
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform()
    // The PRACTICES suite runs many @SpringBootTest/@DataJpaTest contexts alongside several
    // whole-tree ArchUnit imports (layering, no-cycle, DDD decomposition). The default test
    // fork heap is too small for that combined footprint under machine load and OOMs
    // non-deterministically (favoriteRepository/Persistence* "Java heap space"). Pin a fork
    // heap so the suite is deterministic. Raised 2g -> 4g together with the ContextCache
    // sizing below: removing the LRU thrash means more contexts stay resident at once.
    maxHeapSize = "4g"

    // ---- R22 ContextCache: ROOT-CAUSE fix (blanket), replacing per-class @DirtiesContext ----
    //
    // Spring's TestContext ContextCache defaults to maxSize = 32.
    //
    // What is PROVEN: eviction was actually occurring. The observed failure mode is a class
    // whose @LocalServerPort points at an already-closed Tomcat, so every request in it fails
    // uniformly (IllegalStateException / NoHttpResponseException) while the same class passes
    // in isolation. Eviction can only happen once live distinct cache keys exceed the ceiling,
    // therefore this tree's distinct-key count is > 32. That inequality — not any particular
    // number — is what justifies raising the ceiling. The flake moved between classes as cache
    // pressure shifted, which is why per-class @DirtiesContext kept "fixing" it and it kept
    // coming back somewhere else (BillingFlowIT, FeatureFlagFlowIT,
    // PageEnvelopeCatalogSweepTest, GlobalProblemDetailAdviceTest).
    //
    // What is NOT measured: the exact distinct-key count. The census below counts CLASSES
    // BEARING key-forking annotations, which is an indicator of key diversity, not the key
    // count itself (several classes can share one key; one class can be counted twice):
    //   191 @SpringBootTest classes; 57 carry a key-forking annotation
    //   (properties= 19, @TestPropertySource 9, @AutoConfigureMockMvc 14, @Import 8,
    //    @TestConfiguration 5, @MockitoBean 2).
    // Treat it as motivation for the ceiling's magnitude, not as a measurement.
    //
    // maxSize is an eviction CEILING, not a preallocation: raising it to 128 does not create
    // 128 contexts (DefaultContextCache sizes its maps independently of the configured
    // ceiling), it only stops eviction below the number of contexts the run genuinely needs.
    // Cost is bounded by the contexts that actually get built (hence the 2g -> 4g heap); it is
    // strictly cheaper than the alternative of annotating all 191 classes @DirtiesContext,
    // which would force 191 cold context boots and disable caching outright.
    //
    // This is ax-template's OWN test-harness setting. It imposes nothing on fork-receivers'
    // CI policy — that boundary (CLAUDE.md) is about merge gates, not about whether our own
    // suite is deterministic.
    //
    // BACKLOG P3-84 (surveillance, 2026-07-29). Raising an eviction ceiling means contexts
    // are retained LONGER, so a test that unfairly depends on cache state an earlier test
    // left behind would stop being exposed. No such test is known — this is a watch item,
    // and the probe for it is to re-run the aggregate with the OLD ceiling and see whether
    // anything new fails (a new failure under a lower ceiling is real inter-test state
    // leakage, NOT flake — fix the test, never the ceiling).
    //
    // The ceiling is therefore overridable. It has to be: a bare
    // `-Dspring.test.context.cache.maxSize=32` on the gradle command line does NOT reach the
    // forked test JVM (it sets the property on the DAEMON), and the line below would pin 128
    // regardless — the probe would silently run the ordinary suite and "pass" while proving
    // nothing. `-P` (not `-D`) because a project property is per-invocation, whereas a
    // daemon `-D` persists into later runs that reuse the same daemon and would silently run
    // the whole suite at the probe's ceiling. Same findProperty idiom as the pit.* scoping
    // below. Default is unchanged at 128.
    systemProperty("spring.test.context.cache.maxSize",
        (project.findProperty("contextCacheMaxSize") as String?) ?: "128")
}

// PIT mutation testing — the mechanical backstop for non-vacuity. A scoped run mutates a
// single gate method with a single mutator and re-runs only the fast unit/mock + ViolationProof
// slice; if the catalog test does not KILL the mutant the gate is hollow (vacuously passing).
//
// All scoping is parameterized via -P properties so vacuity_class_proof_guard.sh can pin one
// method × one mutator × one test class per spec item:
//   ./gradlew pitest -Ppit.targetClasses=<FQCN> -Ppit.targetTests=<glob> -Ppit.mutators=TRUE_RETURNS
//
// pitest-junit5-plugin (declared as a testImplementation dependency above) is what lets the
// mutation minion DISCOVER + RUN the Jupiter tests. @SpringBootTest IT/E2E/Compliance classes
// are excluded so a scoped run never boots a Spring context — the slice stays fast + deterministic.
pitest {
    // pitest-junit5-plugin is provided via the testImplementation dependency above; the
    // solidsoft plugin auto-detects it on the test classpath and forwards it to PIT.
    pitestVersion.set("1.15.2")

    // BACKLOG P2-88 — see the junit-platform-launcher testRuntimeOnly declaration above.
    // The plugin's own launcher auto-add resolves OUTSIDE this project's Spring Boot BOM
    // (detached configuration, upstream #390) and would inject JUnit Platform 1.10.2 against
    // a 6.0.3 engine, killing the coverage minion. Off here; declared there instead.
    // Do NOT re-enable without re-checking `classPathElements` for the launcher version.
    addJUnitPlatformLauncher.set(false)

    // CRITICAL: PIT forks its coverage/mutation minion with the JVM that runs Gradle, which
    // may be older than the project's Java toolchain. This project compiles to Java 21
    // (class file 65.0); a Java 17 minion silently fails with "has been compiled by a more
    // recent version of the Java Runtime" → "Found 0 tests" → every mutation NO_COVERAGE.
    // Pin the minion to the SAME Java 21 toolchain launcher Gradle resolves for compilation
    // (no hardcoded path — fork-portable).
    jvmPath.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        }.get().executablePath
    )

    targetClasses.set(
        setOf(
            (project.findProperty("pit.targetClasses") as String?)
                ?: "com.ax.template.authblueprint.tokenizedsecurities.*"
        )
    )
    targetTests.set(
        setOf(
            (project.findProperty("pit.targetTests") as String?)
                ?: "com.ax.template.authblueprint.tokenizedsecurities.*ViolationProofTest"
        )
    )
    (project.findProperty("pit.mutators") as String?)?.let { m ->
        mutators.set(m.split(",").map { it.trim() }.filter { it.isNotEmpty() })
    }

    // Exclude every @SpringBootTest-bearing slice — pitest must run only the fast unit/mock
    // tests + *ViolationProofTest (no application context, no Testcontainers).
    excludedTestClasses.set(
        setOf("*IT", "*FlowIT", "*E2ETest", "*DogfoodE2ETest", "*ComplianceTest")
    )

    // Incremental analysis (the gradle-pitest-plugin spelling of pitest's withHistory):
    // reuse prior results so repeated scoped runs in the R25 loop stay fast. Disable with
    // -Ppit.noIncremental for a clean diagnostic run.
    enableDefaultIncrementalAnalysis.set(!project.hasProperty("pit.noIncremental"))
    failWhenNoMutations.set(false)
    timestampedReports.set(false)
    outputFormats.set(setOf("XML", "HTML"))
    threads.set(1)
    verbose.set(project.hasProperty("pit.verbose"))
}

// BACKLOG P2-86 — runtime (not textual/grep) assertion that every Test task in the build MODEL
// has non-empty testClassesDirs + classpath. A grep for "testClassesDirs =" only proves the
// string appears somewhere in this file; it cannot see whether a task registered inside a
// conditional branch, an afterEvaluate block, or plugin-injected configuration actually ends up
// wired at execution time. This task instead walks tasks.withType<Test>() itself — iterating
// that live TaskCollection forces Gradle to realize (create + fully configure, including any
// afterEvaluate mutation) every Test-typed task in the project, whether or not it is part of the
// invoked task graph — and inspects the resulting testClassesDirs/classpath FileCollections
// directly. This is the same class of check the java plugin's own convention wiring is supposed
// to satisfy; it exists so a future Gradle/plugin change that silently breaks that convention
// (as happened between Gradle 8.x and 9.x — see the tasks.withType<Test> comment above) is
// caught by an assertion this project owns, offline, on the current 8.x wrapper, without needing
// to actually reproduce the Gradle 9 failure locally.
tasks.register("assertTestTasksWired") {
    group = "verification"
    description = "BACKLOG P2-86 — asserts every Test task in the build model has non-empty " +
        "testClassesDirs and classpath (catches the Gradle 9 vacuous-green failure mode where " +
        "register<Test>() tasks silently discover 0 tests)."
    doLast {
        // .toList() forces realization of every Test task known to the project, not just ones
        // reachable from this task's dependency graph.
        val testTasks = tasks.withType<Test>().toList()
        val violations = testTasks.filter { it.testClassesDirs.isEmpty || it.classpath.isEmpty }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "assertTestTasksWired FAILED — the following Test tasks have empty " +
                    "testClassesDirs and/or classpath, and would silently discover ZERO tests " +
                    "(BUILD SUCCESSFUL with 0 tests run) instead of failing loudly: " +
                    violations.joinToString(", ") { it.name }
            )
        }
        println(
            "assertTestTasksWired: OK — ${testTasks.size} Test task(s) all have non-empty " +
                "testClassesDirs + classpath"
        )
    }
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

tasks.register<Test>("testAnnouncement") {
    useJUnitPlatform {
        includeTags("ANNOUNCEMENT")
    }
}

tasks.register<Test>("testDispatch") {
    useJUnitPlatform {
        includeTags("DISPATCH")
    }
}

tasks.register<Test>("testCostShare") {
    useJUnitPlatform {
        includeTags("COSTSHARE")
    }
}

tasks.register<Test>("testGovernedRecord") {
    useJUnitPlatform {
        includeTags("GOVERNEDRECORD")
    }
}

tasks.register<Test>("testTransformation") {
    useJUnitPlatform {
        includeTags("TRANSFORMATION")
    }
}

tasks.register<Test>("testReservation") {
    useJUnitPlatform {
        includeTags("RESERVATION")
    }
}

tasks.register<Test>("testRegister") {
    useJUnitPlatform {
        includeTags("REGISTER")
    }
}

tasks.register<Test>("testNetting") {
    useJUnitPlatform {
        includeTags("NETTING")
    }
}

tasks.register<Test>("testCopresence") {
    useJUnitPlatform {
        includeTags("COPRESENCE")
    }
}

tasks.register<Test>("testRecordLinkage") {
    useJUnitPlatform {
        includeTags("RECORDLINKAGE")
    }
}

tasks.register<Test>("testTrueUp") {
    useJUnitPlatform {
        includeTags("TRUEUP")
    }
}

tasks.register<Test>("testUomConversion") {
    useJUnitPlatform {
        includeTags("UOMCONVERSION")
    }
}

tasks.register<Test>("testDivisibility") {
    useJUnitPlatform {
        includeTags("DIVISIBILITY")
    }
}

tasks.register<Test>("testInputPlausibility") {
    useJUnitPlatform {
        includeTags("INPUTPLAUSIBILITY")
    }
}

tasks.register<Test>("testCalendarDeadline") {
    useJUnitPlatform {
        includeTags("CALENDARDEADLINE")
    }
}

tasks.register<Test>("testOrderQuantization") {
    useJUnitPlatform {
        includeTags("ORDERQUANTIZATION")
    }
}

tasks.register<Test>("testAccessGrant") {
    useJUnitPlatform {
        includeTags("ACCESSGRANT")
    }
}

tasks.register<Test>("testInventoryReservation") {
    useJUnitPlatform {
        includeTags("INVENTORYRESERVATION")
    }
}

tasks.register<Test>("testOrgScope") {
    useJUnitPlatform {
        includeTags("ORGSCOPE")
    }
}

tasks.register<Test>("testVarianceGate") {
    useJUnitPlatform {
        includeTags("VARIANCEGATE")
    }
}

tasks.register<Test>("testStateMutation") {
    useJUnitPlatform {
        includeTags("STATEMUTATION")
    }
}

tasks.register<Test>("testRecurringInterval") {
    useJUnitPlatform {
        includeTags("RECURRINGINTERVAL")
    }
}

tasks.register<Test>("testQueryGuard") {
    useJUnitPlatform {
        includeTags("QUERYGUARD")
    }
}

tasks.register<Test>("testSensitiveAccess") {
    useJUnitPlatform {
        includeTags("SENSITIVEACCESS")
    }
}

tasks.register<Test>("testReproducibility") {
    useJUnitPlatform {
        includeTags("REPRODUCIBILITY")
    }
}

tasks.register<Test>("testMandate") {
    useJUnitPlatform {
        includeTags("MANDATE")
    }
}

tasks.register<Test>("testValuationRun") {
    useJUnitPlatform {
        includeTags("VALUATIONRUN")
    }
}

tasks.register<Test>("testReconciliation") {
    useJUnitPlatform {
        includeTags("RECONCILIATION")
    }
}

tasks.register<Test>("testNetMetering") {
    useJUnitPlatform {
        includeTags("NETMETERING")
    }
}

tasks.register<Test>("testTimedOffer") {
    useJUnitPlatform {
        includeTags("TIMEDOFFER")
    }
}

tasks.register<Test>("testDunning") {
    useJUnitPlatform {
        includeTags("DUNNING")
    }
}

tasks.register<Test>("testSettlement") {
    useJUnitPlatform {
        includeTags("SETTLEMENT")
    }
}

tasks.register<Test>("testAuthzParity") {
    useJUnitPlatform {
        includeTags("AUTHZPARITY")
    }
}

tasks.register<Test>("testObligation") {
    useJUnitPlatform {
        includeTags("OBLIGATION")
    }
}

tasks.register<Test>("testDecisionGov") {
    useJUnitPlatform {
        includeTags("DECISIONGOV")
    }
}

tasks.register<Test>("testCommerceCatalog") {
    useJUnitPlatform {
        includeTags("COMMERCECATALOG")
    }
}

tasks.register<Test>("testCommerceOrder") {
    useJUnitPlatform {
        includeTags("COMMERCEORDER")
    }
}

tasks.register<Test>("testCommercePromotion") {
    useJUnitPlatform {
        includeTags("PROMOTION")
    }
}

tasks.register<Test>("testCommercePricing") {
    useJUnitPlatform {
        includeTags("COMMERCEPRICING")
    }
}

tasks.register<Test>("testQuorum") {
    useJUnitPlatform {
        includeTags("QUORUM")
    }
}

tasks.register<Test>("testThresholdTerminal") {
    useJUnitPlatform {
        includeTags("THRESHOLD_TERMINAL")
    }
}

tasks.register<Test>("testTokenizedSecurities") {
    useJUnitPlatform {
        includeTags("TOKENIZED_SECURITIES")
    }
}

tasks.register<Test>("testBandedPricing") {
    useJUnitPlatform {
        includeTags("BANDEDPRICE")
    }
}

tasks.register<Test>("testAsvs") {
    useJUnitPlatform {
        includeTags("ASVS")
    }
    // BACKLOG P2-49 — explicit, opt-in pass-through for the auth-me golden
    // REGENERATION command (AuthMeGoldenContractParityTest), identical to the
    // wiring testCommonAdvice carries for P2-36's validation-errors golden.
    // Absent the flag nothing is forwarded, so the regeneration test stays
    // disabled and the assertion path can never write the fixture it compares
    // against.
    System.getProperty("golden.regenerate")?.let { systemProperty("golden.regenerate", it) }
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
    // BACKLOG P2-36 — explicit, opt-in pass-through for the validation-errors
    // golden REGENERATION command (ValidationErrorsContractParityTest). Absent
    // the flag nothing is forwarded, so the regeneration test stays disabled and
    // the assertion path can never write the fixture it compares against.
    System.getProperty("golden.regenerate")?.let { systemProperty("golden.regenerate", it) }
}

tasks.register<Test>("testAllFilterSentinel") {
    useJUnitPlatform {
        includeTags("ALL_FILTER_SENTINEL")
    }
    description = "P2-35 — sibling-contract `default: ALL` / undeclared-param family (email-outbox, scheduled-task)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testCommonPrimitives") {
    useJUnitPlatform {
        includeTags(
            "COMMON_BREAK_GLASS", "COMMON_BULK_RESULT", "COMMON_CALLER_SCOPE",
            "COMMON_CONSENT", "COMMON_IDEMPOTENCY", "COMMON_PAGE_ENVELOPE",
            "COMMON_PARTICIPANT_SCOPE", "COMMON_BODY_SIZE", "COMMON_HTTP_EXTRACT",
            "OBSERVABILITY"
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

tasks.register<Test>("testApiVersioning") {
    useJUnitPlatform {
        includeTags("API_VERSIONING")
    }
    description = "Run api-versioning-l0 compliance tests (url-path version negotiation + RFC 8594 Sunset/Deprecation: 7 items / 7 families — NEGOTIATION, DEFAULT, COMPATIBILITY, DEPRECATION, MIGRATION, DISCOVERY, OBSERVABILITY)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testRatingSummary") {
    useJUnitPlatform {
        includeTags("RATING_SUMMARY")
    }
    description = "Run rating-summary domain compliance tests (derived-aggregate-consistency-l0: 3 items — CONSISTENCY, ELIGIBILITY, EMPTY)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testIdentityClaim") {
    useJUnitPlatform {
        includeTags("IDENTITY_CLAIM")
    }
    description = "Run identity-claim domain compliance tests (identity-claim-on-auth-l0: 3 items — CLAIM, IDEMPOTENT, GUARD)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testBundlePricing") {
    useJUnitPlatform {
        includeTags("BUNDLEPRICING")
    }
    description = "Run bundle-pricing domain compliance tests (bundle-pricing-l0: 4 items — ITEMSUM, FIXED, DERIVED, AUTHZ; conserving bundle/composite roll-up)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testOfferEligibility") {
    useJUnitPlatform {
        includeTags("OFFER_ELIGIBILITY")
    }
    description = "Run offer-eligibility domain compliance tests (offer-eligibility-l0: 4 families — QUALIFIER-MINQTY, SEGMENT-ELIGIBILITY, FAIL-CLOSED, AUTHZ; deterministic fail-closed applicability gate, distinct from discount math)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testTaxApplication") {
    useJUnitPlatform {
        includeTags("TAX_APPLICATION")
    }
    description = "Run tax-application domain compliance tests (tax-application-l0: 3 families — EXEMPT-SKIP, IDEMPOTENT-RECOMPUTE, AUTHZ; exempt-skip + idempotent single-record tax convergence with an injected rate)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testCurrencyArithmetic") {
    useJUnitPlatform {
        includeTags("CURRENCY_ARITHMETIC")
    }
    description = "Run currency-arithmetic domain compliance tests (currency-arithmetic-l0: 4 families — FAILCLOSED-ADD, FAILCLOSED-SUBTRACT, SAMECCY-OK, EXPLICIT-CONVERT; a currency-tagged value object whose arithmetic is fail-closed across currencies absent an explicit recorded conversion)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testThresholdFiling") {
    useJUnitPlatform {
        includeTags("THRESHOLD_FILING")
    }
    description = "Run threshold-filing domain compliance tests (threshold-filing-obligation-l0: TFO-TRIGGER/FILING-RECORD/DEADLINE — crossing binds an immutable filing obligation exactly-once same-tx, 30-day statutory window, ack-only closure)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testTieredAuthority") {
    useJUnitPlatform {
        includeTags("TIEREDAUTHORITY")
    }
    description = "Run amount-tiered-authority domain compliance tests (amount-tiered-authority-l0: ATA-TIER/BOUNDARY/SNAPSHOT — 전결 규정: half-open amount bands tile without gap/overlap, insufficient authority fails closed 403, decision records snapshot the tier-table version immutably)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testAppealIndependence") {
    useJUnitPlatform {
        includeTags("APPEALINDEPENDENCE")
    }
    description = "Run appeal-decider-independence domain compliance tests (appeal-decider-independence-l0: APPEAL-DISTINCT/CHAIN/OUTCOME — appeal decider differs from every prior decider in the chain (@Check backstop), one appeal per level, outcomes append-only)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testPiecewiseDeadband") {
    useJUnitPlatform {
        includeTags("PIECEWISE_DEADBAND")
    }
    description = "Run piecewise-deadband domain compliance tests (piecewise-deadband-l0: PWDB-SEGMENT/EVAL/IMMUTABLE — segments tile the obligation domain without gap/overlap, per-segment deadband evaluation deterministic, evaluations append-only)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testCountBudget") {
    useJUnitPlatform {
        includeTags("COUNT_BUDGET")
    }
    description = "Run periodic-count-budget domain compliance tests (periodic-count-budget-l0: PCB-CONSUME/RESET/AUDIT/CAP — persisted per-subject count budget, row-lock serialized consume past cap fails closed, calendar-aligned audited reset)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testTieredEligibility") {
    useJUnitPlatform {
        includeTags("TIERED_ELIGIBILITY")
    }
    description = "Run tiered-eligibility domain compliance tests (tiered-eligibility-l0: TIER-LADDER/MONOTONE/DERIVE/TERMINAL — N ordered degradation tiers driven by count thresholds, monotone descent, fail-closed derived reads, explicit audited restore only)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testEventIngest") {
    useJUnitPlatform {
        includeTags("EVENTINGEST")
    }
    description = "Run event-ingest domain compliance tests (monotonic-event-ingest-l0 realized: watermark holds latest EVENT time, stale events ack'd not errored, idempotent apply, bounded drop counters, + INGEST-CAPTURE-001 occurred<=captured<=recorded with server-assigned recorded_at)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testExceptionGate") {
    useJUnitPlatform {
        includeTags("EXCEPTIONGATE")
    }
    description = "Run orthogonal-exception-gate domain compliance tests (orthogonal-exception-gate-l0, generalized from DSR restriction gate: exception dimension independent of primary lifecycle, gated ops fail closed 423 pre-mutation, audited raise/lift)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testAdditiveFacts") {
    useJUnitPlatform {
        includeTags("ADDITIVEFACTS")
    }
    description = "Run additive-fact-ledger domain compliance tests (additive-fact-ledger-l0: period aggregate == sum of append-only facts, late facts post forward as deltas against frozen closed periods with conservation, duplicate delivery accumulates once)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testGeoQuery") {
    useJUnitPlatform {
        includeTags("GEOQUERY")
    }
    description = "Run geo-bounded-query domain compliance tests (geo-bounded-query-l0, honest degraded subset: indexed bounding-box prefilter + exact haversine postfilter, bounded inputs 422, deterministic distance-then-id ordering; PostGIS/GiST explicitly NOT claimed — GEO-GIST-REVIEW-001 is review-only)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testRouteLegs") {
    useJUnitPlatform {
        includeTags("ROUTELEGS")
    }
    description = "Run route-leg-contiguity domain compliance tests (route-leg-contiguity-l0: leg N dest == leg N+1 origin at every mutation, no ordinal gap/overlap, insert/remove/replace re-validates both neighbors atomically, concurrent mutation serialized)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testGeofence") {
    useJUnitPlatform {
        includeTags("GEOFENCE")
    }
    description = "Run geofence-transition domain compliance tests (geofence-transition-l0: min-dwell confirm-after-delay, flap suppression commits zero transitions, committed transitions immutable with dual observed/confirmed timestamps; event-time driven, no wall-clock)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testBilateralHandoff") {
    useJUnitPlatform {
        includeTags("BILATERALHANDOFF")
    }
    description = "Run bilateral-handoff domain compliance tests (bilateral-handoff-l0: two NAMED parties must independently confirm, either declining voids terminally, caller-party binding fail-closed 403, custody flips exactly-once at second confirm under concurrency)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testFacetCount") {
    useJUnitPlatform {
        includeTags("FACETCOUNT")
    }
    description = "Run facet-count domain compliance tests (facet-count-l0: per-field bucket counts scoped to the caller-visible filtered query, allowlisted facet fields fail closed 422, top-K bounded cardinality with explicit otherCount remainder)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testDerivedStatement") {
    useJUnitPlatform {
        includeTags("DERIVEDSTATEMENT")
    }
    description = "Run derived-statement domain compliance tests (derived-statement-l0: statement key = content hash of (subject, period, basis) — retry-safe by construction without client idempotency headers, changed basis appends a version, content immutable)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testSaturatingBalance") {
    useJUnitPlatform {
        includeTags("SATURATINGBALANCE")
    }
    description = "Run saturating-balance domain compliance tests (saturating-balance-l0: accrual clamps at ceiling, debit clamps at floor 0, requested+applied amounts ledgered append-only with conservation, 8-thread concurrent accrual converges exactly on the cap)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testWithholdingSplit") {
    useJUnitPlatform {
        includeTags("WITHHOLDING_SPLIT")
    }
    description = "Run withholding-split domain compliance tests (withholding-split-l0: gross posting splits into withholding+net legs same-tx with sum conservation to the cent, rate snapshot immutable on the posting, remittance run idempotent per period, corrections via reversing entries only)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testCashInLieu") {
    useJUnitPlatform {
        includeTags("CASH_IN_LIEU")
    }
    description = "Run cash-in-lieu domain compliance tests (cash-in-lieu-l0: fractional entitlement remainder converts to cash at an immutable rate snapshot, units+cash reconstructs the entitlement exactly, allocation idempotent per (subject,event))"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testMeceClassification") {
    useJUnitPlatform {
        includeTags("MECE_CLASSIFICATION")
    }
    description = "Run mece-classification domain compliance tests (mece-classification-l0: exactly-one category per (item,scheme) with UNIQUE backstop, schemes must define a residual bucket — no-rule-match lands there never fails open, reclassification is an append-only move history)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testIntervalExclusivity") {
    useJUnitPlatform {
        includeTags("INTERVAL_EXCLUSIVITY")
    }
    description = "Run interval-exclusivity domain compliance tests (interval-exclusivity-l0: half-open [start,end) overlap rejection with back-to-back legal, 2-thread concurrent booking exactly-one-wins via resource row lock — H2 GiST EXCLUDE honestly not claimed, shrink free / extend re-validates / cancel frees immediately)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testDuplicateSubmission") {
    useJUnitPlatform {
        includeTags("DUPLICATESUBMISSION")
    }
    description = "Run duplicate-submission domain compliance tests (duplicate-submission-key-l0: intake-time natural same-loss key exact match 409 with active_key UNIQUE backstop, fuzzy near-match flags REVIEW with linkage never silently accepts/rejects, withdrawal releases the key)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testRangeOwnership") {
    useJUnitPlatform {
        includeTags("RANGEOWNERSHIP")
    }
    description = "Run range-ownership domain compliance tests (range-ownership-l0: identifier assignment only inside the owner's block, half-open non-overlapping blocks with registry-lock serialized registration, porting = append-only ownership events with current owner derive-on-read)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testProvisionalAttestation") {
    useJUnitPlatform {
        includeTags("PROVISIONALATTESTATION")
    }
    description = "Run provisional-attestation domain compliance tests (provisional-attestation-l0: PROVISIONAL→ATTESTED 2-state lifecycle, attestor must differ from author with DB backstop, attested content frozen with content-hash binding, provisional never passes as attested)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testCorrectionRefire") {
    useJUnitPlatform {
        includeTags("CORRECTIONREFIRE")
    }
    description = "Run correction-refire domain compliance tests (correction-refire-l0: corrections append-only supersession, a closed ack loop re-opens against the corrected version same-tx, identical-content republish does not re-fire, per-version ack state independent)"
    group = "verification"
    shouldRunAfter("test")
}

tasks.register<Test>("testSignedArtifact") {
    useJUnitPlatform {
        includeTags("SIGNEDARTIFACT")
    }
    description = "Run signed-artifact domain compliance tests (signed-artifact-l0 realized: JWS asymmetric signing over content hash, verification validates signature + signer key id, tampered content fails verification, algorithm allowlist)"
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
