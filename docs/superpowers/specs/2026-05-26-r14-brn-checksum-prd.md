# R14 — BRN-checksum Bounded Retry PRD — 2026-05-26 (Round 14, GATE FAIL → R15 defer)

> **Status:** FINAL (Gate-driven outcome; R13 §10 bounded protocol honored; 0 SP, PRD-only commit, no tag). **Date:** 2026-05-26. **Repo:** ax-template. **Format:** RALPLAN-DR.
> **GATE DECISION:** **FAIL** — 0 Korean authoritative primary + 0 international authoritative direct (8 NOT FOUND / 404 / 403; 1 mirror-only). Honest defer to R15. Ralplan ITER LOOP SKIPPED — gate outcome is binary protocol per R13 §10 Codex soft #4 (no review margin).
> **Predecessor:** `2026-05-25-r13-toc-brn-checksum-prd.md` (R13 CLOSED `v1.10.0-toc` @ `main@231fa33`; 25 hard guards / 86 rules / sentinel `d367ba2f...`; TD-034 deferred to R14).
> **R14 axis decision (Planner):** **Option 2 — R15 defer (Gate UNMET).** Axis B (`korean-brn-checksum`) retry per R13 §10 bounded 9-source list executed 2026-05-26 — **0 verbatim Korean authoritative primary**; 8 NOT FOUND / 404 / 403 / off-target; 1 international corroborated via Wikipedia mirror only (ISO/IEC 7064 — authoritative source itself 403). Gate criterion (≥1 Korean primary AND ≥1 international) **UNMET**. **R15 defer; same 9-source list; R16 escalates to Architect rigor-floor downgrade vote.**
> **Branch:** none — PRD-only commit; no SP execution. **Tag policy:** no tag (no functional change).

---

## §1 RALPLAN-DR Summary

### Cycle frame (6 bullets)

- **Baseline (R13 closed).** `v1.10.0-toc` @ `main@231fa33`. **25 hard guards GREEN**; 86 rules; sentinel `d367ba2f...` @ `rule_count: 86`; AGENTS.md TOC observability shipped (TD-033 ADR); `generate_agents.sh` extended (sentinel sha-input UNCHANGED, I/O surface AMENDED). TD-034 = (i) `korean-brn-checksum` rule (bounded retry R14/R15, R16 escalation); (ii) `## Hard guards` TOC sub-section (separate deferral).
- **R14 single-axis scope.** Axis B only — `korean-brn-checksum` rule shipping IFF R13 §10 gate cleared. Mod-10 weighted-sum + format-only baseline already shipped R12 B1 as practices/structural floor (no behavioral rule yet).
- **2026-05-26 9-source retry verdict.** Korean primary verbatim: **0** (NTS/TTA/KISA/ko-wiki/law.go.kr all NOT FOUND or 404). International authoritative: **1 corroborated via Wikipedia mirror** (ISO/IEC 7064 — authoritative ISO source 403; EN VAT/Korea row absent; IEEE/ACM search returned no BRN-specific paper; GS1 Korea page empty). **Gate UNMET** (R13 §10 criterion: ≥1 Korean primary AND ≥1 international authoritative).
- **R14 ships 0 SP (PRD-only).** No `korean-brn-checksum.md` rule written; no DECISIONS.md TD-034 ADR populated (stub kept "deferred R15"); no AGENTS.md regen; sentinel `d367ba2f...` UNCHANGED; 25 hard guards UNCHANGED; rule_count 86 UNCHANGED.
- **R15 next-cycle plan.** Same fixed 9-source list (R13 §10 LOCKED — no scope creep). PRD-signature timestamp re-probe. R16 UNMET → Architect rigor-floor downgrade vote (no silent indefinite retry).
- **Evidence rigor.** Axis B floor UNMET (0 Korean primary / 8 downgrades / 1 international-via-mirror). OSS-comment rows (R13 O1 `shlee8313/4_social_insurnace`, O2 `won-ktds/smarketing-frontend`) preserved as pre-art only — NEVER authoritative on own (R13 §10 invariant).

