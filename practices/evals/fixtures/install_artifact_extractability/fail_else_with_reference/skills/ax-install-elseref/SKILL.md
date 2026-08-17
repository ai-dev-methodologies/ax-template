---
name: ax-install-elseref
description: Fixture skill whose ax:else carries a config reference, as if it were an elif.
---

# ax-install-elseref (fixture)

The grammar has no `elif`. An `ax:else` given a reference reads as though it tests
that reference, but it can only invert its enclosing `ax:if` — so the block applies
under the opposite of a DIFFERENT condition than the one written on the line.
Expected: `ELSE_WITH_REFERENCE`.

The fence language is `js` on purpose (see fail_orphan_axelse for why).

<!-- ax:artifact id=else-with-reference-demo path=demo.mjs kind=file base=repo -->
```js
// ax:if config.react.typescript
export const a = 1
// ax:else config.stacks.react
export const b = 2
// ax:endif
```

That is the whole fixture.
