---
name: ax-install-typocfg
description: Fixture — an ax:if naming a config path ax.config.schema.json does not declare.
---

# ax-install-typocfg (fixture)

`config.react.typoField` does not exist in
`practices-react/eslint-plugin-ax/schemas/ax.config.schema.json`. This is the
worst-behaved reference shape in the whole marker grammar: `ax:if` treats a
missing path as FALSY (feature-flag semantics — "the config doesn't mention it"
and "it is off" are the same fact), so a misspelled condition is not an error at
render time. It is a block that gets deleted on EVERY consumer project, forever,
looking exactly like a feature the author deliberately turned off.

`UNKNOWN_CONFIG_PATH` is the only check that can tell those two apart, because
only the schema knows which paths are real.

The `ax:if`/`ax:endif` pair is balanced and correctly prefixed for the `bash`
fence, and the rendered body (with the block dropped) is valid shell, so this
fixture fails for its one named reason only.

<!-- ax:artifact id=typocfg-artifact path=- kind=command base=repo -->
```bash
#!/usr/bin/env bash
set -euo pipefail
# ax:if config.react.typoField
echo "this line is unreachable on every project that will ever exist"
# ax:endif
echo "done"
```
