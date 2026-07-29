# cross_trio/pass_skip_reported — expected exit 0, and the skip must be VISIBLE

BACKLOG P2-45. Two L4 verticals:

- `templates/L4/auth/` — one `.tsx` importing an evidence-anchored L2 block (the normal
  walked path).
- `templates/L4/backend-only-vertical/` — `.gitkeep` only, i.e. the shape a
  `domain_mode: backend_only` vertical (or an emptied fork-copy) has on disk.

The tsx-less dir has no imports to evidence-anchor, so `cross_trio_guard` skips it — but
it must SAY SO. Before P2-45 the skip was silent and the dir vanished from the report,
which is indistinguishable from "checked and clean". Failing on it instead was rejected:
the frontend-artifact completeness axis is owned by `full_trio_artifact_completeness_guard`
(and the spec axis by `domain_spec_trio_guard`), and 4 live verticals are legitimately
backend-only — see the judgement recorded in `cross_trio_guard.sh`'s header.

Expected: exit 0, a `SKIP: templates/L4/backend-only-vertical/ …` line, and a summary
carrying the skip count. Asserted by `../cross_trio_skip_report_run.sh`.
