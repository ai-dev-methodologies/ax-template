# Architect Review — R9 iter 1

## Verdict: APPROVE-WITH-CONDITIONS (lean APPROVE; ITERATE only if Critic blocker emerges)

2 HIGH + 4 MEDIUM findings (matches R6/R7 rigor band). All 6 are 1-line-to-30-min fixes. Synthesis-A trim REJECTED. 3-SP Synthesis-B APPROVED (mirrors R7 precedent).

## Steelman antithesis

The strongest case AGAINST R9: **the catalog is approaching "L4 sprawl by precedent."** Each cycle since R5 has either added an L4 or activated a deferred recipe, citing the previous cycle as license. R9 introduces TD-027 which *codifies* an L4-expansion convention — converting an ad-hoc heuristic into a paved road for future planners. The three conditions are reasonable but weak gates: every realistic primitive can meet them if framed properly. R9 is graduating the catalog from "12 carefully chosen domains" to "expandable indefinitely along claimed-distinct axes."

A more aggressive position: notification L4 *should* absorb webhook-emit as `NOTIF-WEBHOOK-EMIT-001..005`. The "materially distinct semantics" argument leans on implementation details (HMAC vs template rendering) fork-receivers can implement either way. Composition-kit benefit is small relative to cost (Spec Trio quartet + 3 ADRs + new sealed-verdict harness).

## Tradeoff tensions

1. **Catalog discipline vs self-extensibility.** The same property that lets R9 add webhook (spec-first + evidence-anchored = expansionary) is the property that dissolves the "frozen catalog" framing. TD-027 tries to tighten this — but the gate is judged by the same planner who proposes the new L4. No independent reviewer between "I think conditions hold" and "L4 ships". One human-time reviewer per cycle is a thin moat against sprawl.

2. **PRD verbosity (510L) vs decision density.** R7 was 434L, R8 was 449L; R9 is 510L (+13.5% vs R8). Overage rationale (NET-NEW Spec Trio + TD-027 + denser ledger) is load-bearing — TD-027 alone justifies ~40L. But §4.4 has redundancy (~30-40L trim possible).

3. **Synthesis-B's partial-tag table vs catalog-coherence.** Partial-tag `v1.7.0-webhook` ships webhook L4 with `applied_recipes: [internal-it]` even though internal-it might be `active-verdict-pending`. Internally consistent — but fork-receiver inspecting webhook README sees a recipe not active in _MANIFEST.yaml. 3-9 day window of catalog desync.

## Synthesis

**Tighten TD-027 condition (c)** from "at least one in-scope active recipe needs the candidate primitive" → "at least **TWO** in-scope use-cases (active recipe + plausible R10+ deferred candidate)." Internal-it + api-gateway-relay satisfies both. Raises L4-introduction bar without blocking R9.

## Principle check (DELIBERATE)

Auto-trigger: new L4 = architectural mutation.

- **Composition-kit-not-single-product:** PRESERVED. Webhook composes; no new product surface.
- **Spec-before-code + evidence-anchored:** PRESERVED. 4 English + 2 Korean verbatim — strongest L4-introduction evidence chain in catalog history.
- **Tier-1/Tier-2 frozen:** PRESERVED. 4/8 unchanged.
- **L1/L2/L3 unchanged:** PRESERVED.
- **No-new-rule-family:** PRESERVED via `co-shipped-rule: webhook-secret-encryption` (R7 INV-005 precedent).
- **YAGNI:** MILD VIOLATION (LOW). TD-027 authors convention used by zero recipes today; codifies after-the-fact heuristic. Justifiable.
- **L4-cap-as-discipline-signal:** CONTESTED (MEDIUM). L4 11→12 second non-R3 addition (billing R5; webhook R9). Two precedents make a pattern. TD-027 mitigates via gate.

## Findings

### HIGH

**H1 — TD-027 condition (c) self-fulfilling** (PRD `:404-410`). Gate "at least one in-scope active recipe needs the primitive" can be satisfied by writing one recipe alongside the L4 proposal — exactly what R9 is doing. **Fix:** tighten to "at least one **shipped active recipe** consumes it AND at least one **plausible deferred candidate** documents the need" — forces TWO consumer signals. Internal-it + api-gateway hypothetical satisfies. Effort: 1-line edit.

