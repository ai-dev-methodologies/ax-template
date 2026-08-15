---
name: ax-install-hooks-fixture
description: fixture stub — hooks side drifted to a DIFFERENT -P property name (F-024/#86 shape)
---

# Fixture — ax-install-hooks (fail_p_contract_drift)

<!-- ax:artifact id=hook-body path=.githooks/pre-commit kind=file base=repo -->
```bash
[ "$JAVA_TOUCHED" = 1 ] && ( cd "$JAVA_ROOT" && ./gradlew "$JAVA_TEST_TASK" -PaxPackageRoot="$JAVA_ROOT_PACKAGE" )
```
