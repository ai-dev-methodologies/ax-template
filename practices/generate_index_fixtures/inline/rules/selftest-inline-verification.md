---
title: "Selftest Inline-style Verification Rule"
impact: "LOW"
tags: [selftest, inline]
verification: { type: review, reviewer: "self" }
---

Fixture-only rule for `practices/generate_index_selftest.sh`. Exercises the
inline-flow `verification: { type: review, ... }` mapping and inline-flow
`tags: [a, b]` list — expected classification is `review`.
