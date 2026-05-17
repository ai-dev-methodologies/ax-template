# Architect Review — Frontend Templatization PRD (Iteration 1)

> Reviewer: oh-my-claudecode:architect
> Target: `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.draft.md` (956 lines)
> Date: 2026-05-17
> Mode: ralplan consensus loop, Step 3. DELIBERATE pre-mortem present in §7.
> Posture: read-only, evidence-anchored. Every claim cites a PRD line or repository path.

---

## TL;DR verdict

**APPROVE WITH MANDATORY REVISIONS.** The plan's spine (4-layer L1–L4 + Hybrid sequencing + Tier-1/2/3 skills) is sound and consistent with the Payment empirical baseline. But three structural defects must be fixed before Critic / Codex review: (a) **17-skill topology has 4 redundant nodes**; (b) **Frontend Spec Trio invents schemas that are too thin to anchor `trio_integrity_guard`'s binary check** as currently described; (c) **SP9–SP11 parallelism claim is partially false** — at least two shared schemas (UI Contract meta-schema + practices-react ESLint plugin) serialize all three. Fix list at the bottom.

---

## 1. Strongest steelman antithesis

**Headline:** *"The 17-skill topology is over-engineered ceremony imported from a backend cadence that does not generalize. It will collapse under its own discoverability tax before SP12 ever runs."*

The full steelman:

The Payment domain succeeded with **zero** new skills. The whole `/ax-transform` + `practices/SKILL.md` + `practices-react/SKILL.md` surface (3 skills total today) carried a 22-story payment blueprint to L4 sealed sub-agent PASS at maximum verdict (11/11 MUST + 6/6 SHOULD). That is the empirical baseline the PRD claims to honor.

Now the PRD proposes to **5.7×** the skill count (3 → 17) for a single initiative. The justification — "few exposed surfaces, dense feedback loops underneath" (PRD §RALPLAN-DR Principle 3) — confuses two distinct dimensions:

1. **Surface size** = how many entry points the user types. The PRD bounds this at 3 (Tier-1). Good.
2. **Decomposition density** = how many internal skill files an AI agent's `pathPatterns` resolution must disambiguate against. The PRD makes this **17**, not 3.

In Claude Code's actual behavior, pathPattern-triggered skills auto-activate **without** the user typing them. So 14 Tier-2/Tier-3 skills with overlapping path globs (`templates/L*/**`, `practices/**`, `practices-react/**`, `specs/**`) will all fire on most edits. The PRD's SP4 mitigation — "skill-graph integrity probe checks no two skills with overlapping pathPatterns" — is structurally impossible for `/ax-verify-L2`, `/ax-verify-react`, `/ax-guard-evidence`, and `/ax-guard-spec-ref` because they MUST all pattern-match the same `templates/L2/**` paths to do their jobs. The PRD already telegraphs this defect at line 802–803 ("any ambiguity → reshape pathPatterns until unique") but provides no proof the reshape converges.

Worse: the PRD bundles `./gradlew`, `vitest`, `playwright`, and `.sh` files "inside skills, not exposed as surface" (Principle 3, line 28–30). This means every script change now requires a skill edit + AGENTS.md regen + sentinel re-check. The Anthropic workflows-and-feedback-loops pattern requires **the verify surface to be a skill**, not that every script underneath also be a skill. The PRD over-applies the pattern.

The minimal viable topology that satisfies the user brief is:

- `/ax-transform` (existing, extended) — Tier-1 entry
- `/ax-verify` (NEW) — Tier-1 single binary gate
- `/ax-scaffold` (NEW) — Tier-1 domain scaffolder

That's **3 skills total**, not 17. The 6 hard-gate `.sh` files stay as `practices/evals/*.sh` invoked by `/ax-verify`, exactly as they are today. The Tier-2 "axes" (`-java`, `-react`, `-shared`, `-L1/2/3/4`, `-domain`) collapse into `/ax-verify <axis>` flags. The Tier-3 guards collapse into the same `/ax-verify --guard <name>` pattern.

**Why this is the strongest counter, not a nitpick:** the PRD's own §7 Scenario 1 ("Skill topology becomes unmaintainable") rates likelihood "Medium" and admits "We've never run 17 skills in one repo before." The mitigation relies on a quarterly review ("if a Tier-2 skill has not been invoked in 90 days, consider merging it" — line 805–806) which is itself a governance loop **the PRD §10 explicitly forbids** ("거버넌스 무한루프"). The plan is self-inconsistent on this axis.

