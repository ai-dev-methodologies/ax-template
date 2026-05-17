# Architect Review — Frontend Templatization PRD (Iteration 2)

> Reviewer: oh-my-claudecode:architect
> Target: `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md` (1273 lines)
> Date: 2026-05-17
> Mode: ralplan consensus loop, Step 5 iteration 2. Re-review of revision.
> Posture: read-only, evidence-anchored. Every claim cites a PRD line or repository path.
> Iter1 reviews: `…-architect-review.md` (334 lines), `…-critic-codex-iter1.md` (119 lines).

---

## TL;DR verdict

**Iter 2 closes every structural defect named in iter1 — Architect (a)/(b)/(c)/(d)/(e)/(f)/(g) and the SP4 split — at binary-implementable resolution.** The Critic's 6 hard blockers are all addressed in code-anchored, fixture-anchored form (no prose-only resolutions). Two new dimensions (h Autonomous execution safety, i Portability anchoring) are added cleanly. The plan is now substantially implementable as written; remaining gaps are small enough to be SP-internal clarifications, not structural rewrites.

**One new steelman risk emerges:** the schema-rigid form needed to make `trio_integrity_guard` binary now puts strong shape constraints on every future domain Spec Trio. If `practices` (a non-API, catalog-viewer domain) does not naturally carry `backend_operation_id` per route, the schema introduces friction that the iter1 thin schema did not. See §3 new steelman for the full argument. This is a real residual risk but not blocking.

**Recommendation to Critic: likely-APPROVE.** 5 of 6 hard blockers fully closed; 1 partial (G — observability signals are named but most are not wired into existing instrumentation infrastructure that exists in this repo today). Iter2 is execution-ready modulo that note.

---

## 1. Disposition of my own iter1 remediations (8)

### Remediation 1 (collapse 17 → 3 skills OR defend 17 with worked example)

**Status: PARTIAL — defended, not collapsed.**

Iter2 keeps the 17-skill topology (§4.14, lines 425–448) but defends it credibly: Tier-3 guards are explicitly NOT pathPattern-triggered (line 445–447: "Tier-3 guards are NOT pathPattern-triggered; they are invoked by Tier-2 only"). pathPattern table for the 8 Tier-2 skills (lines 435–444) makes overlap mathematically impossible — `templates/L1/**`, `templates/L2/**`, etc. partition the artifact space. SP4b adds `skills/_tests/path-pattern-uniqueness.test.sh` (line 635) as a binary test for the same property.

This is a credible defense of the 17. The collapse-to-3 was my proposed synthesis; the iter1 review explicitly allowed "adopt synthesis OR write a credible defense." Iter2 picked the latter. The defense holds because the disambiguation problem is mechanically solved by the path partition + the Tier-3 non-triggering rule.

**What's still loose:** §4.14 does not show worked examples of edge paths (e.g., `templates/L4/auth/lib/auth-client.ts` — does that fire `/ax-verify-L4` only, or also `/ax-verify-react` whose pathPattern is `frontend/**, practices-react/rules/**`?). The SP4b TDD anchor's 5 sample paths (line 636–638) cover the obvious cases but `frontend/**` vs `templates/L4/**` boundary is unverified. Not a structural failure; an SP4b clarification candidate.

### Remediation 2 (tighten Frontend Spec Trio schema)

**Status: CLOSED.**

§4.8.1 (lines 177–195), §4.8.2 (lines 197–217), §4.8.3 (lines 222–239) define schemas with required-key annotations and explicit value constraints (`≥ 20 chars`, `≥ 4.5`, `≤ 2500`, enum-constrained). §4.8.4 algorithm (lines 245–271) is deterministic and implementable. The 5 required fixtures (lines 285–297) cover happy path + 4 failure modes including the `ZERO_SCAN` case I did not explicitly call for but which is a strong addition.

Critic's hard blocker 1 maps exactly to this and is also closed by the same revision.

### Remediation 3 (Layer Membership Decision Table)

