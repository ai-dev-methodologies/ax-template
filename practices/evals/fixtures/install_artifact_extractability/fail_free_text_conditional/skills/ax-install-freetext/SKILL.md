---
name: ax-install-freetext
description: Fixture — free-text conditional prose left inside a fenced body, outside any directive line.
---

# ax-install-freetext (fixture)

The body below is well-formed shell wrapped in a properly-fenced, properly-id'd
marker — but one comment line inside it is free-text conditional PROSE that is
NOT itself a recognized `ax:if`/`ax:subst` directive line. A human reading the
skill (or an AI agent materializing it by hand) cannot rely on that prose being
applied consistently, and neither can `ax_markers.py`'s parser.

<!-- ax:artifact id=freetext-artifact path=- kind=command base=repo -->
```bash
#!/usr/bin/env bash
set -euo pipefail
# delete this block if you don't need the demo step
echo "demo step"
```