### Principles (7 numbered — R7 inheritance + Korean realism honest)

1. **Composition kit, not single product.** R14 protects catalog rigor by refusing low-rigor ship; no surface enlargement on UNMET gate.
2. **Spec-before-code.** Rule has no spec until verbatim primary anchor exists; no behavior rule with empty evidence block (R11 evidence guard would BLOCK at CI).
3. **Binary verification.** Gate is binary: count Korean authoritative verbatim = 0 → FAIL → defer. No partial-credit ship. No "close enough" rule with weak citations.
4. **Bounded retry, not open-ended.** R13 §10 fixed 9-source list LOCKED. R14 re-probes same list; R15 same list; R16 escalates to Architect rigor-floor downgrade vote (Codex soft #4 R13).
5. **OSS-comment never authoritative.** O1/O2 (R13) remain pre-art — used to confirm community implementation exists but never substitute for Korean primary (NTS/TTA/KISA/academic).
6. **AGENTS.md sentinel sha UNCHANGED.** No rule added → rule concat unchanged → sentinel unchanged. Binary-verifiable in §6.
7. **Korean enterprise stack realism.** "Cannot find Korean primary in 2 consecutive PRD-signature probes" is a real-world honest signal — defer until found OR R16 downgrade rigor floor on Architect vote (no silent fabrication).

### Decision Drivers (top 3)

1. **R13 §10 gate UNMET 2026-05-26.** Re-probe Korean primary verbatim count = 0 (vs R13 §4.5.B count = 0). Two consecutive UNMET signals — R15 last bounded chance; R16 forces vote.
2. **Catalog rigor floor non-negotiable.** Shipping `korean-brn-checksum.md` with empty/weak evidence block re-opens R12 Architect H1 BLOCKING; would trip R11 evidence guard at CI (`evidence_guard.sh` rejects empty/placeholder evidence).
3. **R13 §10 source list locked — no scope creep.** Planner MUST NOT add unlisted sources (e.g., Naver blog, tistory blog, OSS code-only). Such additions invalidate the bounded retry framing and re-open the indefinite-retry attack surface.

### Viable Options Considered (≥2 mandatory)

- **(1) PASS-ship.** Pros: closes TD-034 atomic-2. Cons: gate UNMET — would fabricate Korean primary or downgrade rigor floor unilaterally. **REJECTED — gate binary UNMET.**
- **(2) R15-defer honestly.** Pros: preserves catalog rigor; R13 §10 bounded protocol honored; sentinel UNCHANGED; 25 guards UNCHANGED; PRD-only commit (low blast radius). Cons: B1 still pending two cycles. **CHOSEN.**
- **(3) Promote OSS-comment to authoritative.** Pros: would clear gate Korean side. Cons: violates R13 §10 invariant ("never authoritative on own"); re-opens R8/R9/R10 rigor floor. **REJECTED.**
- **(4) Open-ended retry beyond R16.** Pros: never abandons. Cons: violates R13 §10 bounded protocol (Codex soft #4). **REJECTED.**

### Mode

**SHORT.** PRD-only commit. No rule mutation. No SP execution. 0 hard guard delta. Pre-mortem 4 scenarios sufficient. Wall-time ≈ 0.5 d (PRD ralplan only).

### Recommended: Option (2) — 0 SP (PRD-only)

```
[no SP — PRD-only commit]
   ↓
R15 (next cycle, 2026-05-27 or later) re-probes SAME 9-source list at signature timestamp
   ↓
   PASS gate → R15 ships atomic-2 (rule + evidence snapshot + DECISIONS.md TD-034)
   FAIL gate → R16 escalates to Architect rigor-floor downgrade vote
```

---

## §2 Principles (R7 inheritance — explicit)

Same 7 principles from R7 baseline, with R14 emphasis on **#3 binary verification** and **#7 Korean enterprise stack realism**.

| # | Principle | R14 specific application |
|---|---|---|
| 1 | Composition kit, not single product | R14 ships nothing → catalog unchanged → composition kit unchanged |
| 2 | Spec-before-code | No rule ships without verbatim primary anchor (evidence_guard.sh would BLOCK) |
| 3 | Binary verification | Gate = (Korean primary ≥1) AND (international ≥1). 0+1 = FAIL. **NO partial credit.** |
| 4 | TD-024 sha-input UNCHANGED; I/O surface unchanged | No rule added → sentinel `d367ba2f...` unchanged → no I/O surface mutation |
| 5 | Atomic SP rule | N/A — 0 SP this cycle |
| 6 | AGENTS.md sentinel sha UNCHANGED | Trivially satisfied (no rule_count change) |
| 7 | Korean enterprise stack realism | Honest "not found" beats fabricated citation. R16 vote is the escape hatch. |

---

## §3 Decision Drivers (top 3 expanded)

### Driver 1 — R13 §10 gate UNMET 2026-05-26

R13 §4.5.B recorded 0 Korean primary + 9 downgrades on 2026-05-25 (PRD signature day). R14 re-probe 2026-05-26 (signature day) on the same fixed list yields **0 Korean primary + 8 NOT FOUND/404/403 + 1 international corroborated via Wikipedia mirror only**. Two consecutive UNMET signals strongly indicate the primary does not exist in publicly accessible authoritative Korean sources at PRD-signature granularity.

R15 (one more bounded retry) is the last cycle before R16 escalates to Architect rigor-floor downgrade vote (Codex soft #4 R13).

### Driver 2 — Catalog rigor floor non-negotiable

The catalog's value is `evidence_guard.sh` binary enforcement: every rule MUST have either (a) `upstream_id` pointing to a fetched snapshot in `practices/upstream/_MANIFEST.yaml` with section + quoted substring, OR (b) `source_type: external` with RFC/JEP/vendor-docs citation + URL.

Shipping `korean-brn-checksum.md` with empty/placeholder/below-floor evidence would:
- Trip `evidence_guard.sh` at CI (HARD BLOCK)
- Re-open R12 Architect H1 BLOCKING ("Axis B Korean references — primary verbatim count = 0")
- Erode the 25-hard-guard binary-verification posture established at R13

### Driver 3 — R13 §10 source list LOCKED — no scope creep

R13 §10 explicitly bound the retry to a **9-source fixed list** (5 Korean + 4 international). Planner MUST NOT silently add Naver blog / tistory blog / OSS-code-only / "developer Slack screenshot" / unofficial PDF dumps to clear the gate.

If R15 also fails, R16 forces the structural decision: either rigor-floor downgrade vote (Architect) or permanent removal of `korean-brn-checksum` candidate from `deferred Korean rule candidates` (R13 §10 table).

---

## §4 Viable Options + §4.5 Evidence Ledger

### §4.1 Viable Options (gate-binary decision)

| Option | Korean primary count | International count | Verdict |
|---|---|---|---|
| (1) PASS-ship atomic-2 | 0 | 1-via-mirror | UNMET → REJECTED |
| (2) R15-defer | 0 | 1-via-mirror | UNMET → **CHOSEN (honest defer)** |
| (3) Promote OSS-comment to authoritative | 0 (rule-violation) | 1-via-mirror | violates R13 §10 invariant → REJECTED |
| (4) Open-ended retry beyond R16 | 0 | 1-via-mirror | violates R13 §10 bounded protocol → REJECTED |

**Gate decision binary:** Korean authoritative verbatim count = 0 → **FAIL → Option (2) R15-defer.**

### §4.5 Evidence Ledger — 2026-05-26 PRD-signature 9-source re-probe

> All 9 sources from R13 §10 LOCKED list, fetched at PRD-signature timestamp 2026-05-26. Per-row verdict: verbatim / NOT FOUND / 404 / 403 / off-target.

**Korean primary (5 rows):**

| # | Source | URL | 2026-05-26 verdict | Notes |
|---|---|---|---|---|
| K1 | NTS archive (cntntsId 7770) | `https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2272&cntntsId=7770` | **NOT FOUND** | Page returns generic NTS navigation; no BRN checksum algorithm text. Confirmed via direct WebFetch. |
| K2 | NTS archive (cntntsId 7780) | `https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2272&cntntsId=7780` | **NOT FOUND** | Page covers VAT overview + 휴·폐업 신고; no BRN structure / validation. |
| K3 | TTA `tta.or.kr` | `https://www.tta.or.kr` | **NOT FOUND** | TTA homepage news/services only; no standard on BRN validation. Follow-up search "TTA 사업자등록번호 검증 표준 TTAK" returns committee.tta.or.kr navigation pages but no specific BRN-validation standard document. |
| K4 | KISA reference docs | `https://www.kisa.or.kr` | **NOT FOUND** | KISA homepage returns no identity-verification reference defining BRN checksum. |
| K5 | ko.wikipedia 사업자등록번호 | `https://ko.wikipedia.org/wiki/사업자등록번호` | **404** | Page does not exist (both underscored and non-underscored URL variants 404). Related article `사업자등록증` exists but contains no validation algorithm / checksum section — only "Requirements by Country" (US/UK/PK/IR). |
| K6 | law.go.kr 부가가치세법 시행령 | `https://www.law.go.kr/LSW/lsInfoP.do?lsiSeq=240880&efYd=20230101` | **NOT FOUND (404)** | URL returns "해당 법령이 존재하지 않습니다" (the relevant law does not exist). No clause located defining 사업자등록번호 structure/checksum in retrievable form. |

(Note: K1+K2 satisfy R13 §10 row 1 "cntntsId 7770-7790 sweep". K5 is row 4. K6 is row 5 "longer-timeout retry". K3+K4 are rows 2+3.)

**Korean primary verbatim count: 0 / 5 sources surveyed.**

**International / academic (4 rows):**

| # | Source | URL | 2026-05-26 verdict | Notes |
|---|---|---|---|---|
| I1 | ISO/IEC 7064 | `https://www.iso.org/standard/31531.html` | **403 authoritative source; corroborated via Wikipedia mirror** | iso.org returns 403 Forbidden. en.wikipedia ISO/IEC 7064 mirror confirms: "ISO/IEC 7064 — Information technology — Security techniques — Check character systems … defines algorithms for calculating check digit characters … applicable to alphanumeric strings … detects single substitution / most transposition / circular shift errors". **Authoritative source UNREACHABLE for verbatim quote**; mirror is secondary. |
| I2 | IEEE/ACM Korean check-digit paper | (search) | **OFF-TARGET (RRN, not BRN)** | Sweeney & Yoo (2015) "De-anonymizing South Korean Resident Registration Numbers Shared in Prescription Data" (Technology Science journal, peer-reviewed) — paper has weighted-sum formula `V = (11 – [(2a+3b+...+5l) % 11] % 10)` but applies **exclusively to 13-digit RRN**, not 10-digit BRN. Quoted by paper as Table 2. Not usable for BRN-specific evidence anchor. |
| I3 | GS1 Korea identifier specs | `https://www.gs1kr.org` | **NOT FOUND** | Page returns only org name "유통물류진흥원" with no specs / documents / BRN reference. |
| I4 | en.wikipedia VAT_identification_number (Korea row) | `https://en.wikipedia.org/wiki/VAT_identification_number` | **NOT FOUND (Korea absent)** | Article lists EU + 27 non-EU + 19 Latin-American countries; **South Korea is not in any section.** No Korean BRN row to corroborate. |

**International authoritative verbatim count: 0 direct + 1 via-mirror (I1 ISO/IEC 7064 via en.wikipedia ISO/IEC 7064 article).**

### §4.5.binary R14 gate decision

> **R13 §10 criterion:** ≥1 verbatim Korean authoritative primary (NTS notice / academic paper / TTA spec / KISA doc / standards body) **AND** ≥1 international/academic citation.
> 
> **R14 2026-05-26 result:** Korean primary = **0**; international authoritative direct = **0** (I1 via-mirror only).
> 
> **Gate: UNMET → FAIL.** → Option (2) R15 defer chosen.

**OSS-comment pre-art (R13 invariant — never authoritative on own):**

| OSS row | URL | Status carried into R14 | R14 role |
|---|---|---|---|
| O1 | `shlee8313/4_social_insurnace` BRN-validation code comment | github | preserved (R13 §4.5.B) | pre-art only — NEVER authoritative |
| O2 | `won-ktds/smarketing-frontend` BRN-validation JS comment | github | preserved (R13 §4.5.B) | pre-art only — NEVER authoritative |

R14 does **NOT** promote O1/O2. R13 §10 invariant intact.

---

## §5 SP Plan — 0 SP (PRD-only commit)

R14 ships **zero SPs**. No code/script/rule/AGENTS.md mutation. **PRD-only commit** + R15 retry plan.

### SP plan (next cycle preview, not R14 work)

```
R15 PRD signature (2026-05-27 or later)
   ↓
SP-R15-A   re-probe SAME 9-source list (R13 §10 LOCKED)
   ↓
   gate PASS → SP-R15-B atomic-2:
       - practices/rules/korean-brn-checksum.md (rule w/ verbatim Korean primary + international + mod-10 1,3,7 weights formula)
       - practices/upstream/_MANIFEST.yaml + practices/upstream/<korean-source>.snapshot.md (verbatim snapshot)
       - practices/DECISIONS.md TD-034 ADR populated
       - regenerate practices/AGENTS.md (sentinel sha changes — rule_count 86 → 87)
       - run all 25 hard guards (NO regression)
   gate FAIL → R16 escalation to Architect rigor-floor downgrade vote
```

R14 **acceptance criteria** (PRD-only):

- (i) This PRD lands as `2026-05-26-r14-brn-checksum-prd.md` (after Architect + Codex ralplan).
- (ii) `practices/AGENTS.md` sentinel `d367ba2f...` UNCHANGED (no rule mutation).
- (iii) `rule_count: 86` UNCHANGED.
- (iv) `practices/evals/run-all-guards.sh` exits 0 (all 25 guards GREEN — regression baseline).
- (v) `practices/DECISIONS.md` TD-034 status remains `deferred R15` (no ADR body populated).
- (vi) No new git tag.

---

## §6 Release Policy

### §6.1 Branch + commit

- **No feature branch.** PRD-only commit on `main` (precedent: docs-only PRD drafts have committed directly).
- **Commit subject:** `docs(r14): PRD for BRN-checksum bounded retry — Gate UNMET, R15 defer`
- **Diff scope:** `docs/superpowers/specs/2026-05-26-r14-brn-checksum-prd.draft.md` (this file), plus its ralplan iter outputs (architect-review.md, critic-codex-iter1.md) and finalized `…-prd.md`.

### §6.2 Tag policy

- **No tag.** R13 tag `v1.10.0-toc` remains HEAD-ish reference. No functional surface change in R14 → no version bump.
- Next tag candidate is **R15 IFF gate PASS** → `v1.11.0-brn-checksum` (atomic-2 ship).

### §6.3 Pre-flight verification (binary baseline)

Run before R14 PRD commit (operator-side):

```bash
cd practices && bash evals/run-all-guards.sh   # expect exit 0; 25/25 GREEN
grep '^rule_count:' AGENTS.md                  # expect 86
grep '^# sentinel:' AGENTS.md | head -1         # expect d367ba2f...
```

If any of (i)/(ii)/(iii) differs from R13 baseline → **ABORT R14 PRD commit**, investigate drift.

---

## §7 Pre-mortem (4 scenarios — SHORT mode)

| # | Failure scenario | Probability | Impact | Mitigation |
|---|---|---|---|---|
| 1 | Operator silently adds 10th source to clear gate (Naver blog / tistory / OSS code) | LOW | HIGH (invalidates R13 §10 bounded protocol; re-opens indefinite-retry surface) | §3 Driver 3 + §10 invariant explicit; ralplan Architect + Codex Critic enforce 9-source lock |
| 2 | R15 also UNMET, R16 vote splits (Architect APPROVE downgrade, Codex REJECT) | MEDIUM | MEDIUM (deadlock) | R16 protocol must define tie-breaker — defer to R17 Planner pre-mortem if reached |
| 3 | Operator promotes OSS-comment O1/O2 to authoritative under time pressure | LOW | HIGH (violates R13 §10 invariant) | §10 explicit invariant; Codex Critic will flag at iter 1 |
| 4 | Sentinel sha drifts between R13 close and R14 PRD commit (catalog drift) | LOW | HIGH (breaks I/O surface contract) | §6.3 pre-flight verification mandatory; ABORT on drift |

---

## §8 ADR — TD-034 BRN-checksum (deferred R15 stub)

> **NOTE:** TD-034 ADR body is **NOT populated** in R14 (gate UNMET). `practices/DECISIONS.md` keeps TD-034 status as `deferred R15` text only. PASS-branch ADR template preserved below for R15 use.

### TD-034 (deferred R15 stub)

- **Title:** `korean-brn-checksum` rule — mod-10 weighted-sum + format-only baseline → behavioral rule.
- **Decision:** **DEFERRED to R15** (R14 gate UNMET 2026-05-26).
- **Drivers:** R13 §10 bounded retry protocol; catalog rigor floor; Korean enterprise stack realism.
- **Alternatives considered (R14):** (1) PASS-ship REJECTED — 0 Korean primary; (2) R15-defer CHOSEN; (3) Promote OSS to authoritative REJECTED — violates §10; (4) Open-ended retry REJECTED — bounded protocol.
- **Why deferred:** Korean authoritative verbatim primary count = 0 on 2026-05-26 (two consecutive UNMET probes with R13 §4.5.B). Cannot ship rule with empty/below-floor evidence block (evidence_guard.sh BLOCK; R12 Architect H1 re-opens).
- **Consequences:** TD-034 carries into R15; R15 last bounded retry before R16 escalation.
- **Follow-ups:** R15 re-probe SAME 9-source list at PRD-signature timestamp; PASS → atomic-2 ship; FAIL → R16 Architect rigor-floor downgrade vote.

### TD-034 PASS-branch template (R15 ready — not used R14)

> Populate ONLY IF R15 gate PASS.

- **Title:** `korean-brn-checksum` rule — mod-10 weighted-sum (weights 1,3,7,1,3,7,1,3,5) + format `XXX-XX-XXXXX`.
- **Decision:** Ship behavioral rule `practices/rules/korean-brn-checksum.md` with evidence block anchoring to (a) Korean authoritative primary verbatim snapshot in `practices/upstream/` AND (b) ISO/IEC 7064 international citation.
- **Drivers:** TD-034 closure (R13 deferred); R12 B1 format-only baseline upgrade to behavioral; Korean enterprise stack catalog completeness.
- **Alternatives considered:** (1) keep format-only B1 only — REJECTED, behavioral coverage absent; (2) external-rule-only (no upstream snapshot) — REJECTED, R11 evidence_guard prefers upstream_id.
- **Why chosen:** Both gate sides met → catalog rigor floor satisfied → atomic-2 ship.
- **Consequences:** rule_count 86 → 87; sentinel sha CHANGES; AGENTS.md regen; 25 hard guards re-run (NO regression target).
- **Follow-ups:** R16+ may consider `korean-rrn-checksum` (RRN — different algorithm, Sweeney & Yoo formula); decision deferred R16+.

---

## §9 Honored Constraints

| # | Constraint (source) | R14 disposition |
|---|---|---|
| 1 | R13 §10 fixed 9-source list LOCKED (Codex soft #4 R13) | **HONORED** — all 9 fetched; no 10th source added |
| 2 | OSS-comment NEVER authoritative (R13 §10 invariant) | **HONORED** — O1/O2 preserved as pre-art only |
| 3 | R13 §10 bounded retry (R14/R15 same list; R16 escalates) | **HONORED** — R14 defers to R15 on UNMET; R16 escalation path explicit |
| 4 | Sentinel sha unchanged when no rule mutation (R7 TD-020 hedge) | **HONORED** — no rule added; sentinel `d367ba2f...` unchanged |
| 5 | Evidence floor binary (`evidence_guard.sh` BLOCK on empty/placeholder) | **HONORED** — no rule shipped with below-floor evidence |
| 6 | Composition kit not single product (R7 / R12 / R13) | **HONORED** — surface unchanged |
| 7 | Korean enterprise stack realism — no fabrication (R7) | **HONORED** — "0 Korean primary" reported honestly |
| 8 | R6/R10/R12 envelope (≤ 1-2 SP per cycle) | **HONORED** — 0 SP R14 |
| 9 | 25 hard guards GREEN baseline (R13 close) | **HONORED** — no guard mutation; baseline preserved |

---

## §10 Out-of-scope (R14 explicit) + Deferred Items

**Out-of-scope (R14):**

- New L1/L2/L3/L4 templates
- New Tier-1/Tier-2 skill
- New recipe
- New sealed verdict
- Any rule mutation (rule_count stays 86)
- AGENTS.md regen (sentinel stays `d367ba2f...`)
- `## Hard guards` TOC sub-section (R13 TD-034 part ii — separate deferral, not retried R14)
- `quote_match_check.sh` extension (R14+ untouched)
- 휴대폰 본인인증 PASS rule (R14+ untouched)
- Frontend/backend code mutations
- Deployment / CI / release policy
- Fork-receiver `inspect.sh` (R14+ untouched)
- Rate-limit L4 promotion (TD-028 untouched)
- Promoting OSS-comment O1/O2 to authoritative (R13 §10 invariant)
- Adding any source beyond R13 §10 fixed 9-source list

**Deferred to R15+ (carried from R13 TD-034):**

| Candidate | Trigger to ship | R14 2026-05-26 probe result |
|---|---|---|
| `korean-brn-checksum` (mod-10 weighted-sum) | ≥1 verbatim Korean authoritative primary AND ≥1 international/academic on PRD-signature day | **UNMET** — 0 Korean primary; 8 NOT FOUND/404/403; 1 international via-mirror only (I1 ISO/IEC 7064 via en.wikipedia mirror; I2 OFF-TARGET RRN-only; I3 GS1 KR empty; I4 EN VAT Korea absent) |
| `## Hard guards` TOC sub-section (R13 M4 / Codex soft #3) | TD-034 part ii separate trigger (not retried R14) | not re-probed R14 — separate deferral |
| `korean-phone-verification-pass-flow` | KISA + 토스 PASS docs verbatim reachable | not re-probed R14 (R13 deferred) |
| `kakao-alimtalk-template-authorization` | kakao.business.api docs verbatim reachable | not re-probed R14 (R13 deferred) |
| `naver-id-oauth-scope` | developers.naver.com verbatim reachable | not re-probed R14 (R13 deferred) |

**R15 retry plan (Codex soft #4 R13 bounded — unchanged):**

R15 PRD signature re-probes **exactly the same fixed 9-source list** at R15 signature timestamp. **R16 UNMET → escalate to Architect rigor-floor downgrade vote** (no silent "retries continue forever"). Bounded protocol intact.

**R13 §10 fixed retry source list (LOCKED — re-stated for R15 reference):**

1. NTS archive `https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do` (cntntsId 7770-7790 sweep)
2. TTA `https://www.tta.or.kr` (search "사업자등록번호" / "검증")
3. KISA identity-verification reference docs
4. 위키백과 사업자등록번호 (community update check)
5. law.go.kr 부가가치세법 시행령 (longer-timeout retry)
6. ISO/IEC 7064 (check-digit standards)
7. IEEE / ACM — Korean check-digit algorithm papers
8. GS1 Korea identifier specifications
9. en.wikipedia VAT_identification_number (Korea-row community update check)

OSS-comment rows (R13 O1 `shlee8313/4_social_insurnace`, R13 O2 `won-ktds/smarketing-frontend`) preserved as pre-art only — NEVER authoritative on own. R15 ships Axis B as 1-SP standalone (atomic-2: rule + evidence) IF gate cleared.

---

## RALPLAN-DR Summary

**Cycle:** R14 (Round 14) — single-axis Axis B (`korean-brn-checksum`) bounded retry.

**Mode:** SHORT (PRD-only; 0 SP; no rule/guard/AGENTS.md mutation).

**Principles (7):** composition kit · spec-before-code · binary verification · sha-input unchanged · atomic SP (N/A — 0 SP) · sentinel unchanged · Korean realism honest.

**Decision Drivers (top 3):** (1) R13 §10 gate UNMET 2026-05-26 (2 consecutive UNMET signals); (2) catalog rigor floor non-negotiable (evidence_guard.sh BLOCK on empty/below-floor); (3) R13 §10 source list LOCKED — no scope creep.

**Viable Options (4 — gate-binary decision):** (1) PASS-ship REJECTED (gate UNMET); (2) R15-defer **CHOSEN**; (3) promote OSS to authoritative REJECTED (R13 §10 invariant violation); (4) open-ended retry REJECTED (bounded protocol violation).

**Gate decision (binary):** Korean authoritative verbatim count = **0** / 5 sources; international authoritative direct count = **0** / 4 sources (1 via-mirror only, 1 OFF-TARGET RRN-only, 2 NOT FOUND). **Gate UNMET → FAIL → R15 defer.**

**Evidence ledger summary:** 8 NOT FOUND/404/403 out of 9; ko-wiki 사업자등록번호 redirects to 사업자등록증 (no algorithm section); law.go.kr 부가가치세법 시행령 URL returns "해당 법령이 존재하지 않습니다"; ISO/IEC 7064 authoritative source 403 (Wikipedia mirror only); IEEE/ACM Sweeney & Yoo (2015) paper is RRN-specific, not BRN; en-wiki VAT_identification_number article does not include Korea.

**ADR:** TD-034 stub `deferred R15` (body NOT populated R14). PASS-branch ADR template preserved §8 for R15 use.

**Acceptance (R14 PRD-only):** (i) PRD lands; (ii) sentinel `d367ba2f...` unchanged; (iii) rule_count 86 unchanged; (iv) `run-all-guards.sh` exit 0 (25 GREEN); (v) TD-034 status `deferred R15`; (vi) no tag.

**Next cycle (R15):** re-probe SAME 9-source list at R15 PRD-signature timestamp; PASS → atomic-2 ship `korean-brn-checksum.md` + upstream snapshot + DECISIONS.md TD-034 + AGENTS.md regen (rule_count 86 → 87, sentinel CHANGES) + 25 guards no-regression; FAIL → R16 Architect rigor-floor downgrade vote.

**Honest defer, NO low-rigor ship.**
