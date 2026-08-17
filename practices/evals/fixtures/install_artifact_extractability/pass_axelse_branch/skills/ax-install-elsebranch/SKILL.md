---
name: ax-install-elsebranch
description: Fixture skill proving an ax:else branch is really dropped when its ax:if condition holds.
---

# ax-install-elsebranch (fixture)

**This fixture passes only if `ax:else` is really evaluated as a directive.** It
carries its own `ax.config.json` with `react.typescript: true` — the guard prefers a
scanned root's own config over the default consumer-e2e one — so the premise is
local and explicit: the `ax:if` branch is the one that must survive, and the
`ax:else` branch the one that must be dropped.

The `ax:else` branch body is a bare `fi`, a bash syntax error on its own. The fence
language is `bash`, so check 10 renders this artifact and runs `bash -n` on the
result:

- `ax:else` evaluated as a directive → the else branch is dropped → only
  `echo "if branch rendered"` survives → `bash -n` passes → guard exits 0.
- `ax:else` not recognized as a directive → the `# ax:else` line degrades to
  ordinary body text and both branches render together → guard exits 1. Two
  mutations were run to confirm this is measured, not assumed, and they trip
  different detectors — which is the point of recording both:
  - `else` removed from `_DIRECTIVE_RE_BY_PREFIX` only → the surviving `# ax:else`
    text is still matched by `_RESIDUAL_DIRECTIVE_RE`, so render refuses outright:
    `RENDER_ERROR ... residual directive ... survived render`.
  - `else` removed from **both** that alternation and `_RESIDUAL_DIRECTIVE_RE` →
    nothing objects to the leftover comment, the stray `fi` reaches the shell
    check: `UNPARSEABLE_SHELL ... syntax error near unexpected token 'fi'`.

  The second mutation is why the else-branch body is invalid shell rather than a
  harmless `echo`: without it, neutering both regexes together would render two
  valid branches and pass.

**What this fixture does NOT measure**, stated so nobody reads more into a green
run than it earns: that the else branch's *content* reaches the output when the
condition is false. `bash -n` cannot distinguish "the right lines rendered" from
"nothing rendered" — both parse — and asserting on specific rendered text would be
the fix-shape grep this guard's header refuses. That half is measured where it can
be: the react `eslint.config.mjs` artifact's own render under a
`typescript: false` config, and the downstream consumer run that lints a real
`.jsx` with it.

<!-- ax:artifact id=else-branch-render-demo path=- kind=command base=repo -->
```bash
#!/usr/bin/env bash
set -euo pipefail
# ax:if config.react.typescript   (true in this fixture's own ax.config.json)
echo "if branch rendered"
# ax:else   (must be dropped; its body is a bare `fi`)
fi
# ax:endif
```

That is the whole fixture.
