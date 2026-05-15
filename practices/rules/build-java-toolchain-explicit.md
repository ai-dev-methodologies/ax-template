---
title: Declare an explicit Java toolchain in build.gradle.kts
impact: MEDIUM
impactDescription: "Without an explicit toolchain the build silently follows the developer's $PATH"
tags:
  - build
  - gradle
  - toolchain
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-BUILD-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-BUILD-003
upstream:
  - "https://docs.gradle.org/current/userguide/toolchains.html"
evidence:
  - upstream_id: gradle-toolchains
    section: "Gradle Toolchains for JVM projects"
    quote: "JavaLanguageVersion"
  - source_type: external
    citation: "Gradle User Guide — Toolchains for JVM projects"
    url: "https://docs.gradle.org/current/userguide/toolchains.html"
---

## Declare an explicit Java toolchain in build.gradle.kts

**Impact: MEDIUM — Without an explicit toolchain the build silently follows the developer's $PATH**

Gradle's default behavior is to compile + run tests against whichever JDK is on the user's `JAVA_HOME` / `$PATH`. A developer with JDK 17 produces a working jar; a CI agent with JDK 21 produces a different jar; a teammate with JDK 11 produces a build error. The toolchain block makes the JDK an explicit input to the build — Gradle downloads the correct JDK if necessary and refuses to silently use a different one.

**Incorrect — no toolchain block, $PATH decides:**

```kotlin
plugins { java }
group = "com.example"
// no java { toolchain { ... } } — every developer's local JDK becomes the build's JDK
```

**Correct — explicit toolchain pins the JDK:**

```kotlin
plugins { java }
group = "com.example"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

Verification: `./gradlew testPractices --tests "*JavaToolchainExplicit*"` reads `backend/build.gradle.kts` and asserts both the `toolchain` keyword and a `JavaLanguageVersion.of(...)` call are present.

Reference: [Gradle User Guide — Toolchains for JVM projects](https://docs.gradle.org/current/userguide/toolchains.html)
