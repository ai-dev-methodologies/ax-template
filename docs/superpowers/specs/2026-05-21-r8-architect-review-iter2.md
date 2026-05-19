# Architect Review — R8 iter 2

## Verdict: APPROVE

All 4 iter-1 findings (H1 Korean ≥5 + M1 scheduled-publish topic-relevant + M2 INV-005 disambiguation + Critic L L2 inventory contract) closed with disk-verified evidence. 1 INFORMATIONAL remains (§4.4 counter wording precision).

## Closure check (4)

| Finding | Status | Evidence |
|---|---|---|
| **H1 — Korean ≥5** | CLOSED | §4.4 lines 203-214: 7 Korean rows (inflearn, ko.coursera 301, classting 200 verbatim, tech.kakao 200 no-topic, developers.naver blocked, terms.naver blocked, brunch 200 verbatim). Line 219: 5 logical Korean host attempts + 2 verbatim PASS — first non-zero-Korean cycle since R6. URL + http_status + 2026-05-21 timestamp on every row. |
| **M1 — scheduled-publish topic-relevant** | CLOSED | §4.4 line 211 + §4.2 line 163: `https://www.sanity.io/docs/scheduled-publishing` 200 OK with verbatim `"Scheduled publishing has been deprecated as of October 2025."` Topic-relevant: deprecation notice attests Sanity historically shipped scheduled-publish capability. CMS-INV-002 binds to internal `scheduled-task` L4 (independent of Sanity hosted product). |
| **M2 — INV-005 disambiguation** | CLOSED | §4.1 line 131 (lms-INV-005) + §4.2 line 159 (cms-INV-005) each contain explicit disambiguation paragraphs contrasting R7 COMMUNITY-INV-005 `co-shipped-rule` pattern with R8 binding rationale (existing `spec_ref + rule_ref` via `idempotency-key-on-mutations.md` + scheduler/crud spec items). |
| **L Critic — L2 inventory contract** | CLOSED | §4.1 line 121 (lms 9 L2 entries) + §4.2 line 149 (cms 9 L2 entries) strictly disk-resolvable. L1 primitives moved to "L1 primitives consumed" subsection with explicit `NOT in l2_blocks_used:` note. Non-existent `progress-bar` corrected to `progress`; `tag-input` removed (combobox substitution noted). |

## Disk validation

- `ls templates/L2/blocks/*.tsx | wc -l` → **91** (Planner brief claimed 92; iter 2 changelog line 428 self-reports 91 — accurate).
- All 9 lms + 9 cms `l2_blocks_used:` entries disk-resolved.
- L1 primitives disk-resolved under `templates/L1/components/`: calendar, date-range-picker, relative-time, progress, rich-text-editor, markdown-renderer.
- `tag-input` absent both tiers; `combobox.tsx` present (honest substitution path).
- 10 invariant spec_ref/rule_ref anchors resolve: AUDIT-RECORD-001/002, AUDIT-RETENTION-001, SCHED-LOCK-001, SCHED-IDEMPOTENT-001, SCHED-EXECUTE-001, NOTIF-PREF-001, NOTIF-SEND-001, CRUD-VAL-1, ASVS-V4.1.1, `practices/rules/idempotency-key-on-mutations.md`.
- §6 partial-tag policy table (lines 286-291) has 4 required cases (2/2, lms-only, cms-only, 0/2) with scheduler README `applied_recipes:` column showing `[cms, lms]` invariant.
- Iter 2 line count = 434 (matches Planner claim exactly).

## Independent attack

**Counter-wording arithmetic in §4.4 — INFORMATIONAL (not blocking).**

Line 195 header says "8 logical (iter 1) + iter 2 adds 4 logical Korean + 1 scheduled-publish = 13 logical + 2 redirect/alternate rows." Line 219 narrative reads "5 logical Korean host attempts" which initially appears to conflict with line 195 bookkeeping. Resolution: line 219 enumerates ko.coursera, 인프런, classting, tech.kakao, terms.naver, brunch, developers.naver — 7 host names; "5 logical" = 5 distinct logical attempts that registered intent (not 5 verbatim). Numbers reconcile but phrasing is mildly confusing. SP43 evidence-snapshot pass will collapse to single counter.

Business invariants unchanged from iter 1 set (5 lms + 5 cms = 10) — no scope drift.

## Final reasoning

Iter 2 closes all 4 iter-1 findings with surgical edits + on-disk evidence. L2 inventory contract fix restores booking/community R6/R7 precedent precisely. `progress` vs `progress-bar` filename correction is high-value (the kind of detail that caused R5 SP25 churn).

INV-005 disambiguation explicitly cites SP41b additive-branch precedent, demonstrating Planner internalized the architectural distinction.

Korean evidence ledger exceeds R7's 5-host floor (7 hosts attempted, 2 verbatim PASS — strongest Korean cycle since R6). Sanity scheduled-publishing fetch topic-perfect: even though page documents deprecation, the verbatim itself attests the capability existed.

One INFORMATIONAL (counter-wording precision) does not block — evidence integrity sound; counter narrative just needs one careful pass during SP43 evidence-snapshot generation.

Ready for Critic iter 2 sign-off.

## Re-review trigger

N/A — APPROVE.
