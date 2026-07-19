# Wave-1 canary scorecard — DISK-SUBSTANTIATED, 5/6

Records whether each planted, known-absent need in `canary-gaps.yaml` (Part 3.6 —
the falsifiability test for the wave orchestrator's "a known uncovered need must
produce a logged gap" property) was actually surfaced by wave-1's six dogfood
iterations (`docs/dogfood-ledger/engine-w1-iter1.yaml` through `iter6.yaml`).

**Result: 5/6 surfaced.** CANARY-004 has zero surfacing evidence anywhere on
disk. This scorecard exists precisely so a 6/6 claim is never made without
this file being updated with genuine evidence for CANARY-004 first — no
canary scorecard existed anywhere on disk before this file (fable5 xhigh
wave-exit verification, 2026-07-20), and the prior "6/6" self-report in the
wave summary was NOT substantiated by any persisted record.

## Method

For each canary, "surfaced" requires disk evidence that a wave-1 iteration's
scenario/finding addressed the SAME underlying capability gap the canary's
`need` field describes — either (a) an EXPLICIT citation of the canary ID in
scenario/finding text, or (b) a semantic match to the canary's `need` +
`related_cell` fields backed by a real scenario-guard + executed proof. (b)
is weaker evidence than (a): a semantic match cannot rule out the surfacer
having read `canary-gaps.yaml` before writing the scenario (post-hoc
annotation vs. genuine blind discovery are indistinguishable from disk
alone) — this caveat is noted per canary below, not glossed over.

## Scorecard

| Canary | Need (short) | Surfaced? | Evidence | Citation type |
|---|---|---|---|---|
| CANARY-001 | FE locale-aware number/date formatting rule | **YES** | `docs/dogfood-ledger/engine-w1-iter1.yaml` finding G2 names `CANARY-001` verbatim; closed in-wave by `practices-react/rules/locale-aware-number-date-format.md` + `practices/evals/locale_aware_format_guard.sh` (now wired into `run-all-guards.sh`) | (a) explicit ID citation |
| CANARY-002 | FE webhook-signature-verification UX | **YES** | `practices/consumer-proof/scenarios/S2.AUDIT-PII.XB/scenario-guards/webhook_signature_status_ux_guard.sh` — matches CANARY-002's `need`/`related_cell: S1.webhook.FE`; proof executed via `run-scenario-proof.sh` (2/2 PASS per fable5 xhigh verification). Backfilled to `docs/dogfood-ledger/engine-w1-iter6.yaml` finding G2 (2026-07-20 gap-logging backfill) | (b) semantic match — no explicit "CANARY-002" string found in the scenario dir |
| CANARY-003 | XB pagination-envelope contract-parity test | **YES** | `docs/dogfood-ledger/engine-w1-iter3.yaml` finding G1 (`pagination_envelope_contract_parity.sh`, S3.saas-subscription) — matches CANARY-003's `need`/`related_cell: S2.QUERY-BOUNDS.XB` | (b) semantic match — no explicit "CANARY-003" string found in the scenario dir |
| CANARY-004 | FE chunked-import rollback/undo action | **NO** | `grep -rliE "rollback\|undo\|ImportProgress" practices/consumer-proof/scenarios/ docs/dogfood-ledger/engine-w1-iter*.yaml` → 0 matches (re-verified 2026-07-20). No wave-1 scenario or finding addresses this need at all. | — none |
| CANARY-005 | BE SSRF allowlist at webhook-registration time | **YES** | `docs/dogfood-ledger/engine-w1-iter4.yaml` finding G2 names `canary-gaps.yaml CANARY-005` verbatim (S2.AUTHZ.FE `ssrf_missing_allowlist_check_guard.sh`) — and extends it: the scenario finds the SAME defect class at test-delivery (use) time, which CANARY-005 (registration-time only) does not cover | (a) explicit ID citation |
| CANARY-006 | BE multi-tenant TenantContext runtime primitives | **YES** | `docs/dogfood-ledger/engine-w1-iter5.yaml` finding G3 (hand-rolled ThreadLocal `TenantContext` port, S2.MULTI-TENANT.BE) — matches CANARY-006's `need`/`related_cell: S1.multi-tenant.BE` | (b) semantic match — no explicit "CANARY-006" string found in the scenario dir |

## Do not claim 6/6

CANARY-004 remains open. It is NOT closed, NOT reclassified, and NOT excluded
from future waves — it stays in `canary-gaps.yaml` as a live, unsurfaced
canary. A future wave that surfaces it (a scenario or finding that actually
names or addresses FE import rollback/undo) should update this scorecard to
6/6 with the same evidentiary rigor used above — a citation or a genuine
semantic match backed by a real guard + finding, not a bare assertion.

## Related staleness action (same wave-exit cleanup)

CANARY-001's own `absence_proof` command now MATCHES on disk (the locale
rule it names was shipped this wave) — per `canary-gaps.yaml`'s own
staleness protocol, CANARY-001 is marked `stale: true` / `superseded_by:
CANARY-007`, and CANARY-007 (a genuinely-absent need for wave-2 — a
`frontend/tests/*.vitest.ts` unit test for the new locale-formatting rule)
was added in its place. This does not change the 5/6 surfacing result above
— CANARY-001 was surfaced in wave-1 regardless of its later staleness.
