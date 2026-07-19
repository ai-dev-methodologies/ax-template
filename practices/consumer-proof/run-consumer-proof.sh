#!/usr/bin/env bash
# practices/consumer-proof/run-consumer-proof.sh
#
# ADVERSARIAL CONSUMER-PROOF HARNESS  (hardened — CANNOT silently pass)
# --------------------------------------------------------------------
# Proves ax-template's catalog MECHANICALLY BLOCKS rule-violating code in a
# consumer context, and does NOT block the correct rewrite. The proof is
# falsifiable AND non-vacuous: exit 0 is reachable ONLY when
#   * EVERY expected case genuinely ran (cardinality gate — no dropped rows),
#   * EVERY violating fixture was blocked BY ITS INTENDED VIOLATION (the
#     EXPECTED-BLOCK exit code AND the intended rule/guard signature — never a
#     config/internal error that merely names the rule, never any-non-zero), and
#   * EVERY clean fixture EXISTS + was actually SCANNED (positive scan count) +
#     produced no violation.
# Any deviation → loud FAIL (exit 1) naming the case.
#
# Lane A — React/ESLint  (@ax/eslint-plugin-ax): the three AST-shape rules
#          (no-array-mutate-on-state, prefer-functional-setstate,
#          no-server-state-in-local-state) are path-agnostic — they fire on
#          React/TSX regardless of directory layout. The route/layer rule
#          no-god-route additionally requires the Next.js App-Router path
#          convention (src/app/**/(page|layout).*), so its fixture lives under a
#          real src/app/dashboard/ path. (See README.md for the per-rule scope.)
# Lane B — Java/Spring shell guards: convention-path coupling. The consumer must
#          adopt the repo's package path (com.ax.template.authblueprint) for the
#          guards' --root scan to reach the code — honest about that coupling.
#
# This is a STANDALONE probe. It is NOT wired into R25 (keeps R25 fast +
# dependency-light). Run it manually or as a CI probe (see README.md).
#
# Usage:  bash practices/consumer-proof/run-consumer-proof.sh
# Exit:   0 = proof holds · 1 = proof FALSIFIED, a lane could not run, or a
#             cardinality/environment error made the result untrustworthy.
#
# SHELL MODE — deliberately `set -uo pipefail`, NOT `-e`.
# This harness INTENTIONALLY runs commands that exit non-zero (violating
# fixtures MUST drive their gate to a non-zero exit) and captures every exit
# code explicitly with the `rc=0; cmd || rc=$?` pattern. `set -e` would abort
# the run mid-proof on those intended non-zero captures — exactly the caveat
# against which the hardening spec warns. The silent-green failure mode that
# `-e` guards against is instead closed HERE by the mandatory CARDINALITY GATE:
# exit 0 is unreachable unless every expected case was recorded. Bash 3.2
# compatible (macOS /bin/bash).
set -uo pipefail

# ── self-locate ──────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
EVALS="$REPO_ROOT/practices/evals"
REACT_DIR="$SCRIPT_DIR/react"
JAVA_DIR="$SCRIPT_DIR/java"
PKG_SUFFIX="backend/src/main/java/com/ax/template/authblueprint"

PASS=0
FAIL=0
FAILED_CASES=""

# Cardinality bookkeeping (finding 2). These are CONSTANTS: the proof knows
# exactly how many cases MUST run. If fewer run (a case row was dropped, a
# fixture list came up empty, a lane could not start), the run is NOT a full
# proof and the cardinality gate fails it loudly.
EXPECTED_LANE_A=5          # 4 violating fixtures + 1 clean-aggregate case
EXPECTED_LANE_B=5          # 5 shell guards
EXPECTED_TOTAL=$((EXPECTED_LANE_A + EXPECTED_LANE_B))   # 10
ran_a=0
ran_b=0
lane_a_started=0

green() { printf '\033[32m%s\033[0m' "$1"; }
red()   { printf '\033[31m%s\033[0m' "$1"; }

# record_case <label> <expectation-met? 0/1> <detail>
record_case() {
    local label="$1" ok="$2" detail="$3"
    if [ "$ok" -eq 0 ]; then
        PASS=$((PASS + 1))
        printf '  [%s] %-48s %s\n' "$(green PASS)" "$label" "$detail"
    else
        FAIL=$((FAIL + 1))
        FAILED_CASES="$FAILED_CASES
    - $label ($detail)"
        printf '  [%s] %-48s %s\n' "$(red FAIL)" "$label" "$detail"
    fi
}

