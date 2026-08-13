---
name: ax-install-java-enforcement
description: >
  Installs a small ArchUnit-based `testPractices` gate in a downstream Spring
  project for the minority of `practices/` Java rules that are statically
  checkable (layer-boundary direction, no-cyclic-package, DTO-as-record). Most
  `practices/rules/*.md` entries are `verification.type: review` and are NOT
  affected by this skill — they stay `ax-practices`'s job. Use when a user asks
  to actually enforce (not just review against) the Java catalog, or when
  `ax-practices` reports a rule as "not installed" and the rule is one of the
  three ArchUnit-portable checks.
metadata:
  priority: 2
  tier: 1
  axis: consumption-channel
  docs:
    - "practices/INDEX.md"
    - "skills/ax-init-config/SKILL.md"
    - "skills/ax-practices/SKILL.md"
    - "practices-react/eslint-plugin-ax/schemas/ax.config.schema.json"
  pathPatterns:
    - 'skills/ax-install-java-enforcement/SKILL.md'
    - 'build.gradle.kts'
    - 'build.gradle'
  bashPatterns:
    - './gradlew testPractices'
  importPatterns:
    - 'com.tngtech.archunit'
retrieval:
  aliases:
    - ax-install-java-enforcement
    - install archunit
    - enforce ax java rules
    - wire testPractices
  intents:
    - "enforce the ax-template Java catalog with ArchUnit in my project"
    - "which java rules can actually be machine-checked outside ax-template"
  entities:
    - archunit
    - testPractices
    - ax.rootPackage
    - java.root
    - fork-receiver
---

# ax-install-java-enforcement

Installs a `testPractices` Gradle task backed by ArchUnit for a downstream Spring
project. **Read this scope statement before doing anything else.**

## Scope — say this to the user before installing anything

The `practices/` Java catalog is 233 rules; the great majority carry
`verification.type: review` — a human or AI reviewer judgment, not a check a build
can run. Those rules are **not affected by this skill at all** and remain
`ax-practices`'s job (read the rule, judge the code against it, cite the rule id).

What this skill installs is the **small minority that is statically decidable by
ArchUnit**, independent of ax-template's own reference workload:

1. **layer-boundary direction** — e.g. services must not depend on controllers,
   controllers must not depend on repositories directly.
2. **no-cyclic-package** — feature-slice packages must not form an import cycle.
3. **DTO-as-record** — classes named `*Request`/`*Response` should be Java
   `record`s, not classes.

Do not imply that installing this skill enforces "the Java catalog" — it enforces
these three static checks. Everything else stays a review-time judgment call.

## Procedure (fixed order)

### 1. Prerequisite: `ax.config.json`

Need `java.rootPackage` (and `java.root` if the Gradle module isn't the project
root). If `ax.config.json` doesn't exist, invoke `ax-init-config` and stop —
do not guess a package name.

### 2. Add the ArchUnit dependency

```kotlin
testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
```

### 3. Register the `testPractices` task, parameterized by `rootPackage`

The root package must never be hardcoded into the test class — pass it through a
Gradle `systemProperty`, sourced from a project property so it can be overridden
per-invocation without editing the build file:

```kotlin
// Supplies ax.rootPackage to every Test task WHEN -PaxRootPackage is passed; no error if absent —
// testPractices enforces its own presence below, §4 covers the last gap (an IDE runner bypassing Gradle).
tasks.withType<Test>().configureEach {
    (project.findProperty("axRootPackage") as String?)?.let { systemProperty("ax.rootPackage", it) }
}

tasks.register<Test>("testPractices") {
    // A `Test` task via `tasks.register<Test>(...)` starts EMPTY (only the built-in `test` gets
    // these by convention) — without these two lines it scans nothing, reports `BUILD SUCCESSFUL`.
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("PRACTICES") }

    // #86: a wrong-but-present package scans zero classes (§4's allowEmptyShould(true) then PASSes
    // it silently) — fail LOUDLY instead of the literal fallback that made #86 possible.
    systemProperty(
        "ax.rootPackage",
        project.findProperty("axRootPackage")
            ?: error("axRootPackage unresolved — pass -PaxRootPackage=<your root package>")
    )

    // #87: default logging shows only the failing METHOD, not the ArchUnit violation text naming
    // the class. FULL alone (no `events` needed) restores it — confirmed empirically.
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    // §4's 4th check needs main's compiled-class count. `doFirst`, not inline: `.files` eagerly
    // resolves the FileCollection — this defers to EXECUTION time, after `compileJava` has run.
    doFirst {
        systemProperty(
            "ax.mainClassesDirs",
            sourceSets["main"].output.classesDirs.files.joinToString(File.pathSeparator) { it.absolutePath }
        )
    }
}
```

