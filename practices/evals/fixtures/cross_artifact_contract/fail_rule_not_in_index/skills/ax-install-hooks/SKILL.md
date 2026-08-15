---
name: ax-install-hooks-fixture
description: fixture stub — hooks side agrees with java side here, so check (a) stays clean
---

# Fixture — ax-install-hooks (fail_rule_not_in_index)

<!-- ax:artifact id=hook-body path=.githooks/pre-commit kind=file base=repo -->
```bash
[ "$JAVA_TOUCHED" = 1 ] && ( cd "$JAVA_ROOT" && ./gradlew "$JAVA_TEST_TASK" -PaxRootPackage="$JAVA_ROOT_PACKAGE" )
```