# ── scratch helper + temp capture files (never committed; removed on exit) ────
NODE_HELPER="$(mktemp "${TMPDIR:-/tmp}/axcproof-helper.XXXXXX")"
LINT_OUT="$(mktemp "${TMPDIR:-/tmp}/axcproof-lint-out.XXXXXX")"
LINT_ERR="$(mktemp "${TMPDIR:-/tmp}/axcproof-lint-err.XXXXXX")"
cleanup() { rm -f "$NODE_HELPER" "$LINT_OUT" "$LINT_ERR"; }
trap cleanup EXIT

# The Node verdict helper parses eslint's --format json output. It is the ONLY
# thing that credits a Lane A block, and it credits ONLY the intended rule id
# with NO fatal (parse-error / rule-crash) messages present.
#   exit 0 = OK  ·  exit 1 = FAIL (proof falsified)  ·  exit 2 = ENVIRONMENT ERROR
cat > "$NODE_HELPER" <<'NODEJS'
'use strict';
const fs = require('fs');
const mode = process.argv[2];
const jsonFile = process.argv[3];
let data;
try {
  data = JSON.parse(fs.readFileSync(jsonFile, 'utf8'));
} catch (e) {
  console.log('ENVERR eslint-json-unparseable (' + (e && e.message) + ')');
  process.exit(2);
}
if (!Array.isArray(data)) { console.log('ENVERR eslint-json-not-an-array'); process.exit(2); }
const norm = (p) => String(p).replace(/\\/g, '/');
const fatal = data.reduce((s, f) => s + (f.fatalErrorCount || 0), 0);

if (mode === 'violating') {
  const ruleId = process.argv[4];
  if (data.length < 1) { console.log('FAIL zero-files-scanned'); process.exit(1); }
  // A fatal message = a parse error or a rule crash. eslint reports these with
  // exit 1 too, so exit-code alone cannot distinguish "blocked by the rule"
  // from "the file/tooling exploded". Reject them as ENVIRONMENT errors.
  if (fatal > 0) { console.log('ENVERR fatal-message-present (parse-error-or-rule-crash), not a rule block'); process.exit(2); }
  const rules = new Set();
  data.forEach((f) => (f.messages || []).forEach((m) => { if (m.ruleId) rules.add(m.ruleId); }));
  if (!rules.has(ruleId)) {
    console.log('FAIL intended-signature-absent wanted=' + ruleId + ' saw=' + ([...rules].join('|') || '<none>'));
    process.exit(1);
  }
  console.log('scanned=' + data.length + ' signature=' + ruleId + '=present');
  process.exit(0);
}

if (mode === 'clean') {
  const wants = process.argv.slice(4);           // repo-relative path suffixes
  if (fatal > 0) { console.log('ENVERR fatal-message-present scanning clean set'); process.exit(2); }
  if (data.length < wants.length) {
    console.log('FAIL scanned-fewer-files-than-expected scanned=' + data.length + ' expected>=' + wants.length);
    process.exit(1);
  }
  const missing = [];
  const dirty = [];
  for (const w of wants) {
    const hit = data.find((f) => norm(f.filePath).endsWith(w));
    if (!hit) { missing.push(w); continue; }          // existed on disk but eslint did not lint it
    if ((hit.errorCount || 0) !== 0 || (hit.warningCount || 0) !== 0) dirty.push(w);
  }
  if (missing.length) { console.log('FAIL clean-fixture-not-scanned:' + missing.join(',')); process.exit(1); }
  if (dirty.length) { console.log('FAIL clean-fixture-flagged:' + dirty.join(',')); process.exit(1); }
  console.log('scanned=' + data.length + ' all-clean (>=' + wants.length + ' expected fixtures linted, 0 findings)');
  process.exit(0);
}

console.log('ENVERR unknown-mode:' + mode);
process.exit(2);
NODEJS

echo "=================================================================="
echo " ax-template — ADVERSARIAL CONSUMER-PROOF (hardened)"
echo " (violating -> MUST block by its intended defect · clean -> MUST"
echo "  exist + be scanned + pass · every expected case MUST run)"
echo "=================================================================="

# ══════════════════════════════════════════════════════════════════════════════
# LANE A — React / ESLint
# ══════════════════════════════════════════════════════════════════════════════
echo
echo "── LANE A: React / @ax/eslint-plugin-ax ──"

