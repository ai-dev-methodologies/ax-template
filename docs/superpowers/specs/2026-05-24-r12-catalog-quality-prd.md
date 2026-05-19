# R12 — Catalog Quality Strengthening PRD — 2026-05-24 (Round 12, ralplan iter 3 APPROVED)

> **Status:** APPROVED (3-iter ralplan consensus; Synthesis-A adopted; Architect iter 2 APPROVE + Codex Critic iter 3 FINAL APPROVE). **Date:** 2026-05-24. **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Predecessors:** `2026-05-23-r10-api-gateway-relay-prd.md` (CLOSED `v1.8.0-api-gateway-relay`); `2026-05-23-r11-cleanup-pr10-codex-review.md` (R11 chore — `main@b0b839c`, no tag); `2026-05-19-r6-recipes-prd.md` (atomic-precedent).
> **Iter 1 inputs:** `2026-05-24-r12-catalog-quality-prd.draft.md` (465 L); `2026-05-24-r12-architect-review.md` (3H + 4M, ITERATE, Synthesis-A recommended); `2026-05-24-r12-critic-codex-iter1.md` (ITERATE + 1 BLOCKING + 6 hard + 4 soft).
> **Branch:** `feat/r12-catalog-quality-sp49-sp50`. **Targeted tag:** `v1.9.0-catalog-quality` IFF SP50 PASS (§6); no tag IFF FAIL.
> **Iter 2 scope decision:** **ADOPT Synthesis-A.** R12 ships Axes A + B only (2 guards + 2 Korean rules). Axis C (AGENTS.md TOC) deferred to R13 standalone cycle with own ADR for TD-024 amendment.

---

## §1 RALPLAN-DR Summary

### Cycle frame (6 bullets)

- **Baseline (R10 closed; R11 chore-only).** `v1.8.0-api-gateway-relay` on `main@b0b839c`. 13 sealed verdicts PASS; 22 hard guards GREEN; 84 practices rules; AGENTS.md @ sha `15c54...`.
- **R12 strategy (Synthesis-A trim).** Catalog **quality** strengthening — NOT a recipe-add cycle. Mechanizes 2 currently-manual disciplines (alphabetical `applied_recipes:` + cross-recipe INV-id uniqueness) and adds 2 Korean enterprise rules (BRN **format-only** + VAT 10%). AGENTS.md TOC + generator extension dropped → R13.
- **No new L4 / L3 / L2 / L1 / Tier-1 / Tier-2 skill / recipe / sealed verdict.** AGENTS.md sentinel sha refreshes naturally from the 84 → 86 rule-count concat (no generator code change).
- **Atomic 2-SP cycle (atomic-4 after Axis C drop).** SP49 ships 2 guards + 2 rules + 4 fixtures + evidence snapshot + DECISIONS.md TD-030+TD-031 atomically. SP50 FINAL re-runs verdicts + `/ax-verify all` + tag. TD-027 gate UNCHANGED (zero new L4).
- **R6/R10 atomic shape preserved.** SP49 atomic-4 stays within the R6 SP39 / R8 SP43 / R10 SP47 multi-axis envelope.
- **Evidence rigor.** **4 verbatim source rows** (3 KO + 1 EN) supplying **5 quote occurrences** (위키백과 부가가치세 supplies 2). **8 downgrade rows.** **12 total table rows.** Per-rule Korean-verbatim floor (1) cleared for both rules (B1 = 1 KO; B2 = 3 KO + 1 EN).

### Principles (8 numbered)

1. **Composition kit, not single product.** R12 strengthens catalog quality probes — does not enlarge the catalog surface. Inherited from CLAUDE.md vision.
2. **Spec-before-code, evidence-anchored at PRD signature.** Every new rule has Korean verbatim citation collected during PRD revision and embedded in the rule's `evidence:` block. Every new guard has a failing fixture authored alongside the script (TDD anchor RED → GREEN).
3. **Binary verification per axis.** `/ax-verify` exits 0; both new guards exit 0 across 11 active recipes / 12 L4; `generate_agents.sh` second-run produces zero diff (idempotent — natural sha refresh only, no script mutation).
4. **Tier-1 cap = 4. Tier-2 count = 8. L1 = 49, L2 = 92, L3 = 20, L4 = 12. FROZEN.** Recipes 11 → 11 UNCHANGED. Sealed verdicts 13 → 13 UNCHANGED. Practices rules 84 → 86 (+2 Korean). Hard guards 22 → 24 (+2). AGENTS.md sha refreshes naturally (84 → 86 concat → new sha).
5. **Atomic Spec Trio rule per SP.** SP49 ships 2 guards + 2 rules + 4 fixtures + evidence snapshot + DECISIONS.md appends ATOMICALLY. R6/R10 precedent applied to quality-only scope. **One rollback state machine: all-4-or-rollback.**
6. **Catalog quality as self-strengthening loop.** R12 mechanizes two previously-manual disciplines (alphabetical applied_recipes insertion + cross-recipe INV-id uniqueness). The catalog should not depend on planner attention to maintain invariants that scripts can enforce. **TD-030 is protective (R13+ regression prevention), not corrective** — disk census at PRD signature confirms zero current violations.
7. **No new L2 / L3 / L4 / recipe / sealed verdict / Tier-1 / Tier-2 skill.** Explicit per §9 (Honored Constraints) + §10 (Out-of-scope).
8. **Korean enterprise stack realism.** B1 (BRN **format-only**) + B2 (VAT 10%) anchor on disk-verified verbatim from 위키백과 + NTS + 한국은행 + PwC. Cloud-native Korean fragility (R8/R9/R10 host-wide cascade pattern) is acknowledged: NTS-specific subsections, hometax.go.kr, law.go.kr exhibit 404 / timeout — documented honestly, never fabricated.

### Decision Drivers (top 3)

1. **Mechanize what discipline currently maintains by hand.** Across R6 / R7 / R8 / R9 / R10 the planner manually verified two invariants — (i) `applied_recipes:` list alphabetically sorted, (ii) no two recipes claim the same `(L4_domain, id)` INV pair. Both currently pass through human attention only. R12 promotes them to mechanized guards. Disk census at PRD signature confirms zero current violations on both — guard value is **protective** for future cycles, not corrective.
2. **Korean enterprise stack coverage strengthening.** R12 ships 2 Korean rules with disk-verified verbatim density (BRN format-only: anchored on 한국은행 adjacent enterprise context + format-convention documentation; VAT 10%: 위키백과 부가가치세 × 2 quotes + NTS 부가가치세 기장의무 + PwC Tax Summaries Korea). BRN **checksum** algorithm (mod-10 weighted-sum) is **deferred to R13+** as a separate rule with authoritative source — R12 evidence collection at 2026-05-24 found no authoritative Korean source for the BRN mod-10 checksum (위키백과 사업자등록번호 alt URL is 200 OK but lacks 10-digit/checksum content; namu.wiki 403; en.wikipedia 404; law.go.kr/hometax.go.kr/NTS 7660 host-wide downgraded).
3. **AGENTS.md sentinel sha refreshes naturally.** Adding 2 new Korean rules in R12 triggers a genuine sha refresh from `generate_agents.sh` (84 → 86 rule concat → new sha). **No generator code change** in R12 — TOC extension is R13 work with its own ADR for TD-024 amendment.

### Viable Options Considered (≥2 mandatory)

