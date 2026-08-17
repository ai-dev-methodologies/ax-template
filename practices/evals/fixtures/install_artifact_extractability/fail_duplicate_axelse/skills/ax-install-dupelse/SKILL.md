---
name: ax-install-dupelse
description: Fixture skill whose artifact body gives one ax:if two ax:else branches.
---

# ax-install-dupelse (fixture)

A second `ax:else` re-inverts the branch, resurrecting lines the first `ax:else`
ended — so the third region silently shares the first region's condition instead of
being the alternative its author wrote. Expected: `DUPLICATE_AXELSE`.

The fence language is `js` on purpose (see fail_orphan_axelse for why).

<!-- ax:artifact id=duplicate-else-demo path=demo.mjs kind=file base=repo -->
```js
// ax:if config.react.typescript
export const a = 1
// ax:else
export const b = 2
// ax:else
export const c = 3
// ax:endif
```

That is the whole fixture.