# Explicit, in-script case arrays (finding 2 — NO fragile heredoc/while-read
# feed that can silently drop a row). Each violating entry is  relpath|rule-id.
LANE_A_VIOLATING="\
fixtures/violating/array-mutate.tsx|ax/no-array-mutate-on-state
fixtures/violating/functional-setstate.tsx|ax/prefer-functional-setstate
fixtures/violating/server-state.tsx|ax/no-server-state-in-local-state
fixtures/violating/src/app/dashboard/page.tsx|ax/no-god-route"

# Clean fixtures that MUST exist + be scanned (finding 3). Deleting any one
# fails the proof (existence check below + Node 'not-scanned' backstop).
LANE_A_CLEAN_FILES="\
fixtures/clean/array-mutate.tsx
fixtures/clean/functional-setstate.tsx
fixtures/clean/server-state.tsx
fixtures/clean/src/app/dashboard/page.tsx"

# lint_json <relpath-under-REACT_DIR-or-absolute...>  → JSON to $LINT_OUT,
# stderr to $LINT_ERR, sets $LINT_RC.
#
# --config IS PINNED EXPLICITLY, and cwd IS PINNED TO $REPO_ROOT (do not
# remove either — confirmed reliability gap). ESLint 9's flat config computes
# its "base path" from cwd (NOT from the config file's own directory), and
# silently exits 0 with "File ignored because outside of base path." for any
# target outside that base path — EVEN WITH --config pointing elsewhere.
# `cd $REACT_DIR && npx eslint <relpath>` therefore silently false-cleans any
# fixture that lives outside $REACT_DIR (e.g. a scenario's own fixture tree
# under practices/consumer-proof/scenarios/**) — a SILENT false-clean that
# would credit an unblocked violating variant as "passing" with zero
# indication anything was skipped.
#
# Fix: run with cwd = $REPO_ROOT (an ancestor of every fixture tree in this
# repo, in-tree or out-of-tree) and resolve each arg to an absolute path
# before invoking eslint, so the base-path check always succeeds regardless
# of where the target actually lives. Proven RED-able by
# practices/consumer-proof/engine/fixtures/harness-config-pin/prove-config-pin.sh
lint_json() {
    LINT_RC=0
    local abs_args=() a
    for a in "$@"; do
        case "$a" in
            /*) abs_args+=("$a") ;;
            *)  abs_args+=("$REACT_DIR/$a") ;;
        esac
    done
    ( cd "$REPO_ROOT" && npx --prefix "$REACT_DIR" eslint --format json --config "$REACT_DIR/eslint.config.mjs" "${abs_args[@]}" ) >"$LINT_OUT" 2>"$LINT_ERR" || LINT_RC=$?
}

if ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1; then
    echo "  Lane A CANNOT run: node/npm not on PATH."
    echo "  Install Node >=20 and run: (cd $REACT_DIR && npm install)"
elif [ ! -d "$REACT_DIR/node_modules" ]; then
    echo "  Lane A deps not installed — installing now..."
    if ( cd "$REACT_DIR" && npm install --no-audit --no-fund >/dev/null 2>&1 ); then
        echo "  npm install OK"
    else
        echo "  Lane A CANNOT run: npm install failed."
    fi
fi

if [ -d "$REACT_DIR/node_modules" ]; then
    lane_a_started=1

    # Sanity: the case list must be non-empty before looping (finding 2).
    if [ -z "$LANE_A_VIOLATING" ]; then
        record_case "A/violating: <case-list>" 1 "EMPTY violating case list — cannot prove Lane A"
    fi

    # ── violating fixtures ────────────────────────────────────────────────────
    # Iterate the explicit case list (newline-split). The word list is computed
    # ONCE here (IFS=newline); the body quotes every expansion, so IFS need not
    # be toggled inside. No pipe/subshell — counters mutate the real shell.
    OLD_IFS="$IFS"
    IFS='
'
    for entry in $LANE_A_VIOLATING; do
        [ -n "$entry" ] || continue
        relpath="${entry%%|*}"
        rule_id="${entry##*|}"
        ran_a=$((ran_a + 1))
        base="$(basename "$relpath")"

        if [ ! -s "$REACT_DIR/$relpath" ]; then
            record_case "A/violating: $base" 1 "MISSING violating fixture: $relpath"
            continue
        fi

        lint_json "$relpath"
        if [ "$LINT_RC" -eq 2 ]; then
            record_case "A/violating: $base" 1 "ENVIRONMENT ERROR — eslint exit 2 (config/internal), NOT a lint block (wanted exit 1 + $rule_id)"
            continue
        fi
        if [ "$LINT_RC" -ne 1 ]; then
            record_case "A/violating: $base" 1 "NOT blocked — eslint exit $LINT_RC (expected 1=lint failure; 0=slipped through)"
            continue
        fi
        vrc=0
        verdict="$(node "$NODE_HELPER" violating "$LINT_OUT" "$rule_id")" || vrc=$?
        case "$vrc" in
            0) record_case "A/violating: $base" 0 "blocked by $rule_id (eslint exit 1; $verdict)" ;;
            2) record_case "A/violating: $base" 1 "ENVIRONMENT ERROR — $verdict" ;;
            *) record_case "A/violating: $base" 1 "NOT blocked / wrong signature — $verdict" ;;
        esac
    done
    IFS="$OLD_IFS"

    # ── clean fixtures (single aggregate case; internally per-file) ────────────
    ran_a=$((ran_a + 1))
    clean_missing=""
    for cf in $LANE_A_CLEAN_FILES; do
        [ -n "$cf" ] || continue
        if [ ! -s "$REACT_DIR/$cf" ]; then
            clean_missing="$clean_missing $cf"
        fi
    done
    if [ -n "$clean_missing" ]; then
        record_case "A/clean: fixtures/clean" 1 "MISSING clean fixture(s):$clean_missing"
    else
        lint_json fixtures/clean
        if [ "$LINT_RC" -eq 2 ]; then
            record_case "A/clean: fixtures/clean" 1 "ENVIRONMENT ERROR — eslint exit 2 scanning clean dir"
        elif [ "$LINT_RC" -ne 0 ]; then
            record_case "A/clean: fixtures/clean" 1 "clean fixtures were flagged (eslint exit $LINT_RC)"
            sed 's/^/      /' "$LINT_ERR"
        else
            # shellcheck disable=SC2086
            crc=0
            cverdict="$(node "$NODE_HELPER" clean "$LINT_OUT" $LANE_A_CLEAN_FILES)" || crc=$?
            case "$crc" in
                0) record_case "A/clean: fixtures/clean" 0 "passes (exit 0; $cverdict)" ;;
                2) record_case "A/clean: fixtures/clean" 1 "ENVIRONMENT ERROR — $cverdict" ;;
                *) record_case "A/clean: fixtures/clean" 1 "clean vacuity / flag — $cverdict" ;;
            esac
        fi
    fi
fi

# ══════════════════════════════════════════════════════════════════════════════
# LANE B — Java / Spring shell guards
# ══════════════════════════════════════════════════════════════════════════════
echo
echo "── LANE B: Java shell guards (consumer adopts the package path) ──"

VR="$JAVA_DIR/violating-root"
CR="$JAVA_DIR/clean-root"
CR_CP="$CR/backend/src/main/java/com/ax/template/authblueprint/consumerproof"
CR_MIG="$CR/backend/src/main/resources/db/migration"

# SIGNATURE-AWARE + EXIT-CODE-STRICT + NON-VACUOUS-CLEAN BLOCKING
# ---------------------------------------------------------------------------
# A raw non-zero exit is NOT proof that the guard blocked the INTENDED defect,
# and a raw exit 0 on the clean side is NOT proof that anything was scanned.
# For a case to count:
#   VIOLATING side must exit EXACTLY 1 (guard lint-failure code) — NOT exit 2
#     (usage/config/env), NOT any other code — AND its captured output must
#     CONTAIN the intended violation signature. exit 2 (either side) or any
#     unexpected code => ENVIRONMENT ERROR (fail loudly, never credited).
#   CLEAN side must (a) have every named clean fixture EXIST on disk, (b) exit
#     EXACTLY 0, (c) show a POSITIVE-SCAN proof (the guard reports a >0 scanned
#     count, or — for the count-less money guard — a non-empty .java scan tree),
#     and (d) NOT emit any vacuity token (ZERO_SCAN / "nothing to check" /
#     "no backend source tree" / "0/0 entities"). A vacuous "nothing to scan
#     -> 0" is rejected as a clean pass.
#
# run_guard <label> <guard.sh> <violating-root> <clean-root> <signature> \
#           <clean-scan-spec: re:<ERE> | fs:<dir>> <clean-fixture-path...>
run_guard() {
    local label="$1" guard="$2" varg="$3" carg="$4" signature="$5" scanspec="$6"
    shift 6
    # remaining args = clean fixture paths that MUST exist + be non-empty
    ran_b=$((ran_b + 1))

    if [ ! -f "$EVALS/$guard" ]; then
        record_case "B: $label" 1 "guard script not found: $guard"
        return
    fi

    local vout vc cout cc
    vc=0; vout="$(bash "$EVALS/$guard" --root "$varg" 2>&1)" || vc=$?
    cc=0; cout="$(bash "$EVALS/$guard" --root "$carg" 2>&1)" || cc=$?

    # Exit codes MUST be exactly 0 or 1. exit 2 = usage/env; anything else is
    # unexpected — either way it cannot prove a block or a clean pass.
    if [ "$vc" -eq 2 ] || [ "$cc" -eq 2 ]; then
        record_case "B/$label" 1 "ENVIRONMENT ERROR — guard exited 2 (usage/config/env; violating=$vc clean=$cc); not a valid block or pass"
        return
    fi
    if [ "$vc" -gt 1 ] || [ "$cc" -gt 1 ]; then
        record_case "B/$label" 1 "ENVIRONMENT ERROR — guard produced an unexpected exit (violating=$vc clean=$cc; expected 0 or 1)"
        return
    fi

    # VIOLATING: exit EXACTLY 1 AND intended signature present.
    local v_blocked=1
    if [ "$vc" -eq 1 ] && printf '%s' "$vout" | grep -qF -- "$signature"; then
        v_blocked=0
    fi

    # CLEAN: existence + exit 0 + positive-scan + no vacuity token.
    local clean_ok=0 clean_why="" cf
    for cf in "$@"; do
        if [ ! -s "$cf" ]; then
            clean_ok=1; clean_why="MISSING clean fixture: ${cf##*/}"
            break
        fi
    done
    if [ "$clean_ok" -eq 0 ] && [ "$cc" -ne 0 ]; then
        clean_ok=1; clean_why="clean was BLOCKED (exit $cc)"
    fi
    if [ "$clean_ok" -eq 0 ]; then
        if printf '%s' "$cout" | grep -Eq 'ZERO_SCAN|nothing to check|no backend source tree|no scan dir at|0/0 entities|— SKIP|: SKIP'; then
            clean_ok=1; clean_why="VACUOUS clean pass — guard scanned nothing (output: $(printf '%s' "$cout" | tr '\n' ' ' | cut -c1-90))"
        fi
    fi
    if [ "$clean_ok" -eq 0 ]; then
        local kind="${scanspec%%:*}" arg="${scanspec#*:}"
        if [ "$kind" = "re" ]; then
            if ! printf '%s' "$cout" | grep -Eq -- "$arg"; then
                clean_ok=1; clean_why="NO positive-scan token — clean output did not report a >0 scanned count (vacuous): $(printf '%s' "$cout" | tr '\n' ' ' | cut -c1-90)"
            fi
        elif [ "$kind" = "fs" ]; then
            local n
            n="$(find "$arg" -name '*.java' 2>/dev/null | wc -l | tr -d ' ')"
            if [ "${n:-0}" -eq 0 ]; then
                clean_ok=1; clean_why="NO .java files under scan dir ($arg) — nothing to scan (vacuous)"
            fi
        else
            clean_ok=1; clean_why="internal: unknown clean-scan spec '$scanspec'"
        fi
    fi

    if [ "$v_blocked" -eq 0 ] && [ "$clean_ok" -eq 0 ]; then
        record_case "B/$label" 0 "violating blocked (exit 1, signature '$signature' present), clean scanned+passes (exit 0)"
    else
        local why=""
        if [ "$v_blocked" -ne 0 ]; then
            if [ "$vc" -eq 0 ]; then
                why="violating SLIPPED THROUGH (exit 0)"
            else
                why="violating exit $vc but signature '$signature' ABSENT — INDETERMINATE (unrelated failure e.g. ZERO_SCAN, not a valid block)"
            fi
        fi
        if [ "$clean_ok" -ne 0 ]; then
            why="${why:+$why; }$clean_why"
        fi
        record_case "B/$label" 1 "$why"
    fi
}

