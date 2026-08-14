# consumer-e2e fixture

This directory is the **source of a stock-shaped, 2-stack (Next.js + Spring Boot)
consumer project** consumed by `practices/scripts/verify-downstream.sh` (Layer 1
of the downstream-fixture-E2E harness). Its job: prove that ax-transform's install
skills (`ax-install-hooks`, `ax-install-java-enforcement`, `ax-install-react-enforcement`)
produce artifacts that actually run in a downstream project — not just in
ax-template's own tree.

## Why this shape (and not ax-template's own `backend/`/`frontend/`)

ax-template's own `backend/` and `frontend/` are not valid stand-ins for a consumer
fixture: they assume `root="."`-adjacent conventions, ax-template's own hardened
`build.gradle.kts` (which already carries the `testClassesDirs`/`classpath` fix a
downstream `tasks.register<Test>(...)` needs — see that file's Gradle-9 comment),
and identifiers that belong to this repo, not a fork-receiver. This fixture instead
reproduces what a real consumer repo looks like on day one:

- `backend/` — Spring Boot 4.1.0, Gradle Kotlin DSL, JDK 21, generated in the
  exact shape [start.spring.io](https://start.spring.io) (Spring Initializr)
  ships it — including the eager
  `tasks.withType<Test> { useJUnitPlatform() }` block. That block is kept
  **on purpose**: it configures every `Test` task (including a later
  `tasks.register<Test>("testPractices") { ... }` an install skill adds) at
  Gradle *configuration* time, which is the exact trigger condition for GH #90.
- `frontend/` — Next.js app-router layout with `src/app` (route layer),
  `src/features` (feature layer), `src/components`/`src/lib` (shared layer) —
  matching `ax.config.json`'s `react.layers` mapping.
- `ax.config.json` — `react.root` is `"frontend"` and `java.root` is `"backend"`,
  **neither is `"."`** — matching a real consumer repo's layout, not
  ax-template's self-application. This is the condition under which GH #78/#79/
  #82/#84/#86/#90 actually reproduce (Class A "context-transfer" findings in
  the harness PRD).

## What is committed vs. materialized at runtime

The committed tree has **zero binaries** and does **not** include a Gradle
wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`,
`gradle/wrapper/gradle-wrapper.properties`). `verify-downstream.sh` copies
ax-template's own `backend/gradlew` + `backend/gradle/wrapper/gradle-wrapper.jar`
into a temporary, out-of-repo materialized copy of this fixture at runtime, and
*generates* `gradle-wrapper.properties` there, overriding only `distributionUrl`
to pin **Gradle 9.5.1**. See the recipe below.

`eslint.config.mjs` and the `package.json` `"lint"` script are likewise **not**
committed here — those are exactly the artifacts `ax-install-react-enforcement`
is supposed to install, and the harness verifies they actually get installed and
actually gate. Committing them ourselves would make the fixture "pass" for a
reason that has nothing to do with the install skill (F-031).

## Gradle wrapper substitution — measured recipe (2026-08-14)

Copying `backend/gradlew` + `backend/gradle/wrapper/gradle-wrapper.jar` (which
ship pinned to distribute Gradle 8.14.5) into a tree, and overwriting only
`gradle/wrapper/gradle-wrapper.properties`'s `distributionUrl` with
`gradle-9.5.1-bin.zip`, was verified to boot correctly:

```
$ ./gradlew --version
Gradle 9.5.1
Kotlin 2.3.20
Launcher JVM 21.0.10
EXIT=0
```

This is a **measurement**, not a claim — recorded first in `PRD-consumer-e2e.md`
§3 (M1) and reproduced here so the fixture's own README carries the substitution
recipe its harness depends on. `./gradlew --version` only proves the wrapper
boots; it does not prove the Spring Boot 4.1.0 Gradle plugin, the Kotlin DSL
`sourceSets["test"].output.classesDirs`/`.runtimeClasspath` accessors, or
`useJUnitPlatform { includeTags(...) }` work under Gradle 9.5.1 for *this*
fixture's real `build.gradle.kts` — that is a separate measurement
(`./gradlew help` / `./gradlew compileTestJava` against the materialized copy,
tracked as M2 in the PRD) and is not re-asserted by this README.

## `frontend/package-lock.json`

Committed for reproducibility — it reduces `verify-downstream.sh`'s network
non-determinism (a resolved dependency graph beats a fresh `npm install`
picking up whatever is newest that day). Generated with:

```
npm install --package-lock-only
```

against `frontend/package.json` alone — no `node_modules/` was ever created or
committed; `npm install --package-lock-only` resolves and writes the lockfile
without materializing packages on disk.

## Naming

All Java/config identifiers use `com.example.backend` / generic package names.
No fork-receiver company, brand, or business identifiers appear anywhere in this
fixture (R26).
