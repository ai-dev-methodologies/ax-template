# Codex Critic Review — P1 Absorption PRD (Round 4, Iteration 2)

## Verdict: APPROVE

## Blocker closure verification (6 + 1)

1. Atomic SP30 collapse: CLOSED
   - SP30 is now the single atomic billing full_trio SP: Spec Trio, backend, L2/L3/L4, L1 prereqs, allowlist, and rules all land in one commit cluster.
   - Old billing-close SP31 is gone. Renumbering is SP30 billing, SP31 Korean, SP32 Forms, SP33 Tables, SP34 Admin.
   - `.draft` only appears as a prohibited loophole, not as a deliverable prefix.
   - §5.8 has five SP rows.

2. identity-verification backend_only: CLOSED
   - SP31 adds `identity-verification: backend_only`.
   - It names the required Spec Trio files: `specs/identity-verification-l0.yaml`, `contracts/identity-verification-openapi.yaml`, `blueprints/identity-verification-manifest.yaml`.
   - §5.8 includes `/ax-verify-domain identity-verification`.

3. observability_signal column: CLOSED
   - §5.8 adds `observability_signal`.
   - Every SP row has concrete signal names: billing counters/audit events, identity counters/audit event, forms metrics, table metrics, and admin/a11y metrics.

4. Shared-artifact ownership: CLOSED
   - §6.2 now covers the required shared artifacts: both `_MANIFEST.yaml` files, both `AGENTS.md` sentinels, `templates/DECISIONS.md`, and `trio_integrity_allowlist.yaml`.
   - Race-safe protocol is named and executable: rebase/check HEAD before commit, deterministic YAML insertion or sha256 regeneration, pre-merge consistency hook, retry up to 2 rebases.
   - Halt threshold is explicit: 3 consecutive same-file conflicts halt the SP and write an ESCAPE record.

5. business-registration fixture official data: CLOSED
   - SP31 fixture path is exactly `practices/evals/fixtures/business-registration-checksum/{pass/, fail_invalid_checksum/, fail_format_violation/}`.
   - `pass/` is constrained to 국세청/NTS or open-data.go.kr public sources, with URL and license per fixture entry in README.
   - Mock data is explicitly prohibited.

6. SP-card risks 4-quadruple: CLOSED
   - Spot checks:
   - SP30 Risk 5 has owner `SP30-backend-worker`, curl command, replay-accepted threshold, and timestamp-window/migration recovery.
   - SP31 Risk 1 has owner `SP31-frontend-worker`, Vitest command, misclassification threshold, and checksum-algorithm recovery.
   - SP32 Risk 2 has owner `SP32-L1-worker`, build + Playwright command, build/console threshold, and client-only wrapper recovery.
   - SP33 Risk 2 has owner `SP33-export-worker`, bundle-size command, >30 kB threshold, and dynamic-import recovery.
   - SP34 Risk 3 has owner `SP34-rules-worker`, failing-fixture command, helper-rename bypass threshold, and canonical-session matcher recovery.

S1. Payment/billing boundary: CLOSED
   - §5.2.6 has an explicit boundary table.
   - Payment owns one-shot authorization/capture/refund; billing owns subscription lifecycle, invoice issuance, recurring event normalization, and plans.
   - Shared concerns are declared pattern-only with no cross-import.
   - `no-billing-cross-import-from-payment` is added for Java + React with a failing fixture.

## Residual ambiguity verdict

- R1 Theme SSR: ACCEPTABLE
  - The ambiguity is isolated to SP34 and already has owner/command/threshold/recovery in the SP34 risk card. It does not affect SP30 atomicity or SP31-SP33 execution.

- R2 SP30 wall-time fallback: ACCEPTABLE
  - The fallback keeps SP30 as one atomic SP and limits recovery to rebase iterations, not scope splitting. §6.2/§7 halt thresholds bound the failure mode.

## Spot check (iter 1 closed material intact)

- §1 Options preserved and strengthened with Option F explicitly rejected.
- §10 honored constraints remain intact, including Tier-1 cap = 4, atomic Spec Trio, no raw RRN input, backend_only identity-verification, observability_signal, shared-artifact ownership, official fixture data, and payment/billing boundary.
- Tier-1 cap stays 4; no new top-level skill is proposed.
- Defer list is maintained, and `subscription-management` is removed from deferral/reclassified as absorbed by billing L4 in SP30.

## New attack on iter 2

- Specific criticism: Iter 2 has minor artifact-count and tier-classification drift in the Forms/Admin accounting. `code-block` is listed as L2 in §4 but L1 in SP32 deliverables, SP32 counts vary between 3/4 L1 and 6/9 L2 depending on section, and summary L2 math does not match the itemized totals. This should be normalized by team-builder before creating worker prompts so workers do not disagree on ownership.
- Grade: INFORMATIONAL

## Final verdict reasoning

All six iter1 hard blockers plus the payment/billing steelman are closed with concrete execution text, verification hooks, halt thresholds, and ownership. The two residual ambiguities are bounded to execution-time review inside their SPs and do not weaken /team readiness. The new attack is a documentation normalization issue, not a safety, atomicity, or verification blocker.

## ADR (FINAL — for Step 6 if APPROVE)

- Decision: Approve P1 Absorption PRD iter2 as canonical for Round 4 execution via /team-builder + /team for SP30-SP34.
- Drivers: strict atomic billing full_trio, backend_only identity-verification, preserved Tier-1 cap, official Korean regulatory fixtures, observable verification, and explicit shared-sentinel conflict handling.
- Rejected: keeping billing split across SP30/SP31, because it violates atomic Spec-Trio ordering.
- Rejected: deferring all non-billing polish, because Korean identity, forms/rich-content, tables/filters, and admin/security polish each retain evidence-backed acceptance anchors.
- Consequences: SP30 becomes the atomic billing domain commit cluster; SP31-SP33 may run after SP30 with sentinel serialization; SP34 closes after those SPs. Team-builder should normalize minor artifact-count drift before worker prompt generation.