run_guard "controller_problemdetail_guard"    controller_problemdetail_guard.sh    "$VR"              "$CR" \
    "Map<String,String>" "re:all [1-9][0-9]* @ExceptionHandler method" \
    "$CR_CP/ErrorMappingAdvice.java"

run_guard "controller_repository_shell_guard" controller_repository_shell_guard.sh "$VR/$PKG_SUFFIX"  "$CR/$PKG_SUFFIX" \
    "OrderAdminController" "re:[1-9][0-9]* \*Controller\.java file" \
    "$CR_CP/OrderAdminController.java"

run_guard "money_boundary_seam_guard"         money_boundary_seam_guard.sh         "$VR"              "$CR" \
    "InvoiceCalculator" "fs:$CR/backend/src/main/java" \
    "$CR_CP/InvoiceCalculator.java"

run_guard "entity_migration_guard"            entity_migration_guard.sh            "$VR"              "$CR" \
    "widget_ghost" "re:[1-9][0-9]*/[0-9]* entities have a" \
    "$CR_CP/Widget.java" "$CR_MIG/V001__create_widget.sql"

run_guard "role_literal_guard"                role_literal_guard.sh                "$VR"              "$CR" \
    "ROLE_ADMINS" "re:[1-9][0-9]* @PreAuthorize authority literal" \
    "$CR_CP/RoleProbeController.java"

