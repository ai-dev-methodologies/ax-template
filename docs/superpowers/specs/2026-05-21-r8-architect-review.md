# Architect Review — R8 iter 1

## Verdict: APPROVE WITH MINOR FIXES (1 HIGH + 2 MEDIUM)

R8 PRD is materially stronger than R6 iter 1 (which had 2 HIGH + 4 MEDIUM) and on par with R7 iter 3. All cited spec_refs verify on disk. Evidence ledger meets R6/R7 §4.4 format. R6 SP39 atomic precedent applies cleanly. The one HIGH is a Korean-attempt regression vs R7, and two MEDIUM are tractable framing fixes.

## Steelman antithesis

The strongest case against R8 atomic-2 is **R7 just shipped Synthesis-B (split SPs) over R6 atomic, and R8 reverts to R6 atomic — is the codebase whipsawing between cadences?** R7's pre-mortem 5 documented a real risk (sealed verdict failure forces partial state) and the SP41/SP41b split mitigated it cleanly. R8 ships 2 sealed verdicts in one cycle for the first time (R6 shipped 3 atomic but same R5 verdict shape; R7 shipped 2 staged across SP41+SP41b). If lms-verdict fails, cms ALSO loses its commit even when its evidence chain is stronger. Partial-tag fallback does NOT eliminate the wasted commit + revert cycle.

**Counter-counter:** R7 Synthesis-B was driven by net-new verdict harness shape. R8's verdict shape IS the R5/R6 recipe-shape. Rollback risk is asymmetric: lms has weaker evidence (2 external + 1 documented 404) than cms (3 external). PRD choice survives.

## Tradeoff tensions

**Tension 1 (primary):** Trigger-discipline urgency vs verdict-failure asymmetry. lms's evidence chain is materially weaker than cms's. Atomic bundling means cms suffers commit-revert overhead for a problem cms doesn't have. No third path fully resolves this without abandoning Principle 5 (atomic Spec Trio) or trigger-discipline.

**Tension 2 (secondary):** Composition-kit framing vs "scheduled-publish" CMS specificity. Sanity verbatim `"Real-time database for structured content"` describes storage, not scheduled publishing. CMS-INV-002 is "Scheduled-publish uses scheduled-task lock + idempotency primitive." Trigger letter-clears but verbatim does not specifically attest scheduled-publish capability.

## Synthesis

No viable third path. Option (3) split is the only structural alternative; PRD's rejection rationale sound (R6 SP39 precedent vs R7 L4-introduction novelty). Tension 1 resolves in favor of atomic-with-partial-tag-fallback because split cost is one extra PR cycle for a LOW-probability lms-verdict failure.

## Principle check (deliberate)

DELIBERATE auto-triggered per: 2 sealed verdicts + scheduler README key-add new shape + 3-4 d wall-time + Korean zero-verbatim cycle.

**Principle violations: NONE flagged HIGH.**

- Principle 1 (composition kit): HONORED — zero new L4/L3/L2/L1/skill.
- Principle 2 (spec-before-code, evidence-anchored AT signature): HONORED — all 10 spec_refs disk-verified: AUDIT-RECORD-001/002, SCHED-LOCK-001/IDEMPOTENT-001/EXECUTE-001, NOTIF-PREF-001/SEND-001, ASVS-V4.1.1 (line 139), CRUD-VAL-1.
- Principle 3 (binary verification): HONORED.
- Principle 4 (Tier-1/2 frozen, L1=49 L2=92 L3=20 L4=11): HONORED — disk L4 dir count = 11.
- Principle 5 (atomic Spec Trio): HONORED, see Tension 1.
- Principle 6 (recipe-no-code): HONORED.
- Principle 7 (scheduler L4 consumed correctly): HONORED — scheduler README lacks `applied_recipes:` key (disk-verified).
- Principle 8 (no new rule families): HONORED.

## Findings

### HIGH

**H1 — Korean attempt count regression from R7 baseline.** R7 attempted 5 distinct Korean hosts (toss.tech, d2.naver.com, tech.kakao.com, techblog.woowahan.com, engineering.linecorp.com/ko). R8 attempts only 2 (inflearn.com, developers.naver.com). Both zero-verbatim cycles, so fabrication risk is zero either way, but R8 PRD §4.4 last paragraph explicitly acknowledges the pool should expand for R9. R7's M1 fix established 5 attempts as the bar after R6's 1 attempt. R8 should retroactively meet R7's 5-attempt floor BEFORE SP43, not defer to R9.

**Recommended additional hosts:**
- lms-relevant: ko.coursera.org, classting.com (Korean K12 LMS), tech.kakao.com or kakao tech LMS-related posts — at least 2 more attempts
- cms-relevant: terms.naver.com (different host from blocked developers.naver.com), brunch.co.kr, or notefolio — at least 1-2 more attempts

