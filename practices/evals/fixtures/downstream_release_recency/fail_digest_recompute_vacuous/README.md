# fail_digest_recompute_vacuous

The one check guard [114]'s header calls "genuinely hard to forge" is the digest
recompute: it hashes the `ax:artifact` marker bodies the push actually carries
and requires an exact key-for-key match against the log's `artifact_digests`.

Its teeth were CONDITIONAL on there being artifacts to find. Here the tree's
single SKILL.md declares no marker at all, so the recompute yields `{}` — and
the log line honestly records `"artifact_digests": {}`. The empty map equals the
empty map, so the strongest check in the gate reported a match while measuring
nothing, and the PASS line said "artifact digests match".

MEASURED against the pre-P2-106 guard: exit 0. Expected now: exit 1
(AX_DOWNSTREAM_DIGEST_RECOMPUTE_VACUOUS).

Single-reason by construction: every other field of the line is correct, so
neutering the non-vacuity refusal makes this tree pass.