- **Option (1) — Synthesis-A: Axes A + B only (2 guards + 2 rules).** Pros: atomic-4 inside R6/R10 envelope; isolates failure modes; B1 format-only ships honest evidence; TD-024 invariant intact for R13. Cons: natural sha-refresh trigger doesn't also carry TOC payload (mild lost opportunity). **CHOSEN — adopted from Architect iter 1 recommendation.**

- **Option (2) — Iter 1 Axes A+B+C (2 guards + 2 rules + AGENTS.md TOC + generator extension).** Pros: TOC ships in same atomic sha-refresh surface. Cons: amends TD-024 invariant (`generate_agents.sh` reads only `rules/*.md`) without generator-shape spec/ADR; raises atomic surface to 5; couples 4 unrelated failure modes under one rollback gate. **REJECTED — closes Architect H2 + Codex Hard #2 + Soft #1.**

- **Option (3) — Axes A + B with B1 checksum (mod-10 weighted-sum).** Pros: B1 has stronger normative content. Cons: iter 1 evidence had **zero verbatim citation** for BRN mod-10 (위키백과 주민등록번호 = mod-11 RRN, different family + weights; 한국은행+PwC cite VAT, not checksum). **REJECTED — closes Architect H1 + Codex Hard #1.**

- **Option (4) — Defer R12 entirely.** Pros: lowest-risk no-op. Cons: mis-reads CLAUDE.md vision (catalog self-strengthens without fork-receiver demand); user explicitly invoked "R12 시작해 — catalog quality 강화". **REJECTED.**

### Mode

**SHORT.** No L4 / L3 / L2 / L1 / recipe surface mutation. No harness novelty. No generator-script mutation (Axis C dropped). Pre-mortem 4 scenarios + standard test plan sufficient. Wall-time ≈ 2-3 d.

### Recommended: Option (1) Synthesis-A — 2 SPs (SP49 atomic-4 + SP50 FINAL).

```
SP49   (atomic-4 — 2 hard guards [cross_recipe_inv_uniqueness +
        applied_recipes_alphabetical] + 2 Korean rules
        [korean-brn-format + korean-vat-10-percent-calculation]
        + 4 failing fixtures + evidence snapshot
        + DECISIONS.md TD-030 + TD-031 + AGENTS.md regen for natural sha refresh)
   ↓ gated on both new guards exit 0 + 86-rule AGENTS.md sentinel sha emitted
SP50   (FINAL — /ax-verify all exit 0 + 13 sealed verdicts re-run no regression
        + tag v1.9.0-catalog-quality IFF PASS + PR)
```

Total: **2 SPs, ≈ 2-3 d wall-time.**

---

## §2 Context

### R10/R11 disk-verified state (2026-05-24)

| Surface | Count | Path |
|---|---|---|
| L1 primitives | 49 | `templates/L1/components/` |
| L2 blocks | 92 | `templates/L2/blocks/` |
| L3 pages | 20 | `templates/L3/pages/` |
| L4 domains | **12** (audit-log, auth, billing, crud, feature-flags, file-storage, notification, payment, practices, scheduled-task, search, webhook) | `templates/L4/` |
| Active recipes | **11** (saas-subscription, e-commerce, crm, booking, marketplace, b2b-admin, community, lms, cms, internal-it, api-gateway-relay) | `recipes/` |
| Deferred recipes | **0** | `recipes/_MANIFEST.yaml#deferred_recipes` |
| Sealed verdicts | **13** (all PASS) | `skills/_tests/sealed-verdict/` |
| Hard guards GREEN | **22** | `practices/evals/*.sh` |
| Practices rules | **84** (4 existing Korean: currency-amount-precision-explicit, no-rrn-collection-without-legal-basis, no-rrn-logging, payment-iso-4217-currency) | `practices/rules/*.md` |
| AGENTS.md | 6150 lines @ sha `15c54ebbb876a78f3f17fb04d4cf9fba1573b827a7a70041d4e50785b9e14016`, `rule_count: 84` | `practices/AGENTS.md` (head -5) |
| Current tag | `v1.8.0-api-gateway-relay` on `main@b0b839c` | `git tag --sort=-creatordate \| head -1` |

### MEMORY.md baseline reconciliation (closes Architect H3 + Codex Hard #3)

**Quarantine note.** The user's auto-memory at `~/.claude/projects/.../memory/MEMORY.md` references `payment_blueprint_status.md` claiming "catalog 64→68 rules (80% generic-promoted)". **That reference is a separate downstream project surface, NOT the ax-template baseline.** R12 PRD signature baseline is disk-verified:

- `ls practices/rules/*.md | wc -l` → **84** (this PRD)
- `head -5 practices/AGENTS.md` → `rule_count: 84` (this PRD)

R12 deltas land **on the 84-rule baseline** and produce 86 rules. The `payment_blueprint_status` 64/68 figure is out-of-scope for R12 arithmetic — any future cross-reference between MEMORY.md and ax-template must explicitly state which surface is in scope.

### R12 scope (Axes A + B only — Synthesis-A)

