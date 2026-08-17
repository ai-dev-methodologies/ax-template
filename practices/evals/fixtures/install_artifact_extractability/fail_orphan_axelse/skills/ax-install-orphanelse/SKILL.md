---
name: ax-install-orphanelse
description: Fixture skill whose artifact body carries an ax:else outside any ax:if.
---

# ax-install-orphanelse (fixture)

`ax:else` inverts its ENCLOSING `ax:if`. With no enclosing `ax:if` there is nothing
to invert, so the lines after it belong to no branch at all — the author meant a
condition and got unconditional text. Expected: `UNBALANCED_AXIF`.

The fence language is `js` on purpose: check 10 renders `bash`/`sh` artifacts only,
so this fixture reaches exit 1 through the `ax_markers.lint()` delegation path and
nothing else, which keeps a single anchor able to flip it.

<!-- ax:artifact id=orphan-else-demo path=demo.mjs kind=file base=repo -->
```js
export const a = 1
// ax:else
export const b = 2
```

That is the whole fixture.
