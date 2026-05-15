# Practices PR Checklist

> Advisory metrics never block merges. The only hard gates are spec_ref existence and the
> referenced gradle task passing. Treat the rubric report as a signal, not a gate.

## Required (hard gate)
- [ ] Every new/changed rule.md has a non-empty `spec_ref` pointing to an item in `specs/*.yaml`
- [ ] The referenced spec item exists (and is reachable by `practices/evals/spec_ref_guard.sh`)
- [ ] `./gradlew testPractices` PASSES (or the rule's specific `--tests` selector PASSES)
- [ ] `bash practices/evals/spec_ref_guard.sh` exits 0
- [ ] `bash practices/generate_agents.sh && git diff --exit-code practices/AGENTS.md` clean

## Advisory (warnings; not blocking)
- [ ] Category balance — no single prefix > 25% of total rules (run `bash practices/_balance_guard.sh`)
- [ ] Outcome metrics in `practices/evals/reports/{date}.md` show no new high/critical
- [ ] If touching upstream snapshots, `_MANIFEST.yaml` updated with today's `fetched_at` + new sha

## Notes
- Forced spec-domain promotion is forbidden. Sub-category may be promoted only after ≥3 items accumulate AND maintainer consensus.
- Developer-side enforcement (hooks/IDE) is out of this maintainer-side PR's scope.
