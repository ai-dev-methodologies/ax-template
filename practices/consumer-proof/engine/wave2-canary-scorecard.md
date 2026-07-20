# Wave-2 canary scorecard — 9/9 logged, 0 planted-but-unlogged

Records what happened to every canary touched by wave-2 (`docs/dogfood-ledger/engine-w2-iter1.yaml`
through `iter7.yaml`), mirroring `wave1-canary-scorecard.md`'s evidentiary discipline: a canary's
disposition this wave must be traceable to disk, not asserted from memory.

Wave-2 net-flipped 5 of 7 planned coverage-map.yaml cells (QUERY-BOUNDS.XB, AUDIT-PII.XB, AUTHZ.FE,
TIME-LOCALE.FE to `covered`, plus approval-workflow.XB gap→partial). **The money cell
(S2.MONEY-QUANTITY.XB) stays `partial`, NOT covered** — its canary (CANARY-008) genuinely closed, but a
wave-2 codex adversarial finding (finding-3+4) reverted the cell `covered→partial`: the parity test had
been bound to a DEAD `PaymentResponse` class emitted by no endpoint, and repointing it at the real wire
emitter (`PaymentBodyMapper.toBody`) surfaced two open money-contract defects (P1-68 response major-unit
decimals, P1-69 request 100× overcharge), so `covered` is deferred to wave-3. Cell 7 (CANARY-012) was
REJECTed by adversarial review and stays partial — see `docs/dogfood-ledger/engine-w2-iter7.yaml`. That
activity retired 6 pre-existing canaries (003, 007, 008, 009, 010, 011 — canary closure is distinct from
a cell flip: 008's canary closed while its cell stayed partial) and left 1 (012) genuinely still open.
This 5-flips / money-stays-partial accounting matches `docs/BACKLOG.md`'s 2026-07-20 correction. Retiring
CANARY-003 and CANARY-007 vacated two canary "slots"; wave-2 planted two fresh, genuinely-absent needs
(013, 014) to fill them, per `canary-gaps.yaml`'s own staleness protocol (a stale canary is superseded,
never silently deleted).

## Method

