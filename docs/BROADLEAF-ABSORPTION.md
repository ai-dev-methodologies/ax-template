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

Each vertical ships INDIVIDUALLY, fully verified + committed:
1. **mine** — deep-read the Broadleaf subsystem → feature set + correctness invariants (file:line).
2. **spec** — `<vertical>-l0.yaml` compliance spec (families + items).
3. **rule** — evidence-anchored catalog rule (evidence = Broadleaf GitHub file:line + a genuine
   external standard, verbatim-verified).
4. **domain** — reference implementation under ax gates (@AggregateRoot/@AggregateMember markers,
   sole-mutator service, @Version, @Check, ProblemDetail, immutable identity, injected Clock).
5. **tests** — RestAssured ComplianceTest (BEHAVIORAL — round-trip, not `!=null`) +
   ViolationProofTest + per-domain gradle task.
6. **adversarial review** — opus refute-by-default BEFORE commit (the first catalog pass was
   green-but-hollow; the review caught it — this gate is mandatory).
7. **verify** — test{Vertical} + run-all-guards + R25, then commit.

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
