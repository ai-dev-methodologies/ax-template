---
name: ax-install-demo
description: Minimal fixture skill exercising a well-formed ax:artifact marker.
---

# ax-install-demo (fixture)

A single well-formed artifact: a bash command block, one declared `substs=` token
used exactly once in the body, no stray `ax:` text left over after rendering.

<!-- ax:artifact id=demo-echo path=- kind=command base=repo substs=env.greeting -->
```bash
#!/usr/bin/env bash
set -euo pipefail
# ax:subst env.greeting
MESSAGE="@@env.greeting@@"
echo "$MESSAGE"
```

That is the whole fixture.