# ══════════════════════════════════════════════════════════════════════════════
# CARDINALITY GATE — exit 0 is unreachable unless EVERY expected case ran.
# (finding 2: a dropped case row / failed feed / un-started lane must FAIL, not
# silently green with fewer checks.)
# ══════════════════════════════════════════════════════════════════════════════
CARD_FAIL=0
CARD_MSG=""
if [ "$lane_a_started" -ne 1 ]; then
    CARD_FAIL=1
    CARD_MSG="$CARD_MSG
    - Lane A never started (node/npm/deps unavailable) — 0 of $EXPECTED_LANE_A Lane-A case(s) ran; a partial run is not a full proof."
fi
if [ "$ran_a" -ne "$EXPECTED_LANE_A" ]; then
    CARD_FAIL=1
    CARD_MSG="$CARD_MSG
    - Lane A executed $ran_a of $EXPECTED_LANE_A expected case(s)."
fi
if [ "$ran_b" -ne "$EXPECTED_LANE_B" ]; then
    CARD_FAIL=1
    CARD_MSG="$CARD_MSG
    - Lane B executed $ran_b of $EXPECTED_LANE_B expected case(s)."
fi
total_recorded=$((PASS + FAIL))
if [ "$total_recorded" -ne "$EXPECTED_TOTAL" ]; then
    CARD_FAIL=1
    CARD_MSG="$CARD_MSG
    - $total_recorded case(s) recorded, expected exactly $EXPECTED_TOTAL."
