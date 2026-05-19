# R13 — AGENTS.md TOC + Generator Extension PRD — 2026-05-25 (Round 13, ralplan iter 2 APPROVED)

> **Status:** APPROVED (2-iter ralplan consensus; Architect iter 2 APPROVE + Codex Critic iter 2 FINAL APPROVE). **Date:** 2026-05-25. **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Predecessors:** `2026-05-24-r12-catalog-quality-prd.md` (R12 CLOSED `v1.9.0-catalog-quality` @ `main@ff169cf`); iter 1 draft `2026-05-25-r13-toc-brn-checksum-prd.draft.md` (532 L); Architect review `…-r13-architect-review.md`; Codex iter 1 `…-r13-critic-codex-iter1.md`.
> **R13 axis decision (Planner):** **Option 2 — Single-axis Axis A only.** Axis B (BRN checksum) deferred to R14 — 2026-05-25 gate UNMET (§4.5.B).
> **Branch:** `feat/r13-agents-md-toc-sp51-sp52`. **Targeted tag:** `v1.10.0-agents-toc` IFF SP52 PASS (§6); no tag IFF FAIL.

---

## §1 RALPLAN-DR Summary

### Cycle frame (6 bullets)

- **Baseline (R12 closed).** `v1.9.0-catalog-quality` @ `main@ff169cf`. 13 verdicts PASS; **24 hard guards GREEN**; 86 rules; sentinel `d367ba2f...` @ `rule_count: 86`. Disk-truth iter 2 re-verified: **12 L4 / 11 recipes / 13 verdicts** (`grep -c '^  - pattern:' recipes/_MANIFEST.yaml` = 11; `ls -d templates/L4/*/ | wc -l` = 12; `ls skills/_tests/sealed-verdict/*.md | grep -v README | wc -l` = 13).
- **R13 strategy.** Close R12 TD-032. Extend `generate_agents.sh` to append TOC AFTER rule-concat. **TD-024 sha-input UNCHANGED** (sentinel covers `practices/rules/*.md` concat only); **TD-024 I/O surface AMENDED** (script now reads 3 additional disk surfaces — `templates/L4/*/README.md`, `recipes/_MANIFEST.yaml`, `skills/_tests/sealed-verdict/*.md` — to emit observability TOC outside fingerprint). Wording precision per M1 / hard #4.
- **Axis B (BRN checksum) deferred R14.** Probe: 0 verbatim Korean primary + 0 international; 9 downgrades; 2 OSS-comment below R8/R9/R10 rigor floor.
- **Hard guards 24 → 25 (NEW: `agents_md_toc_disk_truth_guard.sh`).** Architect H2 / Codex hard #2: sha-asymmetry binary-guarded (≤50 LOC guard re-runs generator + diffs TOC). SP51 expands **atomic-3 → atomic-4** (deliverables a-d).
- **Atomic 1-SP cycle (SP51 atomic-4 + SP52 FINAL).** SP51 ships generator + regenerated AGENTS.md + DECISIONS.md TD-033 + new guard. SP52 FINAL re-runs 13 verdicts + `/ax-verify all` + tag.
- **Evidence rigor.** Axis A = owned-code (no external floor); 3 disk-truth rows + Sphinx pattern precedent are reference only. Axis B floor UNMET → R14 bounded retry (§10).

### Principles (7 numbered)

1. **Composition kit, not single product.** R13 strengthens observability; does not enlarge surface.
2. **Spec-before-code.** Owned-code extension — TDD anchor: post-extension script behavior (idempotent + sentinel-invariant + TOC-disk-match + 25 guards GREEN).
3. **Binary verification.** Acceptance: (i) sentinel = `d367ba2f...`; (ii) 2nd-run zero diff; (iii) TOC = 12/11/13; (iv) cross-links comma-space joined; (v) 25th guard exit 0; (vi) `run-all-guards.sh` exit 0 across 25 guards.
4. **TD-024 sha-input UNCHANGED; I/O surface AMENDED.** Sentinel sha covers rule concat ONLY (R7 TD-020 hedge intact); script reads 3 additional surfaces (TD-033).
5. **Atomic SP rule.** SP51 atomic-4: generator + regenerated AGENTS.md + TD-033 + 25th guard. **One state machine: all-4-or-rollback.**
6. **AGENTS.md sentinel sha UNCHANGED across R13.** Rule concat unchanged → SHA unchanged. SP52 re-verifies.
7. **Korean enterprise stack realism.** Axis B deferred R14 with **bounded** retry source list (§10) — no fabrication.

### Decision Drivers (top 3)

1. **TD-032 closure.** R12 PRD §8 deferred TD-032 to R13 standalone ADR. R13 specifies: (i) option (a) sha-input scope + I/O surface amended; (ii) disk-validated script shape §4.1; (iii) 25th TOC drift guard.
2. **Axis B authoritative gate UNMET 2026-05-25** (§4.5.B). Shipping low-rigor Axis B re-opens R12 Architect H1 BLOCKING.
3. **Sha-asymmetry deserves binary guard, not documented-only.** Architect H2 / Codex hard #2: fork-receiver seeing sentinel unchanged might assume whole-file consistency. 25th guard moves load from prose into bash. Cost: ≤50 LOC + fixture; atomic-3 → atomic-4 preserves single-state-machine.

### Viable Options Considered (≥2 mandatory)

- **(1) Dual-axis A+B.** Pros: closes both R12 deferred. Cons: Axis B gate UNMET → re-opens R12 H1 BLOCKING. **REJECTED.**
- **(2) Single-axis A + 25th guard.** Pros: TD-032 clean closure; sha-asymmetry binary-guarded (H2); R14 honest defer; atomic-4 well within R6/R10/R12 envelope. Cons: B1 still pending (acceptable). **CHOSEN.**
- **(3) Defer R13.** Pros: no-op. Cons: 2-cycle stale debt. **REJECTED.**
- **(4) Axis A with option (b) sentinel scope.** Pros: TOC fingerprinted. Cons: amends TD-024 sha-input (R7). **REJECTED.**

