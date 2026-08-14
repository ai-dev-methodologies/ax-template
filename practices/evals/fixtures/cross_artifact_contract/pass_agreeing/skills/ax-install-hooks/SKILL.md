---
name: ax-install-hooks-fixture
description: fixture stub — hooks skill, gradle property contract only
---

# Fixture — ax-install-hooks (pass_agreeing)

```bash
[ "$JAVA_TOUCHED" = 1 ] && ( cd "$JAVA_ROOT" && ./gradlew "$JAVA_TEST_TASK" -PaxRootPackage="$JAVA_ROOT_PACKAGE" )
```
