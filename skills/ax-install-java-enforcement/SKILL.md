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
tasks.register<Test>("testPractices") {
    useJUnitPlatform { includeTags("PRACTICES") }
    systemProperty("ax.rootPackage", project.findProperty("axRootPackage") ?: "com.example.app")
}
```

Run it with the project's actual root package:

```bash
./gradlew testPractices -PaxRootPackage=com.example.app
```

### 4. Write the ArchUnit checks

Write **new** test classes for the target project — variablized on `rootPackage`
via `System.getProperty("ax.rootPackage", ...)`. Do **not** copy ax-template's own
`backend/src/test/.../practices/*ArchTest.java` files: those are hardcoded to
ax-template's own reference-workload root package and exist to test ax-template's
own reference workload, not to be ported byte-for-byte into a different project's
package tree.

Below is one worked example (layer-boundary — check 1 of the 3 above). The other
two checks (no-cyclic-package via `SlicesRuleDefinition.slices().beFreeOfCycles()`,
DTO-as-record via `classes().that().haveSimpleNameEndingWith("Request").or()...
.should().beRecords()`) follow the same shape: swap the `ArchRule` body, keep the
`ROOT_PACKAGE` + `ClassFileImporter` scaffolding identical.

```java
package com.example.app.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
class LayerBoundaryArchTest {

    private static final String ROOT_PACKAGE =
            System.getProperty("ax.rootPackage", "com.example.app");

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT_PACKAGE);

    @Test
    void servicesDoNotDependOnControllers() {
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }
}
```

### 5. Non-vacuous verification (mandatory — do not skip)

A green `testPractices` run with zero violating classes proves nothing if the
check never actually runs against real code. Prove it fires:

1. Temporarily add a probe class in the target project that violates the rule —
   e.g. a `ProbeService` whose method signature references a `ProbeController`
   type, so `dependOnClassesThat().haveSimpleNameEndingWith("Controller")` matches.
2. Run `./gradlew testPractices -PaxRootPackage=<the real root package>` and
   confirm the layer-boundary test **fails (RED)** with the probe class named in
   the failure output.
3. **Delete the probe class.**
4. Re-run `testPractices` and confirm it passes (GREEN) again.

If the probe does not turn the test RED, the most likely causes are: `rootPackage`
mismatch between the `-PaxRootPackage` value and the probe's actual package, or
the probe class living outside the package tree `importPackages(ROOT_PACKAGE)`
actually scans.

## What this skill does NOT do

- It does not enforce or claim to enforce any `verification.type: review` rule —
  those stay `ax-practices`'s job, applied by reading and judging, never by a
  build task.
- It does not modify `ax.config.json` — that is `ax-init-config`'s job.
- It does not wire git hooks — see `skills/ax-install-hooks/SKILL.md`.

## Self-check before reporting "java enforcement installed"

- [ ] The scope statement (3 checks only, not "the Java catalog") was said to the user before installing anything
- [ ] `ax.config.json` existed (or `ax-init-config` was invoked and the run stopped there)
- [ ] `testPractices` reads `ax.rootPackage` via `systemProperty`/`-P`, never a literal package string baked into the build file
- [ ] The written ArchUnit test class(es) are new, project-specific files — no literal ax-template reference-workload package name appears anywhere in them
- [ ] The probe→RED→delete→GREEN check ran and the failure output named the probe class
