---
name: ax-install-typoattr
description: Fixture — a marker carrying an attribute name that is not in the registered vocabulary.
---

# ax-install-typoattr (fixture)

`whn=` is a typo of `when=`. The attribute vocabulary is CLOSED
(`ax_markers.ALLOWED_ATTRS`), so this is `UNKNOWN_ATTR` rather than a silently
ignored extra: with the typo swallowed, the author's intended condition never
applies, the artifact installs unconditionally on every consumer project, and
the marker still looks perfectly well-formed to a human reading the skill.

The typo'd name is deliberately one with NO other consequence — it does not
change what `substs=` declares or what the body uses — so this fixture fails for
its one named reason only.

<!-- ax:artifact id=typoattr-artifact path=- kind=command base=repo whn=config.stacks.react -->
```bash
#!/usr/bin/env bash
set -euo pipefail
echo "installed whether or not the react stack is configured"
```
