# Codex Critic R9 iter 2

## Verdict

APPROVE.

All 10 requested closures are CLOSED with line-specific evidence in `docs/superpowers/specs/2026-05-22-r9-webhook-internal-it-prd.iter2.md`. The only new issue I found is INFORMATIONAL: the PRD is 494 lines, four lines above the apparent 470-490 target band, but the prompt's spot validation explicitly expects `wc -l` to return 494 and the overage is the required iter-2 changelog evidence, not decision sprawl.

## Closure check (10)

1. **H1 TD-027 condition (c) - CLOSED.** Line 396 contains the literal "Two consumer signals" gate and requires one shipped active recipe plus one plausible R10+ deferred candidate. Line 396 names internal-it plus `api-gateway-relay`; line 401 repeats `api-gateway-relay` as the forward-pointer.

2. **H2 DECISIONS.md format - CLOSED.** Lines 368-374 add the "Format resolution" subsection. It preserves pre-R7 `## <RULE_ID>` entries, defines R7+ ADR bullet format, and explicitly says no retrofit/backfill is required.

3. **M3 Section 4.4 trim - CLOSED.** The redundant verbatim summary table is gone. Lines 266-270 replace it with per-deliverable density bullets plus the per-source-class arithmetic.

4. **M4 scheduler-domain.test.sh anchor - CLOSED.** Line 192 adds `semantic_anchor` citing `skills/_tests/L4/scheduler-domain.test.sh` shape with N total assertions and an `>=M PASS` gate.

5. **M5 INV-005 deferred-indefinitely - CLOSED.** Line 208 reframes promotion as "deferred indefinitely" and keeps `webhook-secret-encryption` recipe-level unless cross-domain need emerges. Line 385 repeats the same TD-025 follow-up.

6. **M6 partial-tag webhook README annotation - CLOSED.** Line 282 specifies webhook README key birth with `[internal-it]  # verdict pending until SP46 -- see _MANIFEST.yaml for active status`; line 347 has the partial-tag table annotation and line 350 explains the desync window.

7. **L INFORMATIONAL HMAC anchor - CLOSED.** Line 381 explicitly binds outbound WEBHOOK-SIGN-001/002 to the same RFC 2104 plus OWASP ASVS V13.2.6 HMAC anchor already used by inbound receiver verification, while keeping sender and receiver axes distinct.

8. **Soft #1 per-source-class arithmetic - CLOSED.** Line 270 gives auditable totals: Verbatim PASS = 6, Downgrades = 9, Redirects/alternate-host captures = 5.

9. **Soft #2 CRUD spec path - CLOSED.** Line 200 states CRUD anchors use `specs/crud-security.yaml`, not nonexistent `specs/crud-l0.yaml`.

10. **Soft #3 folded into L INFO - CLOSED.** Line 381 closes the HMAC anchor request; line 492 says soft #3 is covered by that INFORMATIONAL fix.

## Disk validation

- `wc -l docs/superpowers/specs/2026-05-22-r9-webhook-internal-it-prd.iter2.md` returns `494`.
- `rg "Two consumer signals"` locates TD-027 condition (c) at line 396.
- `rg "Per-source-class breakdown"` locates PASS=6 / Downgrades=9 / Redirects=5 at line 270.
- `nl -ba ... | sed -n '468,494p'` confirms the iter-2 changelog spans lines 470-494.
- TD-024 compatibility check: lines 119, 172, and 178 keep webhook keyless in SP45; lines 120, 134, 282, 312, and 342-350 birth `[internal-it]` in SP45b and annotate partial-tag pending state. This matches the first-consumer-arrival convention in `templates/DECISIONS.md` lines 2067-2075.

## Independent attack

**INFORMATIONAL - 494-line PRD is four lines above the apparent 470-490 target band.**

R8 iter 2 had a binary prompt band and Codex correctly ITERATEd on line count. Here the prompt's own spot validation expects 494 lines, and the required iter-2 changelog occupies lines 470-494. I do not recommend a narrow trim because removing four lines would either weaken the closure ledger or fight the explicit disk-validation expectation.

I also checked the two other plausible attacks:

- **Forward-pointer naming inconsistency:** Architect's INFORMATIONAL stands, but I do not elevate it. TD-027's closure surface is unambiguous at lines 396 and 401 (`api-gateway-relay`). Lines 307 and 384 use looser `api-gateway` / `webhook-relay` phrasing, worth normalizing during SP prose cleanup but not blocking.
- **TD-024 first-consumer compatibility:** Not an issue. SP45 has no consumer and no key; SP45b is the first consumer and births the key in the same atomic consumer commit, with partial-tag annotation.

## Final reasoning

The iter-1 HIGH/MEDIUM findings were surgical and all are now closed at the requested anchors. The Codex HMAC informational is stronger than requested because it names RFC 2104, ASVS V13.2.6, the existing inbound rule, and the outbound sender axis. The remaining line-count and forward-pointer concerns are editorial/informational, not blockers to SP45/SP45b/SP46 execution.

## ADR (if APPROVE)

ADR-ready: YES.

- TD-025 is ready with explicit HMAC anchor reuse and webhook L4 rationale.
- TD-026 is ready for internal-it activation and deferred-queue closure.
- TD-027 is ready with the tightened two-consumer-signal L4 split gate.

## Re-review trigger (if ITERATE)

N/A. APPROVE.
