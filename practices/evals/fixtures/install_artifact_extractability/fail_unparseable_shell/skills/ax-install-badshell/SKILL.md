---
name: ax-install-badshell
description: Fixture — a marker that is structurally well-formed but renders to syntactically invalid shell.
---

# ax-install-badshell (fixture)

This marker passes every one of `ax_markers.lint()`'s checks 1-8: exactly one
fence immediately follows, the id is unique, `bash` is a registered fence
language, there is no unbalanced `ax:if`, the directive prefix matches, and the
one declared `substs=` token is used exactly once. It only fails check 10 —
this guard's own `bash -n` pass on the RENDERED text — because the body's
string literal is never closed.

<!-- ax:artifact id=badshell-artifact path=- kind=command base=repo substs=env.token -->
```bash
#!/usr/bin/env bash
set -euo pipefail
# ax:subst env.token
FOO="@@env.token@@
echo "$FOO"
```
