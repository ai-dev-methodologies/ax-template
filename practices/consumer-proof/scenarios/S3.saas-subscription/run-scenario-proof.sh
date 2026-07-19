#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S3.saas-subscription/run-scenario-proof.sh
#
# DOGFOOD cell S3.saas-subscription — SaaS SUBSCRIPTION dashboard UI vertical
# slice (plan cards, usage widget, upgrade CTA) as a Next App-Router client
# route, composed from catalog L2 blocks (pricing-table, usage-meter,
# billing-history, pagination) + the L0 use-url-list-state hook.
#
# Proves the catalog composition mechanically BLOCKS four realistic
# AI-generated rule violations named by this dogfood brief, and lets the
# correct rewrite through:
#   1. god-route            — a 150+-line "use client" src/app/subscription/page.tsx
#                              absorbing plan-change/usage/history logic inline
#                              instead of delegating to @/features/subscription
#   2. server-state          — useState(useSWR(...).data) mirroring the
#                              current-plan query result into local state
#   3. array-mutate          — .push on a useState-derived array, then setState
#                              with the SAME reference
#   4. pagination-envelope-contract-parity — the brief's ADDITIONAL
#                              REQUIREMENT: an FE pagination-response parser
#                              that guesses a legacy/flat shape instead of the
#                              REAL BE PageEnvelope.java canonical envelope
#                              (data + pagination.{page,pageSize,
#                              totalElements,totalPages,hasMore})
#
# Cases 1-3 reuse the catalog's OWN @ax/eslint-plugin-ax, installed exactly as
# practices/consumer-proof/run-consumer-proof.sh's Lane A does (same
# REACT_DIR node_modules, same eslint.config.mjs, same
# EXPECTED-exit-1-plus-signature harness contract — see that script + its
# README.md for the full rationale). Case 4 is HAND-ROLLED (confirmed catalog
# gap — see scenario-guards/pagination_envelope_contract_parity.sh's header)
# and reuses the SAME contract manually: a violating fixture counts as
# BLOCKED only when its guard exits EXACTLY 1 AND the captured output
# contains the INTENDED signature string; a clean fixture must exist, be
# actually scanned, and exit EXACTLY 0; every expected case MUST run
# (cardinality gate).
#
# ISOLATION: everything this script touches lives under this scenario dir
# (react/, scenario-guards/) plus READ-ONLY use of the sibling
# practices/consumer-proof/react/node_modules (@ax/eslint-plugin-ax already
# installed there for the main harness) and a READ-ONLY read of the real
# backend/src/.../common/PageEnvelope.java for case 4's cross-boundary check.
# Nothing here edits backend/src or frontend/src, and this scenario is NOT
# wired into run-all-guards.sh or R25 — it is a standalone probe, run
# manually.
#
# Usage: bash practices/consumer-proof/scenarios/S3.saas-subscription/run-scenario-proof.sh
# Exit:  0 = proof holds · 1 = proof FALSIFIED / a case could not run.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
REACT_DIR="$REPO_ROOT/practices/consumer-proof/react"
SG="$SCRIPT_DIR/scenario-guards"

VR_REACT="$SCRIPT_DIR/react/violating"
CR_REACT="$SCRIPT_DIR/react/clean"

PASS=0
FAIL=0
FAILED_CASES=""
EXPECTED_TOTAL=4
ran=0

green() { printf '\033[32m%s\033[0m' "$1"; }
red()   { printf '\033[31m%s\033[0m' "$1"; }

record_case() {
    local label="$1" ok="$2" detail="$3"
    if [ "$ok" -eq 0 ]; then
        PASS=$((PASS + 1))
        printf '  [%s] %-46s %s\n' "$(green PASS)" "$label" "$detail"
    else
        FAIL=$((FAIL + 1))
        FAILED_CASES="$FAILED_CASES
    - $label ($detail)"
        printf '  [%s] %-46s %s\n' "$(red FAIL)" "$label" "$detail"
    fi
}

echo "══════════════════════════════════════════════════════════════════════════"
echo " S3.saas-subscription — consumer-proof scenario harness"
echo " SaaS SUBSCRIPTION dashboard slice: plan cards -> usage widget -> upgrade CTA"
echo "══════════════════════════════════════════════════════════════════════════"

# ── LANE A — React / @ax/eslint-plugin-ax (catalog-reused) ─────────────────
LANE_A_MISSING=""
if ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1; then
    LANE_A_MISSING="node/npm not on PATH"
elif [ ! -d "$REACT_DIR/node_modules" ]; then
    echo "── Lane A deps not installed — installing now (shared with the main harness) ──"
    if ! ( cd "$REACT_DIR" && npm install --no-audit --no-fund >/dev/null 2>&1 ); then
        LANE_A_MISSING="npm install failed in $REACT_DIR"
    fi
fi
if [ -z "$LANE_A_MISSING" ] && [ ! -x "$REACT_DIR/node_modules/.bin/eslint" ]; then
    LANE_A_MISSING="eslint binary not found at $REACT_DIR/node_modules/.bin/eslint"
