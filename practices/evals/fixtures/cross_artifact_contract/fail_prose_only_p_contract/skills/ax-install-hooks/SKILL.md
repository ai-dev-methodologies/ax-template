---
name: ax-install-hooks-fixture
description: fixture stub — reproduces the P2-110 critic scenario. The MARKED hook-body
  fence carries NO -P flag at all (as if the real -PaxRootPackage= line had been deleted from
  the installed hook); the checklist PROSE outside the fence still narrates the requirement.
  A whole-file grep (the pre-P2-110 shape) is satisfied by the prose alone and PASSes; the
  marker-scoped guard must see 0 names on this side and BLOCK.
---

# Fixture — ax-install-hooks (fail_prose_only_p_contract)

<!-- ax:artifact id=hook-body path=.githooks/pre-commit kind=file base=repo -->
```bash
[ "$JAVA_TOUCHED" = 1 ] && ( cd "$JAVA_ROOT" && ./gradlew "$JAVA_TEST_TASK" )
```

## Self-check checklist (fence-EXTERNAL prose — must NOT be read by check (a))

- [ ] The java block's `./gradlew "$JAVA_TEST_TASK"` call passes `-PaxRootPackage="$JAVA_ROOT_PACKAGE"` — a `-P`-less invocation lets ArchUnit fall back to the build file's generic default package and PASS silently on real violations (F-024 / #86)
