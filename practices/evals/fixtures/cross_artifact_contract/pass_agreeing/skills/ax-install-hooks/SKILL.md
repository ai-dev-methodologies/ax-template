---
name: ax-install-hooks-fixture
description: fixture stub — hooks skill, gradle property contract only
---

# Fixture — ax-install-hooks (pass_agreeing)

<!-- ax:artifact id=hook-body path=.githooks/pre-commit kind=file base=repo -->
```bash
[ "$JAVA_TOUCHED" = 1 ] && ( cd "$JAVA_ROOT" && ./gradlew "$JAVA_TEST_TASK" -PaxRootPackage="$JAVA_ROOT_PACKAGE" )
```

## Self-check checklist (fence-EXTERNAL prose — must NOT be read by check (a))

- [ ] The java block's `./gradlew "$JAVA_TEST_TASK"` call passes `-PaxRootPackage="$JAVA_ROOT_PACKAGE"`