If the antithesis stands, **SP1 + SP4 collapse to roughly one-third of their current scope**, the dependency graph (line 705–718) shortens, and Phase 1's 4–6 day estimate drops to 2–3 days.

---

## 2. Concrete tradeoff tension (Side A vs Side B, real cost on each side)

**Tension: shadcn/ui adoption vs the "composition kit + self-discoverable" vision.**

The plan chose Side A. Side B has legitimate merit. Both have real costs.

### Side A (PRD pick, §4.1, lines 198–223): adopt shadcn/ui as L1

**What is gained:**
- 32 production-grade primitives without ax-template having to author them (lines 207–214).
- Apache-2.0 license is fork-safe for any downstream team.
- Radix-based components carry accessibility behavior (focus management, ARIA) for free.
- Aligns with the `vercel:shadcn` skill ecosystem and downstream Next.js community patterns.

**What is lost:**
- **shadcn is install-time copy-paste source, not a dependency.** When ax-template runs `pnpm dlx shadcn@latest add button` (SP5 line 584), the component files land **in this repo's tree** at `templates/L1/components/button.tsx`. They are then ax-template-owned code. This contradicts the PRD's framing (line 203–206) that shadcn is "external-owned" — the moment shadcn upgrades, ax-template has 32 stale files to re-sync, but with no automated diff mechanism because shadcn's "registry" doesn't ship as a version-pinned npm artifact.
- **time_decay_guard cannot directly enforce shadcn freshness.** The PRD claims (line 593) "pin shadcn registry version in `blueprints/pinned-versions.yaml`; `time_decay_guard.sh` flags > 90d stale." But `time_decay_guard.sh` (read: `practices/evals/time_decay_guard.sh`) walks snapshot `_MANIFEST.yaml`, not shadcn component files. A new mechanism must be invented and the PRD doesn't specify it.
- **Self-discoverability cost.** A context-0 AI agent dropped into a forked repo cannot easily distinguish "this `Button.tsx` is shadcn-imported and should not be hand-edited" vs "this `LoginForm.tsx` is ax-template-owned and is the source of truth." The plan adds wrapper exports (line 221–222) to clarify, but the wrappers themselves become a third class of file the agent must reason about.

### Side B (rejected without explicit weighing): author ax-template's own L1 primitives from Radix Primitives directly

