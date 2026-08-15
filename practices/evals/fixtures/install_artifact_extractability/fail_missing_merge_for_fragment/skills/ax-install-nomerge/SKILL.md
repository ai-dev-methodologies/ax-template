---
name: ax-install-nomerge
description: Fixture — a kind=file-fragment marker that never declares how it merges into its target.
---

# ax-install-nomerge (fixture)

A `file-fragment` names the FILE it belongs to, never the position inside it. If
the marker does not also say HOW to combine the two, the materializing harness
has to guess from the target's basename and the fragment's own text — which is
exactly what `verify-downstream.sh` used to do, a placement rule encoded as
harness code and invisible to the marker the rule is about, free to diverge from
what the skill author meant without anything noticing.

`merge=` is that rule, lifted into the marker contract (P2-112). It is REQUIRED
on `kind=file-fragment`; the fragment below omits it.

Everything else is correct — a real `path=`, a registered `kind` and `base`, a
registered fence language, no directives — so this fixture fails for its one
named reason only.

<!-- ax:artifact id=nomerge-artifact path=package.json kind=file-fragment base=repo -->
```json5
{
  "scripts": {
    "lint": "eslint ."
  }
}
```