**H2 — DECISIONS.md format drift** (`practices/DECISIONS.md`). PRD §8 lands TD-025/026/027 in `practices/DECISIONS.md` — but the file currently uses `## <RULE_ID>` + structured key:value sections (lines 35-50), NOT `- **TD-2026-MM-DD-NNN**` bullets. ZERO `TD-2026-*` entries exist on disk (R6/R7/R8 referenced inline only). R9 is first cycle to ship "append TD-XXX to DECISIONS.md" as SP deliverable but format unclear. **Fix:** SP45/SP45b prep step retrofits format — either backfill prior TD-IDs OR document bullet-format convention with opening note. Effort: 30 min retrofit OR 1-line format-note.

### MEDIUM

**M3** §4.4 redundant verbatim summary table (`:281-294`). Per-deliverable density floor paragraph + per-deliverable verbatim summary partially overlap main ledger. Trim ~7 lines. **Fix:** trim summary table 5 min.

**M4** webhook-domain.test.sh harness assertion-count semantics (`:204-209` + §7 P4). "8/15 assertions PASS" threshold is novel. **Fix:** SP45 §4.5 add 1-line cite: "assertion-count semantics mirror `scheduler-domain.test.sh` shape — N assertions, ≥M PASS gate". Drops P4 risk MEDIUM→LOW.

**M5** INV-005 co-shipped-rule promotion criterion ambiguous (`:223`). "R10 maintainer review may consider promoting `webhook-secret-encryption` once a second recipe (e.g. payment-callback-secret) demonstrates non-webhook use case" — payment-callback-secret is hypothetical, no R10 deferred-recipe entry. **Fix:** either name real R10+ candidate OR mark "promotion deferred indefinitely; remains recipe-level invariant unless cross-domain need emerges". 1 line.

**M6** Partial-tag catalog-desync window (`:362-370`). Second row (webhook-only PASS) ships webhook README `applied_recipes: [internal-it]` while _MANIFEST.yaml has internal-it as `active-verdict-pending`. Observable desync. **Fix:** SP45b commit OR webhook README inline annotation `applied_recipes: [internal-it]  # verdict pending — see _MANIFEST.yaml`. 1 line.

## Synthesis-A trim assessment

**Could R9 ship webhook L4 only (defer internal-it to R10)?** **REJECTED.**

- Internal-it has 4 verbatim PASS (Jira + PagerDuty + Toss + Naver Works) — stronger than R8 lms/cms at PRD signature
- 5 invariants disk-resolvable
- Deferred-queue closure is catalog-state milestone (4-of-4 R6 trim)
- Option 3 already provides trim via partial-tag — internal-it fast-follow R10 if verdict fails

Synthesis-B 3-SP IS the trim mechanism. Adding Synthesis-A on top is redundant.

## Synthesis-B atomic-2 challenge

**Does R9 actually need 3 SPs vs atomic-2?** APPROVED — same as R7:

- Mutation surfaces disjoint at file level
- Hard dependency: INV-003 cites WEBHOOK-SIGN-001/RETRY-001 from SP45 — atomic revert wastes work
- 3-SP Option 3 mirrors R7 precedent

## Recommendations

| # | Priority | Action | Effort |
|---|---|---|---|
| 1 | HIGH | Tighten TD-027 condition (c) — 2-use-case signal | 1 line |
| 2 | HIGH | SP45/SP45b prep retrofits DECISIONS.md format clarification | 30 min OR 1 line |
| 3 | MEDIUM | Trim §4.4 redundant verbatim summary (~7 lines) | 5 min |
| 4 | MEDIUM | §4.5 SP45 cite scheduler-domain.test.sh harness shape | 1 line |
| 5 | MEDIUM | INV-005 promotion criterion concrete or deferred-indefinitely | 1 line |
| 6 | MEDIUM | Partial-tag webhook README inline annotation | 1 line |

## References

- PRD draft `:114` — webhook NET-NEW disk-confirmed
- PRD draft `:217-221` — 4 internal-it spec_refs verified disk-real at exact line numbers (AUDIT-RECORD-001:7, SCHED-LOCK-001:21, SCHED-IDEMPOTENT-001:64, NOTIF-SEND-001:47, NOTIF-PREF-001:131)
- PRD draft `:232-233` — 2 Korean verbatim PASS (Toss + Naver Works) meets R8 floor of 2
- PRD draft `:404-410` — TD-027 webhook-as-extension-axis convention 3-condition gate
- `practices/DECISIONS.md:1-50` — format `## <RULE_ID>` structured sections, 0 TD-2026-* entries
- `templates/L4/scheduled-task/README.md:92-95,146-148` — TD-024 referenced inline, no DECISIONS.md trail entry
- 10 sealed verdicts on disk verifies catalog state matches PRD §2 claim