**Status: CLOSED.**

§4.11 (lines 353–368) is exactly the table I asked for. All three concrete examples (`ProtectedRoute`, `PaymentCheckoutForm`, `AppHeader`) are resolved with rationale; `templates/backend/**` and `templates/DECISIONS.md` are added (closes my Remediation (b) at the same time). The extension policy (lines 365–368: "if an SP author proposes a component not listed here, they must extend this table in their PR") is a real governance rule, not a ceremonial one — it's `evidence:`-style append-only with a single source of truth.

### Remediation 4 (de-parallelize Phase 3 OR partition shared artifacts)

**Status: CLOSED.**

Iter2 picks BOTH: SP9 is serialized before SP10/SP11 (§5.0 dependency graph line 469–478), AND the shared-artifact partition matrix (lines 747–753 + §6.4.2 lines 893–901) explicitly names sole writers + readers for each of: ESLint plugin, AGENTS.md sentinel, UI meta-schema, L2 blocks, DECISIONS.md, `frontend/package.json`. SP12 batches sentinel regenerations once (line 750, 896). This is the strongest possible resolution.

### Remediation 5 (shadcn drift probe)

**Status: CLOSED.**

§4.1 (lines 135–139) + §4.13 (lines 416–418) + SP3 deliverables (line 513) + SP5 verify command (line 652–653). `templates/L1/_check-shadcn-drift.sh` is named, `shadcn-registry-2026-05.snapshot.md` is in the upstream snapshot list, `time_decay_guard.sh` walks it.

### Remediation 6 (rewrite 5 weak TDD anchors)

**Status: CLOSED.**

§5.5 Verification Matrix (lines 829–844) gives every one of 13 SPs a complete row: `verify_skill`, `script_path`, `test_file`, `assertion`, `expected_RED_reason`, `first_green_command`, `observability_signal`. The 5 previously weak anchors (SP2/SP4/SP6/SP11/SP12) are all materially rewritten:

- SP2 (line 833): fixture-based; RED reason names `MISSING_FRONTEND_SPEC` exit. Concrete.
- SP4a (line 834) + SP4b (line 835) split: each has its own RED reason and self-test.
- SP6 (line 838): pre-existing fixture stubs throw `NotImplemented`; clear RED.
- SP11 (line 843): pre-SP11 the viewer 404s; reads AGENTS.md at test-load time. Clear RED.
- SP12 (line 844): `verify/fork-receiver-cold-start.test.sh` exits with `NO_TEMPLATES` pre-SP12. Clear RED.

### Remediation 7 (patch §7 pre-mortem)

**Status: CLOSED.**

§7 now has 4 scenarios with owner/command/threshold/rollback-vs-continue criteria each (Scenario 1 lines 968–990, Scenario 2 lines 992–1020, Scenario 3 lines 1022–1055, Scenario 4 NEW lines 1057–1090). Scenario 3 retarget per my iter1 §5 critique is explicit (line 1036: "NEW (per Architect §5): auth → payment surface validator"). Scenario 4 (Spec Trio drift, the missing scenario I named) is added with binary `spec_trio_drift_probe.sh` (line 1073–1077).

### Remediation 8 (ADR provenance_class field)

**Status: CLOSED.**

§4.12 (lines 370–409) defines a 4-enum with semantics for each, plus guard enforcement (`evidence_guard.sh` validates the value and requires `quote` or `rationale` accordingly). §8 ADR template (line 1107) demonstrates the field in use. The 10 ADRs (line 1163–1172) each carry their class label.

### Bonus iter1 architect ask: SP4 split

**Status: CLOSED.** §5.0 (line 487–488) and SPs 4a/4b (lines 593–642) split cleanly. Different blast radii, different agent counts (1 lead + 1 worker for 4a, 1 lead + 3 workers for 4b).

**Iter1 remediations disposition tally: 7 CLOSED, 1 PARTIAL (the worked-edge-case ask). Zero OPEN.**

---

