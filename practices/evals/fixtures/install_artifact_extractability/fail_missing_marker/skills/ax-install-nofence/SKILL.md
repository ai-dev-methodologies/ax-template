---
name: ax-install-nofence
description: Fixture — a marker with no fenced code block immediately after it.
---

# ax-install-nofence (fixture)

The marker below is followed by a plain paragraph instead of a fenced code block,
which is exactly the "harness silently extracts nothing" failure mode this guard
exists to catch.

<!-- ax:artifact id=nofence-artifact path=- kind=command base=repo -->
This paragraph is not a fenced code block, so the marker above has nothing to
extract. `ax_markers.discover()` still yields an Artifact for it (tolerant by
design), and `lint()` reports MARKER_NO_FENCE.