- **Deliverable A1 (hard guard #1):** `practices/evals/cross_recipe_inv_uniqueness_guard.sh` — block two active recipes from declaring identical `(L4_domain_prefix, id)` INV pairs (recipe-prefixed IDs like `CRM-INV-001` are exempt — recipe-scoped by construction). Mechanizes a discipline that R6-R10 manually maintained. Disk census at PRD signature shows **zero current collisions** (all 11 recipe specs use recipe-prefixed IDs).
- **Deliverable A2 (hard guard #2):** `practices/evals/applied_recipes_alphabetical_guard.sh` — L4 README `applied_recipes:` plural list entries must be alphabetically sorted (ASCII case-insensitive). R5 legacy singular `applied_recipe:` form skipped; keyless L4 READMEs (file-storage, practices) skipped. Disk census at PRD signature confirms **all 9 plural-shape L4 READMEs are already alphabetical** (audit-log/auth/crud/feature-flags/notification/payment/scheduled-task/search/webhook); the remaining 3 L4 (billing singular + file-storage keyless + practices keyless) are SKIP per the guard contract. Soft #4 closure: see §6 pre-flight command.
- **Deliverable B1 (Korean rule #1, format-only):** `practices/rules/korean-brn-format.md` — Spring/Backend rule asserting BRN inputs validate against the 10-digit `NNN-NN-NNNNN` format. **Checksum algorithm deferred to R13+** as separate rule with authoritative source (closes Architect H1 + Codex Hard #1).
- **Deliverable B2 (Korean rule #2):** `practices/rules/korean-vat-10-percent-calculation.md` — Spring/Backend rule asserting VAT 10% computed via `BigDecimal("0.10")` + HALF_UP rounding.
- **Shared SP49 mutations:** `practices/AGENTS.md` regenerated for natural sha refresh (84 → 86 rule concat); `practices/upstream/r12-sp49-evidence-snapshot.md` (new); 4 new fixtures; `practices/DECISIONS.md` append TD-030 + TD-031.

NO new L1 / L2 / L3 / L4 / Tier-1 / Tier-2 skill / recipe / sealed verdict. NO generator script mutation. Hard guards 22 → 24; practices rules 84 → 86; AGENTS.md sha refreshes naturally; everything else FROZEN.

---

## §3 Objectives + Guardrails

### Must Have

- **2 new hard guards** under `practices/evals/`, each with `--fixtures` mode validating PASS + FAIL fixtures.
- **2 new Korean practices rules** under `practices/rules/`. Per-rule Korean verbatim floor: each rule MUST cite ≥1 verbatim Korean external. Global international citation: ≥1 international verbatim row in the shared §4.5 evidence ledger (PwC Tax Summaries serves as global ledger anchor; cited directly under B2 since VAT-rate aligns; B1 (format-only) acknowledges thin direct international anchor honestly per Pre-Mortem #4 — B1's normative claim is a format regex with adjacent-Korean-only verbatim, by design after Codex-iter-1 evidence-decoupling closure).
- **4 failing fixtures** (2 per guard) under `practices/evals/fixtures/` proving RED → GREEN.
- `practices/AGENTS.md` regenerated atomically in SP49 — new sentinel sha emitted by existing unchanged `generate_agents.sh` (84 → 86 concat).
- `practices/upstream/r12-sp49-evidence-snapshot.md` with 4 verbatim PASS rows + 8 downgrade rows + 2026-05-24 timestamp.
- 2 new TD entries in `practices/DECISIONS.md` (TD-030 + TD-031). **No TD-032** — Axis C deferred to R13.
- `/ax-verify all` exit 0 in SP50; both new guards exit 0 against live repo; `generate_agents.sh` second-run produces zero diff (idempotent).
- Tag `v1.9.0-catalog-quality` IFF SP50 PASS (binary policy per §6).

### Must NOT Have

- NO new L4 / L3 / L2 / L1 / Tier-1 / Tier-2 skill / recipe / sealed verdict.
- NO new recipe quartet authored. NO change to `recipes/_MANIFEST.yaml`.
- NO `generate_agents.sh` script mutation — TD-024 invariant preserved (script still reads only `practices/rules/*.md`). Axis C / TOC / TD-032 deferred to R13.
- NO change to existing 22 guard scripts (Soft M4 — `quote_match_check.sh` covers only `upstream_id` evidence and exits 0 unconditionally; this PRD does not claim it covers the 2 new `source_type: external` rules — see §6 pre-flight verification command).
- NO change to existing 84 rules beyond regeneration ordering.
- NO change to fork-receiver SKILL.md / scripts.
- NO Korean reference fabrication. 8 documented downgrades per R8/R9/R10 host-wide cascade precedent.
- NO BRN checksum algorithm normative claim (Architect H1 + Codex Hard #1) — deferred to R13+.
- NO TD-027 re-triggering (no new L4 this cycle).
- NO opening of `deferred_recipes:` queue.
- NO 휴대폰 본인인증 (PASS) rule this cycle — deferred to R13+.
- NO change to git workflow / CI policy / release process.
- NO partial deliverable within SP49 (atomic ships all 4 atomic items or rollback — one state machine; see §6).

---

## §4 Deliverable Inventory + Evidence Ledger

### 4.1 Hard guard A1 — `cross_recipe_inv_uniqueness_guard.sh`

- **Path:** `practices/evals/cross_recipe_inv_uniqueness_guard.sh`
- **Function:** Parse all `specs/recipes/*-recipe-l0.yaml` files; extract `business_invariants[*].id`; index by `(L4_domain_prefix, id)` where `L4_domain_prefix` is derived from each INV's `spec_ref` (e.g. `specs/audit-log-l0.yaml#AUDIT-RECORD-001` → prefix `audit-log`). Block any prefix-id pair appearing in ≥2 recipes. Recipe-prefixed IDs (e.g. `CRM-INV-001`, `API-GATEWAY-RELAY-INV-001`) are exempt.
- **Usage:** `bash practices/evals/cross_recipe_inv_uniqueness_guard.sh` (live repo) / `--fixtures` (fixture mode).
- **Exit codes:** 0 PASS · 1 violation · 2 usage error.
- **TDD anchor:**
  ```yaml
  tdd_anchor:
    test_file: "practices/evals/fixtures/cross_recipe_inv_uniqueness/fail_two_recipes_same_audit_inv/"
    assertion: "two recipe YAMLs both declaring AUDIT-INV-001 trigger guard exit 1; single-occurrence and recipe-prefixed (CRM-INV-001) IDs exit 0"
    expected_RED_reason: "guard script does not exist yet"
    first_GREEN_command: "bash practices/evals/cross_recipe_inv_uniqueness_guard.sh --fixtures"
    owning_SP: "SP49"
  ```

### 4.2 Hard guard A2 — `applied_recipes_alphabetical_guard.sh`

- **Path:** `practices/evals/applied_recipes_alphabetical_guard.sh`
- **Function:** For each `templates/L4/*/README.md` containing an `applied_recipes:` plural-list block, assert entries are alphabetically sorted (ASCII case-insensitive lexical). Single-entry lists pass vacuously. R5 legacy singular `applied_recipe:` form is skipped.
- **Usage:** `bash practices/evals/applied_recipes_alphabetical_guard.sh` / `--fixtures`.
- **Exit codes:** 0 PASS · 1 unsorted · 2 usage error.
- **TDD anchor:**
  ```yaml
  tdd_anchor:
    test_file: "practices/evals/fixtures/applied_recipes_alphabetical/fail_unsorted_two_entries/"
    assertion: "README.md with applied_recipes: [internal-it, api-gateway-relay] (unsorted) triggers exit 1; sorted shape [api-gateway-relay, internal-it] exits 0"
    expected_RED_reason: "guard script does not exist yet"
    first_GREEN_command: "bash practices/evals/applied_recipes_alphabetical_guard.sh --fixtures"
    owning_SP: "SP49"
  ```

### 4.3 Korean rule B1 — `korean-brn-format.md` (FORMAT-ONLY)

- **Path:** `practices/rules/korean-brn-format.md`
- **`rule_id:`** `korean-brn-format`
- **`impact:`** HIGH
- **`tags:`** [validation, identity, brn, korean-compliance, locked_constraint]
- **`provenance_class:`** locked_constraint
- **Constraint (REFRAMED iter 2):** Any backend endpoint accepting a 사업자등록번호 (Business Registration Number) MUST validate the input against the **10-digit `NNN-NN-NNNNN` format** (3-digit 청 코드 + 2-digit individual/corporate code + 5-digit sequence). Inputs failing the format check MUST be rejected with HTTP 400 + RFC 7807 problem detail; never persisted in unvalidated form; never logged in raw form. **Checksum algorithm (mod-10 weighted-sum) is intentionally OUT-OF-SCOPE for R12** — deferred to R13+ as a separate rule contingent on an authoritative source landing (closes Architect H1 + Codex Hard #1).
- **`verification:` type:** review (static analysis: assert any DTO field annotated `@BusinessRegistrationNumber` is wired to a Jakarta `ConstraintValidator` that performs the 10-digit format regex).
- **`evidence:` block (B1 ↔ B2 decoupled per Architect M3 — PwC VAT-rate citation removed from B1; checksum-related 위키백과 주민등록번호 mod-11 row removed):**
  1. **source_type: external** — 한국은행 공식 (adjacent Korean enterprise infrastructure) — verbatim "통화정책의 효율적 수행을 통해 물가 안정과 금융안정을 도모"
     - URL: `https://www.bok.or.kr/portal/main/main.do`
     - quoted_at: 2026-05-24
     - **Provenance note:** adjacent verbatim — anchors Korean enterprise authenticity per R8/R9/R10 adjacent-fallback precedent. BRN-specific 위키백과 사업자등록번호 + namu.wiki + en.wikipedia + law.go.kr / hometax.go.kr / NTS-7660 host-wide downgraded on 2026-05-24 (see §4.4 downgrade rows 1-7).
  2. **source_type: external** — internal_design downgrade cluster (4 BRN-specific Korean docs unreachable / lacking format content on 2026-05-24 — see §4.4 rows 6, 7, 8, 9).

### 4.4 Korean rule B2 — `korean-vat-10-percent-calculation.md`

- **Path:** `practices/rules/korean-vat-10-percent-calculation.md`
- **`rule_id:`** `korean-vat-10-percent-calculation`
- **`impact:`** HIGH
- **`tags:`** [billing, tax, vat, korean-compliance, currency]
- **`provenance_class:`** external
- **Constraint:** Any backend service computing a VAT-inclusive or VAT-exclusive amount MUST compute VAT as `vat_amount = round_half_up(supply_amount × BigDecimal("0.10"), scale=0)` using `java.math.BigDecimal` (cross-link to existing `lang-bigdecimal-for-money.md` + `currency-amount-precision-explicit.md`). Float / double arithmetic is forbidden. The VAT rate constant MUST be expressed as `BigDecimal("0.10")` — not `0.10d` or `0.10f`. Rounding mode MUST be `HALF_UP` per Korean invoice convention. Test fixtures cover (i) supply_amount = 1,000 → vat = 100; (ii) supply_amount = 1,001 → vat = 100; (iii) supply_amount = 1,005 → vat = 101 (HALF_UP on .5 boundary).
- **`verification:` type:** review (static analysis: grep for `0\.10[df]?` literals in `*/billing/**.java` / `*/payment/**.java` outside `BigDecimal(...)` constructor calls returns zero matches).
- **`evidence:` block (3 verbatim Korean + 1 international):**
  1. **source_type: external** — Wikipedia (Korean) 부가가치세 — verbatim "대한민국 10% VAT = 부가세(附加稅) 또는 부가가치세(附加價値稅)"
     - URL: `https://ko.wikipedia.org/wiki/부가가치세`
     - quoted_at: 2026-05-24
  2. **source_type: external** — Wikipedia (Korean) 부가가치세 — verbatim "대한민국에서는 1977년 7월 1일부터 시행하였다."
     - URL: `https://ko.wikipedia.org/wiki/부가가치세`
     - quoted_at: 2026-05-24
  3. **source_type: external** — 국세청 (NTS) 부가가치세 기장의무 — verbatim "직전연도(2024년) 업종별 수입금액 기준으로 판단"
     - URL: `https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2272&cntntsId=7669`
     - quoted_at: 2026-05-24
  4. **source_type: external** — PwC Tax Summaries (Korea) — verbatim "VAT is generally levied at a rate of 10% on the supply of goods and services in Korea."
     - URL: `https://taxsummaries.pwc.com/republic-of-korea/corporate/other-taxes`
     - quoted_at: 2026-05-24

### 4.5 Evidence ledger — normalized counting model (closes Codex L-BLOCKING)

> **Counting model (single normalized).** A **verbatim source row** = one unique `(host, URL, fetch attempt)` returning 200 OK with ≥1 quoted substring usable as rule evidence. A **quote occurrence** = one rule-cite use of a substring from a verbatim source row (one row may supply multiple occurrences). A **downgrade row** = one unique `(host, URL, fetch attempt)` returning non-2xx, host-side timeout, or 200-with-no-rule-content. Following table uses this single model.

| # | Source class | URL | HTTP / fetch result | Verbatim quote | Used by | Resolution |
|---|---|---|---|---|---|---|
| **VERBATIM SOURCE ROWS (4 rows / 5 quote occurrences)** | | | | | | |
| S1 | KO 위키백과 부가가치세 | `https://ko.wikipedia.org/wiki/부가가치세` | **200 OK** (2026-05-24) | `"대한민국 10% VAT = 부가세(附加稅) 또는 부가가치세(附加價値稅)"` AND `"대한민국에서는 1977년 7월 1일부터 시행하였다."` | B2 #1 + B2 #2 (2 quote occurrences from 1 row) | Verbatim cite |
| S2 | KO NTS 부가가치세 기장의무 | `https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2272&cntntsId=7669` | **200 OK** (2026-05-24) | `"직전연도(2024년) 업종별 수입금액 기준으로 판단"` | B2 #3 (1 quote occurrence) | Verbatim cite |
| S3 | KO 한국은행 | `https://www.bok.or.kr/portal/main/main.do` | **200 OK** (2026-05-24) | `"통화정책의 효율적 수행을 통해 물가 안정과 금융안정을 도모"` | B1 #1 (1 quote occurrence — adjacent Korean enterprise anchor per R8/R9/R10 fallback) | Verbatim cite |
| S4 | EN PwC Tax Summaries (Korea) | `https://taxsummaries.pwc.com/republic-of-korea/corporate/other-taxes` | **200 OK** (2026-05-24) | `"VAT is generally levied at a rate of 10% on the supply of goods and services in Korea."` | B2 #4 (1 quote occurrence) | Verbatim cite |
| **DOWNGRADE ROWS (8 rows)** | | | | | | |
| D1 | KO 위키백과 사업자_등록_번호 (underscored) | `https://ko.wikipedia.org/wiki/사업자_등록_번호` | **HTTP 404** (2026-05-24) | — | B1 downgrade cluster | `internal_design` — raw underscored URL 404 |
| D2 | KO 위키백과 사업자등록번호 (alt) | `https://ko.wikipedia.org/wiki/사업자등록번호` | **200 OK — no 10-digit/format content** (2026-05-24) | — | B1 downgrade cluster | `internal_design` — page exists but does not describe Korean BRN 10-digit format |
| D3 | EN Wikipedia Business_registration_number | `https://en.wikipedia.org/wiki/Business_registration_number` | **HTTP 404** (2026-05-24) | — | B1 downgrade cluster | `internal_design` — page does not exist |
| D4 | KO namu.wiki 사업자등록번호 | `https://namu.wiki/w/사업자등록번호` | **HTTP 403** (2026-05-24) | — | B1 downgrade cluster | `internal_design` — bot-blocked |
| D5 | KO law.go.kr 부가가치세법 | `https://www.law.go.kr/법령/부가가치세법` | **timeout 60s × 1** (2026-05-24) | — | B2 downgrade cluster | `internal_design` — host-side latency |
| D6 | KO law.go.kr 부가가치세법/제30조 | `https://www.law.go.kr/법령/부가가치세법/제30조` | **timeout 60s × 1** (2026-05-24) | — | B2 downgrade cluster | `internal_design` — host-side latency |
| D7 | KO hometax.go.kr 메인 | `https://hometax.go.kr/websquare/websquare.html?w2xPath=/ui/sf/index.xml` | **timeout 60s × 1** (2026-05-24) | — | B2 downgrade cluster | `internal_design` — SPA shell timeout |
| D8 | KO NTS 부가가치세 (alt subsection) | `https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2272&cntntsId=7660` | **200 OK — "콘텐츠 내용이 준비되지 않았습니다"** (2026-05-24) | — | B2 downgrade cluster | `internal_design` — official placeholder |

**Totals under normalized model:**

- **Verbatim source rows = 4** (3 KO: S1+S2+S3 + 1 EN: S4)
- **Quote occurrences = 5** (S1 contributes 2; S2+S3+S4 contribute 1 each)
- **Downgrade rows = 8** (D1-D8)
- **Total table rows = 12** (4 verbatim + 8 downgrade)

**Per-rule floor check:**

- **B1 (korean-brn-format):** 1 Korean verbatim (S3 한국은행 adjacent) + 1 international cross-anchor not directly cited (PwC is B2 only — decoupled per M3) + 4 documented BRN-specific downgrades (D1+D2+D3+D4). **Clears 1-Korean-verbatim floor.** Note: B1's adjacent-verbatim is weaker than B2's — this is the **honest** outcome of dropping the checksum claim; the rule normative content (format-only) is correspondingly narrower.
- **B2 (korean-vat-10-percent-calculation):** 3 Korean verbatim occurrences from 2 KO source rows (S1×2 + S2) + 1 EN verbatim (S4) + 4 documented VAT-doc downgrades (D5+D6+D7+D8). **Clears 1-Korean-verbatim floor with 3× buffer.**

**Re-attempt at SP execution.** SP49 pre-flight re-runs one verbatim probe per VERBATIM-SOURCE row (S1+S2+S3+S4) to confirm 2026-05-24 quote substrings remain reachable when SP49 commits the rule files. Single-shot per host; no re-fabrication. Any newly-blocked source triggers SP49 abort with the downgrade documented in evidence snapshot.

### 4.6 SP Plan + Verification Matrix (2 SPs — atomic-4)

| SP | Atomic deliverables | TDD anchors | Verification |
|---|---|---|---|
| **SP49** (atomic-4) | (a) `practices/evals/cross_recipe_inv_uniqueness_guard.sh` + 2 fixtures; (b) `practices/evals/applied_recipes_alphabetical_guard.sh` + 2 fixtures; (c) `practices/rules/korean-brn-format.md`; (d) `practices/rules/korean-vat-10-percent-calculation.md`; shared: regenerated `practices/AGENTS.md` (natural sha refresh 84 → 86), `practices/upstream/r12-sp49-evidence-snapshot.md`, `practices/DECISIONS.md` TD-030 + TD-031, `practices/evals/run-all-guards.sh` registers 2 new guards. | Both guards RED → GREEN via `--fixtures`; live-repo run RED → GREEN. | `bash cross_recipe_inv_uniqueness_guard.sh` exit 0 across 11 recipe specs; `bash applied_recipes_alphabetical_guard.sh` exit 0 across all L4 READMEs; `bash practices/generate_agents.sh && git diff --exit-code practices/AGENTS.md` (idempotent — script unchanged, only input set changed); `bash practices/evals/run-all-guards.sh` exit 0 (24 guards GREEN). |
| **SP50** (FINAL) | 13 sealed verdicts re-run no-regression; `/ax-verify all` exit 0; tag policy applied. | Existing 13 verdict harnesses re-run. | `/ax-verify all` exit 0; binary PASS/FAIL per §6. |

**SP atomicity (one state machine — closes Codex Hard #6):** SP49 ships all 4 atomic items together OR full rollback. **No partial commits, no partial rollback.** The iter 1 wording at L316 ("rolls back the generator extension only") is **removed** — Axis C is no longer in scope, so partial-generator-rollback is not a valid state.
**SP linearization:** SP49 → SP50. No parallel branches.

---

## §5 AGENTS.md sha refresh (R12-specific — natural refresh, no generator change)

**Resolution:** AGENTS.md sentinel sha at R10/R11 close = `15c54ebbb876a78f3f17fb04d4cf9fba1573b827a7a70041d4e50785b9e14016` (84 rules concat). SP49 adds 2 rules (84 → 86); the existing unchanged `generate_agents.sh` reads `practices/rules/*.md` and emits a new `source_concat_sha256:` value reflecting the larger concat. **No generator code change** — TD-024 invariant intact for R13 standalone work.

### Disk evidence (2026-05-24)

- `head -5 practices/AGENTS.md` → `source_concat_sha256: "15c54ebbb876a78f3f17fb04d4cf9fba1573b827a7a70041d4e50785b9e14016"` + `rule_count: 84` + `generated_by: "practices/generate_agents.sh"`.
- `practices/generate_agents.sh` current concat loop produces deterministic output for fixed rule set — adding 2 files changes input set → new sha.

### Migration plan (within SP49)

1. **Author 2 new rule files** (`korean-brn-format.md` + `korean-vat-10-percent-calculation.md`) under `practices/rules/`.
2. **Re-run `bash practices/generate_agents.sh`** — produces new `AGENTS.md` with new `source_concat_sha256:` + `rule_count: 86`.
3. **Verify idempotency** — `bash practices/generate_agents.sh && git diff --exit-code practices/AGENTS.md` produces zero diff on second run (script unchanged).
4. **Commit** — SP49 squash commit includes regenerated `AGENTS.md` + 2 new rule files + 2 new guard scripts + 4 fixtures + evidence snapshot + DECISIONS.md appends.

---

## §6 Autonomous Execution Safety

- **Pre-flight gate (before SP49 starts):**
  - §4.5 evidence captured at PRD signature. SP49 pre-flight re-runs one verbatim probe per VERBATIM-SOURCE row (S1+S2+S3+S4) — single-shot per host; document HTTP status + timestamp in evidence snapshot.
  - Disk-verify absence: `test ! -f practices/evals/cross_recipe_inv_uniqueness_guard.sh && test ! -f practices/evals/applied_recipes_alphabetical_guard.sh && test ! -f practices/rules/korean-brn-format.md && test ! -f practices/rules/korean-vat-10-percent-calculation.md`.
  - Disk-verify current `practices/AGENTS.md` sentinel sha matches `15c54ebbb876a78f...`.
  - **Soft #4 confirmation command — 9 plural-shape L4 READMEs alphabetical (3 SKIP):** `for f in templates/L4/*/README.md; do awk '/^applied_recipes:/{flag=1; next} flag && /^  - /{print FILENAME": "$2} flag && !/^  - /{flag=0}' "$f"; done | awk -F': ' '{print $1}' | sort -u | while read fn; do entries=$(awk '/^applied_recipes:/{flag=1; next} flag && /^  - /{print $2} flag && !/^  - /{flag=0}' "$fn"); sorted=$(echo "$entries" | sort); [ "$entries" = "$sorted" ] && echo "OK $fn" || echo "FAIL $fn"; done` — expected: 9 `OK` rows (audit-log/auth/crud/feature-flags/notification/payment/scheduled-task/search/webhook); 3 skipped (billing singular, file-storage keyless, practices keyless); zero `FAIL`.
  - **M4 + Codex Hard #5 confirmation command — `quote_match_check.sh` coverage probe:** `head -50 practices/evals/quote_match_check.sh | grep -E 'upstream_id|external'` — confirms script inspects only `upstream_id` evidence and does **NOT** scan `source_type: external` rules. **This PRD does not claim `quote_match_check.sh` covers the 2 new external-source rules.** R13+ may extend the script as separate scope; iter 2 explicitly removes that claim.
  - Abort SP49 if any prep step fails.

- **Mid-flight gate (between SP49 and SP50):**
  - `git status` clean.
  - `bash practices/evals/run-all-guards.sh` exit 0 (24 guards GREEN).
  - `bash practices/generate_agents.sh && git diff --exit-code practices/AGENTS.md` (idempotent, second-run zero-diff).
  - Commit message references SP49 + TD-030 + TD-031.

- **Stop conditions (one state machine — closes Codex Hard #6):** If either new guard cannot reach GREEN within 3 iter cycles, **SP49 rolls back all 4 atomic items** (no partial rollback). The offending guard's path is moved to `practices/evals/advisory/` only as a **separate future ADR cycle**, never as a partial SP49 commit.

- **Sealed verdict release policy (binary at quality-cycle scope):** Tag `v1.9.0-catalog-quality` IFF SP50 confirms (i) `/ax-verify all` exit 0, (ii) both new guards exit 0 on live repo, (iii) `generate_agents.sh` idempotent on second run, (iv) 13 existing sealed verdicts re-run with no regression.

- **Rollback:** Each SP is one squash-mergeable commit. Revert SP49 cleanly without disturbing R10/R11 state.

- **No destructive ops:** No `git reset --hard`, no force push.

### Partial-tag policy (atomic-4 — degenerate at quality-cycle scope)

| SP50 verdict outcome | Tag | Practices rules | Hard guards | AGENTS.md state |
|---|---|---|---|---|
| 4/4 SP49 atoms + 13/13 verdicts no-regression | `v1.9.0-catalog-quality` | 86 (84 + 2) | 24 (22 + 2) | natural sha refresh, new sentinel sha emitted |
| Any SP49 atom FAIL or any verdict regression | no tag | 84 (SP49 reverted) | 22 (SP49 reverted) | sentinel sha `15c54...` (SP49 reverted) |

**Rationale:** Catalog-quality cycle does not partial-ship — either all 4 atoms land + 13 verdicts pass, or rollback. No mid-state where some new rules ship but guards don't.

---

## §7 Pre-Mortem (4 scenarios — SHORT mode floor + B1 evidence scenario)

1. **Existing live recipe specs already violate `cross_recipe_inv_uniqueness_guard.sh`.** Likelihood: LOW (disk census at PRD signature shows all 11 recipes use recipe-prefixed IDs; zero collisions today). Impact: SP49 cannot reach GREEN; guard blocks commit. Mitigation: SP49 pre-flight re-runs the census; if collisions found, guard is authored with explicit `legacy_exempt:` allow-list documented in TD-030. **TD-030 value is protective for future cycles, not corrective of current state** (Architect M2 + Codex M2 closure).

2. **Existing L4 READMEs already violate `applied_recipes_alphabetical_guard.sh`.** Likelihood: LOW (Soft #4 pre-flight disk command confirms 9 plural-shape L4 READMEs alphabetical at PRD signature (3 SKIP per guard contract)). Impact: SP49 cannot reach GREEN. Mitigation: SP49 pre-flight re-runs the alphabetical-state probe (§6 command); if any README is unsorted, SP49 fixes it *as part of the same atomic commit* (one-time backfill, not recurring exception) and TD-031 captures the backfill scope.

3. **Korean evidence URLs go 404 / timeout between PRD signature (2026-05-24) and SP49 execution.** Likelihood: LOW. Impact: SP49 pre-flight verbatim re-probe fails for ≥1 source row. Mitigation: 3 Korean verbatim source rows (S1+S2+S3) + 1 international (S4) provide 3× buffer over the 1-Korean-floor. If S1 or S2 goes dark by SP49 execution, S3 (한국은행 adjacent) + S4 (PwC) still anchor B1; B2 retains 1 of S1/S2 + S4. Worst case (all 3 Korean dark): SP49 falls back to cross-reference with existing in-catalog Korean rules (`currency-amount-precision-explicit.md`, `payment-iso-4217-currency.md` — already in catalog; 100% reachable on disk).

4. **B1 evidence is honest but thin** (closes Codex Pre-mortem-gap finding — Criterion G). Likelihood: ACKNOWLEDGED — B1 has only S3 한국은행 adjacent as its direct verbatim Korean anchor (BRN-specific Korean docs all downgraded). Impact: B1's evidence density is below B2's. Mitigation: (a) rule normative content is correspondingly narrowed to **format-only** — the rule does not claim any algorithm content unsourced from cited verbatim; (b) Architect H1 + Codex Hard #1 explicit closure: checksum deferred to R13+ as a separate rule once an authoritative source surfaces; (c) the 4 documented BRN-specific downgrades (D1-D4) constitute honest evidence that the 10-digit format is industry-convention rather than a single citable text — this is the same R8/R9/R10 adjacent-fallback pattern applied to a narrower normative claim.

---

## §8 ADR Template (2 entries — TD-030 + TD-031)

- **TD-2026-05-24-030 (NEW)** — Hard guard `cross_recipe_inv_uniqueness_guard.sh` shipped to mechanize R6-R10 manual INV-id collision discipline.
  - **Decision:** `practices/evals/cross_recipe_inv_uniqueness_guard.sh` added to `practices/evals/run-all-guards.sh` rotation. Hard guard count 22 → 23.
  - **Drivers:** R7 + R8 + R9 + R10 each manually verified no two recipes claim the same `(L4_domain, INV-id)` pair; discipline currently relies on planner attention. Disk census at PRD signature shows current 11-recipe state has zero collisions, so guard is authored against a clean baseline.
  - **Value framing (Architect M2 + Codex M2):** **Protective, not corrective.** The current live value is **R13+ regression prevention**, not closure of existing violations. Disk census at 2026-05-24 confirms all recipe IDs are recipe-prefixed (`API-GATEWAY-RELAY-INV-001`, `CRM-INV-001`, etc.) — collision space is empty today. The guard's value materializes only when a future cycle introduces a recipe that would otherwise collide. R12 ships it now because the discipline is already manual and the marginal cost of mechanizing it is low.
  - **Alternatives considered:** Advisory probe only (rejected — discipline already manual; promoting to hard guard is the value); rename-based avoidance (rejected — every recipe would have to rename to recipe-prefixed IDs, breaking R5-R10 stable INV-IDs).
  - **Why chosen:** Maximum mechanization of an already-manual discipline; zero current violations; clean baseline.
  - **Consequences:** Future recipe additions in R13+ must avoid collision. Adding a recipe that would share `(audit-log, AUDIT-INV-001)` triggers guard exit 1.
  - **Follow-ups:** R13+ verifies guard remains GREEN as new recipes (if any) ship.

- **TD-2026-05-24-031 (NEW)** — Hard guard `applied_recipes_alphabetical_guard.sh` shipped to mechanize R6-R10 manual alphabetical-insertion discipline.
  - **Decision:** `practices/evals/applied_recipes_alphabetical_guard.sh` added to `practices/evals/run-all-guards.sh` rotation. Hard guard count 23 → 24.
  - **Drivers:** R6 dual-form regex accepts plural `applied_recipes:` lists with ≥1 entry but does NOT enforce alphabetical sort. R6 SP39, R7 SP41, R8 SP43, R9 SP45b, R10 SP47 all manually authored the alphabetical-insert.
  - **Alternatives considered:** Auto-sort fixer (rejected — would silently mutate L4 READMEs in unrelated PRs); document-only convention (rejected — unenforceable); R12 backfill-only no-guard (rejected — recurring discipline benefits from recurring check).
  - **Why chosen:** Same rationale as TD-030; quality-cycle pairing reinforces mechanization theme; Soft #4 pre-flight disk command confirms 9 plural-shape L4 READMEs already sorted; 3 SKIP (zero backfill needed in expected case).
  - **Consequences:** Future L4 README mutations in R13+ must respect alphabetical order.
  - **Follow-ups:** If backfill is needed at SP49 execution (Pre-Mortem #2), TD-031 captures backfill scope explicitly.

**Deferred to R13** (closes Architect H2 + Codex Hard #2):

- **TD-032 (AGENTS.md generated TOC + generator extension)** — deferred to R13 standalone cycle. R13 PRD must specify whether the sentinel sha covers (a) rule concat only (current TD-024 invariant) or (b) full generated AGENTS.md content including TOC, and provide the post-extension `generate_agents.sh` script shape before merging the cycle.

---

## §9 Honored Constraints

- **Caps:** Tier-1 = 4 FROZEN · Tier-2 = 8 · L1/L2/L3 = 49/92/20 · L4 = 12 · Recipes = 11 · Sealed verdicts = 13 (all UNCHANGED).
- **Deltas:** Practices rules 84 → 86 (+2 Korean) · Hard guards 22 → 24 (+2) · AGENTS.md sentinel sha refreshes naturally · DECISIONS.md +2 TD entries (TD-032 deferred to R13).
- **Atomic SP rule per axis** (R6/R10 precedent — SP49 atomic-4 + SP50 FINAL).
- **TD-024 invariant UNCHANGED** (`generate_agents.sh` reads only `practices/rules/*.md`; Axis C deferred to R13). **TD-027 2-consumer-signal gate UNCHANGED** (zero new L4).
- **Korean references** — 3 Korean verbatim source rows (S1+S2+S3, 4 quote occurrences) + 1 international (S4, 1 occurrence). 12 logical hosts attempted (§4.5). R7+ 5-host floor MET. R8/R9/R10 1-Korean-PASS target MET (B2 3×, B1 1×). 8 downgrade rows documented honestly per R8/R9/R10 host-wide cascade precedent.
- **B1 ↔ B2 evidence decoupling** (Architect M3 + Codex M3) — PwC is B2-exclusive; 위키백과 주민등록번호 mod-11 row removed (irrelevant to format-only B1).
- **`quote_match_check.sh` coverage claim REMOVED** (Architect M4 + Codex Hard #5) — script covers only `upstream_id` evidence + exits 0 unconditionally. R13+ may extend as separate scope.
- **`deferred_recipes:` queue stays CLOSED. No new L4/L3/L2/L1/recipe/verdict/Tier-1/Tier-2 skill. No 휴대폰 본인인증 (R13+). No fork-receiver `inspect.sh` (Axis D R13+). No generator script mutation (Axis C R13).**

---

## §10 Out-of-scope (R12 explicit) + Deferred Items

**Deferred recipes:** `recipes/_MANIFEST.yaml#deferred_recipes:` stays `[]` post-SP49.

**Deferred Korean rule candidates (R13+):**

| Candidate | Trigger to ship |
|---|---|
| `korean-brn-checksum` (mod-10 weighted-sum) | Authoritative source (academic paper / NTS notice / standards doc) reachable on PRD-signature day. |
| `korean-phone-verification-pass-flow` | KISA + 토스 PASS docs verbatim reachable. |
| `kakao-alimtalk-template-authorization` | kakao.business.api docs verbatim reachable. |
| `naver-id-oauth-scope` | developers.naver.com verbatim reachable. |

**Deferred catalog quality (R13 standalone cycle):** AGENTS.md generated TOC + `generate_agents.sh` extension; standalone ADR for TD-024 amendment + post-extension script.

**Out-of-scope (R12):** new L1/L2/L3/L4 surface · new Tier-1/Tier-2 skill · new recipe · new sealed verdict · BRN checksum (deferred H1) · AGENTS.md TOC (deferred H2) · `quote_match_check.sh` extension to `source_type: external` (R13+) · 휴대폰 본인인증 PASS rule · frontend/backend code mutations · deployment/CI/release · fork-receiver `inspect.sh` (Axis D) · rate-limit L4 promotion (TD-028).

---

## §11 Architect H/M + Codex Hard/Soft disposition table (closes Codex Criterion I)

| Finding | Severity | Iter 2 disposition | PRD reference |
|---|---|---|---|
| **Architect H1** — B1 mod-10 checksum claim has zero verbatim citation | BLOCKING | **CLOSED.** B1 reframed to format-only (`NNN-NN-NNNNN` 10-digit format). Checksum algorithm deferred to R13+ (§10 deferred rules table). 위키백과 주민등록번호 mod-11 row removed from evidence ledger entirely. | §4.3 + §10 + §1 Option (3) rejected |
| **Architect H2** — Axis C amends TD-024 invariant without generator code shape | BLOCKING | **CLOSED via Synthesis-A.** Axis C (AGENTS.md TOC + generator extension) entirely dropped from R12; deferred to R13 standalone cycle with its own ADR. TD-024 invariant preserved unchanged. | §1 Cycle frame bullet 4 + §1 Option (2) rejected + §5 + §8 deferred-TD-032 |
| **Architect H3** — Catalog count baseline reconciliation | BLOCKING | **CLOSED.** §2 explicit MEMORY.md quarantine note: `payment_blueprint_status` 64/68 reference is a separate downstream surface; ax-template R12 baseline is disk-verified 84 rules. | §2 MEMORY.md baseline reconciliation block |
| **Architect M1** — SP49 atomic-5 orthogonal failure modes | MEDIUM | **CLOSED via Synthesis-A.** SP49 reduced from atomic-5 to atomic-4 after Axis C drop; failure modes now: guard A1 / guard A2 / rule B1 / rule B2 (4 independent atoms under one rollback gate; tighter coupling because rules + guards are quality-pair). | §1 Cycle frame bullet 4 + §4.6 + §6 |
| **Architect M2 / Codex M2** — Pre-Mortem #1 mitigation circular | MEDIUM | **CLOSED.** TD-030 ADR explicit "Protective, not corrective" framing. Live value = R13+ regression prevention; current state has zero violations. | §1 Driver 1 + §8 TD-030 Value framing + §7 Pre-Mortem #1 |
| **Architect M3 / Codex M3** — B1↔B2 artificial cross-citation | MEDIUM | **CLOSED.** B1 evidence list no longer cites PwC VAT-rate quote. PwC stays B2-exclusive. | §4.3 evidence block + §9 honored constraint |
| **Architect M4 / Codex Hard #5** — `quote_match_check.sh` coverage unverified / overstated | MEDIUM → Hard #5 BLOCKING | **CLOSED.** §6 pre-flight adds explicit `head -50 practices/evals/quote_match_check.sh \| grep` command confirming the script covers only `upstream_id` evidence (verified at PRD signature: only `upstream_id` branch + exits 0 unconditionally at line 86). Coverage claim removed from iter 2; R12 does **not** claim `quote_match_check.sh` covers the 2 new external-source rules. | §6 M4+Hard #5 confirmation command + §3 Must NOT Have + §9 |
| **Codex L-BLOCKING** — Evidence ledger arithmetic contradictory | BLOCKING | **CLOSED.** §4.5 normalized to single counting model ("verbatim source rows = N (M quote occurrences)" style + downgrade rows + total table rows). Iter 2 totals: 4 verbatim source rows / 5 quote occurrences / 8 downgrade rows / 12 total table rows. All summary lines (§1 bullet 7, §4.5 totals block, §9 Korean references, §12 verdict line) use the same model. | §4.5 counting model header + per-rule floor check + §1 + §9 + §12 |
| **Codex Hard #1-#5** — subsumed by Architect H1/H2/H3 + L-BLOCKING + M4 above | BLOCKING | **CLOSED** — Hard #1 = H1; Hard #2 = H2; Hard #3 = H3; Hard #4 = L-BLOCKING; Hard #5 = M4. | (see rows above) |
| **Codex Hard #6** — Atomicity contradiction (L316 partial-generator-rollback vs L145/L330 all-or-rollback) | BLOCKING | **CLOSED.** Axis C drop removes partial-generator-rollback scenario. SP49 = atomic-4 / one state machine / all-or-rollback. Iter 1 L316 wording removed. | §6 Stop conditions + §4.6 atomicity |
| **Codex Soft #1** — Adopt Synthesis-A | SOFT | **ADOPTED.** Axis C deferred to R13. | All sections |
| **Codex Soft #2 / #3** — A1 live-value framing + B1/B2 ledger separation | SOFT | **CLOSED** (subsumed by M2 + M3). | §8 TD-030 + §4.3 + §4.4 |
| **Codex Soft #4** — Disk command for sorted `applied_recipes` across all plural L4 READMEs | SOFT | **CLOSED.** §6 explicit `for f in templates/L4/*/README.md` loop with per-file `OK`/`FAIL`. PRD-signature run: 9 plural-shape L4 READMEs `OK`; 3 SKIP (billing singular + file-storage/practices keyless). | §6 Soft #4 cmd |

---

## §12 Verdict line

R12 iter 2 ships **Synthesis-A trim**: 2 hard guards (A1+A2, protective per TD-030+TD-031) + 2 Korean rules (B1 format-only + B2 VAT 10%; BRN checksum deferred R13+ per H1) + natural AGENTS.md sha refresh (84→86 concat; no generator mutation; TOC deferred R13 per H2). Evidence ledger normalized to single model (closes L-BLOCKING): 4 verbatim source rows / 5 quote occurrences / 8 downgrades / 12 total. B1↔B2 decoupled (PwC B2-only; mod-11 row removed). `quote_match_check.sh` claim removed (Hard #5). §2 MEMORY.md quarantine (H3). SP49 atomic-4 one-state-machine all-or-rollback (Hard #6). 2 SPs ≈ 2-3 d. 2 ADRs (TD-030+TD-031); TD-032 deferred R13.

---

## Iter 2 changelog

| Blocker ID | Severity | Iter 1 location | Iter 2 disposition | Iter 2 target lines |
|---|---|---|---|---|
| Architect H1 | BLOCKING | Iter 1 L190-203, 256, 270 | B1 reframed format-only; checksum deferred R13+; 위키백과 주민등록번호 row removed from ledger | §4.3, §10, §1 Option (3), §11 |
| Architect H2 | BLOCKING | Iter 1 L230-246, 366 | Axis C dropped via Synthesis-A; TD-024 invariant preserved; TD-032 deferred to R13 | §1 bullet 4, §5, §8 deferred, §11 |
| Architect H3 | BLOCKING | Iter 1 vs MEMORY.md | §2 explicit quarantine note: MEMORY.md `payment_blueprint_status` 64/68 ≠ ax-template baseline 84 | §2 MEMORY.md baseline reconciliation, §11 |
| Architect M1 | MEDIUM | Iter 1 §4.7 | SP49 atomic-5 → atomic-4 after Axis C drop | §1 bullet 4, §4.6, §6, §11 |
| Architect M2 / Codex M2 | MEDIUM | Iter 1 L336 | TD-030 ADR "Protective not corrective" framing | §1 Driver 1, §8 TD-030, §7 #1, §11 |
| Architect M3 / Codex M3 | MEDIUM | Iter 1 L200-202 | B1 evidence drops PwC cross-citation; PwC B2-exclusive | §4.3, §9, §11 |
| Architect M4 / Codex Hard #5 | MEDIUM→BLOCKING | Iter 1 L31, 130, 285, 315, 388 | §6 pre-flight head-50 verify command; claim removed | §6, §3 Must NOT Have, §9, §11 |
| Codex L-BLOCKING (new) | BLOCKING | Iter 1 L25, 140, 273, 275, 380, 381, 414 | Single normalized counting model: 4 verbatim rows / 5 occurrences / 8 downgrades / 12 total | §4.5 counting-model header + totals block, §1, §9, §12, §11 |
| Codex Hard #1 / #2 / #3 / #4 / #5 | BLOCKING | Iter 1 L190 / L230-246 / vs MEMORY.md / ledger / L31+L130+L285+L315+L388 | Subsumed by Architect H1 / H2 / H3 / L-BLOCKING / M4 above (same dispositions) | §4.3 / §1+§5+§8 / §2 / §4.5 / §6+§3+§9 |
| Codex Hard #6 | BLOCKING | Iter 1 L316 vs L145/L330 | L316 partial-generator-rollback removed; SP49 atomic-4 one-state-machine all-or-rollback | §6 Stop conditions, §4.6 SP atomicity |
| Codex Soft #1 | SOFT | Iter 1 Option (2) | Synthesis-A adopted as Option (1) | §1 Option (1), all sections |
| Codex Soft #2 / #3 | SOFT | Iter 1 TD-030 + B1/B2 | Subsumed by M2 (protective) + M3 (decoupled) | §8 TD-030 + §4.3 + §4.4 |
| Codex Soft #4 | SOFT | Iter 1 §6 | §6 explicit `for f in templates/L4/*/README.md` disk command + per-file OK/FAIL | §6 Soft #4 cmd |

**Net delta from iter 1:** Axis C entirely dropped (TD-032, generator extension, TOC section, AGENTS.md TOC migration plan removed); B1 checksum claim removed (rule reframed format-only; 위키백과 주민등록번호 mod-11 evidence row removed); evidence ledger renormalized to single counting model; `quote_match_check.sh` coverage claim removed; SP49 atomic-5 → atomic-4 with one rollback state machine; new disposition table §11 + Soft #4 disk command + M4/Hard #5 head-50 verify command + §2 MEMORY.md quarantine note added.

---

## Iter 3 changelog

Surgical iter 2 → iter 3 fix per Codex iter 2 narrow verdict (2 issues: closure #9 actual 9 OK not 11 + §3 L114 international citation gate unmet for B1).

- **Codex iter 2 #9 REOPEN (L4 alphabetical 9 OK actual vs 11 claim)** — CLOSED. §4.1 A2 + §6 pre-flight command output expectation + §7 Pre-Mortem #2 + §8 TD-031 Why-chosen + §11 disposition table Soft #4 row — all updated to: "9 plural-shape L4 READMEs OK (audit-log/auth/crud/feature-flags/notification/payment/scheduled-task/search/webhook); 3 SKIP (billing singular `applied_recipe:` + file-storage keyless + practices keyless)". Guard contract acknowledged: SKIP rows are by-design (R5 legacy singular + unused-L4 keyless precedent — file-storage + practices).
- **Codex iter 2 independent attack BLOCKING (§3 L114 international citation gate unmet for B1)** — CLOSED. §3 L114 reworded to acknowledge Synthesis-A evidence model honestly: per-rule Korean verbatim floor (1) per rule + global international row in shared ledger (PwC cited directly under B2 only; B1 = format-only with adjacent-Korean-only verbatim by design after evidence-decoupling). B1 still satisfies per-rule Korean floor (≥1) via 한국은행 adjacent + 위키백과 BRN downgrade context; international citation is collected at ledger level for the cycle, not enforced per-rule for format-only B1.
- **All iter 2 closures preserved verbatim** — Axes A + B intact; Axis C remains deferred R13; evidence arithmetic 4/5/8/12 unchanged; quote_match_check.sh non-claim preserved; SP49 atomic-4 one rollback state machine preserved; TD-030 protective-not-corrective preserved; B1↔B2 decouple preserved; §11 disposition table preserved.
- **Architect iter 2 INFORMATIONAL (L353 wording awkwardness)** — non-blocking, retained as-is (cosmetic only per Architect iter 2 grading).
- **Ready for Codex iter 3 narrow re-review.** Expected verdict: APPROVE (2 narrow closures land).