### Mode

**SHORT.** No L4/L3/L2/L1/recipe/rule mutation. **+1 hard guard.** Generator ≤ 50 LOC + 25th guard ≤ 50 LOC. Pre-mortem 5 scenarios sufficient. Wall-time ≈ 1-2 d.

### Recommended: Option (2) — 2 SPs (SP51 atomic-4 + SP52 FINAL)

```
SP51   (atomic-4 — generate_agents.sh extension
        + regenerated practices/AGENTS.md (TOC after sentinel, same sentinel sha)
        + practices/DECISIONS.md TD-033 ADR
        + practices/evals/agents_md_toc_disk_truth_guard.sh (25th hard guard))
   ↓ gated on (i) sentinel sha == d367ba2f... unchanged; (ii) 2nd-run zero diff;
            (iii) TOC counts 12/11/13; (iv) all 25 guards exit 0
SP52   (FINAL — /ax-verify all exit 0 + 13 sealed verdicts re-run no regression
        + tag v1.10.0-agents-toc IFF PASS + PR)
```

Total: **2 SPs, atomic-4 SP51, ≈ 1-2 d wall-time.**

---

## §2 Context

### R12 disk-verified state (2026-05-25, re-verified iter 2)

| Surface | Count | Path |
|---|---|---|
| L1 primitives | 49 | `templates/L1/components/` |
| L2 blocks | 92 | `templates/L2/blocks/` |
| L3 pages | 20 | `templates/L3/pages/` |
| L4 domains | **12** | `templates/L4/` |
| Active recipes (`- pattern:`) | **11** | `recipes/_MANIFEST.yaml` |
| Deferred recipes | **0** | `recipes/_MANIFEST.yaml#deferred_recipes` |
| Sealed verdicts | **13** (all PASS) | `skills/_tests/sealed-verdict/` |
| Hard guards GREEN | **24** (→ 25 in SP51) | `practices/evals/*.sh` |
| Practices rules | **86** | `practices/rules/*.md` |
| AGENTS.md sentinel | `source_concat_sha256: d367ba2f...` @ `rule_count: 86` | `practices/AGENTS.md` (head -5) |
| `generate_agents.sh` | 42 lines (rule concat only) | `practices/generate_agents.sh` |
| Current tag | `v1.9.0-catalog-quality` on `main@ff169cf` | — |

### Manifest schema disk-truth (iter 2 re-verified — H1 / hard #1 fix)

```yaml
# recipes/_MANIFEST.yaml
recipes:                                 # top-key (NOT active_recipes:)
  - pattern: saas-subscription           # per-row (NOT - id:)
    status: active
    spec: specs/recipes/saas-subscription-recipe-l0.yaml
    sealed_verdict: skills/_tests/sealed-verdict/saas-subscription-verdict.md
    enabled_l4_domains:
      - billing
      - auth
      ...
# templates/L4/audit-log/README.md frontmatter
applied_recipes:
  - api-gateway-relay
  ...
```

### R12 deferred carried into R13

- **TD-032** (Axis C dropped via R12 Synthesis-A) — R13 closes via option (a) + §4.1 + 25th guard §5.
- **B1 checksum `korean-brn-checksum.md`** — trigger UNMET → R14 bounded retry §10.

### R13 scope (Axis A only)

- **A1 script extension** `practices/generate_agents.sh` ≤ 50 LOC added, disk-validated parsers, awk join helper.
- **A2 regenerated AGENTS.md** — sentinel `d367ba2f...` UNCHANGED.
- **A3 DECISIONS.md TD-033** — TD-024 sha-input UNCHANGED + I/O surface AMENDED.
- **A4 25th guard `agents_md_toc_disk_truth_guard.sh`** ≤50 LOC — re-runs generator into `/tmp`, diffs TOC body.

NO new L1/L2/L3/L4/recipe/verdict/rule. **+1 hard guard (24 → 25).** Everything else FROZEN.

---

## §3 Objectives + Guardrails

### Must Have

- **1 generator script extension** `practices/generate_agents.sh` — appends TOC AFTER rule-concat; idempotent; sentinel sha covers rule concat ONLY.
- **1 regenerated `practices/AGENTS.md`** — sentinel `d367ba2f...` UNCHANGED.
- **1 new TD entry** `practices/DECISIONS.md` TD-033 with explicit TD-024 amendment (sha-input UNCHANGED + I/O surface AMENDED).
- **1 new hard guard** `practices/evals/agents_md_toc_disk_truth_guard.sh` ≤50 LOC. **Guards 24 → 25.**
- **TOC scans three paths in lexical order:** `templates/L4/*/README.md` applied_recipes (12); `recipes/_MANIFEST.yaml` top-key `recipes:` / `- pattern:` (11); `skills/_tests/sealed-verdict/*.md` (13, excluding README).
- **Cross-link rows comma-space joined via awk helper** (NOT `paste -sd ', '`).
- `/ax-verify all` exit 0 in SP52; 2nd-run zero diff; 13 verdicts no-regression; **25 guards exit 0**; tag `v1.10.0-agents-toc` IFF SP52 PASS.

### Must NOT Have