fi

LINT_OUT="$(mktemp)"
LINT_ERR="$(mktemp)"
NODE_HELPER="$(mktemp --suffix=.mjs 2>/dev/null || mktemp).mjs"
mv "${NODE_HELPER%.mjs}" "$NODE_HELPER" 2>/dev/null || true
cat > "$NODE_HELPER" <<'NODEJS'
// Parses eslint --format json output and checks for the intended ax/<rule> signature.
import fs from 'node:fs'
const mode = process.argv[2]
const jsonFile = process.argv[3]
let data
try {
  data = JSON.parse(fs.readFileSync(jsonFile, 'utf8'))
} catch (e) {
  console.log('ENVERR eslint-json-unparseable (' + (e && e.message) + ')')
  process.exit(2)
}
if (!Array.isArray(data)) { console.log('ENVERR eslint-json-not-an-array'); process.exit(2) }

if (mode === 'violating') {
  const ruleId = process.argv[4]
  const rules = new Set()
  let fatal = false
  for (const file of data) {
    for (const msg of file.messages || []) {
      if (msg.fatal) fatal = true
      if (msg.ruleId) rules.add(msg.ruleId)
    }
  }
  if (fatal) { console.log('FAIL fatal-parse-error'); process.exit(2) }
  if (!rules.has(ruleId)) {
    console.log('FAIL intended-signature-absent wanted=' + ruleId + ' saw=' + ([...rules].join('|') || '<none>'))
    process.exit(1)
  }
  console.log('scanned=' + data.length + ' signature=' + ruleId + '=present')
  process.exit(0)
} else if (mode === 'clean') {
  const wanted = process.argv.slice(4)
  const seenFiles = new Set(data.map((f) => f.filePath))
  const missing = []
  for (const w of wanted) {
    const hit = [...seenFiles].some((f) => f.replace(/\\/g, '/').endsWith(w))
    if (!hit) missing.push(w)
  }
  if (missing.length > 0) {
    console.log('ENVERR clean-file-not-scanned ' + missing.join(','))
    process.exit(2)
  }
  console.log('scanned=' + data.length + ' files, zero violations')
  process.exit(0)
}
console.log('ENVERR unknown-mode ' + mode)
process.exit(2)
NODEJS

# Fixtures live OUTSIDE $REACT_DIR (this scenario is isolated in its own
# dir), and ESLint 9's flat config refuses to lint a file outside its config's
# base path when invoked via `cwd`-relative `npx eslint` (verified: it emits
# "File ignored because outside of base path." with a zero exit — a SILENT
# false-clean, not a real proof). Point the REACT_DIR-installed eslint binary
# at the REACT_DIR config file EXPLICITLY via --config so the base-path check
# is satisfied while the plugin resolution (relative to the config file) is
# unchanged — this still reuses the exact SAME shared install/config as the
# main harness, just invoked correctly for an out-of-tree fixture.
ESLINT_BIN="$REACT_DIR/node_modules/.bin/eslint"
lint_json() {
    LINT_RC=0
    "$ESLINT_BIN" --format json --config "$REACT_DIR/eslint.config.mjs" "$@" >"$LINT_OUT" 2>"$LINT_ERR" || LINT_RC=$?
}

echo
echo "── LANE A: React / @ax/eslint-plugin-ax (catalog-reused) ──"
if [ -n "$LANE_A_MISSING" ]; then
    ran=$((ran + 3))
    record_case "A/violating: god-route" 1 "Lane A CANNOT run: $LANE_A_MISSING"
    record_case "A/violating: server-state" 1 "Lane A CANNOT run: $LANE_A_MISSING"
    record_case "A/violating: array-mutate" 1 "Lane A CANNOT run: $LANE_A_MISSING"
else
    LANE_A_VIOLATING="\
$VR_REACT/src/app/subscription/page.tsx|ax/no-god-route
$VR_REACT/server-state.tsx|ax/no-server-state-in-local-state
$VR_REACT/array-mutate.tsx|ax/no-array-mutate-on-state"

    OLD_IFS="$IFS"
    IFS='
