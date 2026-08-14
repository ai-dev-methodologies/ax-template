# pass_fresh

Version changed (0.1.0 -> 0.2.0), and the latest `.ax-downstream/runs.jsonl` line
satisfies every requirement: head_sha matches expected_head.txt, tree_clean is
true, every recorded assertion is boolean true, and artifact_digests matches
what the guard recomputes from `skills/*/SKILL.md` on disk (this fixture is not
a git repo — see the guard header's FIXTURE-SHAPED mode). Expected: exit 0.