**Precedent asymmetry.** `backend/build.gradle.kts` also uses `?:` inside `tasks.withType<Test>` —
`... ?: "128"` — but that's a harmless tuning knob; `ax.rootPackage` decides WHAT gets scanned, so
it gets `?: error(...)` instead. Omitting it now fails immediately (`BUILD FAILED`), not silently:

```bash
./gradlew testPractices -PaxRootPackage=com.example.app
```

### 4. Write the ArchUnit checks

Write **new** test classes for the target project — variablized on `rootPackage` via
`System.getProperty("ax.rootPackage")` (no literal fallback — see below). Do **not** copy
ax-template's own `backend/src/test/.../practices/*ArchTest.java` files; those test its own tree.

All three ArchUnit rule checks below are spelled out **in full**, not two in prose plus one worked
example. `.allowEmptyShould(true)` is required on **every** rule: a fresh project has an empty
match set by construction (zero sub-packages, zero `*Request`/`*Response` classes, often zero
violations on day one), and ArchUnit fails a rule matching nothing unless it's set — omitting it
leaves the baseline **RED before a single real violation exists**, indistinguishable from step 5's
probe. The natural "fix" (adding it reflexively wherever a test fails) masks genuine violations too.

A **4th check**, `practicesGateActuallyScansTheProject`, is not an ArchRule and takes no
`.allowEmptyShould(true)` — it tells that legitimate empty baseline apart from #86's wrong-package
silent pass, which looks identical to the three rules above:

```java
package com.example.app.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import java.io.File;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
class LayerBoundaryArchTest {

    // No literal default (#86). Reading here is harmless (returns null); classes() below, called
    // from each @Test body, throws — a named test failure, not an ExceptionInInitializerError.
    private static final String ROOT_PACKAGE = System.getProperty("ax.rootPackage");

    private static JavaClasses cachedClasses;

    private static JavaClasses classes() {
        if (ROOT_PACKAGE == null || ROOT_PACKAGE.isBlank()) {
            throw new IllegalStateException(
                    "ax.rootPackage unresolved — run with -PaxRootPackage=<your root package>.");
        }
        if (cachedClasses == null) {
            // DO_NOT_INCLUDE_TESTS: not redundant — `classpath` includes main transitively.
            cachedClasses = new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages(ROOT_PACKAGE);
        }
        return cachedClasses;
    }

    @Test
    void servicesDoNotDependOnControllers() {
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
                .allowEmptyShould(true);
        rule.check(classes());
    }

    @Test
    void featureSlicesAreFreeOfCycles() {
        ArchRule rule = SlicesRuleDefinition.slices()
                .matching(ROOT_PACKAGE + ".(*)..")
                .should().beFreeOfCycles()
                .allowEmptyShould(true);
        rule.check(classes());
    }

    @Test
    void requestAndResponseClassesAreRecords() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Request")
                .or().haveSimpleNameEndingWith("Response")
                .should().beRecords()
                .allowEmptyShould(true);
        rule.check(classes());
    }

    // F1: neither this nor the rules above can tell a wrong root package apart from a legitimately
    // empty baseline — both scan zero. Compares main's compiled count vs. ROOT_PACKAGE's match count.
    @Test
    void practicesGateActuallyScansTheProject() {
        String mainClassesDirsProp = System.getProperty("ax.mainClassesDirs");
        if (mainClassesDirsProp == null || mainClassesDirsProp.isBlank()) {
            // Absent, not empty: §3's doFirst never ran — unverifiable, FAIL.
            throw new IllegalStateException(
                    "ax.mainClassesDirs unresolved — the testPractices doFirst block in §3 did not run.");
        }

        // Unscoped by ROOT_PACKAGE (unlike classes()); never fed into an ArchRule.
        JavaClasses mainClasses = new ClassFileImporter()
                .importPaths(mainClassesDirsProp.split(File.pathSeparator));

        if (mainClasses.isEmpty()) {
            return; // legitimately empty scaffold/aggregator — SKIP, not FAIL
        }

        int scoped = classes().size();
        if (scoped == 0) {
            // Non-empty main, zero under ROOT_PACKAGE: the exact #86 shape.
            throw new AssertionError(String.format(
                    "ax.rootPackage=%s matched 0 of %d compiled main classes — wrong root package.",
                    ROOT_PACKAGE, mainClasses.size()));
        }
    }
}
```

