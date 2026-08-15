---
name: ax-install-java-enforcement-fixture
description: fixture stub — java side declares axRootPackage
---

# Fixture — ax-install-java-enforcement (fail_p_contract_drift)

<!-- ax:artifact id=java-gradle-testpractices path=build.gradle.kts kind=file-fragment base=java.root -->
```kotlin
val axRootPackage = providers.gradleProperty("axRootPackage")
```
