---
name: ax-install-dup
description: Fixture — two ax:artifact markers declaring the same id.
---

# ax-install-dup (fixture)

Two separately-fenced, otherwise well-formed artifacts that both declare
`id=dup-artifact`. Ids must be globally unique across the skill file (and, in the
live tree, across all three install skills) — a second marker for the same id
would collide with whatever the harness extracted for the first.

<!-- ax:artifact id=dup-artifact path=- kind=command base=repo -->
```bash
echo "first declaration"
```

Later in the same file, a second marker reuses the same id:

<!-- ax:artifact id=dup-artifact path=- kind=command base=repo -->
```bash
echo "second declaration"
```
