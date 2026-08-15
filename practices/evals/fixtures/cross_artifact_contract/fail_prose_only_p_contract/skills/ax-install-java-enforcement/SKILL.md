---
name: ax-install-java-enforcement-fixture
description: fixture stub — java side declares axRootPackage; irrelevant to this fixture's
  failure (which is entirely check (a)'s hook side going to 0 names), included so check (a)'s
  java-side derivation still succeeds and the fixture isolates the failure cleanly.
---

# Fixture — ax-install-java-enforcement (fail_prose_only_p_contract)

<!-- ax:artifact id=java-gradle-testpractices path=build.gradle.kts kind=file-fragment base=java.root -->
```kotlin
val axRootPackage = providers.gradleProperty("axRootPackage")
```
