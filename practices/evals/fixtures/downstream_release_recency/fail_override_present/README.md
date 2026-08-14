# fail_override_present

Every assertion is true, the key set is complete, digests match, verdict is
`pass` — but `"override": ["hook-body"]` says the run installed an artifact body
that is NOT what the SKILL.md carries. That is a regression differential by
construction (`--artifact-override` exists to re-inject a pre-fix shape and
watch an assertion go RED); its green cannot back a release no matter how
complete it looks.
Expected: exit 1, AX_DOWNSTREAM_LOG_OVERRIDE_PRESENT.
