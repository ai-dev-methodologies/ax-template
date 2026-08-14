---
name: ax-install-wrongprefix
description: Fixture — a directive using the wrong comment prefix for its fence's registered language.
---

# ax-install-wrongprefix (fixture)

`js` is registered for the `//` comment prefix (see ax_markers.py's
FENCE_COMMENT_PREFIX table). The body below spells its `ax:if` directive with a
`#` prefix instead — the shell-style prefix, wrong for a `js` fence. The parser
still recognizes it as an ATTEMPTED directive (so it does not silently fall
through as ordinary body text) and flags DIRECTIVE_PREFIX_MISMATCH.

<!-- ax:artifact id=wrongprefix-artifact path=- kind=file-fragment base=repo -->
```js
export const config = {
  # ax:if config.react.typescript
  strict: true,
  # ax:endif
}
```
