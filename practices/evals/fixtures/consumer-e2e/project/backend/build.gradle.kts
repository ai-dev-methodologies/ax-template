plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// This is the unmodified Spring Initializr stock scaffold shape for a Gradle Kotlin DSL
// project (start.spring.io, Spring Boot 4.1.0 / Gradle 9.x / JDK 21) -- kept verbatim,
// deliberately not hand-tuned to ax-template's own build.gradle.kts. The eager
// `tasks.withType<Test> { useJUnitPlatform() }` block below configures every Test task
// (including any later `tasks.register<Test>("testPractices") { ... }` a downstream
// install skill adds) at Gradle *configuration* time, before that later task's own body
// runs -- this is the fixture's reproduction of GH #90's trigger condition. Do not
// "fix" this block; a real consumer project ships exactly this.
tasks.withType<Test> {
	useJUnitPlatform()
}