## 2. Disposition of Codex Critic's 6 hard blockers

### Blocker 1 — `trio_integrity_guard.sh` binary-implementable

**Status: CLOSED.** §4.8.4 (lines 241–297) gives a deterministic 8-step algorithm with named exit codes (`MISSING_FRONTEND_SPEC`, `COVERAGE_SHORTFALL: N/M`, `ZERO_SCAN`), domain allowlist (line 273–279), and 5 required fixtures. This is the single largest improvement in iter2.

### Blocker 2 — Remove placeholder `exit 0` guard stubs

**Status: CLOSED.** §3.2 line 118 ("No placeholder `exit 0` guard stubs. A guard file either ships its real binary implementation in the SP that creates it or is not created in that SP. Iter1's plan to land 2 placeholder `.sh` files in SP1 and fill them in SP3 is forbidden."). SP3 is repositioned to land FIRST (§5.0 line 461, §5.0 line 483–486). SP1 explicitly does not ship guard stubs (line 546).

### Blocker 3 — TDD anchors with file + assertion + RED reason + first green command

**Status: CLOSED.** §5.5 Verification Matrix (lines 829–844). Every cell is filled across all 13 SPs. The format matches exactly what the Critic asked for (line 47 of Critic's review: `test_file`, `assertion`, `expected_RED_reason`, `first_green_command`).

### Blocker 4 — Per-SP observability metrics/events

**Status: PARTIAL.** §5.5 `observability_signal` column (rightmost) names a specific event/metric per SP (`guard.execution.duration`, `frontend.route.rendered`, `spec_trio.coverage_ratio`, `skill.pathpattern.disambiguation`, `token.contract.violations`, `fork.receiver.smoke.duration`, `route.render.success_rate`, `lint.cross_block_import_violations`, `traceId_propagated`, `server_action.completed`, `payment.idempotency.replay_match`, `practices.viewer.broken_link.count`, `cold_start.duration`, `l4_sealed.must_pass.count`, `skill.topology.tier_count`).

The names are good. The gap: the PRD does not specify WHERE these events are emitted from (e.g., is `frontend.route.rendered` an OpenTelemetry span, a `console.log` parsed by Playwright, a custom hook?). For `traceId_propagated`, the assertion "Playwright asserts request `X-Trace-Id` header reaches Spring Boot logs; backend log probe confirms match" (line 840) is concrete enough. For `guard.execution.duration` the description "per-guard wall-time emitted to stderr; assert < 30s each" (line 831) is concrete. But for `frontend.route.rendered`, `server_action.completed`, `payment.idempotency.replay_match` etc., the emission mechanism is implicit — likely intended to be Playwright-level page-event probes, but the spec doesn't say.

This is a PARTIAL because the binary verification gates still work (each test_file has a real assertion), but the observability column adds a soft requirement that isn't fully wired. The plan can ship; the implementer will have to choose the emission mechanism. If the Critic's intent was production-style observability infrastructure (Prometheus, OTel), that's a deeper gap. If the intent was "test-level event probes," that's adequate.

### Blocker 5 — Autonomous `/team` safety: rollback, ownership, stale-state, halt

**Status: CLOSED.** §6.4 (lines 862–957) addresses all four:
- §6.4.1 rollback boundary per SP with named pre-start tag table (lines 874–889)
- §6.4.2 shared-artifact ownership matrix (lines 893–901)
- §6.4.3 stale-state invalidation rule (lines 905–913)
- §6.4.4 explicit halt thresholds (3-fail / 30-min idle / 5-rebase) (lines 917–935)
- §6.4.5 ESCAPE valve with file format (lines 939–954)

This is more thorough than the Critic asked for. The 30-min idle halt and 5-rebase halt are clean additions beyond the 3-fail halt.

### Blocker 6 — SP9–SP11 serial/partition

**Status: CLOSED.** Same as my Remediation 4 disposition above. Both serialization AND partition shipped.

**Critic hard-blockers tally: 5 CLOSED, 1 PARTIAL. Zero OPEN.**

---

## 3. New steelman (iter 2)

**Headline:** *"The schema-rigid form needed to make `trio_integrity_guard` binary now over-determines every future domain's Spec Trio shape. The 100%-coverage threshold + `backend_operation_id`-required schema works for the 4 in-scope domains; it will silently shed the composition-kit property for the 5th, 6th, 7th domains we haven't anticipated."*

**Full argument:**

The iter1 Spec Trio schema (`specs/<domain>-frontend-l0.yaml` with thin fields) was too thin to support a binary guard. My iter1 review correctly flagged this. The remedy in iter2 §4.8.1 makes every frontend Spec Trio item carry:

- `backend_operation_id: <string|null>` — REQUIRED key (line 188)
- `backend_spec_ref: <DOMAIN-NNN|null>` — REQUIRED key (line 189)
- `verification_type: <e2e_test|unit_test|a11y_test|cwv_test>` — REQUIRED enum (line 191)
- `coverage_threshold: <decimal>` — REQUIRED (line 193)
- `backend_only_marker: false` — REQUIRED boolean (line 194)

And §4.8.4 step 7 enforces 100% coverage of backend items where `frontend_required: true`.

This is shape that works for the 4 in-scope domains (auth, crud, payment, practices) because all four have clean API-bound routes. But consider:

1. **`practices` viewer (SP11)** is NOT API-bound — it's a Server Component reading static files. The `backend_operation_id` field is `null` for every route. The schema accepts `null` (line 188), so technically valid, but the 100% coverage check is degenerate. Every practices "item" passes by virtue of having `null` on both sides. The guard is ceremonial for this domain.

2. **Future static-content domains** (a docs site, a marketing page, a status dashboard with no API): same issue. The schema is API-oriented; static-content domains pay the schema-fill cost without getting the verification benefit.

3. **The 100% threshold is brittle.** If `specs/auth-asvs-l1.yaml` adds an ASVS item that is intrinsically backend-only (e.g., a rate-limit hardening rule that has no UI surface), the maintainer must either (a) add a `frontend_required: false` per-item marker (the schema mentions file-level inheritance at line 264, but the per-item version isn't fully specified) or (b) add a degenerate frontend item to satisfy 100%. Both are friction the iter1 thin schema did not impose.

**Cross-cut with CLAUDE.md "selective expansion" framing:**

CLAUDE.md vision (lines 7–43) emphasizes "composition kit" — each layer independently adoptable, each rule independently evident. Iter2's schema rigidity for the cross-trio guard pushes toward "every domain MUST fit this shape." If a fork-receiver wants to add a 5th L4 domain that is, say, an interactive Storybook viewer (purely L1+L2, no backend), the `trio_integrity_guard.sh` would need either a per-domain opt-out (`frontend_required: false` doesn't apply because there's no backend spec to mark) or a new allowlist entry (`frontend_only` analogue). The schema as written assumes backend-leads-frontend; the inverse case is unspecified.

**Is this fatal?** No. The 4 in-scope domains all have backends. The plan ships fine. But the "selective expansion" property (CLAUDE.md L9–L43) is mildly degraded — domains 5+ may have to negotiate with the schema instead of just inheriting the pattern.

**Counter to my own steelman:** The PRD acknowledges this implicitly in Open Question #6 (line 1191) and the §4.8.4 allowlist structure (line 274–279) is split into `full_trio` vs `backend_only`. A `frontend_only` third bucket could be added in a future SP without breaking the plan. So the steelman points to a future-work item, not an iter2 blocker.

**Does this change the verdict?** No. **The steelman names a real residual cost that the iter1-too-thin → iter2-too-rigid swing has introduced**, but the cost is bounded to "the 4 in-scope domains work cleanly; future static-only domains will need an SP-level schema extension." That is exactly the iterative growth pattern CLAUDE.md calls "catalog 확장 = 정상 활동."

---

## 4. Architectural soundness verdict for iter 2 (PASS / WEAK / FAIL per 9 dimensions)

| Dim | Topic | Iter1 verdict | Iter2 verdict | One-line justification |
|---|---|---|---|---|
| (a) | Boundary integrity (Layer Membership Decision Table §4.11) | WEAK | **PASS** | §4.11 resolves all three iter1 ambiguities + adds `templates/backend/**` and `templates/DECISIONS.md`; extension policy is real, not ceremonial. |
| (b) | Verification closure (`templates/backend/**`, `templates/DECISIONS.md` ownership) | WEAK | **PASS** | §4.10 line 342–343 adds `templates/backend/**` and `templates/DECISIONS.md` to all 4 guards' walk targets; §4.12 defines the ADR `evidence:` schema; §4.11 assigns named verify skills. |
| (c) | Evidence chain density (`provenance_class` §4.12) | PASS w/ caveat | **PASS** | §4.12 4-enum + guard enforcement closes the internal-design opt-out gap. Caveat from iter1 dissolved. |
| (d) | Anti-pattern resistance (quarterly review removed) | PASS | **PASS** | §3.2 line 124 + §10 line 1210 explicitly remove quarterly review; binary SP4b/SP12 skill-topology probe replaces it (line 800). |
| (e) | TDD anchoring (Verification Matrix §5.5) | WEAK | **PASS** | §5.5 covers all 13 SPs with `test_file`, `assertion`, `expected_RED_reason`, `first_green_command`. Every previously-weak anchor is rewritten with a concrete pre-SP failure state. |
| (f) | Cross-Trio integrity (§4.8.4/4.8.5 schemas + fixtures) | FAIL | **PASS** | §4.8.4 algorithm is deterministic + 5 fixtures + named exit codes + domain allowlist. §4.8.5 `cross_trio_guard.sh` is similarly specified with 3 fixtures. Binary-implementable. |
| (g) | Parallelizability (SP9 serialized, partition matrix §6.4.2) | WEAK | **PASS** | §5.0 graph + §6.4.2 ownership matrix: SP9 sole writer for crud-derived L2 amendments; SP12 batches sentinel regenerations; meta-schema frozen at SP2. No false-parallelism claim survives. |
| (h) | **NEW** Autonomous execution safety (§6.4 rollback / ownership / stale-state / halt / ESCAPE) | n/a | **PASS** | §6.4 covers rollback boundaries (13 named tags), ownership matrix, stale-state rules, 3 named halt thresholds, ESCAPE valve with file format. Exceeds Critic's ask. |
| (i) | **NEW** Portability anchoring (fork-receiver smoke moved to SP5.5) | n/a | **PASS-WEAK borderline** | SP5.5 (lines 665–690) lands smoke after SP5 (L1 only), with PATH_LEAK assertion. Good. But Open Question #6 (line 1191) defers L2/L4 increment-smokes to follow-up. The plan validates portability at L1 + full-tree (SP12), not at L2/L3/L4 increments. For an L1-only fork-receiver this is fine; an L2/L4 fork-receiver gets the same coverage only at SP12. Borderline because the 4 in-scope L4 domains land before SP12. |

**Verdict summary: 9/9 PASS, with (i) at PASS-WEAK borderline.** Every iter1 WEAK/FAIL is upgraded. The two new dimensions both clear.

---

## 5. New synthesis (if applicable)

**No further structural synthesis needed.** Iter2 has consumed every iter1 synthesis item I proposed (where applicable) or defended against them (the 17-skill case). The plan is internally consistent.

**Two small synthesis-class clarifications worth folding in before Critic:**

1. **§4.14 worked edge-case pathPattern examples.** Add 2–3 paths that sit on layer boundaries (e.g., `frontend/lib/auth/client.ts`, `templates/L4/auth/lib/auth-client.ts`, `practices-react/eslint-plugin-ax/src/rules/no-domain-import.ts`) showing which Tier-2 skill auto-triggers. Closes the PARTIAL in Remediation 1.

2. **§5.5 observability emission mechanism.** Add a single sentence to each row stating where the signal is emitted: `Playwright `page.on('console')` probe`, `Spring Boot Micrometer counter`, `custom event in `app/layout.tsx``, etc. Closes the PARTIAL in Blocker 4.

Neither is structural. Both are ≤ 1 hour Planner edits.

**No FAIL-class issue requires a third iteration.**

---

## 6. Recommendation to Critic

**Likely-APPROVE.**

Iter2 closes 5 of 6 Critic hard blockers fully and the 6th (observability emission mechanism) at concrete-name level. Every iter1 Architect FAIL is upgraded to PASS. The new steelman (§3) names a future-work cost, not a blocker. The two clarifications I named in §5 are SP-internal — they can ship in the SP3/SP4b PRs themselves.

The plan is execution-ready. The only honest reasons to ITERATE again would be:

- The Critic decides the observability signal emission mechanism is a structural blocker (I judged it PARTIAL but defensible).
- The Critic objects to the 17-skill defense over the 3-skill collapse — but iter1 explicitly permitted "defend OR collapse," so this would be a verdict change, not a new defect.
- The Critic spots a fresh defect introduced by the +317 lines I didn't catch.

I scanned for the third category. The +317 lines are concentrated in: §4.8.4/4.8.5 algorithms (binary), §4.11 layer table (closes a gap), §4.12 provenance_class (closes a gap), §5.5 Verification Matrix (closes a gap), §6.4 autonomous execution safety (closes a gap), §7 pre-mortem revisions (closes a gap). None of the additions introduces inconsistency with pre-existing PRD content. The §11 revision-provenance matrix (lines 1222–1264) gives the Critic a single audit table.

If the Critic APPROVES, hand off SP3 → SP1 → SP2 → SP4a → SP4b → SP5 → SP5.5 → SP6 → SP7 → SP8 → SP9 → SP10‖SP11 → SP12 to `/team` per line 1273.

---

## 7. References

- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md:118-124` — placeholder-guard ban + quarterly-review removal in §3.2.
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md:177-297` — Frontend Spec Trio schemas §4.8.1–4.8.5 with binary algorithm + fixtures.
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md:353-368` — §4.11 Layer Membership Decision Table.
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md:370-409` — §4.12 ADR provenance_class enum + guard enforcement.
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md:425-448` — §4.14 17-skill defense + pathPattern partition table.
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md:461-494` — §5.0 SP dependency graph (SP3 first, SP4 split, SP5.5 NEW).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md:496-531` — SP3 deliverables (real guards before placeholders).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md:665-690` — SP5.5 fork-receiver smoke (criterion L from Critic).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md:747-753` — Phase 3 shared-artifact partition table.
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md:824-844` — §5.5 Verification Matrix (13 SPs × 8 columns).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md:862-957` — §6.4 Autonomous Execution Safety (rollback, ownership, stale-state, halt, ESCAPE).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md:963-1090` — §7 pre-mortem 4 scenarios with thresholds.
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md:1208-1218` — §10 honored-constraints cross-check (quarterly review removed).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.iter2.md:1222-1264` — §11 revision provenance matrix (audit table).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-architect-review.md:1-334` — iter1 Architect review (this iter2 closes 7/8 of its remediations).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-critic-codex-iter1.md:85-93` — Critic's 6 hard blockers (this iter2 closes 5/6, 1 PARTIAL).
- `practices/evals/evidence_guard.sh`, `practices/evals/substance_guard.sh`, `practices/evals/spec_ref_guard.sh`, `practices/evals/time_decay_guard.sh` — existing 4 hard gates (iter2 extends walk targets, adds zero-scan check).
- `CLAUDE.md:7-43` — Project Vision: composition kit, React + Spring equal partner. §3 new steelman cross-references "selective expansion."

---

**End of Architect iter2 review. Hand off to Critic / Codex for re-evaluation. Recommendation: likely-APPROVE with 2 SP-internal clarifications.**