Same as wave-1: "surfaced/closed" requires disk evidence — an explicit citation of the canary ID in
a `docs/dogfood-ledger/engine-w2-iter*.yaml` finding, cross-checked against `canary-gaps.yaml`'s own
`stale`/`staleness_note`/`superseded_by` fields (the authoritative record of what actually closed each
canary) and `practices/consumer-proof/engine/coverage-map.yaml`'s cell status. A "newly planted"
canary is scored PASS if (a) its `absence_proof` command was actually re-run and returned 0 matches at
plant time (not merely asserted), and (b) it is cited by name in at least one wave-2 ledger file or
this scorecard — the bar for "not silently orphaned," not "already surfaced" (a fresh plant cannot be
surfaced in the same wave it was planted; that is wave-3's job).

## Scorecard

| Canary | Need (short) | Wave-2 disposition | Evidence | Citation type |
|---|---|---|---|---|
| CANARY-003 | XB pagination-envelope schema/contract parity test | **CLOSED, logged** | `canary-gaps.yaml` CANARY-003 `stale: true` / `superseded_by: CANARY-013` with a re-run `staleness_note` (grep now matches `frontend/tests/page-envelope-parity.vitest.ts`); `docs/dogfood-ledger/engine-w2-iter1.yaml` F1 names `CANARY-003` verbatim; `coverage-map.yaml` `S2.QUERY-BOUNDS.XB` gap→covered | (a) explicit ID citation, both files |
| CANARY-007 | FE application-level vitest for Intl.NumberFormat/DateTimeFormat usage | **CLOSED, logged** | `canary-gaps.yaml` CANARY-007 `stale: true` / `superseded_by: CANARY-014` with a re-run `staleness_note` (grep now matches `frontend/tests/locale-format.vitest.tsx`, after an honest glob-extension correction — see the note); `docs/dogfood-ledger/engine-w2-iter5.yaml` F1 names `CANARY-007` verbatim; `coverage-map.yaml` `S2.TIME-LOCALE.FE` partial→covered | (a) explicit ID citation, both files |
| CANARY-008 | XB money.ts↔BE payment JSON minor-unit contract parity | **CANARY CLOSED, cell STAYS partial, logged** | `canary-gaps.yaml` CANARY-008 `stale: true` with a re-run `staleness_note` (grep now matches `MoneyContractParityTest.java` + `money-contract-parity.vitest.ts` + the golden fixture); `docs/dogfood-ledger/engine-w2-iter2.yaml` F1 names `CANARY-008` verbatim; `coverage-map.yaml` `S2.MONEY-QUANTITY.XB` **stays `partial`** — the parity test was reverted covered→partial (wave-2 codex finding-3+4) after it was found bound to a DEAD `PaymentResponse` class; repointed at the real emitter `PaymentBodyMapper.toBody`, exposing P1-68/P1-69, so `covered` defers to wave-3 | (a) explicit ID citation, both files |
| CANARY-009 | XB masked-PII-survives-into-FE-render test | **CLOSED, logged** | `canary-gaps.yaml` CANARY-009 `stale: true` with a re-run `staleness_note` (grep now matches `frontend/tests/audit-log-redaction-render.vitest.tsx`); `docs/dogfood-ledger/engine-w2-iter3.yaml` F1 names `CANARY-009` verbatim; `coverage-map.yaml` `S2.AUDIT-PII.XB` partial→covered | (a) explicit ID citation, both files |
| CANARY-010 | FE ax/* ESLint rule mechanizing caller-identity/impersonation-source enforcement | **CLOSED, logged** | `canary-gaps.yaml` CANARY-010 `stale: true` with a re-run `staleness_note` (grep now matches `practices-react/eslint-plugin-ax/rules/no-caller-identity-from-props.js`); `docs/dogfood-ledger/engine-w2-iter4.yaml` F1 names `CANARY-010` verbatim; `coverage-map.yaml` `S2.AUTHZ.FE` partial→covered | (a) explicit ID citation, both files |
| CANARY-011 | XB black-box RestAssured IT for approval-workflow | **CLOSED, logged** | `canary-gaps.yaml` CANARY-011 `stale: true` with a re-run `staleness_note` (`find` now matches `ApprovalFlowIT.java`); `docs/dogfood-ledger/engine-w2-iter6.yaml` F1 names `CANARY-011` verbatim; `coverage-map.yaml` `S1.approval-workflow.XB` gap→partial (only e2e-flow leg closes — 3 of 4 XB legs remain, so the cell itself stays partial even though the canary is genuinely closed) | (a) explicit ID citation, both files |
| CANARY-012 | FE composition-behavioral gate for saas-subscription recipe | **STAYS GAP, logged** | `canary-gaps.yaml` CANARY-012 unchanged (no `stale` field) — its own inline 2026-07-20 note records the opus REJECTion explicitly; `docs/dogfood-ledger/engine-w2-iter7.yaml` F1 names `CANARY-012` verbatim and states "remains LIVE (not stale)"; `coverage-map.yaml` `S3.saas-subscription` notes enriched but status UNCHANGED at partial; `docs/BACKLOG.md` P2-29 registers the root cause (S3 nonvacuity bar undefined) as an open finding | (a) explicit ID citation, both files |
| CANARY-013 | XB automated schema-parity test for the ERROR half of the envelope (ProblemDetail vs parse-error.ts) | **PLANTED** | `canary-gaps.yaml` CANARY-013 — `absence_proof` grep re-run this wave, confirmed 0 matches (`grep -rliE "problem.?detail.{0,40}parity\|parity.{0,40}problem.?detail\|error.?contract.{0,30}(parity\|schema)" backend/src/test frontend/tests contracts/*.yaml` → exit 1); cited by name in `docs/dogfood-ledger/engine-w2-iter1.yaml` F1 (as CANARY-003's successor) and this scorecard | targets wave-3; not surfaceable in the same wave it was planted |
| CANARY-014 | FE test asserting UTC→local-timezone-correct rendering | **PLANTED** | `canary-gaps.yaml` CANARY-014 — `absence_proof` grep re-run this wave, confirmed 0 matches (`grep -rliE "timezone.{0,60}(render\|correct)\|render.{0,60}(utc\|timezone)" frontend/tests` → exit 1); cited by name in `docs/dogfood-ledger/engine-w2-iter5.yaml` F2 (as CANARY-007's successor) and this scorecard | targets wave-3; not surfaceable in the same wave it was planted |

## Result: 9/9 logged, 0 FAIL

Every canary this wave touched (6 closures + 1 stays-gap + 2 plants) has a citation trail across at
least two independent disk artifacts (`canary-gaps.yaml` and a `docs/dogfood-ledger/engine-w2-iter*.yaml`
finding, plus `coverage-map.yaml`'s cell status for the 7 coverage-affecting rows). No planted-but-unlogged
canary exists this wave.

## What did NOT change

CANARY-002, CANARY-004, CANARY-005, CANARY-006 (wave-1 canaries not touched by wave-2's 7 cells) are
untouched — still whatever `stale`/live state wave-1 left them in. This scorecard does not re-verify
them; see `wave1-canary-scorecard.md` for their record.

## Honesty note carried over from wave-1's exit cleanup

CANARY-007's own stored `absence_proof` glob (`frontend/tests/*.vitest.ts`) does NOT literally match
the closing file (`locale-format.vitest.tsx` is a `.tsx`) — re-running the exact stored command still
returns exit 1 (0 matches). The `staleness_note` on CANARY-007 in `canary-gaps.yaml` records this
honestly rather than silently widening the glob and calling it unchanged: the canary is genuinely
closed (a corrected glob including `.tsx` matches), but the ORIGINAL absence_proof text undercounts by
one file extension. A future canary-gaps.yaml hygiene pass should default new FE-test absence_proof
globs to `*.vitest.{ts,tsx}` to avoid this class of near-miss.
