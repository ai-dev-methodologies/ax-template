---
name: ax-install-java-enforcement-fixture
description: fixture stub — java enforcement skill, gradle property contract only
---

# Fixture — ax-install-java-enforcement (pass_agreeing)

<!-- ax:artifact id=java-gradle-testpractices path=build.gradle.kts kind=file-fragment base=java.root -->
```kotlin
// Lazy handle on -PaxRootPackage.
val axRootPackage = providers.gradleProperty("axRootPackage")
```
