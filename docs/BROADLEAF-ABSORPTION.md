# Broadleaf-absorption program — backlog + methodology

**Goal:** absorb Broadleaf Commerce's complete e-commerce feature set into the ax-template
catalog as **grounded, catalog-compliant reference domains** — NOT by copying Broadleaf's
code, but by extracting its feature set + correctness invariants and re-expressing them under
ax-template's spec-trio + rules + verification gates (often *stricter* than Broadleaf).

**Why:** the prior `ecommerce` domain was a 1457-LOC toy (Cart/Order/Product). ax-template's
catalog had only ever been validated against self-built toy demos. Broadleaf (real Spring Boot
3.5, 355k LOC, maintained) is the most comprehensive real e-commerce; absorbing it (a) validates
the catalog against real code, (b) grounds new rules in a production codebase (anti-fabrication),
(c) grows ax-template toward a Broadleaf-complete e-commerce capability.

**License posture:** copyright protects *expression* (code), not *ideas/functionality*
(17 USC §102(b)). We extract features + invariants (uncopyrightable) and write independent,
structurally-different, often-stricter code; we do NOT copy or redistribute Broadleaf source.
The only Broadleaf bytes in this repo are short verbatim line-quotes in rule `evidence:` blocks
(citation/criticism — fair use). The reference clone (`../broadleaf-modernized`) lives OUTSIDE
this repo's git and is never committed/redistributed. *(IANAL — for commercial redistribution,
get legal review of the Broadleaf Fair Use License.)*

## Per-vertical methodology (the absorption pipeline)

Each vertical ships INDIVIDUALLY, fully verified + committed. **No exception — accuracy over
speed.** The pipeline is MECHANICALLY ENFORCED (see "Mechanical enforcement" below):
1. **mine** — deep-read the Broadleaf subsystem → feature set + correctness invariants (file:line).
2. **anti-re-find census** — grep existing specs/rules; if the invariant is already absorbed, do
   NOT re-find it (record an explicit RE-FIND pointer instead). Most verticals are mostly re-find.
3. **spec** — `<vertical>-l0.yaml` compliance spec (families + items), or extend an existing spec.
4. **rule** — evidence-anchored catalog rule (evidence = Broadleaf GitHub file:line **single-line
   quote** + a genuine external standard, verbatim-verified). NEVER port Broadleaf source.
5. **domain** — independent reference implementation under ax gates (@AggregateRoot/@AggregateMember,
   sole-mutator service, @Version, @Check, ProblemDetail, immutable identity, injected Clock). Our
   classes/structure — zero Broadleaf bytes (`broadleaf_no_port_guard`).
