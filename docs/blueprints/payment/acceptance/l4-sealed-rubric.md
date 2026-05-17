## SEALED — L4 Sub-Agent Acceptance Rubric

This rubric is **sealed**. It was committed BEFORE any Payment-derived catalog
rules were added (P6), BEFORE the Payment implementation was written (P3.0–
P3.9), and BEFORE the sub-agent was prompted (P9). It MUST NOT be edited to
match whatever the sub-agent ultimately produced.

**Sealed at commit**: `<populated in P1.3 / US-006 — sealed_at_commit: TBD>`
**Pass/fail philosophy**: the rubric tests whether the **catalog discovers
itself** to a fresh agent — not whether the sub-agent's code is identical to
what the maintainer would have written.

---

## Evaluation criteria

Each criterion is **MUST_PASS** (failure invalidates the L4 result) or
**SHOULD_PASS** (failure is a discoverability gap to log + fix in a follow-up).

### Critical (MUST_PASS) — failure means the catalog has a load-bearing gap

| # | Criterion | Evidence required | Pass condition |
|---|-----------|-------------------|----------------|
| C1 | Sub-agent read `CLAUDE.md` BEFORE writing code | Output contains a verbatim quote of any non-trivial line from CLAUDE.md (≥10 words) | Quote present + correctly attributed |
| C2 | Sub-agent read `practices/AGENTS.md` BEFORE writing code | Output contains a verbatim quote of any rule frontmatter or rule body line | Quote present + correctly attributed |
| C3 | Sub-agent read `specs/payment-l0.yaml` BEFORE adding to it | Output contains a verbatim quote of an existing spec item from the file | Quote present + correctly attributed |
| C4 | The new spec item `PAYMENT-PROVIDER-007` is added to `specs/payment-l0.yaml` | The yaml file diff shows the new item with all required schema fields (id, chapter, requirement, test_method, verification_type, applicable, notes) | All fields populated, parses as YAML |
| C5 | The new test exists and is appropriately tagged | A new test class/method in `backend/src/test/.../payment/` with `@Tag("PAYMENT")` + `@Tag("PAYMENT-PROVIDER-007")` | Both tags present |
| C6 | Test was RED before implementation (TDD discipline) | Sub-agent's output shows a sequence: write test → `./gradlew testPayment --tests "*Provider007*"` RED → write impl → re-run GREEN | RED-then-GREEN sequence in output, with both gradle outputs included |
| C7 | After implementation, `./gradlew testPayment --tests "*Provider007*"` exits 0 | Sub-agent's terminal output (last 5 lines) shows `BUILD SUCCESSFUL` and the test name with status PASS | GREEN |
| C8 | After implementation, `./gradlew testPractices` exits 0 (no Java-rules regression) | Sub-agent's terminal output shows `BUILD SUCCESSFUL` for testPractices | GREEN |
| C9 | All 4 Java hard gates exit 0 after the work | Sub-agent's terminal output includes results of `bash practices/evals/{spec_ref,substance,time_decay,evidence}_guard.sh` | All PASS |
| C10 | AI2-3 paper exercise produced | Output contains a markdown table comparing Stripe PaymentIntent + Toss V2 against the existing `PaymentProvider` interface, with explicit Y/N abstraction-survival conclusion | Table present + conclusion stated |
| C11 | Single commit at the end on the active branch | `git log -1 --format='%s'` returns a message starting with `feat(payment): PAYMENT-PROVIDER-007 — ` | Match |

### Recommended (SHOULD_PASS) — failure is a soft signal of catalog roughness