'
    for entry in $LANE_A_VIOLATING; do
        [ -n "$entry" ] || continue
        relpath="${entry%%|*}"
        rule_id="${entry##*|}"
        ran=$((ran + 1))
        label="A/violating: $(basename "$relpath")"

        if [ ! -s "$relpath" ]; then
            record_case "$label" 1 "MISSING violating fixture: $relpath"
            continue
        fi

        lint_json "$relpath"
        if [ "$LINT_RC" -eq 2 ]; then
            record_case "$label" 1 "ENVIRONMENT ERROR — eslint exit 2 (config/internal), NOT a lint block (wanted exit 1 + $rule_id)"
            continue
        fi
        if [ "$LINT_RC" -ne 1 ]; then
            record_case "$label" 1 "NOT blocked — eslint exit $LINT_RC (expected 1=lint failure; 0=slipped through)"
            continue
        fi
        vrc=0
        verdict="$(node "$NODE_HELPER" violating "$LINT_OUT" "$rule_id")" || vrc=$?
        case "$vrc" in
            0) record_case "$label" 0 "blocked by $rule_id (eslint exit 1; $verdict)" ;;
            2) record_case "$label" 1 "ENVIRONMENT ERROR — $verdict" ;;
            *) record_case "$label" 1 "NOT blocked / wrong signature — $verdict" ;;
        esac
    done
    IFS="$OLD_IFS"

    CLEAN_FILES="$CR_REACT/src/app/subscription/page.tsx $CR_REACT/server-state.tsx $CR_REACT/array-mutate.tsx"
    for cf in $CLEAN_FILES; do
        if [ ! -s "$cf" ]; then
            record_case "A/clean: $(basename "$cf")" 1 "MISSING clean fixture: $cf"
        else
            lint_json "$cf"
            if [ "$LINT_RC" -eq 2 ]; then
                record_case "A/clean: $(basename "$cf")" 1 "ENVIRONMENT ERROR — eslint exit 2"
            elif [ "$LINT_RC" -ne 0 ]; then
                record_case "A/clean: $(basename "$cf")" 1 "clean fixture was flagged (eslint exit $LINT_RC)"
                sed 's/^/      /' "$LINT_ERR"
            else
                record_case "A/clean: $(basename "$cf")" 0 "passes (exit 0)"
            fi
        fi
    done
fi
rm -f "$LINT_OUT" "$LINT_ERR" "$NODE_HELPER"

# ── LANE B — hand-rolled: pagination-envelope contract-parity ──────────────
echo
echo "── LANE B: pagination-envelope contract-parity (HAND-ROLLED — confirmed catalog gap) ──"
ran=$((ran + 1))
GUARD="$SG/pagination_envelope_contract_parity.sh"
if [ ! -f "$GUARD" ]; then
    record_case "B/pagination-envelope-contract-parity" 1 "guard script not found: $GUARD"
else
    vc=0; vout="$(bash "$GUARD" --root "$VR_REACT" 2>&1)" || vc=$?
    cc=0; cout="$(bash "$GUARD" --root "$CR_REACT" 2>&1)" || cc=$?

    if [ "$vc" -eq 2 ] || [ "$cc" -eq 2 ]; then
        record_case "B/pagination-envelope-contract-parity" 1 "ENVIRONMENT ERROR — guard exited 2 (violating=$vc clean=$cc)"
    elif [ "$vc" -gt 1 ] || [ "$cc" -gt 1 ]; then
        record_case "B/pagination-envelope-contract-parity" 1 "ENVIRONMENT ERROR — unexpected exit (violating=$vc clean=$cc)"
    else
        signature="PAGINATION_ENVELOPE_CONTRACT_MISMATCH"
        v_blocked=1
        if [ "$vc" -eq 1 ] && printf '%s' "$vout" | grep -qF -- "$signature"; then
            v_blocked=0
        fi
        clean_ok=0 clean_why=""
        if [ "$cc" -ne 0 ]; then
            clean_ok=1; clean_why="clean was BLOCKED (exit $cc): $(printf '%s' "$cout" | tail -3 | tr '\n' ' ')"
        fi
        if [ "$clean_ok" -eq 0 ] && ! printf '%s' "$cout" | grep -q "scanned "; then
            clean_ok=1; clean_why="VACUOUS clean pass — no positive-scan evidence in output"
        fi

        if [ "$v_blocked" -eq 0 ] && [ "$clean_ok" -eq 0 ]; then
            record_case "B/pagination-envelope-contract-parity" 0 "violating BLOCKED (exit 1, signature '$signature'), clean scanned+PASS (exit 0)"
        else
            why=""
            if [ "$v_blocked" -ne 0 ]; then
                if [ "$vc" -eq 0 ]; then
                    why="violating SLIPPED THROUGH (exit 0)"
                else
                    why="violating exit $vc but signature '$signature' ABSENT"
                fi
            fi
            [ "$clean_ok" -ne 0 ] && why="${why:+$why; }$clean_why"
            record_case "B/pagination-envelope-contract-parity" 1 "$why"
        fi
    fi
fi

echo
echo "══════════════════════════════════════════════════════════════════════════"
if [ "$ran" -ne "$EXPECTED_TOTAL" ]; then
    echo "CARDINALITY GATE FAILED: expected $EXPECTED_TOTAL cases, ran $ran"
    exit 1
fi
echo "Cases: $PASS passed, $FAIL failed (of $EXPECTED_TOTAL expected)"
if [ "$FAIL" -gt 0 ]; then
    echo "PROOF FALSIFIED. Failing cases:$FAILED_CASES"
    exit 1
fi
echo "PROOF HOLDS — S3.saas-subscription composition mechanically blocks every"
echo "named violation and lets every clean rewrite through, non-vacuously."
exit 0
