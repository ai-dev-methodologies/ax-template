## SEALED — L4 Sub-Agent Acceptance Prompt

This prompt is **sealed**. It was committed BEFORE the Payment implementation
(P3.0–P3.9), BEFORE any Payment-derived catalog rules (P6), and BEFORE the
sub-agent was invoked (P9). It MUST NOT be edited to make the sub-agent's
work look better after the fact. If the catalog doesn't guide the sub-agent
to satisfy this prompt's constraints from a cold start, the catalog has a
gap.

**Sealed at commit**: `sealed_at_commit: fc73323`
**Source of pass/fail**: `l4-sealed-rubric.md` (sibling file, also sealed)

---

## The prompt that will be given to the sub-agent at P9 (verbatim, no edits)

```
You are a general-purpose engineering agent. You have no context from any
prior conversation. The repository at /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-template
is the ax-template composition kit. You have full read + write access.

## Your task

Add a new acceptance item to the Payment blueprint:

  PAYMENT-PROVIDER-007 — provider response latency > 3s logged as warning
                          with metrics counter increment.

This is a small addition. You should not need to rewrite existing payment
code. You should:
1. Add the spec item (with full schema parity to existing PAYMENT-PROVIDER-*
   items in specs/payment-l0.yaml).
2. Update blueprints/payment-manifest.yaml#observability.metrics to include
   the new counter name `payment_provider_slow_total`.
3. Write a @Tag("PAYMENT") + @Tag("PAYMENT-PROVIDER-007") test that proves
   a slow provider response (>3s) increments the metric and emits a WARN
   log. The test must follow the TDD discipline: write the test first,
   confirm it FAILS (RED), then write the implementation, then confirm it
   PASSES (GREEN). Both runs of `./gradlew testPayment --tests "*Provider007*"`
   should appear in your transcript.
4. Write the implementation. It should be a thin observability concern
   (e.g., a wrapper / decorator / aspect around PaymentProvider calls).
   Avoid putting timing logic in business-service code.
5. Verify regression: `./gradlew testPractices` and the 4 Java hard gates
   (bash practices/evals/{spec_ref,substance,time_decay,evidence}_guard.sh)
   must all exit 0 after your work.
6. Do a paper exercise: in your final message, include a markdown table
   that maps Stripe PaymentIntent's API surface AND Toss Payments V2's API
   surface against the existing PaymentProvider Java interface. State
   explicitly whether the abstraction survives both mappings without
   breaking changes. If it does not, specify the minimal breaking change.

## Mandatory reading BEFORE any code

You MUST read all of the following files BEFORE you write any spec, test,
or implementation code. Demonstrate that you have read each by quoting at
least one non-trivial line (≥10 words) from each file in your output:

- /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-template/CLAUDE.md
- /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-template/METHODOLOGY.md
- /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-template/practices/AGENTS.md
- /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-template/specs/payment-l0.yaml
- /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-template/blueprints/payment-manifest.yaml

## Constraints

- Single commit at the end on the active branch (main). Use a conventional
  commit message starting with `feat(payment): PAYMENT-PROVIDER-007 — `.
- Do NOT add new rules to practices/rules/ unless a generalization-audit
  rationale clearly justifies the addition (see practices/MAINTAINER.md
  and docs/blueprints/payment/decisions.md if it exists for prior audit
  decisions). If you add a rule, document the rationale inline in the
  rule frontmatter and in the commit body.
- Do NOT modify contracts/payment-openapi.yaml unless the new item
  actually requires a contract change. PROVIDER-007 is an internal
  observability concern.
- Do NOT skip the RED step. The transcript must show a failing
  `./gradlew testPayment --tests "*Provider007*"` run before the
  implementation is written.
- Do NOT edit any file under `docs/blueprints/payment/acceptance/` —
  these are sealed acceptance artifacts.

## Definition of done (your responsibility to confirm before claiming complete)

- `./gradlew testPayment --tests "*Provider007*"` exits 0
- `./gradlew testPractices` exits 0
- `bash practices/evals/spec_ref_guard.sh && substance_guard.sh && time_decay_guard.sh && evidence_guard.sh` all exit 0
- Single semantic commit on main
- Markdown paper exercise (Stripe PaymentIntent + Toss V2 mapping) in
  your final message
- You quoted ≥10 words from each of the 5 mandatory reading files

When done, output a final summary explicitly listing what changed (file
paths), what tests pass, and the result of the paper exercise. Do not
ask for permission — finish the work.
```

---

## Why this specific delta-feature?

PAYMENT-PROVIDER-007 was chosen as the L4 acceptance task because:

1. **Small enough to complete in one sitting** — adding one spec item + one
   test + one wrapper class + one manifest line is < 100 lines of net change.
2. **Exercises the full methodology** — requires spec edit, test-first
   discipline, impl, manifest update, hard-gate compliance, regression check.
3. **Discoverable** — the existing PAYMENT-PROVIDER-001..006 items in
   `specs/payment-l0.yaml` are clear templates the sub-agent can pattern-match.
4. **No rule-quota trap** — the prompt explicitly says no new rule unless
   audit-justified, so the sub-agent must judge generalization properly
   (failing this is a SHOULD_PASS gap in the rubric, not a MUST_PASS fail).
5. **Forces the AI2-3 paper exercise** — the paper exercise is the rigging-
   resistant version of testing whether the provider abstraction survives
   real-world variation. Sub-agents that skip it have not done the work.
6. **Observability concern, not business logic** — separates the sub-agent's
   competence on cross-cutting concerns from its competence on payment
   semantics, which is where the Payment blueprint has already done the hard
   work.

If a future L4 fails on PAYMENT-PROVIDER-007 specifically (rather than on the
catalog reading sequence), it means the observability extension pattern is
underspecified. Use that signal.

---

## Post-execution: how the result will be evaluated

See `l4-sealed-rubric.md` (sibling file). The rubric evaluates the sub-agent's
output against 11 MUST_PASS criteria and 6 SHOULD_PASS criteria.

The maintainer (or a follow-up reviewer) at P9 will:
1. Invoke a fresh sub-agent via the Agent tool with the prompt above (no
   edits).
2. Capture the sub-agent's full transcript.
3. Evaluate against the sealed rubric criterion-by-criterion.
4. Record results in `docs/blueprints/payment/acceptance/l4-subagent-test.md`.

If the rubric is edited between sealing and execution, the L4 result is VOID.
