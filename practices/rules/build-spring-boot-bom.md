---
title: Apply the Spring Boot dependency-management plugin (BOM pinning)
impact: HIGH
impactDescription: "Hand-picked starter versions drift and produce class-version mismatches"
tags:
  - build
  - gradle
  - dependency-management
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-BUILD-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-BUILD-001
upstream:
  - "https://docs.spring.io/dependency-management-plugin/docs/current/reference/html/"
evidence:
  - upstream_id: spring-dependency-management
    section: "Spring dependency-management plugin — importing BOMs"
    quote: "dependency management"
  - source_type: external
    citation: "Spring dependency-management Plugin Reference"
    url: "https://docs.spring.io/dependency-management-plugin/docs/current/reference/html/"
---

## Apply the Spring Boot dependency-management plugin (BOM pinning)

**Impact: HIGH — Hand-picked starter versions drift and produce class-version mismatches**

`spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa` are not independent libraries — they each pull in dozens of transitive Spring + Jackson + Hibernate + Tomcat versions that must be aligned. The Spring Boot BOM pins those versions for one tested combination. Without the dependency-management plugin (or the Spring Boot Gradle plugin's equivalent), Gradle picks "newest version wins" per transitive, and most teams discover the resulting drift via a `NoSuchMethodError` in production months later.

**Incorrect — hand-pinned starter versions, no BOM:**

```kotlin
plugins {
    java
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:3.2.0")
    implementation("org.springframework.boot:spring-boot-starter-security:3.1.5")
    // transitive Jackson, Tomcat, Hibernate versions resolved by Gradle's mediation — drift inevitable
}
```

**Correct — Spring Boot plugin + dependency-management BOM:**

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.2.12"
    id("io.spring.dependency-management") version "1.1.6"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")     // version from BOM
    implementation("org.springframework.boot:spring-boot-starter-security")
}
```

Verification: `./gradlew testPractices --tests "*SpringBootBom*"` reads `backend/build.gradle.kts` and asserts both plugin ids are applied.

Reference: [Spring dependency-management Plugin](https://docs.spring.io/dependency-management-plugin/docs/current/reference/html/)