fi

# ══════════════════════════════════════════════════════════════════════════════
# VERDICT
# ══════════════════════════════════════════════════════════════════════════════
echo
echo "=================================================================="
echo " RESULT: $PASS passed · $FAIL failed · (expected $EXPECTED_TOTAL cases; ran $((ran_a + ran_b)))"
echo "=================================================================="

if [ "$CARD_FAIL" -ne 0 ]; then
    echo
    echo "PROOF INCOMPLETE — the cardinality gate failed (not every expected case ran):"
    printf '%s\n' "$CARD_MSG"
    echo
    echo "A run that did not execute every expected case is NOT a full proof, even if"
    echo "every case that DID run passed. Fix the shortfall and re-run."
    exit 1
fi

if [ "$FAIL" -ne 0 ]; then
    echo
    echo "PROOF FALSIFIED — the following case(s) broke the thesis:"
    printf '%s\n' "$FAILED_CASES"
    echo
    echo "A violating fixture that slipped through (or was blocked by a non-intended"
    echo "error), or a clean fixture that was missing / unscanned / wrongly blocked,"
    echo "means the catalog is NOT mechanically enforcing as claimed."
    exit 1
fi

echo
echo "PROOF HOLDS: all $EXPECTED_TOTAL expected cases ran; every violating fixture was"
echo "blocked by its intended defect and every clean fixture existed, was scanned, and"
echo "passed. ax-template's catalog mechanically enforces in a consumer context."
exit 0