6. **tests + verification-goal parity** — RestAssured ComplianceTest (BEHAVIORAL — round-trip, not
   `!=null`) + ViolationProofTest + per-domain gradle task. AND map Broadleaf's test **intent**
   (the scenarios its own tests verify — read Broadleaf's test files where they exist) to our
   behavioral assertions in `docs/broadleaf-parity/<vertical>.md`. We capture the same VERIFICATION
   GOAL as Broadleaf, never its test code (FUL-licensed). Review-tier verticals (no domain) record
   the goal mapping against the rule.
7. **adversarial review** — opus refute-by-default BEFORE commit (the first catalog pass was
   green-but-hollow; the review caught it — this gate is mandatory). Record the verdict in the parity record.
8. **verify** — test{Vertical} + run-all-guards (incl. `broadleaf_no_port` + `broadleaf_absorption_parity`)
   + R25, then commit + push + parity record.

## Mechanical enforcement (no exception)

The methodology is not advisory — it is enforced inside R25 by these guards (and the existing gate stack):
- **`broadleaf_no_port_guard.sh` [78]** — LICENSE safety. Broadleaf is under the **Fair Use License
  v1.0** (not OSI/permissive); its source must never be ported. Asserts our implementation tree
  (`backend/src` + `frontend/src`) has zero Broadleaf bytes (no `import`/`package org.broadleafcommerce`,
  no FUL header). The clone lives OUTSIDE git and is never committed. Only short single-line citations
  in `practices/rules/*.md` evidence blocks are allowed (fair-use grounding, not scanned).
- **`broadleaf_absorption_parity_guard.sh` [79]** — METHODOLOGY completeness + VERIFICATION-GOAL
  parity. Every absorbed vertical MUST have a complete `docs/broadleaf-parity/<vertical>.md` record
  (vertical/broadleaf_source/spec_items/rule/behavioral_test/adversarial_review + ≥1 verification-goal
  parity row). Referenced spec items / rule / test artifacts are validated to EXIST — a record cannot lie.
- **`broadleaf_module_exhaustion_guard.sh` [80]** — the FINITE module-set sweep: `docs/BROADLEAF-COMPLETENESS.md`
  classifies EVERY Broadleaf core subsystem (ABSORBED/RE-FIND/SKIP/RESIDUE) with non-empty evidence; row count ==
  `module_count`, RESIDUE rows == `residue_count`, no unledgered residue parity record.
- **`quick_verify_no_audit_guard.sh` [81]** — keeps the ITERATION-ONLY `verify/quick-verify.sh` dev helper from
  being mistakable for the gate (no audit-log write, no `verify-completion.sh` invocation, banner present).
- Plus the existing gates: `evidence_guard` (no fabricated evidence), per-domain `test{Domain}` +
  `ViolationProofTest` (the invariant holds + violations are structurally impossible), R25.

The parity registry is `docs/broadleaf-parity/REGISTRY.md`.

## Reviewer + checkpoint operational notes (session-retrospective captures)

**codex `--critic` reviewer — get the verdict with a SHORT focused call.** `codex exec --critic` (the
`oh-my-claudecode:ralplan`/`ralph` codex reviewer) reliably runs a long preamble (serena symbol lookups,
runs guards itself) and frequently TIMES OUT before emitting the literal `VERDICT:` token on a deep review.
Do NOT burn three timed-out runs. Instead, after the analysis is established (or for the final verdict), make
a SHORT call: pipe the prompt via **stdin** (`codex exec -s read-only < prompt.txt` — inline single-quotes
inside a single-quoted bash arg break the quoting and make codex wait on stdin), state the established facts,
and ask for "EXACTLY ONE line starting with `VERDICT:`". Or rely on the substantive findings (0 blockers +
all guards PASS) when the token never renders.

**ultragoal final quality-gate — the EXACT JSON shape (one-shot, no trial-and-error).** For the LAST story,
`omc ultragoal checkpoint --goal-id <id> --status complete` requires (each field enforced in this order):
```jsonc
// --quality-gate-json
{
  "aiSlopCleaner": { "status": "passed", "evidence": "…" },
  "verification":  { "status": "passed", "commands": ["…","…"], "evidence": "…" },
  "codeReview":    { "status": "passed", "recommendation": "APPROVE", "architectStatus": "CLEAR", "evidence": "…" }
}
// --claude-goal-json  → "goal": { "objective": "<the exact aggregate objective>", "status": "complete" }
```
Notes: `status` must be the string `"passed"` (not `clean`); the key is `evidence` (not `detail`);
`verification.commands` must be a non-empty array; `codeReview.recommendation` must be `"APPROVE"` and
`architectStatus` `"CLEAR"`; the claude-goal `status` must be `"complete"` for the final story.

## Absorption backlog (dependency-ordered)

| # | Vertical | Broadleaf source | status |
|---|---|---|---|
| 1 | **catalog** (Product · default/variant SKU · option→SKU resolution · category · lifecycle · price hook) | `core/.../catalog` (21 entities/21k LOC) | **✅ shipped** — `commercecatalog`, spec `catalog-commerce-l0`, strengthens 2 Broadleaf gaps (variant-resolution determinism, price-presence) |
| 2 | pricing (price-list · dynamic · pricing workflow) | `core/.../pricing` (3.3k LOC) | mined-pending |
| 3 | promotion/offer (B1-B9 mined in pilot) | `core/.../offer` (32 entities/21k LOC) | **mined** (B1/B3/B7/B8 cross-cutting candidates; promotion-l0 spec pending) |
| 4 | order/cart (cart · order · item · fulfillment-group · lifecycle) | `core/.../order` (28 entities/25k LOC) | pending |
| 5 | checkout (workflow) | `core/.../checkout` (2.5k LOC) | pending |
| 6 | payment (txn · gateway) | `core/.../payment` (4.6k LOC) | pending (extend existing `payment`) |
| 7 | inventory · fulfillment · customer | `core/.../inventory` + profile | pending |
| 8 | search · rating · CMS · cross-cutting (money/currency/i18n/admin-metadata) | various | pending (supporting) |

## Key findings so far

- **The catalog is real-world-valid, not self-referential** (offer pilot): Broadleaf independently
  enforces ax-template's money/conservation/rounding/determinism rules; no ax rule was shown wrong.
  Violations classified as real-Broadleaf-gaps (TOCTOU max-uses, no @Version), ax-improvements
  (append-only audit), or era-conventions (RFC-9457 absent from pre-Spring-6 core).
- **Broadleaf has real defects ax-template's rules prevent**: offer max-uses TOCTOU race (no
  lock/unique); catalog variant-resolution ambiguity (`iterator().next()`); nullable sellable
  price. The absorbed verticals STRENGTHEN these (unrepresentable duplicate, price-presence gate).
- **Absorption is hard — the first AI pass is often green-but-hollow.** The catalog vertical's
  first build had 20 green tests but a non-functional core (resolveSku 404'd everything, reparent
  was a no-op, 5 tests asserted nothing). The mandatory adversarial-review gate caught it; the
  rework made it genuinely functional (behavioral round-trip tests). Every vertical must pass
  this gate — that is the difference between absorption and another toy.
- **Test-schema note**: tests run `ddl-auto: create-drop` (no Flyway), so entity-level
  @Check/@UniqueConstraint/@Version DO run (Hibernate-generated), but V### migration-only SQL does
  not — migration ViolationProof assertions are text-substring checks, so uniqueness must also be
  on the entity (where it actually runs) and proven by a behavioral duplicate-insert test.
