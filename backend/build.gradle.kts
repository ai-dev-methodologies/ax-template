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

    runtimeOnly("com.h2database:h2")

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

tasks.register<Test>("testPortability") {
    useJUnitPlatform {
        includeTags("PORTABILITY")
    }
    // Fixtures must be built first: practices/evals/portability/run.sh --full
    // If a fixture's classes dir is missing, the test is skipped via JUnit Assumptions
    // (not failed) — see PortabilityFixtures.importFixture().
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
