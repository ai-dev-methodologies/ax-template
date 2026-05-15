---
title: Production builds must not depend on -SNAPSHOT artifacts
impact: HIGH
impactDescription: "SNAPSHOT versions mutate underfoot — reproducibility and bisectability lost"
tags:
  - build
  - dependency-management
  - reproducibility
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-BUILD-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-BUILD-002
upstream:
  - "https://docs.gradle.org/current/userguide/dynamic_versions.html"
evidence:
  - source_type: external
    citation: "Gradle User Guide — Dynamic and changing versions"
    url: "https://docs.gradle.org/current/userguide/dynamic_versions.html"
  - source_type: external
    citation: "Maven — SNAPSHOT versioning semantics"
    url: "https://maven.apache.org/repository/internal-and-snapshot-repositories.html"
---

## Production builds must not depend on -SNAPSHOT artifacts

**Impact: HIGH — SNAPSHOT versions mutate underfoot — reproducibility and bisectability lost**

A `-SNAPSHOT` coordinate is, by Maven / Gradle contract, a "the latest build of that version line, fetched fresh". Two consecutive `./gradlew build` runs against a SNAPSHOT can resolve to different artifacts. CI green at 09:00 becomes CI red at 10:00 with no commit in between. `git bisect` cannot identify the change because the change is *not in the repo*. A bug fixed against one SNAPSHOT resurfaces against another. Production builds must depend on released, immutable versions only — SNAPSHOTs belong in local experiments, not committed `build.gradle.kts`.

**Incorrect — SNAPSHOT in a production dependency declaration:**

```kotlin
dependencies {
    implementation("com.example:my-internal-lib:2.4.0-SNAPSHOT")     // mutates underfoot
}
```

**Correct — released version:**

```kotlin
dependencies {
    implementation("com.example:my-internal-lib:2.4.0")              // immutable, reproducible
}
```

Verification: `./gradlew testPractices --tests "*NoSnapshotDependencies*"` scans `backend/build.gradle.kts` line-by-line (skipping comments) and asserts no line contains `-SNAPSHOT`.

Reference: [Gradle — Dynamic and changing versions](https://docs.gradle.org/current/userguide/dynamic_versions.html) · [Maven — SNAPSHOT semantics](https://maven.apache.org/repository/internal-and-snapshot-repositories.html)