- NO new L4/L3/L2/L1/Tier-1/Tier-2 skill/recipe/verdict/rule.
- NO amendment to TD-024 **sha-input** clause (I/O surface IS amended — see TD-033).
- NO BRN checksum rule (R14 §10); NO Korean reference fabrication.
- NO change to existing 24 guards (1 NEW added) / 86 rules / fork-receiver SKILL.md / scripts.
- NO `## Hard guards` TOC sub-section (R14 TD-034 per M4 / soft #3).
- NO `deferred_recipes:` queue opening; NO 휴대폰 본인인증 (R14+); NO TD-027 re-triggering.
- NO partial deliverable within SP51 (atomic-4 all-or-rollback).

---

## §4 Deliverable Inventory + Evidence Ledger

### 4.1 Generator script extension — post-extension `generate_agents.sh` shape (iter 2 REWRITTEN)

- **Path:** `practices/generate_agents.sh` (extended; current 42 lines → ~85-95 lines)
- **Function (post-extension):** Same as current BEFORE the rule-concat loop. AFTER the rule-concat loop, append a TOC section header + 3 sub-sections (L4 / active recipes / sealed verdicts) + cross-link rows. **The sentinel sha is computed BEFORE the TOC append — covering rule concat only.**

**Post-extension shape (~85-95 lines bash, iter 2 disk-validated; exact wording deferred to SP51 implementation):**

```bash
#!/usr/bin/env bash
# practices/generate_agents.sh — produce practices/AGENTS.md by concatenating
# rules/*.md (sentinel sha covers rule concat ONLY) and appending an
# observability TOC. Idempotent: 2nd run with no changes produces no diff.
set -euo pipefail

cd "$(dirname "$0")"
OUT="AGENTS.md"
RULES_GLOB="rules/*.md"

# --- Section A: existing rule concat + sha (UNCHANGED from R12) -------------
shopt -s nullglob
RULE_FILES=()
for f in $RULES_GLOB; do
    [[ "$(basename "$f")" == ".gitkeep" ]] && continue
    RULE_FILES+=("$f")
done
IFS=$'\n' SORTED=($(printf '%s\n' "${RULE_FILES[@]}" | sort)); unset IFS

CONCAT="$(printf '' && for f in "${SORTED[@]}"; do cat "$f"; printf '\n'; done)"
SHA="$(printf '%s' "$CONCAT" | shasum -a 256 | awk '{print $1}')"
COUNT="${#SORTED[@]}"

# --- Section B: parse recipes/_MANIFEST.yaml ONCE (Codex soft #1) -----------
# Output one line per active recipe: "pattern|spec|verdict|domain1,domain2,..."
MANIFEST="../recipes/_MANIFEST.yaml"
MANIFEST_ROWS="$(awk '
    /^recipes:/ {in_recipes=1; next}
    in_recipes && /^  - pattern:/ {
        if (pat != "") emit()
        pat=$3; spec=""; verdict=""; doms=""; in_doms=0
        next
    }
    in_recipes && /^    spec:/        {spec=$2; in_doms=0; next}
    in_recipes && /^    sealed_verdict:/ {verdict=$2; in_doms=0; next}
    in_recipes && /^    enabled_l4_domains:/ {in_doms=1; next}
    in_recipes && in_doms && /^      - / {
        if (doms == "") doms=$2; else doms=doms ", " $2
        next
    }
    in_recipes && in_doms && /^    [a-z]/ {in_doms=0}
    in_recipes && /^[a-z]/ {in_recipes=0}
    END {if (pat != "") emit()}
    function emit() {printf "%s|%s|%s|%s\n", pat, spec, verdict, doms}
' "$MANIFEST" | sort)"

# Helper: awk-based comma-space join over stdin (Codex L fix — replaces broken
# `paste -sd ", "`). Pure awk; no sed pipe; deterministic on macOS 3.2 + Linux 5.x.
join_cs() {
    awk 'NR>1{printf ", "} {printf "%s", $0} END{if (NR>0) print ""}'
}

{
  # --- frontmatter sentinel (covers rule concat ONLY; TD-024 sha-input) -----
  printf -- '---\n'
  printf 'sentinel:\n'
  printf '  source_concat_sha256: "%s"\n' "$SHA"
  printf '  rule_count: %s\n' "$COUNT"
  printf '  generated_by: "practices/generate_agents.sh"\n'
  printf -- '---\n\n'

  # --- header ---------------------------------------------------------------
  printf '# Practices — AGENTS.md (auto-generated)\n\n'
  printf 'This file is auto-generated from `practices/rules/*.md` in lexical order.\n'
  printf 'Do not edit by hand — re-run `practices/generate_agents.sh` after rule changes.\n\n'
  printf 'Sentinel sha covers rule concat ONLY (TD-024 sha-input clause).\n'
  printf 'TOC section below is observability outside the fingerprint (TD-033 R13).\n\n'

  # --- rule concat body (UNCHANGED from R12) --------------------------------
  for f in "${SORTED[@]}"; do
    printf -- '<!-- @source %s -->\n\n' "$f"
    cat "$f"
    printf '\n\n'
  done

  # --- NEW: TOC section (outside sentinel fingerprint; TD-033) --------------
  printf -- '---\n\n'
  printf '# Catalog TOC (observability — not part of sentinel sha)\n\n'

  # L4 domains (lexical) with applied_recipes cross-link
  printf '## L4 domains\n\n'
  for d in ../templates/L4/*/; do
    name="$(basename "$d")"
    applied="$(awk '
        /^applied_recipes:/ {flag=1; next}
        flag && /^  - / {print $2}
        flag && /^[a-z]/ {flag=0}
    ' "$d/README.md" 2>/dev/null | join_cs)"
    [[ -z "$applied" ]] && applied="(none)"
    printf -- '- **%s** — applied by: %s\n' "$name" "$applied"
  done
  printf '\n'

  # Active recipes from cached MANIFEST_ROWS (single manifest pass — Codex soft #1)
  printf '## Active recipes\n\n'
  while IFS='|' read -r pat spec verdict doms; do
    [[ -z "$doms" ]] && doms="(unknown)"
    printf -- '- **%s** — enabled L4: %s\n' "$pat" "$doms"
  done <<< "$MANIFEST_ROWS"
  printf '\n'

  # Sealed verdicts (lexical, excluding README.md)
  printf '## Sealed verdicts\n\n'
  for v in ../skills/_tests/sealed-verdict/*.md; do
    [[ "$(basename "$v")" == "README.md" ]] && continue
    name="$(basename "$v" .md)"
    printf -- '- %s\n' "$name"
  done
  printf '\n'

} > "$OUT"

# --- Inline disk-truth assertions (proof of 12 / 11 / 13) -------------------
L4_COUNT=$(ls -d ../templates/L4/*/ 2>/dev/null | wc -l | tr -d ' ')
REC_COUNT=$(printf '%s\n' "$MANIFEST_ROWS" | grep -c '|' || true)
VER_COUNT=$(ls ../skills/_tests/sealed-verdict/*.md 2>/dev/null | grep -v README | wc -l | tr -d ' ')
[[ "$L4_COUNT"  == "12" ]] || { echo "ASSERT FAIL: L4 count $L4_COUNT != 12" >&2; exit 1; }
[[ "$REC_COUNT" == "11" ]] || { echo "ASSERT FAIL: recipe count $REC_COUNT != 11" >&2; exit 1; }
[[ "$VER_COUNT" == "13" ]] || { echo "ASSERT FAIL: verdict count $VER_COUNT != 13" >&2; exit 1; }

echo "wrote $OUT — $COUNT rules, sha=$SHA, TOC: $L4_COUNT L4 / $REC_COUNT recipes / $VER_COUNT verdicts"
```

**Iter 2 fixes (full disposition in §11):** (1) manifest awk → `^recipes:` / `^  - pattern:` (H1); (2) awk `join_cs()` replaces `paste -sd ', '` (L); (3) single manifest pass via `MANIFEST_ROWS` (soft #1); (4) inline 12/11/13 assertions.

**Example TOC output (verification concreteness, 3 rows / sub-section):**

```markdown
## L4 domains
- **audit-log** — applied by: api-gateway-relay, b2b-admin, booking, cms, community, crm, e-commerce, internal-it, lms, marketplace, saas-subscription
- **auth** — applied by: b2b-admin, internal-it, saas-subscription
- **billing** — applied by: saas-subscription
... (12 total)

## Active recipes
- **api-gateway-relay** — enabled L4: audit-log, auth, crud, scheduled-task, webhook
- **b2b-admin** — enabled L4: auth, audit-log, crud, feature-flags, notification
- **booking** — enabled L4: crud, notification, audit-log, scheduled-task
... (11 total)

## Sealed verdicts
- api-gateway-relay-verdict
- b2b-admin-verdict
- booking-verdict
... (13 total)
```

- **Usage:** `bash practices/generate_agents.sh` · **Exit codes:** 0 PASS / non-zero on shell error or assertion fail.
- **TDD anchor:**
  ```yaml
  tdd_anchor:
    test_file: "practices/evals/agents_md_toc_disk_truth_guard.sh (NEW R13 25th guard) + practices/evals/generate_agents_idempotent_check.sh (existing R7 TD-020)"
    assertion: "post-extension generate_agents.sh emits TOC after rule-concat; sentinel sha stays d367ba2f...; 2nd run zero diff; 12/11/13 counts assert; cross-link rows use comma-space join helper"
    expected_RED_reason: "TOC section absent from R12 baseline AGENTS.md"
    first_GREEN_command: "bash practices/generate_agents.sh && bash practices/generate_agents.sh && git diff --exit-code practices/AGENTS.md && bash practices/evals/agents_md_toc_disk_truth_guard.sh"
    cross_host_policy: "macOS bash 3.2 + Linux bash 5.x both PASS — verified via CI matrix on first SP51 push; if either fails, SP51 rolls back. Single-host acceptance NOT permitted (M2 dual-host gate)."
    owning_SP: "SP51"
  ```

### 4.2 Regenerated `practices/AGENTS.md` — content shape

Same as iter 1 — frontmatter sentinel UNCHANGED; rule concat UNCHANGED; `---` separator; `# Catalog TOC` section with 3 sub-sections + cross-link rows. Sentinel sha invariance: `head -5 practices/AGENTS.md` returns identical `source_concat_sha256:` as R12 baseline.

### 4.3 DECISIONS.md TD-033 — TOC extension ADR

Path: `practices/DECISIONS.md` (append-only). Entry: TD-2026-05-25-033 — see §8 ADR for full body (iter 2 wording precision per M1 / Codex hard #4).

### 4.4 25th hard guard — `agents_md_toc_disk_truth_guard.sh` (NEW)

- **Path:** `practices/evals/agents_md_toc_disk_truth_guard.sh` (≤50 LOC; illustrative shape in §5).
- **Function:** cp committed AGENTS.md → /tmp; re-run `generate_agents.sh`; `diff` whole file (idempotent check) + slice TOC body via `awk '/^# Catalog TOC/,EOF'` and re-diff defensively; restore committed AGENTS.md on failure; exit 0/1.
- **Fixture:** `practices/evals/fixtures/agents_md_toc_disk_truth/` — minimal `_MANIFEST.yaml` + 1 L4 + 1 verdict for unit smoke (Codex soft #2 negative-delimiter fixture).
- **Registration:** `practices/evals/run-all-guards.sh` appends invocation (25th line).

### 4.5 Evidence ledger

> Counting model unchanged from iter 1 §4.5 / R12 §4.5.

**4.5.A Axis A pattern precedent (3 rows, reference only):** P1 Sphinx toctree docs (`https://www.sphinx-doc.org/en/master/usage/restructuredtext/directives.html`, 200 OK 2026-05-25, "Tables of contents from all those documents are inserted...") — TD-033 design lineage. P2 R12 AGENTS.md sentinel `head -5` (disk read, `source_concat_sha256: "d367ba2f..."` + `rule_count: 86`) — invariance baseline. P3 R12 `generate_agents.sh` 42 L (disk read, rule-concat + sha-then-emit ordering) — extension anchor.

**4.5.B Axis B (BRN checksum) re-probe — gate UNMET (unchanged iter 1).** Authoritative-primary verbatim: **0**. OSS-comment below-floor pre-art: **2** (O1 `shlee8313/4_social_insurnace`, O2 `won-ktds/smarketing-frontend`). Downgrades: **9** (D1 KO 위키 underscored 404 · D2 KO 위키 alt content-gap · D3 EN BRN 404 · D4 namu.wiki 403 · D5 NTS mi=2443 no format · D6 EN NTS no format · D7 EN VAT no Korea · D8 EN TIN US-only · D9 EN RRN different family). **Gate UNMET → R14 bounded retry §10.**

**Per-axis floor check:** Axis A — owned-code; no external floor applies. Axis B — floor NOT MET (0/9/2) → R14. SP51 pre-flight re-runs P2+P3 probes one-shot; baseline drift → abort.

### 4.6 SP Plan + Verification Matrix (2 SPs — atomic-4)

| SP | Atomic deliverables | TDD anchors | Verification |
|---|---|---|---|
| **SP51** (atomic-4) | (a) `practices/generate_agents.sh` extended ≤ 50 LOC added; (b) regenerated `practices/AGENTS.md` (sentinel `d367ba2f...` UNCHANGED); (c) `practices/DECISIONS.md` append TD-033 (with explicit TD-024 amendment wording); (d) **NEW: `practices/evals/agents_md_toc_disk_truth_guard.sh` ≤ 50 LOC + fixture + run-all registration**. | Idempotency, sentinel invariance, TOC disk-truth match, 25-guard sweep. | `bash practices/generate_agents.sh && bash practices/generate_agents.sh && git diff --exit-code practices/AGENTS.md`; `head -5 practices/AGENTS.md \| grep source_concat_sha256:` = `d367ba2f...`; `bash practices/evals/agents_md_toc_disk_truth_guard.sh` exit 0; `bash practices/evals/run-all-guards.sh` exit 0 (**25 guards GREEN**); TOC inline assertions surface 12/11/13. |
| **SP52** (FINAL) | 13 sealed verdicts no-regression; `/ax-verify all` exit 0; tag policy. | Existing verdict harnesses re-run. | `/ax-verify all` exit 0; binary PASS/FAIL §6. |

**SP atomicity (one state machine):** SP51 ships all 4 atomic items OR full rollback. **No partial commits, no partial rollback.**
**SP linearization:** SP51 → SP52. No parallel branches.

---

## §5 AGENTS.md sentinel sha invariance + 25th guard (TD-024 sha-input UNCHANGED, I/O surface AMENDED)

**Resolution:** R12 baseline `source_concat_sha256: d367ba2f...` over 86-rule concat. R13 SP51 appends TOC AFTER sentinel+rule-concat. Rule concat UNCHANGED → sha UNCHANGED. **Proof of TD-024 sha-input clause honored.** TD-024 I/O surface clause IS amended (script reads 3 additional disk surfaces) — TD-033 documents explicitly.

### 25th guard shape (illustrative, ≤50 LOC bash)

```bash
#!/usr/bin/env bash
# practices/evals/agents_md_toc_disk_truth_guard.sh — R13 25th hard guard.
# Binary-verifies sha-asymmetry: L4/recipe/verdict-add must surface as TOC drift here.
set -euo pipefail
cd "$(dirname "$0")/.."   # → practices/
cp AGENTS.md /tmp/agents_md_committed.txt
bash generate_agents.sh > /tmp/agents_md_regen_stdout.log
if ! diff -q /tmp/agents_md_committed.txt AGENTS.md > /dev/null; then
    echo "FAIL: generate_agents.sh non-idempotent or TOC drift" >&2
    cp /tmp/agents_md_committed.txt AGENTS.md   # restore
    exit 1
fi
# Defensive TOC-body slice diff
awk '/^# Catalog TOC/,/^$/' /tmp/agents_md_committed.txt > /tmp/toc_committed.txt
awk '/^# Catalog TOC/,/^$/' AGENTS.md > /tmp/toc_regen.txt
if ! diff -q /tmp/toc_committed.txt /tmp/toc_regen.txt > /dev/null; then
    echo "FAIL: TOC body drift" >&2
    exit 1
fi
echo "PASS: TOC disk-truth matches committed AGENTS.md"
```

### Migration plan (within SP51)

(1) Edit `generate_agents.sh` — append TOC block AFTER rule-concat for-loop; preserve sha-compute position (~85-95 LOC). (2) Re-run; verify sentinel UNCHANGED + 12/11/13 assertions PASS + idempotency. (3) Add 25th guard + fixture; register in `run-all-guards.sh`. (4) Run 25-guard sweep. (5) Append TD-033 to DECISIONS.md (§8 wording). (6) SP51 squash-commit all 4 atomic items.

---

## §6 Autonomous Execution Safety

**Pre-flight gate (before SP51):** Re-run P2+P3 disk probes one-shot. Assert: `head -5 practices/AGENTS.md | grep source_concat_sha256:` = `d367ba2f...`; `grep -c '^  - pattern:' recipes/_MANIFEST.yaml` = 11 (**iter 2 fix — was `^  - id:`**); `ls -d templates/L4/*/ | wc -l` = 12; `ls skills/_tests/sealed-verdict/*.md | grep -v README | wc -l` = 13; `run-all-guards.sh` exit 0 (24 baseline). Abort SP51 if any fails.

**Mid-flight gate (SP51 → SP52):** `git status` clean; `generate_agents.sh && git diff --exit-code AGENTS.md` (idempotent); sentinel `head -5` = `d367ba2f...`; `grep -c '^- \*\*' AGENTS.md` ≥ 23 (12 L4 + 11 recipes bullets); `agents_md_toc_disk_truth_guard.sh` exit 0; `run-all-guards.sh` exit 0 (**all 25 GREEN**); **dual-host CI matrix gate (M2)** macOS bash 3.2 + Linux bash 5.x both PASS — single-host NOT permitted; commit message references SP51 + TD-033.

**Stop conditions (one state machine):** If sentinel-invariant + idempotent + TOC-disk-match + 25-guard sweep not reached in 3 iter cycles, **SP51 rolls back all 4 atomic items** (no partial). TD-033 records REJECTED; cycle re-plans.

**Release policy (binary):** Tag `v1.10.0-agents-toc` IFF SP52 confirms (i) `/ax-verify all` exit 0, (ii) sentinel `d367ba2f...` UNCHANGED, (iii) generator idempotent, (iv) 25 guards exit 0, (v) 13 verdicts no-regression.

**Rollback:** SP51 = single squash-mergeable commit; revert cleanly. **No destructive ops:** no `git reset --hard`, no force push.

### Partial-tag policy (atomic-4 — degenerate)

| SP52 outcome | Tag | Practices rules | Hard guards | AGENTS.md |
|---|---|---|---|---|
| 4/4 SP51 atoms + 13/13 verdicts + sentinel invariant + 25 guards GREEN | `v1.10.0-agents-toc` | 86 | **25** | sentinel `d367ba2f...` UNCHANGED; TOC appended |
| Any SP51 atom FAIL or sentinel drift or verdict regression or guard FAIL | no tag | 86 (SP51 reverted) | 24 (SP51 reverted) | sentinel `d367ba2f...` UNCHANGED; TOC reverted |

---

## §7 Pre-Mortem (5 scenarios — iter 2 adds 1)

1. **Generator mutates sentinel sha.** LOW. Impact: TD-024 sha-input violated → SP51 rollback. Mitigation: pre-flight + mid-flight verify sha = `d367ba2f...`.
2. **TOC drifts from disk truth between SP51 / SP52.** LOW (atomic-4 single commit). Impact: wrong counts. Mitigation: generator scans at runtime; 25th guard binary-verifies; idempotency catches drift.
3. **Bash glob portability — `../templates/L4/*/`.** LOW (`shopt -s nullglob`). Impact: literal `*/` row. Mitigation: nullglob + M2 dual-host CI matrix.
4. **AGENTS.md size growth.** LOW (+~80 L on ~6150). Impact: editor warnings. Mitigation: R14+ split via TD-034 if >10K.
5. **NEW iter 2 — Schema/parser drift in `_MANIFEST.yaml` (Codex G coverage).** LOW (stable R6-R12). Impact: awk returns 0 rows → empty recipe section. Mitigation: inline `[[ "$REC_COUNT" == "11" ]]` assertion fails fast; 25th guard surfaces regression on first SP51 push.

---

## §8 ADR Template (1 entry — TD-033; TD-034 deferred R14)

- **TD-2026-05-25-033 (NEW)** — AGENTS.md TOC + `generate_agents.sh` extension + `agents_md_toc_disk_truth_guard.sh` (25th hard guard).
  - **Decision:** Extend `generate_agents.sh` ≤ 50 LOC to append `# Catalog TOC` AFTER sentinel+rule-concat. Single-pass `MANIFEST_ROWS` cache parses `recipes/_MANIFEST.yaml` once; sub-sections read from cache + `templates/L4/*/README.md` + `skills/_tests/sealed-verdict/*.md`. Cross-link joins use awk helper `join_cs()` — NOT `paste -sd ', '` (Codex BLOCKING L closure). New 25th hard guard binary-verifies sha-asymmetry.
  - **TD-024 amendment (M1 / hard #4 precision):**
    - **sha-input clause UNCHANGED** — sentinel still covers `practices/rules/*.md` concat ONLY. Rule add/remove/modify triggers sha refresh; TOC-only mutations do NOT.
    - **I/O surface clause AMENDED** — generator now reads 3 additional disk surfaces (L4 README applied_recipes; manifest active recipes + enabled_l4_domains; sealed-verdict listing) to emit observability TOC outside fingerprint. Explicit documented expansion, not side effect.
  - **Drivers:** R12 §8 mandate (scope + script shape); H2 / hard #2 require binary guard.
  - **Alternatives:** option (b) full-AGENTS.md sha (REJECTED — amends sha-input); defer R13 (REJECTED — stale debt); bundle Axis B (REJECTED — gate UNMET); documented-only asymmetry (REJECTED iter 2 — fork-receiver semantic risk); hand-edited TOC (REJECTED — round-trip violation, re-opens R12 H2).
  - **Why chosen:** sha-input preserved; I/O expansion explicit (M1); ≤ 50 LOC generator + ≤ 50 LOC guard; sha-asymmetry binary-verified.
  - **Consequences:** rule add → sha refresh (R12 behavior); L4/recipe/verdict add → TOC drift WITHOUT sha refresh, surfaced by 25th guard (NEW R13+); hand-edit reverted + guard surfaces (NEW R13+); schema drift fails inline 12/11/13 assertion (NEW R13+).
  - **Follow-ups:** R14+ regen verifies guard catches drift; `## Hard guards` TOC sub-section deferred R14 TD-034 (M4 / soft #3).

**Deferred to R14:** TD-034 — (i) `korean-brn-checksum` rule (trigger: ≥1 Korean verbatim primary + ≥1 international/academic on PRD-signature day; bounded 9-source retry §10); (ii) `## Hard guards` TOC sub-section.

---

## §9 Honored Constraints

- **Caps:** Tier-1 = 4 FROZEN · Tier-2 = 8 · L1/L2/L3 = 49/92/20 · L4 = 12 · Recipes = 11 · Verdicts = 13 (all UNCHANGED).
- **Deltas:** Rules 86 → 86 · **Guards 24 → 25 (NEW: `agents_md_toc_disk_truth_guard.sh`)** · Sentinel `d367ba2f...` UNCHANGED · AGENTS.md gains TOC AFTER sentinel · DECISIONS.md +1 TD (TD-033).
- **Atomic SP rule** (R6/R10/R12 precedent — SP51 **atomic-4** + SP52 FINAL).
- **TD-024 sha-input UNCHANGED; I/O surface AMENDED** (3 additional disk surfaces — see TD-033). **TD-027 2-consumer gate UNCHANGED** (zero new L4).
- **Axis B Korean references:** 0 verbatim primary; 9 downgrades; 2 OSS-comment below-floor pre-art. **R14 bounded retry §10.**
- **B1 ↔ B2 decoupling, `quote_match_check.sh` coverage, `deferred_recipes:` queue** — all unchanged from R12.
- **CLOSED:** no new L4/L3/L2/L1/recipe/verdict/Tier-1/Tier-2; no 휴대폰 본인인증; no fork-receiver `inspect.sh`; no `## Hard guards` TOC sub-section (R14 TD-034).

---

## §10 Out-of-scope (R13 explicit) + Deferred Items

**Deferred recipes:** `recipes/_MANIFEST.yaml#deferred_recipes:` stays `[]` post-SP51.

**Deferred Korean rule candidates (R14+):**

| Candidate | Trigger to ship | R13 2026-05-25 probe |
|---|---|---|
| `korean-brn-checksum` (mod-10 weighted-sum) | ≥1 verbatim Korean authoritative primary (NTS notice / academic paper / standards doc / TTA spec) AND ≥1 international/academic on PRD-signature day | UNMET — 0 primary; 9 downgrades; 2 OSS-comment below-floor |
| `korean-phone-verification-pass-flow` | KISA + 토스 PASS docs verbatim reachable | not re-probed R13 |
| `kakao-alimtalk-template-authorization` | kakao.business.api docs verbatim reachable | not re-probed R13 |
| `naver-id-oauth-scope` | developers.naver.com verbatim reachable | not re-probed R13 |

**R14 BRN bounded retry plan (Codex soft #4 — bounded, not open-ended):**

R14 PRD signature re-probes **exactly the fixed 9-source list below** (5 Korean primary + 4 international). R15 retries same list; **R16 UNMET → escalate to Architect rigor-floor downgrade vote** (no silent "retries continue forever").

**Fixed retry source list (lock R14/R15):**

1. NTS archive `https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do` (cntntsId 7770-7790 sweep).
2. TTA `https://www.tta.or.kr` (search "사업자등록번호" / "검증").
3. KISA identity-verification reference docs.
4. 위키백과 사업자등록번호 (community update check).
5. law.go.kr 부가가치세법 시행령 (longer-timeout retry).
6. ISO/IEC 7064 (check-digit standards).
7. IEEE / ACM — Korean check-digit algorithm papers.
8. GS1 Korea identifier specifications.
9. en.wikipedia VAT_identification_number (Korea-row community update check).

OSS-comment rows (O1+O2) preserved as pre-art; never authoritative on their own. R14 ships Axis B as 1-SP standalone (atomic-2: rule + evidence) IF gate cleared.

**Out-of-scope (R13):** new L1/L2/L3/L4 · new Tier-1/Tier-2 skill · new recipe · new sealed verdict · new rule · BRN checksum (R14+) · `quote_match_check.sh` extension (R14+) · 휴대폰 본인인증 PASS rule (R14+) · frontend/backend code mutations · deployment/CI/release policy · fork-receiver `inspect.sh` (R14+) · rate-limit L4 promotion (TD-028) · `## Hard guards` TOC sub-section (R14 TD-034).

---

## §11 Disposition table — Architect + Codex iter 1 findings (iter 2 NEW)

Codex hard #1-#4 alias to Architect H1 / H2 / BLOCKING L / M1 respectively (single closure row each).

| # | Finding | Severity | Iter 2 disposition | PRD reference |
|---|---|---|---|---|
| 1 | **Architect H1 / Codex hard #1** — §4.1 awk parses `active_recipes:` / `- id:`; disk has `recipes:` / `- pattern:` | HIGH | **CLOSED.** §4.1 awk rewritten to `/^recipes:/` + `/^  - pattern:/`. §6 pre-flight `grep -c '^  - pattern:'` = 11. Inline `REC_COUNT == 11` assertion. SP51 pre-flight `grep -c '^- \*\*' AGENTS.md` ≥ 23. | §2 + §4.1 + §6 + §7 P5 |
| 2 | **Architect H2 / Codex hard #2** — sha-asymmetry no binary guard | HIGH | **CLOSED.** New 25th hard guard added as SP51 atomic-4 deliverable (d). 24 → 25. | §1 + §3 + §4.4 + §4.6 + §5 + §6 + §9 |
| 3 | **Codex BLOCKING L / hard #3** — `paste -sd ', '` cycles delimiter chars, producing malformed tokens `a,b c,d` | BLOCKING | **CLOSED.** Replaced with awk join helper `join_cs()` (Codex recommended option b — pure awk, deterministic). Sample TOC output in §4.1 shows correct comma-space joins. | §4.1 generator + §4.1 sample TOC |
| 4 | **Architect M1 / Codex hard #4** — TD-033 wording softens TD-024 I/O expansion | MEDIUM | **CLOSED.** Explicit wording "TD-024 sha-input clause UNCHANGED; I/O surface clause AMENDED" throughout §1 / §8 / §9. | §1 + §5 + §8 + §9 |
| 5 | **Architect M2** — macOS bash 3.2 vs Linux 5.x dual-host | MEDIUM | **CLOSED.** §4.1 TDD anchor adds `cross_host_policy:` (CI matrix; single-host NOT permitted); §6 mid-flight dual-host gate. | §4.1 TDD anchor + §6 |
| 6 | **Architect M3** — TD-033 vs in-place TD-024 amendment | MEDIUM | **CLOSED.** TD-033 chosen (R7-R12 precedent); §8 Decision body references TD-024 directly. | §8 |
| 7 | **Architect M4 / Codex soft #3** — `## Hard guards` TOC sub-section | LOW | **DEFERRED R14 TD-034.** | §3 Must NOT + §8 follow-ups + §10 |
| 8 | **Codex soft #1** — single manifest parse pass | SOFT | **APPLIED.** `MANIFEST_ROWS` cache (`pat\|spec\|verdict\|doms`); no path re-derivation. | §4.1 Section B |
| 9 | **Codex soft #2** — negative-delimiter test fixture | SOFT | **APPLIED.** 25th guard fixture `practices/evals/fixtures/agents_md_toc_disk_truth/` surfaces `paste -sd ', '` regression if reintroduced. | §4.4 + §5 |
| 10 | **Codex soft #4** — bounded R14 BRN retry source list | SOFT | **APPLIED.** §10 fixes 9-source list (5 Korean primary + 4 international); R14/R15 same list; R16 escalates to rigor-floor downgrade vote. | §10 |
| 11 | **R12 Architect H1** (B1 mod-10) — carry-over | DEFERRED | **CARRIED to R14** with bounded retry §10. | §4.5.B + §10 |
| 12 | **R12 Architect H2** (Axis C amends TD-024 without script shape) — carry-over | CLOSED | **CLOSED via option (a) sha-input + 25th guard.** | §1 Driver 1 + §4.1 + §5 + §8 |
| 13 | **R12 §10 deferred BRN checksum** — carry-over | DEFERRED | Same as R12 H1 (row 11). | §10 |

---

## §12 Verdict line

R13 iter 2 ships **single-axis Axis A only**: 1 generator-script extension (`practices/generate_agents.sh` 42 → ~85-95 LOC; disk-validated parsers + awk join helper) + regenerated `practices/AGENTS.md` with TOC AFTER sentinel (sentinel `d367ba2f...` UNCHANGED — proof of TD-024 sha-input honored) + DECISIONS.md TD-033 ADR (with explicit TD-024 I/O surface amendment wording) + **NEW 25th hard guard** `practices/evals/agents_md_toc_disk_truth_guard.sh` (binary-verifies sha-asymmetry). **SP51 atomic-4 one-state-machine all-or-rollback.** SP52 FINAL re-runs 13 sealed verdicts + `/ax-verify all` + tag policy. **Hard guards 24 → 25.** 2 SPs ≈ 1-2 d. 1 ADR (TD-033); TD-034 deferred R14+. **Architect H1 + H2 + M1-M4 + Codex BLOCKING L + 4 hard + 4 soft all dispositioned (§11).**

---

## RALPLAN-DR Summary

**Cycle:** R13 iter 2 — TOC + generator extension + 25th hard guard. **Mode:** SHORT. **Recommended:** Option 2 — SP51 atomic-4 + SP52 FINAL. **Wall-time:** ≈ 1-2 d.

**Drivers (top 3):** TD-032 closure (R12 deferred); Axis B gate UNMET → R14; sha-asymmetry deserves binary guard (Architect H2 / Codex hard #2).

**Evidence floor:** Axis A = owned-code (no external floor); Axis B = floor UNMET (0/9/2) → R14 bounded retry §10.

**ADR (TD-033):** Extend `generate_agents.sh` (TOC AFTER sentinel + single manifest pass + awk join helper) + add 25th guard. **TD-024 sha-input UNCHANGED; I/O surface AMENDED.** Alternatives rejected: option (b) full-AGENTS.md sha; defer R13; bundle Axis B; documented-only asymmetry; hand-edited TOC.

**TD-034 deferred R14+:** `korean-brn-checksum` rule (9-source bounded retry); `## Hard guards` TOC sub-section (M4 / Codex soft #3).

**Ready for:** Architect iter 2 re-review → Codex iter 2 re-review → APPROVE or iter 3.

---

## Iter 2 changelog

- **§1 cycle frame + principles + drivers** — 24 → 25 guards; TD-024 sha-input/I-O-surface split; atomic-3 → atomic-4. (H2 / hard #2 / M1 / M3)
- **§2 context** — manifest schema excerpt + L4 frontmatter excerpt; re-verified 12/11/13. (H1)
- **§3 Must Have / Must NOT Have** — added 25th guard; awk join helper mandated; `## Hard guards` TOC excluded R14 TD-034. (H2 / L / M4 / soft #3)
- **§4.1 generator REWRITTEN** — awk `^recipes:` / `^  - pattern:` (H1); `MANIFEST_ROWS` single-pass cache (soft #1); `join_cs()` replaces `paste -sd ', '` (L / hard #3); inline 12/11/13 assertions; 3-row sample TOC.
- **§4.1 TDD anchor** — `cross_host_policy:` macOS 3.2 + Linux 5.x dual-host (M2).
- **§4.4 NEW** — 25th hard guard + fixture (H2 / soft #2).
- **§4.6 SP table** — atomic-4; 25-guard sweep verification.
- **§5** — rewritten "sentinel + 25th guard"; ≤50 LOC guard shape.
- **§6** — `^  - id:` → `^  - pattern:` (H1); mid-flight dual-host gate (M2); 24 → 25 partial-tag table.
- **§7 P5 NEW** — schema/parser drift in `_MANIFEST.yaml` (Codex G coverage).
- **§8 TD-033** — explicit TD-024 sha-input/I-O wording (M1 / hard #4); references TD-024 directly (M3); 2 new REJECTED alternatives.
- **§9** — 24 → 25; TD-024 split wording; `## Hard guards` TOC deferred.
- **§10 R14 retry** — open-ended → **bounded 9-source list**; R16 rigor-floor downgrade vote (soft #4).
- **§11 NEW disposition** — 13 rows covering H1 / H2 / L / M1-M4 / soft #1-#4 / 3 R12 carry-over (Codex criterion I FAIL closure).
- **§12 + RALPLAN-DR summary** — refreshed to atomic-4 + 25 guards.
- **Line count:** iter 1 532 L → iter 2 ~540 L (within target 480-540).
