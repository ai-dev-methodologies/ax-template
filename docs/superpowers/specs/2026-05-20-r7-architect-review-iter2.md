# Architect Review — R7 iter 2

> **Reviewer:** Architect (ralplan consensus, iter 2 narrow verification)
> **Date:** 2026-05-18
> **Subject:** `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.iter2.md` (433 lines)
> **Iter 1:** `2026-05-20-r7-architect-review.md` (2 HIGH + 4 MEDIUM + Synthesis-B)
> **Critic iter 1:** `2026-05-20-r7-critic-codex-iter1.md` (1 BLOCKING + 5 hard + 5 soft)

---

## Verdict: APPROVE

All 6 iter-1 findings closed with disk evidence. Synthesis-B (Option 4) adopted with explicit rationale. One INFORMATIONAL concern surfaced about INV-005 `co-shipped-rule:` key vs existing guard schema — self-correcting at SP41b execution gate. PRD is ready for Critic re-review.

---

## Closure check (6)

| # | Finding | Status | Evidence |
|---|---|---|---|
| **H1** | L1 = 48 → 49 | **CLOSED** | `iter2.md:28` "L1 = 49", `:90` table cell "**49**", `:359` constraints "= 49", `:412` verdict "L1 = 49". No `L1 = 48` remains except in change-log historical context. Disk: `ls templates/L1/components/ \| wc -l = 49` ✓ |
| **H2** | TD-022 + `applied_recipes: []` | **CLOSED** | `iter2.md:324` "§8 ADR Template (3 entries — TD-022 DELETED per H2)"; `:350` strikethrough "~~TD-2026-05-20-022 (DELETED iter 2 per H2)~~"; `:394` out-of-scope reaffirms; `:144,367` explicit "NO `applied_recipes: []` literal". Zero `applied_recipes: []` literal in §4.1 / §5 / §11. Scheduler README pattern = no `applied_recipes:` key (matches `file-storage` + `practices` precedent) ✓ |
| **M1** | Korean retry (5 hosts) | **CLOSED** | §4.4 evidence ledger contains 5 Korean ledger rows: `toss.tech` (200 OK no verbatim), `d2.naver.com` (fetcher-blocked), `tech.kakao.com` (200 OK no verbatim), `techblog.woowahan.com` × 2 (200 OK no scheduler verbatim), `engineering.linecorp.com/ko` (200 OK no relevant verbatim) — all timestamped 2026-05-20 (NEW iter 2 attempt rows). Zero-Korean-cycle exception explicitly rationale'd at `:237` (stack-level vision honored; per-cycle Korean signal could not replicate R6's channel.io find without fabrication) ✓ |
| **M2** | Reddit upgrade to `external` | **CLOSED** | Reddit row at `iter2.md:221`: `github.com/reddit-archive/reddit/wiki/API` 200 OK with 2 verbatim quotes (OAuth2 + 60-rpm rate). `provenance_class: external`. Upgrade noted explicitly "from iter-1 `internal_design` to `external`". PRAW + Devvit retry attempts also logged (lines 222–223). §8 TD-023 retained with verbatim. Community external anchor density = 2 (Discourse + Reddit-archive) ✓ |
| **M3** | 3 invariant refs | **CLOSED** | (a) INV-003 cites `NOTIF-PREF-001` at `:191` — disk: `specs/notification-l0.yaml:131` `id: "NOTIF-PREF-001"` ✓; (b) INV-004 cites `ASVS-V2.2.1` at `:192` — disk: `specs/auth-asvs-l1.yaml:47` `id: "ASVS-V2.2.1"` + line 49 anti-automation requirement (Planner picked the existing rate-limit anchor) ✓; (c) INV-005 reshaped as `co-shipped-rule: community-html-sanitization` at `:193` — recipe-level invariant only, no new `practices/rules/*.md` file, honors Principle 8 ✓ (see Independent attack #2 below for guard-schema concern) |
| **M4 + Critic L + Synthesis-B** | bundled iter-1 closure | **CLOSED** | (a) No `applied_recipes: []` literal anywhere — already verified in H2; (b) `/ax-verify-domain scheduled-task` REMOVED from SP41 binary gate — `iter2.md:148` Must NOT Have explicit; replaced with `bash skills/_tests/L4/scheduler-domain.test.sh` at `:128,168,179,247`; (c) Synthesis-B Option (4) at `:57–60` with explicit pros/cons + **CHOSEN** rationale; §4.5 has 3 SPs (SP41/SP41b/SP42) with hard gate "SP41b commit only opens after SP41 + scheduler-l4-verdict ≥10/12 MUST + ≥5/8 SHOULD" at `:248,251`. Partial-tag policy at `:249,304,406` ✓ |

---

## Disk validation

| Check | Expected | Actual | Status |
|---|---|---|---|
| `grep -c "NOTIF-PREF-001" specs/notification-l0.yaml` | ≥1 | 1 | PASS |
| `grep -c "ASVS-V2.2.1" specs/auth-asvs-l1.yaml` | ≥1 | 2 | PASS |
| `ls templates/L4/file-storage/README.md` | exists | exists | PASS |
| `ls templates/L4/practices/README.md` | exists | exists | PASS |
| `grep -c "applied_recipe" templates/L4/{file-storage,practices}/README.md` | 0 (no key) | 0 | PASS (precedent confirmed) |
| Iter 2 line count | 433 | 433 | PASS |
| §4.4 Korean ledger rows (NEW iter 2) | 5 | 5 (toss/d2naver/kakao/woowahan/linecorp) | PASS |
| §4.4 Reddit ledger rows (incl. retries) | ≥4 | 6 (github-archive + praw + devvit-quickstart + 2 superseded iter-1 rows + reddit.com/dev) | PASS |

---

## Independent attack (1 NEW concern)

### INFORMATIONAL — INV-005 `co-shipped-rule:` key is not understood by existing `recipe_spec_referential_integrity_guard.sh`

**Finding.** PRD §4.2 line 193 declares INV-005 as a recipe-level invariant with `co-shipped-rule: community-html-sanitization` and explicitly states "No `rule_ref:` line; instead `invariant_test:` cites `frontend/tests/recipes/community-sanitize.spec.ts` co-shipped in SP41b."

Disk evidence (`practices/evals/recipe_spec_referential_integrity_guard.sh:135–141`):

```python
has_spec_ref = bool(re.search(r'spec_ref:', block))
has_rule_ref = bool(re.search(r'rule_ref:', block))
if not has_spec_ref and not has_rule_ref:
    print(f"VIOLATION [{spec_path.name}]: invariant {inv_id_str} has neither spec_ref nor rule_ref")
```

The guard treats absence of BOTH `spec_ref:` and `rule_ref:` as a VIOLATION. The new `co-shipped-rule:` key is not in the guard's allowlist. As written, INV-005 will trigger the integrity guard violation at SP41b execution.

**Why iter 1 Architect + Critic missed this.** Iter 1 Architect rec 3c offered option "(a) include `practices/rules/sanitize-user-html-server-side.md` as a co-shipped rule in SP41 atomic (with §4.2 marking it `co-shipped-rule:` not `rule_ref:`)" — Architect's option (a) assumes a *real* rule file is co-shipped (so `rule_ref:` could still be present pointing to the newly-created file). Critic iter 1 (line 73) said "replace/remove or co-ship the nonexistent sanitize-user-html rule". The Planner chose a hybrid: no new rule file AND no `rule_ref:`. Neither reviewer specifically addressed whether `co-shipped-rule:` (a brand-new key) would satisfy the existing guard.

**Why this is INFORMATIONAL, not BLOCKING.** SP41b is gated by `recipe_spec_referential_integrity_guard.sh` (§4.5 verification column line 248). The Planner cannot land SP41b with a violating recipe spec — the gate is mechanical and will force a fix-in-flight. Three resolution paths are all viable:
- (a) Add `rule_ref: practices/rules/community-html-sanitization.md` and co-ship the rule file (Architect's original option (a); minor Principle 8 stretch but smallest delta).
- (b) Find an existing `practices/rules/*.md` covering HTML sanitization and reuse it.
- (c) Extend the guard to recognize `co-shipped-rule:` as a third valid key (one-line additive change in `recipe_spec_referential_integrity_guard.sh:135`).

PRD §6 "Pre-flight gate (before SP41b starts)" line 301 already prescribes "Disk-verify 5 community spec_ref anchors per §4.2 (all 5 already resolved in iter 2 — re-check at execution)". Adding a guard-shape check at the same gate is mechanical.

**Recommendation.** Flag this to Planner during execution-prep; not a blocker for PRD approval. Planner has 3 viable resolution paths and the gate is self-detecting.

**Also-verified-clean (negative findings):**
- **SP41b sequential gate** — `iter2.md:248,251` explicit "SP41b commit only opens after SP41 + scheduler-l4-verdict ≥10/12 MUST + ≥5/8 SHOULD" and "**SP41b failure does NOT roll back SP41**". Correct sequencing; no race.
- **Partial-tag atomicity** — `iter2.md:249,304` "tag `v1.5.0-scheduler-community` IFF 2/2 pass; partial `v1.5.0-scheduler` IFF only scheduler passes; no tag IFF scheduler fails (SP41 reverted)". Clear policy; no ambiguity about partial-tag-during-rollback.
- **`skills/_tests/L4/scheduler-domain.test.sh` net-new path** — directory `skills/_tests/L4/` does not currently exist on disk; SP41 must `mkdir -p`. PRD §4.5 row (a)–(d) lists this as an SP41 atomic-create artifact alongside the README. TDD anchor at `:175–181` is concrete (test_file, assertion, expected_RED_reason, first_GREEN_command, owning_SP all specified). Acceptable.

---

## Final reasoning

All 6 iter-1 findings are closed with concrete disk evidence and surgical edits. Synthesis-B (Option 4) is adopted with full rationale and visible 3-SP structure (SP41 scheduler-atomic → SP41b community-atomic-sequential → SP42 partial-tag-aware FINAL). The Korean zero-verbatim exception is explicitly justified rather than hidden; Reddit is genuinely upgraded to `external` via GitHub-archive verbatim; all 5 community invariants resolve at PRD signature (INV-003 spec_ref corrected, INV-004 spec_ref swapped to existing ASVS-V2.2.1 anchor, INV-005 reshaped as recipe-level invariant).

The one new concern surfaced (INV-005 `co-shipped-rule:` vs existing integrity-guard schema) is INFORMATIONAL because the SP41b execution gate is mechanically self-detecting — the integrity guard will block landing if the recipe spec violates the schema, forcing the Planner to pick one of 3 viable resolution paths. This is exactly the kind of "self-correcting at the gate" behavior the binary-verification principle exists for.

PRD is ready for Critic re-review. No iter 3 needed unless Critic raises new BLOCKING findings.

---

## Re-review trigger

N/A — APPROVE. No iter 3 required from Architect axis.

---

## References

- `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.iter2.md:28,90,359,412` — H1 closure
- `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.iter2.md:144,324,350,367,394` — H2 closure
- `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.iter2.md:228–232,237` — M1 Korean 5-attempt ledger
- `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.iter2.md:197,221,222,223,342–348` — M2 Reddit upgrade
- `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.iter2.md:189–193` — M3 invariant resolutions
- `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.iter2.md:57–60,148,247–253` — M4 + Critic L + Synthesis-B
- `specs/notification-l0.yaml:131` — NOTIF-PREF-001 disk-verified
- `specs/auth-asvs-l1.yaml:47–50` — ASVS-V2.2.1 disk-verified
- `templates/L4/file-storage/README.md`, `templates/L4/practices/README.md` — unused-L4 precedent (no `applied_recipes` key)
- `practices/evals/recipe_spec_referential_integrity_guard.sh:135–141` — INFORMATIONAL concern source