**What would be gained:**
- Single source of authorship: every file in `templates/L1/` is ax-template-owned, period.
- `time_decay_guard` can walk `templates/L1/` directly and check `evidence:` against the Radix upstream snapshot. No new mechanism.
- No "shadcn registry pin" abstraction needed. Tailwind v4 becomes either fully optional or fully owned, not a transitive blob (Open Question #1, line 916–919, vanishes).
- Aligns more cleanly with the "composition kit" framing — every layer is something a fork-receiver can adopt or replace at one well-defined boundary.

**What would be lost:**
- 3–5 engineering days authoring the 32 primitives (estimate parity with SP5 budget).
- Loss of community-recognized component names (`button`, `dialog`, etc.) — though the PRD's wrapper export plan already obscures the names.
- Lighter integration with the broader Next.js/shadcn ecosystem; harder to copy fixes from upstream issues.

### The honest tension

Side A optimizes for **arrival velocity** (32 primitives in 1–2 days via `pnpm dlx`); Side B optimizes for **provenance integrity** (every L1 file evidence-anchored without inventing a shadcn-pin mechanism).

The PRD picks Side A but does not acknowledge that the **time_decay enforcement gap** is the cost. Per the project's own evidence-anchored doctrine (CLAUDE.md "Evidence-anchored rule provenance"), this is a real principle violation if not patched. The plan needs either (a) explicit shadcn snapshot + diff mechanism in SP3, or (b) a downgrade to Side B for the bottom layer.

**Architect's recommendation:** keep Side A but require SP3 to add `practices-react/upstream/shadcn-registry-<date>.snapshot.md` per blessed component (a tarball-style snapshot of the 32 components frozen at install date) plus a `templates/L1/_check-shadcn-drift.sh` script that diffs current files against the snapshot. This closes the provenance gap without rolling back to Side B.

---

## 3. Architectural soundness verdict (PASS / WEAK / FAIL per dimension)

### (a) Boundary integrity — **WEAK**

The 4 layers (L1/L2/L3/L4) have plausible boundaries, but two ambiguities will produce contributor arguments:

- **L2 vs L4 ambiguity.** `templates/L2/blocks/ProtectedRoute.tsx` (line 232) is listed as an L2 auth block; `templates/L4/auth/app/(protected)/protected/page.tsx` (line 635–636) is L4 auth. Where does the `<ProtectedRoute>` wrapper live? Both layers claim auth surface. Worse, `templates/L2/blocks/PaymentCheckoutForm` is listed as an L2 block while `templates/L4/payment/` (line 663) "wires L1 + L2 + L3 to the backend's existing OpenAPI contract" — but the contract-binding is precisely what makes a form a domain object. Two reasonable contributors will disagree on whether `PaymentCheckoutForm` is L2 (reusable across payment-like flows) or L4 (payment-bound).
- **L1 vs L2 ambiguity.** The PRD lists `templates/L2/cross-cutting (4)` blocks (line 236): `AppHeader`, `AppSidebar`, `ThemeSwitcher`, `BreadcrumbTrail`. None of these are "feature blocks" — they're app-shell primitives that arguably belong in L1 layout primitives or in a separate L1.5 layer. The current taxonomy forces them into L2 by default.

**Remediation:** Add §4.0 "Layer Membership Decision Table" with a worked example resolution for: `ProtectedRoute`, `PaymentCheckoutForm`, and `AppHeader`. Without this, the SP7 lead and SP8 lead will write the same component twice.

### (b) Verification closure — **WEAK**

Most artifact categories have exactly one verifying skill, but two orphans exist:

- **Orphan: `templates/backend/*.java` (§4.5, lines 281–300, 10 files).** No skill in §4.12 (lines 402–434) explicitly verifies the backend cross-cutting templates. `/ax-verify-java` (line 416) is described as wrapping `./gradlew test{Asvs,Crud,Payment,Practices}` — but these templates are not exercised by those tasks (they're pattern-extractions, not compiled). The PRD assumes `evidence:` blocks suffice but provides no evidence_guard application target spec (§4.10 lists `templates/L{1,2,3,4}/**` only, not `templates/backend/**`).
- **Orphan: `templates/DECISIONS.md` ADRs.** §4.10 extends the 4 guards to walk `templates/**` but the ADR format (§8 line 866–907) has no `evidence:` block schema described. The PRD assumes ADRs auto-pass, but `evidence_guard.sh` (read: `practices/evals/evidence_guard.sh`) actually requires `evidence:` frontmatter on every walked file.
- **Double coverage: `practices-react/rules/*.md`.** Both `/ax-verify-react` (line 417) and the 4 Tier-3 guards (lines 428–433) walk this directory. Not necessarily wrong, but the PRD does not specify a precedence: if `/ax-verify-react` passes and `/ax-guard-evidence` fails, which decides? §4.12 inter-skill invocation graph (lines 438–452) shows guards as the terminal leaves, so guards win — but this should be stated.

**Remediation:** SP3 acceptance criteria (line 550) must add: "evidence_guard.sh walks `templates/backend/**` and `templates/DECISIONS.md`; both produce structured `evidence:` block schemas defined in SP3 deliverables." Without this, §10 honored-constraints row "Catalog 확장 = 정상 활동" is unverifiable for backend templates.

### (c) Evidence chain density — **PASS (with one caveat)**

§4.11 (lines 387–401) lists 7 new upstream snapshots covering Next.js 16, Server Actions, `use cache`, shadcn, TanStack Query v5, WCAG 2.2, CWV 2026. The 10 ADRs in §4.9 (lines 359–371) map 1:1 to either a snapshot or an established external standard (Zod, Zustand are pinned to npm package versions). This is dense enough.

**Caveat:** TD-2026-05-17-008 (3-tier skill topology, line 369) and TD-2026-05-17-009 (`templates/` directory shape, line 370) are **self-referential** — they cite no external evidence because no external standard governs "how many tiers an internal skill topology should have." This is acceptable for purely internal design ADRs **only if** the ADR text in §8 acknowledges this and explicitly opts out of the external-citation requirement. The PRD §8 template does not currently make that opt-out visible.

**Remediation:** Add to §8 ADR template a `provenance_class: internal_design | external_anchored` field, default `external_anchored`, with `internal_design` requiring an explicit "why external evidence is N/A" sentence.

### (d) Anti-pattern resistance — **PASS**

§10 (lines 936–948) explicitly cross-checks against all five CLAUDE.md anti-patterns and the four CLAUDE.md vision principles. The mappings are concrete (not ceremonial):

- "거버넌스 무한루프" → "No promotion-gate docs. Every artifact ships with binary verification." Verifiable: the plan ships no `*-GOVERNANCE.md` file.
- "MockMvc 전용 테스트" → "RestAssured + Playwright + real Spring Boot RANDOM_PORT." Verifiable: lines 181, 645.
- "React + Spring 둘 다 active equal partner" → backend gains 10 cross-cutting templates + up to 4 rules; frontend gains everything. Verifiable: §4.5, §4.6.
- "단일 product framing 금지" → "No `npm publish ax-template` build target proposed." Verifiable: no `package.json` at repo root.

**One soft-flag, not a fail:** §7 Scenario 1 mitigation (line 805–806) — "quarterly review: if a Tier-2 skill has not been invoked in 90 days, consider merging it" — is itself a soft governance loop. If the antithesis (§1) lands and the topology shrinks to 3 skills, this risk disappears.

### (e) TDD anchoring — **WEAK**

Each SP names a TDD anchor file (good) but five of the twelve are too abstract for a cold-start agent to execute without inventing details:

| SP | TDD anchor as written | Defect |
|---|---|---|
| SP2 (line 528) | `frontend/tests/auth/spec-trio-coverage.spec.ts` | Asserts what schema? The frontend Spec Trio meta-schema is also a SP2 deliverable — the test cannot reference a schema not yet defined. |
| SP4 (line 571) | `skills/_tests/skill-topology.test.sh` | "Asserts skill count, file existence, frontmatter shape." Frontmatter shape spec is itself produced in SP4 — circular. |
| SP6 (line 604) | `templates/L3/_fixtures/smoke-app/tests/route-resolution.spec.ts` | "Every L3 template's example route resolves." But the example routes are produced inside SP6. No assertion target predates the implementation. |
| SP11 (line 679) | "Rendering test asserts that every rule in `practices/rules/` is reachable by URL." | Reachable how? URL scheme produced inside SP11. |
| SP12 (line 698) | "The L3 fork simulation IS the test." | This is RGREEN-on-arrival, not a RED-first anchor. |

The five GOOD anchors (SP1, SP3, SP5, SP7, SP8, SP9, SP10) all name a concrete file path + a specific assertion against a target that **predates** the SP's implementation work. SP5's "asserts each blessed shadcn component renders with token-driven CSS variables, not hardcoded values" (line 590) is the gold standard — a cold-start agent can execute it.

**Remediation:** Rewrite the 5 weak TDD anchors so each names (a) a file path that does not depend on the SP's own deliverables, and (b) an assertion against a fixture or upstream artifact that exists at SP-start. For SP12, replace "L3 fork simulation IS the test" with "Playwright e2e from cold-clone reaches `/dashboard` in ≤ 300s."

### (f) Cross-Trio integrity (`trio_integrity_guard`) — **FAIL**

The PRD describes `trio_integrity_guard.sh` (line 383) as checking: "For every domain with a backend `specs/<domain>-*.yaml`, a frontend `specs/<domain>-frontend-l0.yaml` must also exist (and vice versa). UI Contract route paths must reference backend OpenAPI operation IDs."

This guard cannot be made binary-executable from the schemas defined in §4.8 (lines 343–353) as written.

**Why it fails:**

1. **No `operation_id` linkage spec.** §4.8 says `contracts/<domain>-ui.yaml` describes "routes, params, query strings, loading/error/empty states, redirect destinations." Nowhere does it require a field like `backend_operation_id: <openapi_operation_id_from_contracts/auth-openapi.yaml>`. Without that field, the guard has no string to grep for.
2. **Symmetric existence is not symmetric coverage.** The current spec ("a frontend yaml must also exist") would pass even if `specs/auth-frontend-l0.yaml` contained zero items as long as the file exists. The guard becomes ceremonial.
3. **`ratelimit` and `security` backend domains have no UI counterpart.** §1.1 (line 118) lists 7 backend domains (`auth`, `crud`, `payment`, `practices`, `ratelimit`, `security`, `user`). §4.4 (lines 264–280) ships L4 workloads for only 4 (`auth`, `crud`, `payment`, `practices`). What does `trio_integrity_guard.sh` do for `ratelimit` / `security` / `user`? The PRD's strict reading ("vice versa") would fail the guard immediately. Either the guard has a domain allowlist (then specify it) or "vice versa" is wrong (then say so).

**Remediation:** SP2 deliverable list must add:
- `contracts/templates/ui-contract.schema.yaml` MUST require an `operation_id` field on each route, validated against the backend OpenAPI document by `swagger-cli`-equivalent.
- `trio_integrity_guard.sh` spec must include a `domain_allowlist: [auth, crud, payment, practices]` explicit field, or a marker file `specs/<domain>-no-frontend.marker` that domains without UI carry.
- Coverage check is on item count, not file existence: "if backend spec has N items, frontend spec must have ≥ ceil(N × 0.6) items" (or similar concrete threshold).

Without these three changes, `/ax-guard-trio-integrity` (line 433) cannot be implemented as a binary gate. This is a structural defect.

### (g) Parallelizability claim (SP9/10/11) — **WEAK**

§5.bonus (line 720) claims "Phase 3 (SP9/10/11) can run in parallel under `/team` orchestration with 3 worker teams." Three shared artifacts secretly serialize them:

1. **`practices-react/eslint-plugin-ax/`** (line 619): SP7 adds "up to 3 new ESLint rules." But SP9/10 may surface domain-specific lint needs (e.g., payment-idempotency-key prop linting), and any SP9/10/11 team that touches this directory will write the same `package.json` `version` field. Three parallel teams editing one npm package = guaranteed merge conflict.
2. **`practices-react/AGENTS.md` sha256 sentinel** (line 621): every catalog change regenerates the sentinel. Three parallel SPs each adding rules will produce three competing sentinel sha256s, only one of which can land.
3. **`contracts/<domain>-ui.yaml` meta-schema** (§4.8 + SP2 lines 514–516): if SP9 discovers the meta-schema needs an additional field for CRUD list-pagination contracts, SP10/11 will be stuck on the old schema or have to rebase.

Plus a fourth, more subtle:

4. **L2 retro-edit budget** (line 658, SP9 risk mitigation): "SP9 is allowed to file 1 retro-edit PR against `templates/L2/blocks/DataTable.tsx` IF needed." If SP9 amends L2, SP10/11 must rebase. Three teams holding rebase locks against the same L2 directory creates an effective serialization.

**Remediation:** Either drop the parallelism claim ("SP9 first, then SP10/SP11 can parallelize after SP9 lands") OR partition the shared artifacts explicitly:
- AGENTS.md sentinel regeneration runs once, at SP12, not per-SP.
- ESLint plugin version bumps are batched at SP12, not per-SP.
- Meta-schema is frozen at SP2 acceptance; SP9/10/11 may not amend it (any amendment forces an SP9.5 sync sub-phase).

The plan should pick one of these and write it down. As written, the parallelism claim will not survive contact with `/team`'s default file-lock behavior.

---

## 4. Synthesis (where viable)

The antithesis (§1) and three of the WEAK/FAIL findings (§3a, §3b, §3e, §3f, §3g) yield a coherent synthesis that improves the plan without rejecting its spine:

### Proposed synthesis: "Lean topology + tightened schemas"

1. **Collapse 17 skills → 3 skills + 14 invoked scripts.**
   - Keep `/ax-transform`, `/ax-verify`, `/ax-scaffold` as the only SKILL.md files.
   - Move Tier-2 axes to `/ax-verify --axis <java|react|shared|L1|L2|L3|L4|domain>` flags.
   - Move Tier-3 guards to `/ax-verify --guard <evidence|substance|time-decay|spec-ref|trio-integrity|cross-trio>` flags.
   - The guards remain as `.sh` files in `practices/evals/` (their natural home today). The Anthropic workflows-and-feedback-loops pattern is satisfied because the **verify surface is a skill** (`/ax-verify`), not because every script is wrapped in a SKILL.md.
   - **Outcome:** SP1 + SP4 collapse to ≈ 50% scope. The "skill-graph integrity probe" (§7 Scenario 1) becomes "no probe needed — there are only 3 skills."

2. **Tighten Frontend Spec Trio schema in SP2.**
   - `contracts/<domain>-ui.yaml` MUST carry `backend_operation_id: <id>` per route. Validated by an explicit `swagger-cli`-style cross-check.
   - `specs/<domain>-frontend-l0.yaml` items each carry `backend_spec_ref: <DOMAIN-NNN>` linking to the backend compliance spec item they UI-test.
   - This makes `trio_integrity_guard.sh` a binary check (verdict §3f → PASS).

3. **Replace cross-trio "existence" with "coverage threshold."**
   - 60% backend-item coverage on the frontend side (auth: 26 items × 0.6 ≈ 16 frontend items minimum). Domains with no UI carry an explicit `no_frontend: true` marker in the backend spec.

4. **Add §4.0 Layer Membership Decision Table.** Concrete worked resolution for: `ProtectedRoute` (L2), `PaymentCheckoutForm` (L2, because it's reusable across payment-like flows), `AppHeader` (L1.5 — or rename L1 to "primitives + chrome").

5. **De-parallelize Phase 3 honestly.** SP9 first; SP10/SP11 then parallel after SP9 lands. Phase 3 wall-time estimate drops from "3–4 days parallel" to "5–6 days sequential-then-parallel," but the estimate becomes credible.

6. **Add explicit shadcn drift probe** as discussed in §2. `templates/L1/_check-shadcn-drift.sh` diffs current files against `practices-react/upstream/shadcn-registry-<date>.snapshot.md`.

### What this synthesis preserves

- Hybrid Option C sequencing (foundation → vertical → horizontalize → integrate) — unchanged.
- 4 frontend template layers L1/L2/L3/L4 — unchanged.
- Frontend Spec Trio schema concept — refined, not removed.
- 4 hard gates + 2 new guards — unchanged (just lifted to flags on `/ax-verify`).
- DECISIONS.md ADR series — unchanged.
- Payment-cadence empirical bar (L4 sealed sub-agent) — unchanged.

### What this synthesis sheds

- 14 of the 17 SKILL.md files.
- The "tier" terminology where it serves no observable purpose.
- The unverifiable §7 Scenario 1 "quarterly review" governance loop.
- The false parallelism claim in Phase 3.

**No synthesis is available** for the §3f trio_integrity guard FAIL without the schema tightening in SP2. The Planner must revise that section regardless.

---

## 5. DELIBERATE mode pre-mortem adequacy

§7 (lines 777–854) lists 3 failure scenarios. Two are well-chosen; one is the wrong scenario.

- **Scenario 1 (Skill topology unmaintainable) — CORRECT.** This is the same defect the steelman §1 identifies. The PRD acknowledges it but mitigates with a governance loop that §10 forbids. Fix per synthesis.
- **Scenario 2 (Next.js migration breaks auth) — CORRECT.** Likelihood "High" is honest. Mitigation (git tag pre-nextjs-migration, SP1 Playwright anchor) is concrete. Adequate.
- **Scenario 3 (L2 abstractions over-fit to auth) — MISDIRECTED.** The PRD mitigates with a synthetic items-domain fixture at `templates/_fixtures/items-domain/` (line 844). But the actual stress test is not "auth → items"; it is **"auth → payment → catalog viewer."** Payment has idempotency keys, status state machines, and replay semantics that `LoginForm` does NOT exercise. The catalog viewer has Server-Components-reading-static-files that NO other domain exercises. The PRD should add explicit "L2 surface area validators" for each of SP9/10/11, not assume the synthetic items-domain covers all three.

**Missing scenario (would-be Scenario 4): "Frontend Spec Trio coverage drift relative to backend Spec Trio."** The PRD does not pre-mortem the case where backend adds items but frontend doesn't follow (or vice versa), drifting the bidirectional integrity claim. This is the very thing `trio_integrity_guard.sh` is supposed to catch, and §3f shows the guard as written cannot catch it. A pre-mortem here would force the SP2 schema tightening.

**Verdict on DELIBERATE adequacy: WEAK.** 2 of 3 scenarios are well-formed; 1 is mis-targeted; 1 important scenario is missing. The pre-mortem should be revised to mirror the synthesis (§4) changes.

---

## 6. Principle violation flags (CLAUDE.md vision lines)

Per the architect's deliberate-mode duty to flag principle violations explicitly:

| Principle | Violation level | Where |
|---|---|---|
| "React + Spring 둘 다 active equal partner" | **NONE.** Both stacks receive deliverables in every phase (§4.5 backend templates, §4.6 backend rules). Plan respects this principle. | n/a |
| "Catalog 확장은 정상 활동" | **NONE.** Plan welcomes 4 backend rules + 10 React rules under S7 audit (lines 303–316, 320–340). Plan respects this principle. | n/a |
| "Skill composition kit not single product" | **WEAK violation.** §1 antithesis shows 17-skill bundling pushes the project toward "single skill product with sub-products" framing. The synthesis (§4) restores the composition-kit framing. Current wording risks the principle. | §4.12 lines 402–434 |
| "Evidence-anchored rule provenance" | **STRUCTURAL violation.** §2 tradeoff shows shadcn adoption introduces a provenance gap that no current guard mechanism closes. Synthesis (§4 item 6) closes it via `_check-shadcn-drift.sh`. Must be remediated before SP5. | §4.1 lines 198–223, mitigation gap at line 593 |
| "Spec-before-code" | **WEAK violation.** §3e shows 5 SPs have TDD anchors that are circular (test references a deliverable produced inside the same SP). Synthesis (§4 implicit — fix the 5 anchors) closes it. | SP2/SP4/SP6/SP11/SP12 TDD anchors |
| "Few exposed surfaces" | **NONE on surface, WEAK on internals.** 3 Tier-1 caps user surface (good). But 14 internal SKILL.md files inflate AGENTS.md scope and pathPattern resolution density — same defect as the skill-topology principle issue above. | §4.12 |

**No FAIL-level violations** in the plan's intent. Two STRUCTURAL/WEAK violations in execution that the synthesis (§4) repairs.

---

## 7. Mandatory remediations before Codex Critic review

Numbered for the Planner's revision pass:

1. **Adopt synthesis §4 item 1** OR write a credible defense of 17 skills with a worked example showing two skills with overlapping `templates/L2/**` pathPatterns resolving disambiguously for an LLM. The current §7 Scenario 1 mitigation is not a defense.
2. **Adopt synthesis §4 items 2–3** (tighten Frontend Spec Trio schema with `backend_operation_id` + `backend_spec_ref` + coverage threshold). Without this, `trio_integrity_guard.sh` is not implementable as a binary gate. Hard requirement.
3. **Adopt synthesis §4 item 4** (Layer Membership Decision Table). Concrete worked examples for `ProtectedRoute`, `PaymentCheckoutForm`, `AppHeader`.
4. **De-parallelize Phase 3** per synthesis §4 item 5, OR explicitly partition the shared artifacts (AGENTS.md regen, ESLint plugin version, meta-schema freeze) and write the partition policy.
5. **Add shadcn drift probe** per synthesis §4 item 6 to SP3 deliverables. Closes the evidence-anchored provenance gap.
6. **Rewrite the 5 weak TDD anchors** (SP2/SP4/SP6/SP11/SP12) so each references a target that predates the SP's own implementation.
7. **Patch §7 pre-mortem**: re-target Scenario 3 to surface the auth → payment/catalog stress test (not the auth → items synthetic), add Scenario 4 for Spec Trio coverage drift.
8. **Add §8 ADR template field** `provenance_class: internal_design | external_anchored` per §3c remediation.

Items 1, 2, 4 are **structural blockers** — without them the plan ships an unverifiable claim. Items 3, 5, 6, 7, 8 are **clarity remediations** — the plan can ship with current wording but will cost engineer-days in interpretation.

---

## 8. Recommendations summary (effort / impact)

| # | Recommendation | Effort | Impact |
|---|---|---|---|
| 1 | Collapse 17 skills → 3 + flag-based axes (synthesis §4.1) | 0.5 d Planner | HIGH — removes the structural defect §1 names; cuts SP1 + SP4 scope by ~50% |
| 2 | Tighten Frontend Spec Trio schema with `operation_id` + `spec_ref` (synthesis §4.2–3) | 0.5 d Planner | HIGH — makes `trio_integrity_guard` binary-implementable |
| 3 | Add §4.0 Layer Membership Decision Table | 0.25 d Planner | MEDIUM — prevents L2/L4 contributor disputes during SP7–SP8 |
| 4 | De-parallelize SP9–SP11 or partition shared artifacts (synthesis §4.5) | 0.25 d Planner | MEDIUM — restores credibility of Phase 3 estimate |
| 5 | Add shadcn drift probe to SP3 (synthesis §4.6) | 0.25 d Planner | MEDIUM — closes evidence-anchored provenance gap |
| 6 | Rewrite 5 weak TDD anchors (§3e remediation) | 0.5 d Planner | MEDIUM — cold-start agent can execute every SP without invention |
| 7 | Patch §7 pre-mortem (Scenario 3 retarget, add Scenario 4) | 0.25 d Planner | LOW-MEDIUM — sharper failure modeling, no new work surfaced |
| 8 | Add ADR `provenance_class` field (§3c remediation) | 0.1 d Planner | LOW — closes internal-design ADR opt-out path |

Total Planner revision effort: **~2.6 days**. Cuts implementation scope (Phase 1+2) by an estimated 2–3 engineering days net positive.

---

## 9. Trade-offs table (Architect's alternatives matrix)

| Option | Pros | Cons | Architect verdict |
|---|---|---|---|
| **Ship PRD as-is** | Planner velocity preserved; gets to Codex faster | 3 structural defects (§3f FAIL, §3a/b/e/g WEAK) ship with the plan; SP9 contributors will hit them | Reject — defects are not stylistic |
| **Ship PRD with cosmetic edits only** | Faster than full synthesis | §3f trio_integrity FAIL is not cosmetic; remediation requires SP2 deliverable list change | Reject |
| **Adopt synthesis §4 wholesale (recommended)** | Reduces scope; binary-verifiable; respects all 7 CLAUDE.md principles; matches Anthropic workflows-and-feedback-loops pattern correctly | Requires Planner to rewrite §4.12 (skills inventory) and §SP1/SP4 scope sections; ~2.6 days Planner work | **Adopt** |
| **Reject and re-brainstorm from scratch** | Forces deeper rethink | Loses Hybrid Option C, the L1–L4 layering, and the Payment-cadence alignment — all of which are sound | Reject — antithesis is fixable, not fatal |

---

## 10. References

- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.draft.md:14-108` — RALPLAN-DR Summary + Viable Options A–D + recommended Option C.
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.draft.md:118-126` — baseline state (7 backend domains, Vite frontend, 68 React rules).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.draft.md:198-223` — L1 shadcn adoption (Side A of the §2 tradeoff).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.draft.md:264-280` — L4 domain workloads inventory (4 domains).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.draft.md:343-353` — Frontend Spec Trio schema artifacts (§3f FAIL evidence).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.draft.md:380-386` — guard extensions including new `trio_integrity_guard.sh` and `cross_trio_guard.sh` (§3f FAIL evidence).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.draft.md:402-434` — 3-tier skill topology, 17 skills (§1 antithesis target).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.draft.md:438-452` — inter-skill invocation graph (§3b double-coverage evidence).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.draft.md:528-571` — SP2/SP4 TDD anchors (§3e WEAK evidence).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.draft.md:604-679` — SP6/SP11 TDD anchors + SP9/10/11 parallelism claim (§3e + §3g evidence).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.draft.md:783-854` — DELIBERATE pre-mortem scenarios 1–3 (§5 evidence).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.draft.md:936-948` — §10 anti-pattern cross-check (§3d PASS evidence).
- `CLAUDE.md:7-43` — Project Vision: composition kit, React + Spring equal partner, catalog 확장 = 정상 활동.
- `CLAUDE.md:84-118` — Anti-Patterns (governance infinite loop, MockMvc, skill enforcing fork policy).
- `METHODOLOGY.md:296-323` — A.1 Auth + A.2 Rate-limit worked examples (empirical baseline for trio guard).
- `METHODOLOGY.md:373-393` — Appendix C 12-step procedure (Payment empirical cadence the PRD claims to inherit).
- `METHODOLOGY.md:441-496` — Recipe A Spring + Recipe B React verification primitive (§3f trio_integrity context).
- `practices/evals/evidence_guard.sh`, `practices/evals/substance_guard.sh`, `practices/evals/spec_ref_guard.sh`, `practices/evals/time_decay_guard.sh` — the 4 existing hard gates (§3b orphan-coverage evidence).
- `practices-react/SKILL.md:1-35` — current React pathPatterns (§1 antithesis evidence on overlap risk).
- `skills/ax-transform/SKILL.md:1-18` — current sole Tier-1 entry skill (baseline for §1).

---

**End of Architect review. Hand off to Critic / Codex (Step 4) with the mandatory remediations §7 as the gating change list.**
