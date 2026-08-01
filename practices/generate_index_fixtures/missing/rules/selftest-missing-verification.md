---
title: "Selftest Missing-verification Rule"
impact: "HIGH"
tags:
  - selftest
  - missing
---

Fixture-only rule for `practices/generate_index_selftest.sh`. Deliberately has
no `verification:` key at all — expected classification is `unclassified`,
which trips the generator's non-vacuity census and BLOCKs with exit 1.
