# pass_manifest_decommissioned

BACKLOG P2-111(c). The tree carries **no `.claude-plugin/plugin.json`** while
`.ax-downstream/prev_version.txt` records `0.1.0` — the fixture-shaped spelling of "the
base side of this range HAD a plugin manifest and the head does not". That is a
**decommission**: the push unpublishes the plugin.

Expected: **exit 0**, with the DECOMMISSION notice printed loudly on stderr.

## Why a pass fixture is the right shape here, and what it is load-bearing for

Removing the manifest is a legitimate operation and there is no release to audit, so
blocking it would be a false positive. The defect P2-111(c) named is not that decommission
was blocked — it is that decommission was **indistinguishable from "not a plugin tree"**,
both absorbed into one silent "does not apply" line. That made "delete the manifest, push,
restore it in a later commit" a quiet path past the gate. It is now announced, and the
restore fires the gate normally anyway (the restoring range's base has no version and its
head does, which reads as a changed value).

The fixture is a genuine differential, not decoration: before the fix this shape produced
`version_before="0.1.0"` vs `version_after=None`, which differ, so the gate FIRED and this
tree exited 1 on `AX_DOWNSTREAM_LOG_MISSING` — it carries no `runs.jsonl` on purpose. It
exits 0 only because the applicability check now looks at whether the manifest is PRESENT
on each side rather than at the parsed version alone. A regression that reverts the
applicability logic turns this fixture red, and one that widens the decommission branch to
swallow a real release is caught by `pass_fresh` and every `fail_*` sibling, all of which
do carry the manifest.