Verified empirically (Gradle 8.5 / ArchUnit 1.3.0 / JUnit 6.0.3): violation→RED, wrong package→FAIL, clean→GREEN, empty scaffold→SKIP.

### 5. Non-vacuous verification (mandatory — do not skip)

A green `testPractices` run with zero violating classes proves nothing if the check never actually
runs against real code — either no rule matched anything real, or the task executed zero tests
(§3). Prove it fires, **in this order**:

1. **Confirm the task executed tests at all.** After any `testPractices` run:
   ```bash
   ls <java.root>/build/test-results/testPractices/*.xml
   ```
   Check the test count. An absent directory, or `tests="0"`, means the task is inert — go back to
   §3; don't treat a `0 tests` `BUILD SUCCESSFUL` as passing, or move on until it's nonzero.
2. Temporarily add a probe class that violates the rule — e.g. a `ProbeService` whose method
   signature references a `ProbeController`, so `haveSimpleNameEndingWith("Controller")` matches.
3. Run `./gradlew testPractices -PaxRootPackage=<the real root package>` and confirm the
   layer-boundary test **fails (RED)** and the failure output **names the probe class** — a
   nonzero exit code alone is not proof; it could be a compile error or an unrelated failure.
4. **Delete the probe class.**
5. Re-run and confirm GREEN again, with the same nonzero test count as step 1 — not `0 tests`.
6. **If a pre-commit hook is installed**, repeat the RED check **through the hook**, not just step
   3's manual `-P` call — ax-template #86 was exactly this gap: the manual call passed
   `-PaxRootPackage`, the hook's own invocation didn't. Re-add the probe, then:
   ```bash
   git add path/to/ProbeService.java   # substitute the probe class's actual path
   git commit -m "probe: verify testPractices hook wiring"
   ```
   Confirm the commit is **blocked**, RED, probe class named. Delete/uncommit after.

If the probe does not turn RED: (a) step 1 already showed `tests="0"` — fix §3, not the probe;
(b) `rootPackage` mismatch with the probe's package; (c) the probe lives outside
`importPackages(ROOT_PACKAGE)`'s tree; (d) RED only under step 3, not 6 — the hook is missing `-P`.

## What this skill does NOT do

- It does not enforce or claim to enforce any `verification.type: review` rule —
  those stay `ax-practices`'s job, applied by reading and judging, never by a
  build task.
- It does not modify `ax.config.json` — that is `ax-init-config`'s job.
- It does not wire git hooks — see `skills/ax-install-hooks/SKILL.md`.

## Self-check before reporting "java enforcement installed"

- [ ] The scope statement (3 checks only, not "the Java catalog") was said to the user before installing anything, and `ax.config.json` existed (or `ax-init-config` was invoked and the run stopped there)
- [ ] The `testPractices` task sets `testClassesDirs`/`classpath` from `sourceSets["test"]` — not just `useJUnitPlatform` — so it scans compiled test classes instead of running an inert empty task
- [ ] `testPractices` reads `ax.rootPackage` via `systemProperty`/`-P`, never a literal package string baked into the build file — an unresolved value fails the task immediately (`?: error(...)`), it does not fall back to `"com.example.app"` (ax-template #86)
- [ ] The written ArchUnit test class(es) are new, project-specific files (no literal ax-template package name anywhere), and all three rule bodies include `.allowEmptyShould(true)`
- [ ] The 4th check (`practicesGateActuallyScansTheProject`) is present, its non-null/empty resolution lives in a `@Test` body (never a `static` initializer), and `testLogging { exceptionFormat = FULL }` is set (ax-template #86/#87)
- [ ] `build/test-results/testPractices/*.xml` was checked and showed a nonzero test count before trusting any GREEN result
- [ ] The probe→RED→delete→GREEN check ran (RED output named the probe class, GREEN re-run matched step 1's count), and — if a pre-commit hook is installed — the same RED was **also** observed through `git commit` (step 5.6), not only the manual `-P` call
