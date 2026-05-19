# Architect Review — R7 iter 1

> **Reviewer:** Architect (ralplan consensus, DELIBERATE mode auto-triggered by new L4 = architectural mutation)
> **Date:** 2026-05-20
> **Subject:** `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.draft.md` (467 lines, Planner ITER 1)
> **Predecessor state:** `v1.4.0-recipes-complete` @ `main@ab44cce` — 6 active + 4 deferred recipes, 84 practice rules, 10 L4 domains, AGENTS.md sentinel `15c54e…14016` (disk-confirmed)
> **Mode:** DELIBERATE (Planner's choice — Architect concurs; first new L4 since R3 + first scheduler verdict harness = architectural mutation)

---

## Verdict: ITERATE

**Reason in one paragraph.** The Planner's central architectural claim — that `specs/scheduled-task-l0.yaml` + `contracts/scheduled-task-openapi.yaml` + `blueprints/scheduled-task-manifest.yaml` already exist on disk from R3 catalog-extension PR `26de945` and that SP41 is therefore a **stub-completion** rather than a **new L4 primitive introduction** — is **disk-CONFIRMED** (3/3 files present; SCHED-REGISTER-001 / SCHED-LOCK-001/002 / SCHED-EXECUTE-001 / SCHED-IDEMPOTENT-001 IDs match the §4.1 enumeration verbatim). That reframing is sound and materially reduces R7's architectural risk. **But** four specific issues block APPROVE: (a) a numeric premise (L1 = 48) is disk-WRONG (actual = 49) which propagates into the §2 R6 state table and §9 Honored Constraints; (b) TD-2026-05-20-022 (conditional empty-list guard specialization) is **architecturally unnecessary** — the existing guard already correctly handles unused L4 (`practices` + `file-storage` carry empty applied_recipe state today and the guard passes); (c) Korean evidence regresses from R6's `channel.io` 1-verbatim to **zero** Korean verbatim, weakening the "Korean enterprise standard stack" CLAUDE.md vision; (d) `applied_recipes` plural-list block on the scheduler README is presented as a routine empty-list scaffold, but the existing guard's `check_applied_recipe_declared` function explicitly REJECTS empty plural lists with "VIOLATION" — Planner must verify the guard treats the unused-L4 case correctly (it does, but via _absence of applied_recipe_ key, not via empty list), and the README's literal `applied_recipes: []` line as proposed in §4.1 / §5 Migration would actually TRIGGER the guard, not pass it. After these 4 fixes, the plan is approvable and matches R6 SP39/SP40 atomic-2 cadence. **Synthesis-A trim (defer community → R8, ship scheduler L4 alone)** is considered and REJECTED below — the bundling is honest cadence parity, not scope creep.

---

## Steelman antithesis

### Strongest case against the favored direction: "Scheduler-only L4 introduction is architecturally honest; pairing it with `community` recipe is cadence-theater that imports community's borderline-evidence-density risk into the L4 surface mutation."

The Planner explicitly frames §4.3 as "logical clustering by catalog-self-extension theme" and admits "Scheduler L4 + community recipe are not topically related". The Planner then justifies bundling on three grounds: (a) "first-of-kind in their layer for R7", (b) "share the same atomic SP41 verification suite", (c) "prove the composition kit's self-extension axis works in BOTH directions".

The counter-argument is sharper than the Planner allows. R5/R6 atomicity bundled recipes that **shared the same L4 mutation set** (R6's booking+marketplace+b2b-admin all touched `crud`/`audit-log`/`notification`/`search`/`auth` — append-only mutations on overlapping L4 README files, mechanically conflict-free). R7's bundle is heterogeneous: scheduler L4 mutates `templates/L4/scheduled-task/` (newly created) + `practices/AGENTS.md` (sentinel) + `practices/DECISIONS.md` + `skills/_tests/L4/scheduler-domain.test.sh` (NEW test harness); community recipe mutates `recipes/community/` (newly created) + `recipes/_MANIFEST.yaml` + 5 distinct L4 READMEs (`crud`, `audit-log`, `notification`, `search`, `auth`) + `specs/recipes/community-recipe-l0.yaml`. These are **two disjoint mutation surfaces** with no shared file overlap.

The architectural risk this surfaces: if scheduler-l4-verdict scores below threshold (Planner's §7 Pre-Mortem 5 admits "MEDIUM" likelihood — context-0 sub-agent may not find scheduler anchors in `practices/AGENTS.md` because AGENTS.md aggregates **practices rules only**, not L4 catalog — Planner even half-admits this in §11 "if generate_agents.sh does NOT depend on L4, this line is a no-op"), the SP42 tag policy ("hold tag IFF 2/2 verdicts pass") will hold tag for a community-recipe success because of a scheduler-verdict harness failure that is **fundamentally a harness-design problem unrelated to community**. The community recipe then sits in `active-verdict-pending` purgatory through no fault of its own.

**Steelman residual after considering Planner defense:** §4.3's "shared verification suite" claim is true at the script-invocation level (both use `recipe_governance_guard.sh`, `recipe_spec_referential_integrity_guard.sh`) but FALSE at the sealed-verdict level — community has prior verdict-harness precedent (6 sealed verdicts already exist at `skills/_tests/sealed-verdict/`); scheduler-l4-verdict.md is a **net-new harness shape** (Planner §7 Pre-Mortem 5). Bundling a proven-pattern recipe with a net-new-harness L4 in one atomic SP imports the harness risk into the recipe.

---

## Tradeoff tensions

### Tension: Atomic-2 cadence parity vs. mutation-surface disjointness

| Axis | Planner's choice (atomic-2 SP41+SP42) | Alternative (scheduler-only SP41, community → R8) |
|---|---|---|
| **Wall-time** | 5-6 d | 3-4 d (scheduler alone) + 3-4 d R8 (community alone) |
| **R6 cadence parity** | Strong — matches SP39/SP40 atomic-2 pattern | Breaks cadence (1 deliverable per cycle) |
| **Tag risk** | Tag held if EITHER verdict fails | Tag held only on scheduler verdict |
| **Mutation surface coupling** | Two disjoint surfaces in one commit | Single surface per cycle |
| **Reintroduction-trigger discipline** | All 3 R6-named gates (community + lms/cms unblocking) addressed | Scheduler-named gates only |
| **Composition kit "self-extension axis" proof** | Demonstrated in BOTH directions (L4 + recipe) | Only L4 axis demonstrated in R7 |
| **Verdict harness risk** | Net-new scheduler harness + proven community harness in same atomic | Net-new scheduler harness alone; community gets proven-pattern timing in R8 |
| **Catalog density** | 7 active recipes + 11 L4 domains at R7 close | 6 active recipes + 11 L4 at R7 close; community defers to R8 cycle |
| **Korean evidence rigor** | Zero Korean verbatim in §4.4 (regress from R6) | Same risk, different cycle |

The Planner picks atomic-2. The legitimate alternative is **Synthesis-A trim** — scheduler-only SP41+SP42 in R7, community → R8 — because community's evidence chain leans on a single Discourse verbatim and a Reddit-blocked × 3 downgrade, while scheduler has 2 external verbatim (Spring + Quartz) and an existing R3 Spec Trio. The cadence-parity argument cuts both ways: R5 shipped 3 recipes per cycle, R6 shipped 3, R7 at 1 recipe + 1 L4 is already a deliberate downshift. Going to 0 recipes + 1 L4 in R7 is **not** "regression" — it is honest evidence-rigor discipline, the same discipline R6 used when it Synthesis-A-trimmed 7→3.

**Why this is a real tradeoff, not perfect-optimization theater:** if Reddit fetch remains blocked at SP41 execution (Planner §7 Pre-Mortem 1 = HIGH likelihood, already observed × 3), the community recipe's external-anchor density is **exactly 1 verbatim** (Discourse) — at the R5/R6 floor with no buffer. Critic Pre-Mortem scenarios from R6 explicitly warned against bundling weak-evidence recipes with strong-evidence ones. R7's Planner acknowledges this in Option (3) rejection (paragraph "Cramming all 4 risks 1-2 verdicts falling below threshold") but does not apply the same logic to its own 1-recipe-+-1-L4 bundling.

---

## Synthesis

**A genuine third path exists.** Ship scheduler L4 in SP41 atomic (as Planner specs); ship community recipe in a **non-atomic** SP41b that runs sequentially AFTER SP41 succeeds. Specifically:

- **SP41** — scheduler L4 README + scaffold + AGENTS.md sentinel + DECISIONS.md TD-020 + `skills/_tests/L4/scheduler-domain.test.sh` + scheduler-l4-verdict.md (PENDING) — atomic single commit on scheduler axis only. SP41 closes on `bash skills/_tests/L4/scheduler-domain.test.sh` exit 0 + scheduler-l4-verdict.md ≥10/12 MUST + ≥5/8 SHOULD.
- **SP41b** — `recipes/community/` quartet + `specs/recipes/community-recipe-l0.yaml` + 5 L4 README appends + `recipes/_MANIFEST.yaml` move + community-verdict.md — separate commit, sequentially gated on SP41 passing. SP41b closes on community-verdict.md ≥10/12 MUST + ≥5/8 SHOULD.
- **SP42** — FINAL: tag `v1.5.0-scheduler-community` IFF 2/2 sealed verdicts pass; otherwise tag `v1.5.0-scheduler` (scheduler-only) and defer community → R8.

**Why this synthesis is viable (not theater):**
1. Mutation surfaces are disjoint, so two separate commits add zero merge complexity vs one atomic commit.
2. If scheduler-l4-verdict fails (Planner §7 Pre-Mortem 5 MEDIUM risk), community work is **never started** — saves the 3-4 d community implementation effort that would otherwise be stranded in `active-verdict-pending` purgatory.
3. If community-verdict fails (Reddit-blocked external-anchor density risk), scheduler ships clean at `v1.5.0-scheduler`, community defers to R8 with a refreshed `internal_design`-only rationale.
4. Atomic-Spec-Trio rule (Principle 5) is preserved **within each SP** — SP41 is atomic on scheduler axis, SP41b is atomic on community axis.

**Why the Planner's atomic-2 is still defensible:** wall-time-net (one PR → one tag) is faster on the happy path; cadence-parity with R6 SP39/SP40 is real signal value; the L4-cap-vs-frozen distinction in §5 is explicitly easier to argue when both deliverables ship together (the new L4 doesn't look "lonely" without a recipe-side companion in the same release).

**Architect recommendation on synthesis:** present this as an explicit alternative in §1 Viable Options, not as a critic-imposed override. Planner may legitimately stay with Option (2) atomic-2; Critic should see the synthesis was considered and rejected with reason, not omitted.

---

## Principle check (deliberate mode)

DELIBERATE mode auto-triggered (Planner self-declares this in §1 Mode). Per the ralplan consensus protocol, Architect must flag any internal principle inconsistency:

| Principle | Status | Notes |
|---|---|---|
| 1. Composition kit, not single product | **CONSISTENT** | Scheduler reuses existing R3 Spec Trio; community composes 5 existing L4. No skill / L3 / L2 / L1 addition. |
| 2. Spec-before-code, evidence-anchored | **PARTIAL VIOLATION** | §4.2 cites 2 invariants (`COMMUNITY-INV-003 → NOTIFICATION-PREF-001`, `COMMUNITY-INV-004 → ASVS-V11.1.4`, `COMMUNITY-INV-005 → sanitize-user-html-server-side.md`) marked "TO BE DISK-VERIFIED" — i.e., 3 of 5 community invariants are NOT spec-before-code at PRD time. They are spec-before-code-pending-SP41-prep, which is weaker discipline than R6 (where Critic iter-2 explicitly required all spec_refs disk-verified at PRD signature). Severity: MEDIUM. See Finding M3. |
| 3. Binary verification per axis | **CONSISTENT** | All 6 verification axes named (`/ax-verify`, 2 guards, scheduler-domain test, 2 sealed verdicts, AGENTS.md sentinel). |
| 4. Tier-1/Tier-2 caps FROZEN | **CONSISTENT** | No skill mutation. |
| 5. Atomic Spec Trio rule | **SOFT VIOLATION** | §4.3 explicitly admits scheduler + community are "not topically related"; atomicity within a single SP is defensible cadence-wise but stretches "atomic Spec Trio" semantics (Trio = spec + contract + manifest; here it bundles two independent Trios in one SP). Severity: LOW. Critic precedent (R6 iter-3) accepted similar bundling. |
| 6. Recipe does not ship code | **CONSISTENT** | Fork-receiver implements thread/post/comment entities. |
| 7. Scheduler is bona fide L4 primitive | **CONSISTENT** | §5 disk-verifies all 3 Spec Trio components from R3 PR `26de945`. R3-stub-completion framing is honest. |
| 8. No new L2/L3 this cycle | **CONSISTENT** | But §4.2 references `rich-text-editor (SP32 from R5)` — disk-verify this exists in current catalog before SP41 prep. |

**Principle 2 partial violation** is the most material. R5 iter-2 + R6 iter-2 both elevated "disk-verify all spec_refs at PRD signature" to a hard gate. R7's "TO BE DISK-VERIFIED in SP41 prep" language is a relaxation of that gate. Either re-tighten in iter 2 (verify the 3 pending refs now), or document the relaxation explicitly with rationale.

---

## Findings

### HIGH

**H1 — L1 component count disk-CONTRADICTS PRD §2 R6 state table (48 claimed vs 49 actual).**

`docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.draft.md:84` claims "L1 primitives | 48 | `templates/L1/components/`". Disk reality: `ls templates/L1/components/ | wc -l` returns **49**. The same incorrect count propagates to §9 Honored Constraints line 375 ("L1 catalog **= 48** (UNCHANGED)") and to §11 Branch+path summary.

Why this matters: §2 is the disk-verified baseline; numeric premises are exactly the kind of claim ralplan trusts without re-verification at execution time. A wrong baseline propagates into post-R7 numeric claims (R8 will inherit "49→49 UNCHANGED" but PRD-narratively R7 closed at 48). Critic will catch this if Architect does not.

**Fix:** Update §2 + §9 to "L1 = 49" with disk reference. Note: §9 line 375 also claims "L4 = 10 → 11" — confirmed by disk (`ls templates/L4/ | wc -l` = 10 currently; +1 for scheduled-task = 11 post-SP41).

References:
- `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.draft.md:84` — wrong baseline
- `templates/L1/components/` (49 entries on disk, 2026-05-20)

---

**H2 — TD-2026-05-20-022 (conditional empty-list guard specialization) is architecturally UNNECESSARY; the existing guard already correctly handles unused L4.**

Planner §8 TD-022 proposes a "one-line guard branch: empty `applied_recipes: []` on an L4 README is VALID IFF that L4 has no row in any active recipe's `enabled_l4_domains:`". Disk evidence proves this branch is **already implicitly correct in the existing guard logic** (`practices/evals/recipe_governance_guard.sh:55-77`):

```
check_applied_recipe_declared() {
    ...
    if grep -qE "^applied_recipes:" "$readme" 2>/dev/null; then
        # Must have at least one list item — empty list = VIOLATION
        ...
    fi
    echo "VIOLATION ..." — only emitted if NEITHER applied_recipe: nor applied_recipes: present
}
```

**The function is only invoked for L4 domains listed in an active recipe's `enabled_l4_domains:`.** Currently `practices` + `file-storage` L4 READMEs have **zero applied_recipe field** and the guard passes (exit 0, just verified) — because no active recipe enables them. Scheduler will be in the same state after SP41 lands: it has no recipe consumer in R7, so no recipe will list `scheduled-task` in its `enabled_l4_domains:`, so the guard will never invoke `check_applied_recipe_declared` against scheduler's README, so the README needs NO `applied_recipes:` block at all — not empty, not present.

Two consequences:
1. **TD-022 should be removed from §8.** It is not conditional; it is unnecessary. The §4.1 / §5 "applied_recipes: []" empty-list scaffold should also be removed — its presence would actively trigger the guard's empty-list violation, the opposite of Planner's intent.
2. **The L4 README structure for scheduler should match `practices/README.md` and `file-storage/README.md` (the two existing unused L4) — no `applied_recipes` key at all.** When R8 lms/cms recipes consume scheduler, they will add `scheduled-task` to their `enabled_l4_domains:` AND add `applied_recipes:` (plural, with `[lms, cms]`) to scheduler's README in the same R8 atomic commit — the standard recipe-arrival pattern.

This is a substantive simplification: removes 1 ADR, removes 1 fixture, removes 1 README structural decision.

**Fix:**
- Remove TD-022 entirely.
- §4.1: change `applied_recipes:` block initially empty list `[]` → "no `applied_recipes:` key (matches the `file-storage` + `practices` L4 pattern for unused-by-recipe L4)".
- §5 Migration step 1: remove "empty `applied_recipes: []` block".

References:
- `practices/evals/recipe_governance_guard.sh:55-77` — function body
- `templates/L4/practices/README.md` + `templates/L4/file-storage/README.md` — existing unused-L4 precedent
- `bash practices/evals/recipe_governance_guard.sh` exit 0 today proves the pattern works

---

### MEDIUM

**M1 — Korean evidence verbatim count regresses from R6 (1 verbatim = channel.io) to R7 (zero Korean verbatim).**

R6 PRD §4.4 ledger (line 212): `channel.io/ko` → "200 OK — verbatim Korean" `"AI로 더 편해진 사내 메신저"`. R7 PRD §4.4 ledger contains 5 fetch attempts but **zero Korean verbatim** — both `dcinside.com` and `clien.net` returned 200 OK with no API documentation visible and were classified `internal_design`. The remaining 3 verbatims (Spring + Quartz + Discourse) are English-tier sources.

This is a real regression against the CLAUDE.md vision line ("Korean enterprise standard stack (React + Spring Boot)…AI agent가 규칙 안에서만 동작하는 코드"). R5 + R6 both anchored at least one Korean verbatim per cycle. R7 is the first cycle since R5 with a zero-Korean-verbatim ledger.

The Planner mitigates by saying "Korean refs have no public API docs" — but that is not the same as "no Korean verbatim is available". Alternative Korean anchors that were not attempted:
- 토스 (toss.im) tech blog — public technical content
- 네이버 D2 (d2.naver.com) — Naver's developer publication
- 카카오 tech (tech.kakao.com) — public Kakao tech blog
- 우아한형제들 (woowahan.com tech blog) — public Korean engineering blog
- LINE engineering (engineering.linecorp.com) — has Korean content

Any of these has a non-zero chance of producing a verbatim quote about scheduling, jobs, or community/forum patterns. R7 §4.4 does not attempt any.

**Severity rationale (MEDIUM not HIGH):** the 3 English verbatim anchors clear the R5/R6 1-external-floor for each deliverable. Korean is not a hard gate; it is a project-vision alignment signal. But the regression is real and the Planner should either (a) attempt 2-3 additional Korean fetches before iter 2, or (b) explicitly document why R7 cycle has zero Korean verbatim (with the alternatives above considered and rejected with reason).

**Fix:** Either expand §4.4 with 2-3 additional Korean fetch attempts, or add §4.5 explicit rationale for Korean-verbatim absence this cycle.

References:
- `docs/superpowers/specs/2026-05-19-r6-recipes-prd.md:212` — R6 channel.io verbatim
- `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.draft.md:224-225` — R7 Korean fetches (both `internal_design`)
- CLAUDE.md project vision — Korean enterprise standard stack framing

---

**M2 — Reddit `internal_design` downgrade × 3 may be premature; URL variants not exhausted.**

§4.4 records 3 Reddit-prefix fetch attempts (`developers.reddit.com/`, `developers.reddit.com/docs`, `www.reddit.com/dev/api/`), all blocked with "Claude Code is unable to fetch from {developers,www}.reddit.com". The Planner concludes the fetcher is host-blocked.

Alternative paths not attempted:
- `https://www.reddit.com/r/redditdev/` — the official Reddit dev subreddit landing page is public web content; if also blocked, the host-block hypothesis is confirmed. If 200 OK, an arbitrary post's content is verbatim-able.
- `https://github.com/reddit-archive/reddit/wiki/API` — Reddit's open-source archive on GitHub; same content, different host (github.com is well-known to NOT be fetcher-blocked).
- `https://praw.readthedocs.io/` — PRAW (Python Reddit API Wrapper) ReadTheDocs page; describes the same API surface, often quoted verbatim from Reddit's docs.
- `https://devvit.dev/` — Reddit's newer developer platform front-door, separate host from `developers.reddit.com`.

Any of these has a different host fingerprint from the 2 blocked Reddit domains. A "fetcher-blocked" conclusion is honest only if alternative-host attempts also fail.

**Severity rationale (MEDIUM):** the Planner's stated rule is "no fabrication; downgrade only after attempt". The downgrade IS post-attempt — but the attempts were all against 2 of 3 possible Reddit host fingerprints. A more rigorous attempt set would test the GitHub archive + PRAW + Devvit hosts before concluding. This is not the same as fabrication; it is "incomplete attempt set". The fix is small and increases evidence rigor.

**Fix:** Add 2-3 additional Reddit-content fetch attempts via different hosts (github.com/reddit-archive, praw.readthedocs.io, devvit.dev) in §4.4 SP41 pre-flight re-attempt step. If any returns verbatim, upgrade Reddit row to `external`.

References:
- `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.draft.md:221-223` — current Reddit attempts (2 hosts only)
- Planner §6 SP41 pre-flight already specifies "SP41 re-runs WebFetch on the 3 Reddit-prefix URLs" — easy extension

---

**M3 — 3 of 5 community business_invariants cite `spec_ref:` anchors marked "TO BE DISK-VERIFIED".**

§4.2 lists 5 COMMUNITY-INV-XXX invariants. Disk verification (2026-05-20):

| Invariant | spec_ref claim | Disk reality |
|---|---|---|
| COMMUNITY-INV-001 | `specs/audit-log-l0.yaml#AUDIT-RECORD-001` + `#AUDIT-RECORD-002` | **PASS** — both anchors found at lines 7, 23 |
| COMMUNITY-INV-002 | `specs/search-l0.yaml#SEARCH-AUTHZ-001` | **PASS** — found at line 7 |
| COMMUNITY-INV-003 | `specs/notification-l0.yaml#NOTIFICATION-PREF-001` (Planner notes "TO BE DISK-VERIFIED") | **FAIL** — actual anchor is `NOTIF-PREF-001` at line 131 (verb prefix is `NOTIF-`, not `NOTIFICATION-`). Planner already flagged "if anchor name differs, planner re-maps". Fix is mechanical. |
| COMMUNITY-INV-004 | `specs/auth-asvs-l1.yaml#ASVS-V11.1.4` + `practices/rules/idempotency-key-on-mutations.md` | **FAIL** — `ASVS-V11.1.4` does NOT exist in `auth-asvs-l1.yaml` (no V11 entries at all; the file covers V2/V3/V4/V5 ASVS sections — verified by `grep -nE "V11" specs/auth-asvs-l1.yaml` returning empty). The idempotency-key rule_ref PASSES (`practices/rules/idempotency-key-on-mutations.md` exists). |
| COMMUNITY-INV-005 | `practices/rules/sanitize-user-html-server-side.md` | **FAIL** — rule file does not exist (`ls practices/rules/ \| grep -iE "sanitize\|html\|xss"` returns empty). Planner already flagged "if absent, recipe-level invariant authored in same SP41 as recipe spec" — i.e., Planner plans to author the spec, not the rule. Acceptable, but `rule_ref:` cannot point at a non-existent rule file at PRD signature without `recipe_invariants_must_resolve` guard failure. |

R6 iter-2 Critic precedent: "every cited anchor below appears in the actual spec file" was elevated to a hard gate. R7 has 2 INV anchors that are FALSE and 1 rule_ref that points at a non-existent file. The Planner labels these "TO BE DISK-VERIFIED" — accurate self-flagging, but **not yet resolved**. PRD signature with 3 of 5 invariants unresolved is weaker than R6.

**Fix paths:**
- **COMMUNITY-INV-003:** Change `#NOTIFICATION-PREF-001` → `#NOTIF-PREF-001`. Mechanical 1-character correction.
- **COMMUNITY-INV-004:** Either find the actual ASVS rate-limit anchor in `specs/auth-asvs-l1.yaml` (V11.1.4 is "Verify that the application has anti-automation controls" — but this anchor must be searched for under whatever ID the disk file uses; if absent, swap to a different rate-limit anchor that DOES exist, e.g., search `practices/rules/` for rate-limit related rules), OR mark the invariant as `rule_ref:` only (pointing to `practices/rules/idempotency-key-on-mutations.md` alone), OR author the missing rule in SP41 atomic.
- **COMMUNITY-INV-005:** If the sanitize-user-html rule will be authored in SP41, the recipe spec must NOT cite it as `rule_ref:` at PRD-signature time; instead, label the invariant `co-shipped-rule` and pair it with a TDD anchor that ships the rule file + invariant in the same atomic commit.

Iter 2 should land these 3 resolutions before Critic re-review.

References:
- `specs/audit-log-l0.yaml:7,23` — PASS
- `specs/search-l0.yaml:7` — PASS
- `specs/notification-l0.yaml:131` — NOTIF-PREF-001 (PRD cited wrong prefix)
- `specs/auth-asvs-l1.yaml` — no V11 entries
- `practices/rules/idempotency-key-on-mutations.md` — exists
- `practices/rules/` — no sanitize-user-html rule

---

**M4 — `applied_recipes: []` on scheduler README as proposed would actively TRIGGER the existing guard's empty-list violation; this is the inverse of the Planner's intent.**

Closely related to H2 but distinct severity. §4.1 (Composition note) + §5 Migration step 1 + §11 path summary all reference an explicit `applied_recipes: []` (empty plural list) block on the scheduler README. The existing `recipe_governance_guard.sh` `check_applied_recipe_declared` function (lines 55-77, disk-verified) explicitly emits:

> `VIOLATION [business-domain-must-declare-applied-recipe]: $domain_label README has applied_recipes: but list is empty (no list entries found)`

…whenever an L4 README contains `applied_recipes:` followed by no list items.

The Planner's intent ("scheduler README's `applied_recipes:` IS allowed to be empty AT INTRODUCTION because it is a NEW primitive") is contradicted by the guard implementation. The guard only emits its violation if the function is **invoked**, and the function is only invoked when an active recipe lists the L4 in `enabled_l4_domains:`. Since no R7 recipe enables scheduler, the function is not invoked, so the violation does not fire. But this only works **if scheduler's README has no `applied_recipes:` block at all** — if it has `applied_recipes: []`, the README is structurally inconsistent (it asserts membership state but has none) and any future SP that touches the recipe-governance code path will hit confusion.

This is essentially M4 = "the literal `[]` syntax should not appear in the README; absence-of-key is the correct expression of 'unused L4'". H2's broader claim is "TD-022 is unnecessary"; M4's narrower claim is "the README syntax in §4.1/§5/§11 is structurally inconsistent with how unused L4 are expressed today".

**Fix:** Same as H2 — replace `applied_recipes: []` with no `applied_recipes` key. Document in §4.1 Composition note: "No `applied_recipes:` block at introduction; key + plural-list will be added by R8 when first recipe (lms or cms) consumes scheduler. Matches existing `file-storage` + `practices` L4 README pattern for unused-by-recipe L4."

References:
- `practices/evals/recipe_governance_guard.sh:55-77`
- `templates/L4/file-storage/README.md`, `templates/L4/practices/README.md` (existing unused-L4 precedent)
- `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.draft.md:162,276,280` — current `applied_recipes: []` references

---

## Synthesis-A trim assessment

**Could R7 ship scheduler L4 ONLY (defer community → R8)?**

**Pro-trim argument (legitimate):**
- Community evidence chain is borderline: Discourse single verbatim + Reddit blocked × 3 + Korean × 2 internal_design. R5/R6 floor is 1-verbatim + 1-internal_design backup — community has 1+3+ effectively, which technically clears the floor but with zero buffer.
- Scheduler L4 introduction is the architecturally novel work this cycle. Bundling it with a borderline-evidence recipe doubles the verdict failure surface for SP42.
- Mutation surfaces are disjoint (scheduler axis vs community axis); no commit-atomicity reason to bundle.
- 3 of 5 community business_invariants are unresolved at PRD signature (M3). A trim defers exactly those unresolved bindings to R8 where they can be resolved properly.
- R6 already trimmed 7→3 for the same reason ("over-bundling weak-evidence with strong-evidence risks rolling back everything"). R7 self-quotes this argument in Option (3) rejection but does not apply it to its own 1-recipe + 1-L4 bundling.

**Pro-bundle argument (Planner's choice, defensible):**
- R5 = 3 recipes, R6 = 3 recipes. R7 at 1 recipe + 1 L4 is already a deliberate downshift. Going to 0 recipes + 1 L4 is a stronger downshift that signals "L4 introduction is the work; recipe absorption pauses".
- The R6 reintroduction_trigger language explicitly names `community` as "Korean community platform OR public Discourse-style API integration request" — the Discourse arm is satisfied, so community is genuinely eligible. Deferring an eligible recipe is also a signal-loss.
- Cadence-parity with R6 SP39/SP40 atomic-2 has documentation value: future PRDs can cite "R7 followed R6's atomic-2 pattern" without exception.
- Atomic-2 saves 1 PR-cycle wall-time on the happy path.
- The community work is small (5 L4 README appends + 1 manifest move + 4 new files + 1 spec). Trimming saves perhaps 1-2 days, not 5-6.

**Architect verdict on trim:** **Recommend Synthesis (the 3rd path above — SP41 atomic on scheduler, SP41b sequential on community, SP42 tags partial-state-aware)** rather than full trim. Reasons:

1. The mutation-surface-disjointness observation is the strongest data point — it favors split-SP, not full-defer.
2. Community is genuinely eligible per its `reintroduction_trigger:` text. Full-defer would be over-conservative.
3. The split-SP path preserves R7's "community absorbed" narrative while protecting against scheduler-verdict failure stranding community.
4. The 3 unresolved spec_refs (M3) can be resolved in SP41b prep without holding SP41 scheduler.

If Planner declines the split and stays with atomic-2, that is defensible — but the rationale for declining should be documented in iter 2 §1 Viable Options (currently the split-SP option is absent from Options (1)/(2)/(3)).

---

## Recommendations to Planner

For iter 2 (priority order):

1. **(H1)** Correct L1 count: 48 → 49 in §2 R6 state table + §9 Honored Constraints + §11. Disk-verify with `ls templates/L1/components/ | wc -l`.

2. **(H2)** Remove TD-2026-05-20-022 from §8. Remove `applied_recipes: []` empty-list scaffold from §4.1 Composition note + §5 Migration step 1 + §11. Document in §4.1: "scheduler README has no `applied_recipes:` block at introduction (matches `file-storage` + `practices` unused-L4 pattern); R8 lms/cms recipe consumption adds the key + plural list in the same R8 atomic commit."

3. **(M3)** Resolve the 3 unresolved community spec_refs at PRD signature, not in SP41 prep:
   - COMMUNITY-INV-003: `#NOTIFICATION-PREF-001` → `#NOTIF-PREF-001`
   - COMMUNITY-INV-004: replace `#ASVS-V11.1.4` with an actual rate-limit anchor that exists in `specs/auth-asvs-l1.yaml`, or remove the spec_ref and rely on `rule_ref: practices/rules/idempotency-key-on-mutations.md` alone
   - COMMUNITY-INV-005: either (a) include `practices/rules/sanitize-user-html-server-side.md` as a co-shipped rule in SP41 atomic (with §4.2 marking it `co-shipped-rule:` not `rule_ref:`), or (b) swap to an existing XSS-related rule

4. **(M1)** Either expand §4.4 with 2-3 additional Korean fetch attempts (토스, 네이버 D2, 카카오 tech, 우아한형제들, LINE engineering Korean), or add §4.5 with explicit "Korean verbatim absence this cycle" rationale.

5. **(M2)** Add 2-3 additional Reddit-content fetch attempts via different host fingerprints to §4.4 SP41 pre-flight (github.com/reddit-archive, praw.readthedocs.io, devvit.dev) before concluding Reddit is `internal_design`-only.

6. **(Synthesis — optional but recommended)** Add Synthesis Option (4) to §1 Viable Options Considered: "SP41 scheduler-atomic + SP41b community-atomic-sequential + SP42 FINAL-with-partial-tag-policy". Either adopt it or document explicit rationale for staying with Option (2) (likely: cadence-parity-with-R6-and-atomic-2-saves-1-PR-cycle).

7. **(LOW — cleanup)** §4.2 references `rich-text-editor (SP32 from R5)` — disk-verify this L2 block exists in current catalog. If absent, either find alternative or add to override_allowed notes.

8. **(LOW — clarity)** §11 line 421 contains a hedged passage ("if generate_agents.sh does NOT depend on L4, this line is a no-op and no sentinel sha change occurs") — resolve the hedge by running `generate_agents.sh --dry-run` (or equivalent) at iter 2 to confirm whether L4 changes affect the sentinel sha. If not, remove the §1/§3 claims that "AGENTS.md sentinel sha recomputed via `practices/generate_agents.sh`" — they would be misleading.

**After these 7-8 fixes are resolved**, PRD is approvable for SP41 execution. The R3-stub-completion reframing (the central architectural claim) is sound and disk-confirmed; the issues above are all surface-level integrity gaps, not structural plan problems.

---

## Disk verifications performed (audit trail)

| Claim | Method | Result |
|---|---|---|
| `specs/scheduled-task-l0.yaml` exists | `ls` + content read | PASS — 75 lines, 10 items, IDs match §4.1 enumeration verbatim (SCHED-REGISTER-001 / SCHED-LOCK-001/002 / SCHED-EXECUTE-001 / SCHED-IDEMPOTENT-001) |
| `contracts/scheduled-task-openapi.yaml` exists | `ls` | PASS |
| `blueprints/scheduled-task-manifest.yaml` exists | `ls` | PASS |
| `templates/L4/scheduled-task/` absent | `ls templates/L4/` | PASS (absent — confirms SP41 will create) |
| L1 count = 48 (claimed) | `ls templates/L1/components/ \| wc -l` | **FAIL — 49 on disk** (H1) |
| L4 count = 10 (claimed) | `ls templates/L4/ \| wc -l` | PASS — 10 |
| AGENTS.md sentinel sha = `15c54e…14016` | `grep` in `practices/AGENTS.md` | PASS |
| COMMUNITY-INV-001 spec_ref `#AUDIT-RECORD-001` | `grep -nE 'id: "AUDIT-RECORD-001"' specs/audit-log-l0.yaml` | PASS (line 7) |
| COMMUNITY-INV-001 spec_ref `#AUDIT-RECORD-002` | `grep -nE 'id: "AUDIT-RECORD-002"'` | PASS (line 23) |
| COMMUNITY-INV-002 spec_ref `#SEARCH-AUTHZ-001` | `grep -nE 'id: "SEARCH-AUTHZ-001"'` | PASS (line 7) |
| COMMUNITY-INV-003 spec_ref `#NOTIFICATION-PREF-001` | `grep -nE 'NOTIFICATION-PREF' specs/notification-l0.yaml` | **FAIL — actual anchor `NOTIF-PREF-001` at line 131** (M3) |
| COMMUNITY-INV-004 spec_ref `#ASVS-V11.1.4` | `grep -nE 'V11' specs/auth-asvs-l1.yaml` | **FAIL — no V11 entries** (M3) |
| COMMUNITY-INV-004 rule_ref `practices/rules/idempotency-key-on-mutations.md` | `ls` | PASS |
| COMMUNITY-INV-005 rule_ref `practices/rules/sanitize-user-html-server-side.md` | `ls practices/rules/` filter | **FAIL — rule does not exist** (M3) |
| Existing guard handles unused L4 correctly | `bash practices/evals/recipe_governance_guard.sh; echo $?` | PASS (exit 0; `practices` + `file-storage` carry no applied_recipe key and guard tolerates) (H2) |
| Existing guard rejects empty `applied_recipes: []` lists | code read at `practices/evals/recipe_governance_guard.sh:55-77` | CONFIRMED (M4) |
| R6 Korean verbatim count = 1 (channel.io) | `grep` in `docs/superpowers/specs/2026-05-19-r6-recipes-prd.md:212` | CONFIRMED (M1) |
| R7 Korean verbatim count = 0 | `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.draft.md:224-225` (both `internal_design`) | CONFIRMED (M1) |

---

## References

- `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.draft.md` (467 lines, Planner ITER 1)
- `docs/superpowers/specs/2026-05-19-r6-recipes-prd.md:212` — R6 Korean verbatim baseline
- `docs/superpowers/specs/2026-05-19-r6-recipes-architect-review.md` — R6 Architect tone/rigor reference (2 HIGH + 4 MEDIUM + Synthesis-A trim)
- `specs/scheduled-task-l0.yaml` — R3 stub confirmed
- `contracts/scheduled-task-openapi.yaml` — R3 stub confirmed
- `blueprints/scheduled-task-manifest.yaml` — R3 stub confirmed
- `specs/audit-log-l0.yaml:7,23` — INV-001 anchors PASS
- `specs/search-l0.yaml:7` — INV-002 anchor PASS
- `specs/notification-l0.yaml:131` — INV-003 anchor mislabeled (NOTIF- vs NOTIFICATION-)
- `specs/auth-asvs-l1.yaml` — no V11 ASVS entries (INV-004 anchor FAIL)
- `practices/rules/idempotency-key-on-mutations.md` — exists
- `practices/rules/` — no sanitize-user-html rule (INV-005 anchor FAIL)
- `practices/evals/recipe_governance_guard.sh:55-77` — empty-list rejection logic
- `practices/AGENTS.md:3` — sentinel sha matches PRD claim
- `templates/L1/components/` — 49 entries on disk (PRD claims 48)
- `templates/L4/` — 10 entries on disk
- `templates/L4/file-storage/README.md`, `templates/L4/practices/README.md` — existing unused-L4 precedent (no applied_recipe key)
- `recipes/_MANIFEST.yaml` — current deferred_recipes block (community + lms + cms + internal-it)