| # | Criterion | Evidence required | Pass condition |
|---|-----------|-------------------|----------------|
| R1 | Sub-agent quoted `blueprints/payment-manifest.yaml` and `METHODOLOGY.md` (not just CLAUDE.md + AGENTS.md + payment-l0) | Output contains 5 quotes total (one per file in the prompt's mandatory reading list) | All 5 present |
| R2 | Test uses an injectable slow provider (not `Thread.sleep` in the controller) | The test diff shows a test-double of `PaymentProvider` configured to delay > 3s | Test-double pattern, not sleep in production code |
| R3 | Manifest `observability.metrics` updated to include the new counter name `payment_provider_slow_total` | `blueprints/payment-manifest.yaml` diff shows the metric added to the list | Metric present in manifest |
| R4 | No new `practices/rules/payment-*.md` rule added unless generalization-audit-justified | If a rule is added, it must include a rationale citing the audit; if no rule, the sub-agent must explain why (e.g., "observability assertion already covered by existing rule, no new rule needed") | Either justified rule OR justified absence |
| R5 | API contract assessment correct | Output states "no `contracts/payment-openapi.yaml` change needed because PROVIDER-007 is an internal observability concern" (or similar) | Contract correctly NOT touched |
| R6 | AI2-3 verdict is honest | If the sub-agent says the abstraction survives, the comparison table must actually demonstrate it (each Stripe/Toss surface mapped to existing interface methods). If it says abstraction does NOT survive, the minimal breaking change is specified concretely | Verdict matches the evidence shown |

---

## Overall verdict logic

- **PASS**: all 11 MUST_PASS + ≥4/6 SHOULD_PASS criteria met
- **PASS-WITH-CONCERNS**: all 11 MUST_PASS + 0–3 SHOULD_PASS met → log a follow-up to address the SHOULD_PASS gaps
- **FAIL**: any MUST_PASS criterion failed → catalog has a load-bearing gap; investigate which (most likely C1/C2/C3 — the documentation reading sequence — meaning the catalog is not self-discovering enough)

---

## Anti-rigging discipline

The following actions, taken AFTER the sub-agent runs, would invalidate the L4 result:

1. Editing this rubric to lower the bar so the sub-agent passes.
2. Editing the sealed prompt to remove a constraint the sub-agent violated.
3. Counting a sub-agent quote as "present" when it was generated AFTER the code was written (look for ordering signals in the transcript).
4. Letting the sub-agent re-run after seeing the rubric.

If any of these happen, the L4 result is **VOID** and must be rerun.

---

## On FAIL — diagnostic flowchart

If MUST_PASS C1/C2/C3 fail → agent didn't read docs first → catalog is not self-bootstrapping. Investigate:
- Is AGENTS.md sentinel placement obvious from a `ls` of the repo root?
- Does CLAUDE.md surface the path to AGENTS.md prominently?
- Should a top-level `START_HERE.md` or `README.md` directory section guide cold-start agents?

If MUST_PASS C4/C5 fail → spec / test scaffolding pattern not discoverable. Investigate:
- Are existing spec items (auth, CRUD, rate-limit, payment) clear enough as templates?
- Is the test class location convention discoverable?

If MUST_PASS C6 fails → TDD discipline not enforced. Investigate:
- Does METHODOLOGY.md or AGENTS.md mention "RED → GREEN" explicitly in the 5-step?

If MUST_PASS C8/C9 fail → catalog hard gates not surfaced. Investigate:
- Are the gates documented in CLAUDE.md / AGENTS.md as required pre-commit steps?

If MUST_PASS C10 fails → AI2-3 paper exercise skipped → sub-agent treated the prompt as "implement this" rather than "follow the full methodology". This is the most concerning failure mode: it means the catalog teaches doing, not thinking.

---

## Recording the result

The L4 evaluator (maintainer or follow-up reviewer at P9) writes the verdict
to `docs/blueprints/payment/acceptance/l4-subagent-test.md` with the
following structure:

```
# L4 Sub-Agent Acceptance Test — Result

**Sealed prompt commit**: <hash from P1.3>
**Sealed rubric commit**: <hash from P1.3>
**Execution date**: YYYY-MM-DD
**Overall verdict**: PASS | PASS-WITH-CONCERNS | FAIL | VOID

## MUST_PASS criteria (11)
- C1 ... PASS | FAIL — <evidence>
- ...

## SHOULD_PASS criteria (6)
- R1 ... PASS | FAIL — <evidence>
- ...

## AI2-3 paper exercise output
<verbatim from sub-agent>

## Diagnostic notes
<if any criterion failed, the diagnostic flowchart outcome>

## Follow-up actions
<if PASS-WITH-CONCERNS or FAIL, the specific catalog gaps to fix>
```
