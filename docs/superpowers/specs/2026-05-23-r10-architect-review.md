# Architect Review — R10 iter 1

## Verdict: APPROVE-WITH-CHANGES (effectively ITERATE)

Plan structurally sound and disk-grounded. 5 spec_refs verified resolvable, L4 cap held at 12, R6/R8 atomic precedent applied correctly. Two HIGH findings warrant planner iter 2 pass.

## Steelman antithesis

Strongest case AGAINST R10: **evidence-chain reach via Toss Payments**. Toss is a *payment processor*. R10 reframes the same vendor R9 cited (R9 quoted `docs.tosspayments.com/guides/webhook`; R10 cites `docs.tosspayments.com/reference`) as "canonical Korean enterprise REST API platform offering gateway-style integration semantics." That's a semantic stretch:
1. Toss served as the Korean anchor for **webhook** L4 evidence in R9. Reusing same vendor + sibling docs path for a **gateway** recipe one cycle later weakens "1 Korean verbatim per cycle" freshness signal.
2. "API endpoint reference" docs ≠ "API gateway product." The Toss quote describes *consuming an API*, NOT *operating a gateway* (route registry, rate-limit, circuit breaker). Verbatim text doesn't attest INV-001-INV-005 gateway semantics.

Combined with 3-platform Korean cloud-native 0-verbatim streak (9 attempts × 0 PASS), Korean evidence chain for this domain is genuinely thin. R11+ planners now have precedent that "1 Korean verbatim adjacent fallback after host-wide cascade" suffices indefinitely.

## Tradeoff tensions

1. **TD-027 discipline vs evidence freshness.** Honoring TD-027 (no new L4) non-negotiable — but R10 ships zero net-new Korean voice for Korean cloud-native domain.
2. **Composition reuse maxima vs recipe distinctness.** All 5 INVs bind existing anchors — celebrated as "strongest catalog-reuse" but invites §7 P3 concern that context-0 sub-agent may struggle to distinguish api-gateway-relay from webhook L4 itself.
3. **Cross-cutting spec binding vs catalog uniformity.** INV-003 binds `specs/ratelimit-l0.yaml` directly with NO `templates/L4/ratelimit/` directory. Novel framing — recipe-spec referential integrity guard may not yet handle this. HIGH-impact verification risk (see H2).

## Synthesis

**Synthesis-B: add 1 supplementary Korean fresh-voice attempt.** Keep Option (1), keep Toss adjacent fallback documented, but add second Korean adjacent attempt from fresh vendor — NAVER D2, TossTech engineering blog, KakaoTech, Banksalad, NCSOFT — anything where verbatim Korean describes gateway/proxy/relay/rate-limit semantics from different vendor than R9. Costs ≈5 min WebFetch in SP47 pre-flight; breaks "same Korean vendor 2 cycles in a row" optic.

## Principle check (DELIBERATE)

**Central principle: TD-027 honored — no new L4.**

- Disk verify (`ls templates/L4 | wc -l = 12`) confirms unchanged.
- Recipe count 10 → 11 consistent.
- R9 TD-027 (c.2) forward-pointer retroactive validation: R10 SP47 ships api-gateway-relay as SECOND shipped consumer of webhook (internal-it + api-gateway-relay). H1-tightened gate satisfied without new L4. **Closure confirmed.**
- R10 forward-pointer: rate-limit-as-L4 promotion explicitly deferred to R11+ pending 2nd organic consumer. Right discipline.
- `deferred_recipes:` stays `[]`. R10 does not re-open closed queue.

Principle 7 PASSES. No violations.

## Findings

### HIGH

**H1 — Toss Payments re-use is Korean evidence-chain reach.** §4.4 line 207 cites `docs.tosspayments.com/reference` with quote describing API consumer documentation, NOT gateway-product. R9 already used Toss as Korean anchor for webhook L4 + internal-it recipe. Reusing Toss a 3rd time across 2 consecutive cycles, plus reframing it from "payment webhook docs" to "Korean gateway-style integration semantics," weakens per-cycle Korean-freshness signal. Verbatim does NOT independently attest INV-001-005 gateway semantics. **Fix:** add 1 fresh-vendor Korean adjacent attempt (NAVER D2 / TossTech / Banksalad / NCSOFT / KakaoTech blog) to §4.4 in SP47 pre-flight one-shot. Effort: 5 min WebFetch + 1 ledger row.

