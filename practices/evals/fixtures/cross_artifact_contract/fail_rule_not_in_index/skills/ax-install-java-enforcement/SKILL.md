---
name: ax-install-java-enforcement-fixture
description: fixture stub — java side declares axRootPackage (agrees with hooks side here)
---

# Fixture — ax-install-java-enforcement (fail_rule_not_in_index)

<!-- ax:artifact id=java-gradle-testpractices path=build.gradle.kts kind=file-fragment base=java.root -->
```kotlin
val axRootPackage = providers.gradleProperty("axRootPackage")
```
