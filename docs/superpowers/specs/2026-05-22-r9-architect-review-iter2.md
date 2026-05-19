# Architect Review — R9 iter 2

## Verdict: APPROVE

All 10 closures verified at exact iter 2 line refs. 1 INFORMATIONAL (forward-pointer naming inconsistency; courtesy cleanup only, not blocking).

## Closure check (10/10 CLOSED)

| # | Finding | Status | Evidence |
|---|---------|--------|----------|
| H1 | TD-027 (c) 2-consumer-signal gate w/ api-gateway-relay | CLOSED | L396: "Two consumer signals — at least one **shipped active recipe** AND at least one **plausible R10+ deferred candidate**"; L401 Follow-ups names `api-gateway-relay` forward-pointer; L474 changelog confirms. Self-fulfilling risk extinguished. |
| H2 | DECISIONS.md format resolution (recommendation b) | CLOSED | L368-374 "Format resolution" subsection: SP45 prepends opening note; pre-R7 retains `## <RULE_ID>`; R7+ as bullets; explicit no-backfill. L452 §11 wires SP45 prep step. |
| M3 | §4.4 redundant verbatim summary trimmed | CLOSED | Verbatim summary table removed; replaced by L268-271 per-deliverable + per-source-class arithmetic bullets. |
| M4 | scheduler-domain.test.sh anchored | CLOSED | L192 `semantic_anchor:` TDD YAML cites `skills/_tests/L4/scheduler-domain.test.sh` shape (N total, ≥M PASS gate). L361 §7 P4 likelihood MEDIUM→LOW. |
| M5 | INV-005 promotion deferred-indefinitely | CLOSED | L208 reframed: "promotion deferred indefinitely; remains recipe-level invariant unless cross-domain need emerges". Payment-callback-secret hypothetical removed. L385 TD-025 Follow-ups matches. |
| M6 | Partial-tag webhook README annotation | CLOSED | L282 SP45b row item (e) births key with inline `# verdict pending until SP46 — see _MANIFEST.yaml for active status`. L347 §6 table row 2 matches. L350 rationale explains 3-9d desync-window auditability. |
| L INFO | HMAC anchor (RFC 2104 + ASVS V13.2.6 sender/receiver) | CLOSED | L381 dedicated TD-025 "HMAC cryptographic anchor" bullet: sender (WEBHOOK-SIGN-001/002) and receiver (PRACTICES-INTEG-001) distinct axes, identical RFC 2104 construction, no new primitive. |
| Soft #1 | Per-source-class arithmetic | CLOSED | L270: PASS=6 (4 EN + 2 KO), Downgrades=9, Redirects=5. 28 ledger rows / 6 Verbatim cite / 20 Downgrade occurrences cross-check. |
| Soft #2 | CRUD spec path `crud-security.yaml` | CLOSED | L200 §4.2 explicit clarification: anchors on `specs/crud-security.yaml`, NOT `crud-l0.yaml`; R5 absorbed. §9 honored constraint matches L413. |
| Soft #3 | (folded into L INFO) | CLOSED | Covered by HMAC anchor bullet (changelog L492). |

## Disk validation

- `wc -l` → **494** lines (matches Planner claim).
- TD-027 (c) literal "Two consumer signals" present at L396; 3 `api-gateway-relay` occurrences locate forward-pointer.
- §4.4 per-source-class breakdown present at L270.
- Iter 2 changelog spans L470-494.
- Cross-section consistency: §1 P7 (L35), §3 Must-Have, §4.5 SP-rows, §9 Honored Constraints, §12 verdict line, RALPLAN-DR summary all carry matching delta language.

## Independent attack — INFORMATIONAL (not blocking)

**Naming inconsistency for the R10+ forward-pointer:** Three surface tokens appear for the same conceptual candidate:
- L307 §3: "future api-gateway recipes"
- L384 TD-025 Consequences: "R10+ recipes (api-gateway, webhook-relay) unblocked"
- L396/401/474 (H1 closure surface): `api-gateway-relay`

The H1 closure itself is unambiguous — TD-027 (c) names exactly one candidate (`api-gateway-relay`). But §3 P3 and TD-025 Consequences gesture at adjacent-but-different names without explicitly saying they are aliases. A pedantic future planner could read L384 "(api-gateway, webhook-relay)" as TWO candidates. **Recommend** SP45b commit prose normalize to canonical `api-gateway-relay` token or note the trio is one candidate. Does NOT block iter 2 sign-off.

## Final reasoning

All 6 Architect + 1 Codex INFORMATIONAL + 3 soft closed at exact target sections per iter 2 changelog L470-494. Disk validation matches Planner claim. Single new INFORMATIONAL is prose-consistency observation, not structural defect. Sender/receiver HMAC distinction explicitly anchored to RFC 2104 + ASVS V13.2.6 closes iter-1 INFORMATIONAL cleanly.

## Re-review trigger

None. APPROVE for SP45/SP45b/SP46 execution.
