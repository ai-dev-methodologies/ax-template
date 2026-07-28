// Fixture stand-in for backend/build.gradle.kts. The parity guard's `verified_by:`
// checker requires the named per-domain Test task to exist AND to include the tag the
// proof carries — otherwise the "runtime proof" would be a test nothing runs.
tasks.register<Test>("testDemo") {
    useJUnitPlatform {
        includeTags("DEMO")
    }
}
