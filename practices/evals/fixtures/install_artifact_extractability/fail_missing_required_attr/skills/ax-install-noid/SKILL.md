---
name: ax-install-noid
description: Fixture — a marker missing a REQUIRED attribute (id=).
---

# ax-install-noid (fixture)

The marker below is otherwise flawless: a properly-fenced `bash` body, a
registered `kind`, a registered `base`, no directives at all. It just never
declares `id=`.

Before P2-109 this linted CLEAN. `discover()` defaulted every absent attribute
to the empty string and no later check ever asked whether the value was real, so
a marker with no id (and, in the critic's reproduction, no `path=` or `kind=`
either) was reported as a well-formed artifact — one that no harness can name,
override, or hash. `MISSING_REQUIRED_ATTR` closes that.

Every other attribute is correct, so this fixture fails for its one named reason
only.

<!-- ax:artifact path=- kind=command base=repo -->
```bash
#!/usr/bin/env bash
set -euo pipefail
echo "an artifact nobody can address"
```
