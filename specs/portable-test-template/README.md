# Portable ASVS Test Template

This directory contains RestAssured-based test templates that can be copied into any Spring Boot project.
Unlike the implementation tests in `backend/src/test/`, these use black-box HTTP testing and are not coupled to any specific package structure.

## Usage
1. Copy the test files to your project's `src/test/java/` directory
2. Adjust the package name
3. Add RestAssured dependency to build.gradle.kts:
   ```kotlin
   testImplementation("io.rest-assured:rest-assured:5.4.0")
   ```
4. Run: `./gradlew testAsvs`

## Key patterns
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` — starts real HTTP server
- `@Tag("ASVS")` + `@Tag("ASVS-V2.1.1")` — enables `./gradlew testAsvs` filtering
- RestAssured `given().when().then()` — black-box HTTP assertions
- `@BeforeEach` DB seeding for verified users