**H2 — `specs/ratelimit-l0.yaml` cross-cutting binding is harness-unverified.** INV-003 binds `ratelimit-l0.yaml#RATELIMIT-1/2` directly. No prior recipe has bound a spec where no corresponding `templates/L4/*` directory exists. PRD §4.1 frames as deliberate "cross-cutting concern" + §7 P2 marks LOW likelihood — but the *guard* (`recipe_spec_referential_integrity_guard.sh`) on disk may not yet handle this case. If guard inspects spec resolvability via L4 directory presence (vs pure file existence), RED→GREEN won't close on SP47. **Fix:** SP47 pre-flight step grep `practices/evals/recipe_spec_referential_integrity_guard.sh` to confirm tolerates spec_ref to file with no L4 directory. If not, patch guard atomically inside SP47 OR demote INV-003 to bind webhook-l0.yaml + add recipe-level invariant. Effort: 30 min audit + ≤10 line patch IFF needed.

### MEDIUM

**M1** — Ledger arithmetic drift (§4.4). PRD claims "Downgrades = 8" but disk count shows 7 rows literally marked `Downgrade`. Cloudflare /api-gateway 404 + NHN 302→error tagged `(alternate fetched)` not `Downgrade`. Per-source-class arithmetic should match table count (R8/R9 Codex soft #1 precedent). **Fix:** restate as "Verbatim cite rows = 6; Downgrade rows = 7; Followed-redirect rows = 3; alternate-fetched-as-bridge rows = 1; final-verbatim-via-alternate rows = 1; Toss adjacent row = 1; total 19 rows incl. header+separator." 3 min.

**M2** — Same-vendor 2-cycle Korean reuse should be ADR follow-up. Toss now Korean adjacent fallback in R9 (twice) + R10. Without explicit acknowledgement, R11+ planners may default to "Toss always available". TD-028 should add Follow-ups bullet: "Korean adjacent fallback rotation — if R11+R12 also fall back to Toss, escalate to dedicated Korean-vendor-diversity guard or accept precedent explicitly." 1 line.

**M3** — §7 P3 (sealed verdict disambiguation) mitigation under-specified. P3 names risk that context-0 sub-agent can't distinguish api-gateway-relay from webhook L4 itself. Mitigation says RECIPE.md "explicitly distinguishes" but doesn't pin distinguishing sentence. **Fix:** pre-commit RECIPE.md preamble sentence in PRD §4.1: "api-gateway-relay is a GATEWAY-PATTERN COMPOSER that registers and routes inbound traffic to multiple backend services via webhook L4's outbound-emit primitive; NOT itself a primitive." Sealed sub-agent reads verbatim. 2 lines.

**M4** — INV-005 spec_ref count drift from header claim. PRD §4.1 says "5 business_invariants, ALL `spec_ref:` / `rule_ref:` disk-resolvable" but INV-005 binds 3 anchors (CRUD-VAL-1 + AUDIT-RECORD-002 + idempotency-key-on-mutations.md). All disk-resolvable but §3 Must-Have wording should read "5 INVs, each with ≥1 anchor; all anchors disk-resolvable." 1 line.

## Synthesis-A trim assessment

**REJECTED.** PRD §4.1 marks scheduled-task mandatory (circuit-breaker reconciliation + dead-letter replay). Dropping it would break INV-004, reduce composition 5+2→4+2, eliminate `SCHED-LOCK-001` reuse anchor. At n=1 recipe there is no meaningful Synthesis-A trim.

## Recommendations

| # | Priority | Action | Effort |
|---|----------|--------|--------|
| 1 | HIGH | Add 1 fresh-vendor Korean adjacent attempt in §4.4 (NAVER D2 / TossTech / Banksalad) | 5 min |
| 2 | HIGH | Audit recipe_spec_referential_integrity_guard.sh for ratelimit cross-cutting tolerance; patch atomically in SP47 if needed | 30 min audit |
| 3 | MEDIUM | Restate §4.4 per-source-class arithmetic | 3 min |
| 4 | MEDIUM | TD-028 Follow-ups bullet for Korean vendor rotation | 1 line |
| 5 | MEDIUM | Pre-commit RECIPE.md disambiguation sentence in PRD §4.1 | 2 lines |
| 6 | MEDIUM | §3 Must-Have wording fix | 1 line |
