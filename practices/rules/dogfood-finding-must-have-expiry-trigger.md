---
title: Dogfood-ledger scope_deferral findings MUST include an explicit expiry trigger
impact: MEDIUM
impactDescription: "A scope_deferral entry without a concrete re-open condition becomes permanent technical debt: the catalog cannot mechanically detect when the underlying constraint changes (cap-bump, new audit emission, new entity domain) and the deferral silently outlives its rationale. Catalog quality regresses one ledger entry at a time."
tags:
  - dogfood
  - ledger
  - catalog-quality
  - scope-deferral
  - expiry-trigger
  - technical-debt
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-DOGFOOD-LEDGER-001"
verification:
  source: "practices/evals/dogfood_finding_expiry_trigger_guard.sh (R85b — 45th hard guard)"
  pattern: "Every docs/dogfood-ledger/*.yaml entry where classification=scope_deferral MUST contain at least one expiry-trigger marker in its finding text: 'expiry trigger:', 're-opens when', 're-opens before', 'reopens before', 'defer until', or 'before <fork-receiver action>'. Markers are case-insensitive."
upstream:
  - "https://martinfowler.com/bliki/TechnicalDebtQuadrant.html"
  - "https://csrc.nist.gov/pubs/sp/800/53/r5/upd1/final"
evidence:
  - source_type: external
    citation: "Martin Fowler — TechnicalDebtQuadrant. 'When you decide to take on technical debt, you should always know when you intend to pay it back. Otherwise, the debt becomes permanent.' Applied to dogfood scope_deferrals: a deferral with no recorded re-open condition is the inadvertent-deliberate quadrant — known shortfall, no payback plan."
    url: "https://martinfowler.com/bliki/TechnicalDebtQuadrant.html"
    quoted_at: "2026-05-26"
  - source_type: external
    citation: "NIST SP 800-53 Rev. 5 — Control RA-7 (Risk Response) requires that risk-acceptance decisions document the assumptions, the rationale, AND the conditions under which the decision is to be reviewed. The control text: 'Document risk responses for risks the organization decides to mitigate, accept, share, or avoid.' For ax-template scope_deferrals (a risk-acceptance act), the re-review condition is the expiry trigger."
    url: "https://csrc.nist.gov/pubs/sp/800/53/r5/upd1/final"
    quoted_at: "2026-05-26"
---

## Dogfood-ledger scope_deferral findings MUST include an explicit expiry trigger

**Impact: MEDIUM — defer-without-trigger is invisible technical debt that the catalog cannot self-audit.**

R71 `dogfood_ledger_guard.sh` mechanically enforces ledger structure (iteration, persona, finding, classification, references_artifact_path). It does NOT enforce content quality — specifically, a `scope_deferral` entry can ship with reasoning like "fork-receiver decides" and pass the guard, even though that reasoning gives the catalog no way to mechanically detect when the deferral should re-open.

This rule closes that gap: every `scope_deferral` entry MUST include an explicit re-open condition. A reader (or a future maintainer scanning the ledger) MUST be able to answer "what would make this no longer deferred?" by reading the finding text alone.

**Two acceptable trigger shapes:**

1. **Explicit phrase**: `"expiry trigger: <condition>"` or `"re-opens when <condition>"` or `"re-opens before <condition>"`.
2. **Inline "before X" pattern**: a sentence containing `"before <fork-receiver action>"` where the action is concrete (cap bump, first audit-log emission, first PII-linked entity wired, etc.).

The guard is intentionally lenient on phrasing — any one of the above patterns satisfies it. The strictness is on **presence**, not form.

**Incorrect — defer with no re-open condition:**

```yaml
- persona: P2
  finding: "F7: body column is TEXT plain; fork-receiver-owned decision (catalog refuses to choose Hibernate @ColumnTransformer / pgcrypto / RDS at-rest)"
  classification: scope_deferral
```

The reader knows the catalog deferred, but cannot tell when the deferral should re-open. Is it when fork-receiver enables column-at-rest encryption? When the body content gets a new field that's clearly sensitive? When a compliance audit flags it? The text is silent.

**Correct — explicit "before" trigger:**

```yaml
- persona: P2
  finding: "F7: body column is TEXT plain; fork-receiver-owned decision (catalog refuses to choose Hibernate @ColumnTransformer / pgcrypto / RDS at-rest). Expiry trigger: re-opens before the fork-receiver renders ANY user-typed prose into the body template (template engine variable substitution from a user-controlled field), because at that point a verbatim-stored body crosses into the PII surface that demands at-rest encryption."
  classification: scope_deferral
```

Reader now knows: the deferral persists while body content is system-generated transactional text; it re-opens the moment user-typed content enters the template.

**Apply this rule to**: every entry in `docs/dogfood-ledger/*.yaml` whose `classification` is `scope_deferral`.

**When NOT to apply**: entries classified as `real_bug` (those are closed in the same wave per the dogfood protocol) or `methodology_gap` (those should be addressed by changing the methodology, not deferred indefinitely). Only `scope_deferral` carries the trigger requirement.

A pair-with rule: R71 `dogfood_ledger_guard` already enforces the classification schema. R85 is the content-quality layer on top — same ledger, finer-grained discipline.

Reference: [Martin Fowler — Technical Debt Quadrant](https://martinfowler.com/bliki/TechnicalDebtQuadrant.html)

Reference: [NIST SP 800-53 Rev. 5 — RA-7 Risk Response](https://csrc.nist.gov/pubs/sp/800/53/r5/upd1/final)