**Fix:** Iter 2 §4.4 ledger expands to ≥5 Korean attempts total split across lms + cms (3+2 or 4+1).

### MEDIUM

**M1 — Sanity verbatim topic-mismatch with cms scheduled-publish trigger semantics.** cms `reintroduction_trigger:` says "Sanity/Contentful verbatim citation" — generic. But cms's defining novelty is scheduled-publish (CMS-INV-002), and the Sanity verbatim describes storage, not scheduled publishing. Same for Contentful CMA quote and Strapi. All three letter-clear the trigger but none topically attest to the scheduled-publish capability.

**Fix:** Iter 2 SP43 pre-flight adds one additional WebFetch targeting Sanity scheduled publishing docs (`https://www.sanity.io/docs/scheduled-publishing`) OR Contentful scheduled publishing (`https://www.contentful.com/help/scheduled-publishing/`) OR Strapi cron/job docs (`https://docs.strapi.io/dev-docs/configurations/cron`). One topic-relevant verbatim closes the gap. If all 4xx, document as `topic_relevant_internal_design` and ship.

**M2 — INV-005 co-shipped-rule precedent absent for both lms and cms.** R7 SP41b established `co-shipped-rule:` as a recipe-level invariant pattern when no `practices/rules/*.md` file is appropriate. R8 lms-INV-005 + cms-INV-005 bind to existing rules — fine — but the choice vs co-shipped-rule should be explicit.

**Fix:** Iter 2 adds 1-paragraph disambiguation to §4.1/§4.2: "community needed co-shipped-rule because no existing rule covered XSS HTML sanitization; lms/cms invariants fully cover via existing rules + spec items." Currently absent; reads like oversight, not deliberate decision.

## Synthesis-A trim assessment

Synthesis-A would mean SP43 (lms-only) → SP43b (cms-only) → SP44 FINAL.

**Arguments FOR:** Tension 1 above; Pre-Mortem 1 (edX 404) affects only lms; partial-tag is reactive while split is proactive.

**Arguments AGAINST (PRD's position, defensible):** Mutation surfaces overlap intentionally (BOTH consume scheduler L4 first-consumer key-add). R7 Synthesis-B was driven by disjoint surfaces + harness novelty. R6 SP39 atomic-3 shipped 3 recipes with overlapping L4 README append surface; R8 atomic-2 is strict subset. Verdict shape is non-novel (R6 recipe-shape proven 3x in R6 + 1x in R7). 1 extra PR cycle is overhead for LOW-probability lms-verdict failure.

**Assessment:** Synthesis-A NOT structurally necessary for R8 (unlike R7). PRD atomic-2 acceptable.

## Recommendations

1. **Iter 2 MUST:** Add ≥3 more Korean attempts to §4.4 ledger (H1 fix) — meet R7's 5-attempt floor. Document each with HTTP status + 2026-05-21 timestamp.
2. **Iter 2 MUST:** Attempt ≥1 topic-relevant scheduled-publish WebFetch (M1 fix). One topic-relevant attempt closes the gap regardless of HTTP status outcome.
3. **Iter 2 SHOULD:** Add 1-paragraph disambiguation to §4.1 + §4.2 contrasting R7 community-html-sanitization rationale with R8 binding via existing rules (M2 fix).
4. **Iter 2 may IGNORE:** Synthesis-A pressure. PRD atomic-2 + partial-tag fallback is correct shape.

## References

- R8 PRD draft §4.4 evidence ledger (5 verbatim + 3 downgrades verified)
- R7 PRD `:229-233` — 5-Korean-host precedent
- `specs/recipes/community-recipe-l0.yaml:56-64` — R7 COMMUNITY-INV-005 co-shipped-rule precedent
- `specs/scheduled-task-l0.yaml:21,49,64` — SCHED-LOCK-001, SCHED-EXECUTE-001, SCHED-IDEMPOTENT-001 disk-verified
- `specs/audit-log-l0.yaml:7,23,88` — AUDIT-RECORD-001/002, AUDIT-RETENTION-001 disk-verified
- `specs/notification-l0.yaml:47,131` — NOTIF-SEND-001, NOTIF-PREF-001 disk-verified
- `specs/auth-asvs-l1.yaml:139` — ASVS-V4.1.1 disk-verified
- `specs/crud-security.yaml:22` — CRUD-VAL-1 disk-verified
- `templates/L4/scheduled-task/README.md:96-100` — scheduler README explicit R7→R8 transition language + key-absence confirmed
- R6 SP39 commit ab44cce: 46 files / +4249 lines — R8 PRD claim "similar to R6 SP39" approximately correct (within 12% file count, 9% line count).
