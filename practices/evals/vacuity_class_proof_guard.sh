#!/usr/bin/env bash
# practices/evals/vacuity_class_proof_guard.sh — the MECHANICAL non-vacuity gate.
#
# THE INVARIANT (binary): a spec item that DECLARES a non-vacuity contract
# (vacuity_class + gate_method + kill_mutator) must have a catalog test that ACTUALLY
# KILLS the declared mutant. A green test that survives mutation of its own gate is a
# hollow (vacuously-passing) test — exactly the green-but-hollow failure mode that this
# session's adversarial reviews kept catching by hand. This guard makes the catch
# mechanical via PIT (METHODOLOGY.md "Non-Vacuity / Hollow-Test Enforcement").
#
# For each declared item the guard asserts:
#   (a) gate_method resolves      — FQCN#method points at a real backend/src/main class+method;
#   (b) mutator ↔ class consistency — the kill_mutator is valid for the vacuity_class
#                                     (fail_closed_default ⇒ TRUE_RETURNS|FALSE_RETURNS, etc.);
#   (c) scoped PIT proves the KILL — runs `./gradlew pitest` scoped to that ONE class × ONE
#       mutator × the package's *ViolationProofTest slice, parses mutations.xml, and asserts
#       the mutant ON THAT METHOD is KILLED. SURVIVED / NO_COVERAGE / zero-matching ⇒ exit 1.
#
# Usage:
#   bash practices/evals/vacuity_class_proof_guard.sh
#       LIVE: scan specs/*.yaml, run scoped PIT per declared item, assert each KILLED.
#
#   bash practices/evals/vacuity_class_proof_guard.sh \
#       --report <mutations.xml> --gate-method <FQCN#m> --kill-mutator <M> --vacuity-class <C>
#       PARSE-ONLY (fixture / offline): skip gradle + source resolution; run consistency (b)
#       and the report-assertion (c) against a canned mutations.xml. This is how the pass/fail
#       fixtures (hollow→SURVIVED→exit 1, tight→KILLED→exit 0) prove the assertion is non-vacuous.
#
# Exit 0 = every declared gate is non-vacuous (mutant KILLED). Exit 1 = a hollow gate (BLOCK).
# Exit 2 = usage / tooling error.

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SPECS_DIR="$REPO_ROOT/specs"
BACKEND_DIR="$REPO_ROOT/backend"

REPORT=""
ARG_GATE=""
ARG_MUTATOR=""
ARG_VCLASS=""
while [ $# -gt 0 ]; do
    case "$1" in
        --report)        REPORT="$2"; shift 2 ;;
        --gate-method)   ARG_GATE="$2"; shift 2 ;;
        --kill-mutator)  ARG_MUTATOR="$2"; shift 2 ;;
        --vacuity-class) ARG_VCLASS="$2"; shift 2 ;;
        --specs-dir)     SPECS_DIR="$2"; shift 2 ;;
        --backend-dir)   BACKEND_DIR="$2"; shift 2 ;;
        *) echo "vacuity_class_proof_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── vacuity_class → allowed kill_mutator set (METHODOLOGY consistency table) ────────────
mutator_allowed_for_class() {
    local vclass="$1" mutator="$2"
    case "$vclass" in
        fail_closed_default)  [ "$mutator" = "TRUE_RETURNS" ] || [ "$mutator" = "FALSE_RETURNS" ] ;;
        two_sided_invariant)  [ "$mutator" = "VOID_METHOD_CALLS" ] || [ "$mutator" = "NEGATE_CONDITIONALS" ] ;;
        exemption_carveout)   [ "$mutator" = "REMOVE_CONDITIONALS" ] ;;
        *) return 1 ;;
    esac
}

# ── (c) parse a mutations.xml and assert the mutant ON THE GATE METHOD is KILLED ─────────
# Args: <xml> <fqcn> <method>. Echoes a one-line verdict. Exit 0 = KILLED, 1 = hollow/none, 3 = bad xml.
assert_gate_killed() {
    local xml="$1" fqcn="$2" method="$3"
    python3 - "$xml" "$fqcn" "$method" <<'PY'
import sys, xml.etree.ElementTree as ET
xml_path, fqcn, method = sys.argv[1], sys.argv[2], sys.argv[3]
try:
    root = ET.parse(xml_path).getroot()
except Exception as e:
    print(f"BADXML {e}"); sys.exit(3)
matched = []
for m in root.findall('mutation'):
    if (m.findtext('mutatedClass') == fqcn) and (m.findtext('mutatedMethod') == method):
        matched.append(m.get('status'))
if not matched:
    # no mutant on the declared gate method => PIT could not reach the gate (NO_COVERAGE-equivalent)
    print(f"NO_MUTANT no {fqcn}#{method} mutant in report (gate uncovered / wrong method)")
    sys.exit(1)
non_killed = [s for s in matched if s != 'KILLED']
if non_killed:
    print(f"SURVIVED gate {fqcn}#{method}: {len(matched)} mutant(s), non-KILLED={non_killed}")
    sys.exit(1)
print(f"KILLED gate {fqcn}#{method}: {len(matched)} mutant(s) all KILLED")
sys.exit(0)
PY
}

FAIL=0

# ════════════════════════════════════════════════════════════════════════════════════════
# PARSE-ONLY MODE (fixtures / offline): consistency (b) + report assertion (c). No gradle.
# ════════════════════════════════════════════════════════════════════════════════════════
if [ -n "$REPORT" ]; then
    [ -n "$ARG_GATE" ] && [ -n "$ARG_MUTATOR" ] || {
        echo "vacuity_class_proof_guard: --report requires --gate-method and --kill-mutator" >&2; exit 2; }
    [ -f "$REPORT" ] || { echo "vacuity_class_proof_guard: FAIL — report not found: $REPORT" >&2; exit 1; }

    fqcn="${ARG_GATE%%#*}"; method="${ARG_GATE##*#}"

    if [ -n "$ARG_VCLASS" ] && ! mutator_allowed_for_class "$ARG_VCLASS" "$ARG_MUTATOR"; then
        echo "vacuity_class_proof_guard: FAIL — kill_mutator '$ARG_MUTATOR' is inconsistent with vacuity_class '$ARG_VCLASS'" >&2
        exit 1
    fi

    verdict="$(assert_gate_killed "$REPORT" "$fqcn" "$method")"; rc=$?
    if [ "$rc" -eq 3 ]; then
        echo "vacuity_class_proof_guard: FAIL — unparseable report: $verdict" >&2; exit 1
    elif [ "$rc" -ne 0 ]; then
        echo "vacuity_class_proof_guard: FAIL — hollow gate $ARG_GATE: $verdict" >&2
        echo "  the catalog test does NOT kill the declared mutant — it is vacuously passing. Add an" >&2
        echo "  assertion that exercises the gate so flipping it fails the test." >&2
        exit 1
    fi
    echo "vacuity_class_proof_guard: PASS — $verdict (parse-only / report=$REPORT)"
    exit 0
fi

# ════════════════════════════════════════════════════════════════════════════════════════
# LIVE MODE: scan specs for declared items, run scoped PIT per item, assert KILLED.
# ════════════════════════════════════════════════════════════════════════════════════════

# Enumerate declared items: (spec_file, item_id, vacuity_class, gate_method, kill_mutator)
ITEMS="$(python3 - "$SPECS_DIR" <<'PY'
import sys, os, glob, yaml
specs_dir = sys.argv[1]
rows = []
for f in sorted(glob.glob(os.path.join(specs_dir, '*.yaml'))):
    try:
        doc = yaml.safe_load(open(f, encoding='utf-8'))
    except Exception:
        continue
    if not isinstance(doc, dict):
        continue
    for it in (doc.get('items') or []):
        if not isinstance(it, dict):
            continue
        vc, gm, km = it.get('vacuity_class'), it.get('gate_method'), it.get('kill_mutator')
        if vc or gm or km:
            rows.append('\t'.join([f, str(it.get('id','?')), str(vc), str(gm), str(km)]))
for r in rows:
    print(r)
PY
)"

if [ -z "$ITEMS" ]; then
    echo "vacuity_class_proof_guard: PASS — no spec item declares a vacuity_class contract (nothing to prove)"
    exit 0
fi

command -v python3 >/dev/null 2>&1 || { echo "vacuity_class_proof_guard: python3 required" >&2; exit 2; }
[ -x "$BACKEND_DIR/gradlew" ] || { echo "vacuity_class_proof_guard: FAIL — $BACKEND_DIR/gradlew not found" >&2; exit 1; }

PROVEN=0
while IFS=$'\t' read -r spec_file item_id vclass gate_method kill_mutator; do
    [ -n "$item_id" ] || continue
    label="$(basename "$spec_file")#$item_id"

    # All three fields are mandatory once any is declared.
    if [ "$vclass" = "None" ] || [ "$gate_method" = "None" ] || [ "$kill_mutator" = "None" ]; then
        echo "vacuity_class_proof_guard: FAIL — $label declares a partial contract (need vacuity_class + gate_method + kill_mutator)" >&2
        echo "    vacuity_class=$vclass gate_method=$gate_method kill_mutator=$kill_mutator" >&2
        FAIL=1; continue
    fi

    # (b) consistency
    if ! mutator_allowed_for_class "$vclass" "$kill_mutator"; then
        echo "vacuity_class_proof_guard: FAIL — $label: kill_mutator '$kill_mutator' inconsistent with vacuity_class '$vclass'" >&2
        FAIL=1; continue
    fi

    fqcn="${gate_method%%#*}"; method="${gate_method##*#}"
    if [ "$fqcn" = "$gate_method" ] || [ -z "$method" ]; then
        echo "vacuity_class_proof_guard: FAIL — $label: gate_method must be 'FQCN#method', got '$gate_method'" >&2
        FAIL=1; continue
    fi

    # (a) resolve gate_method to a real class + method
    src="$BACKEND_DIR/src/main/java/$(echo "$fqcn" | tr '.' '/').java"
    if [ ! -f "$src" ]; then
        echo "vacuity_class_proof_guard: FAIL — $label: gate class source not found: $src" >&2
        FAIL=1; continue
    fi
    if ! grep -qE "\b${method}[[:space:]]*\(" "$src"; then
        echo "vacuity_class_proof_guard: FAIL — $label: method '$method' not declared in $src" >&2
        FAIL=1; continue
    fi

    # (c) scoped PIT run — one class × one mutator × the package's *ViolationProofTest slice
    pkg="${fqcn%.*}"
    target_tests="${pkg}.*ViolationProofTest"
    echo "vacuity_class_proof_guard: [$label] running scoped PIT — $fqcn#$method × $kill_mutator ..."
    rm -f "$BACKEND_DIR/build/reports/pitest/mutations.xml" 2>/dev/null
    # stdin from /dev/null: gradle drains stdin, which would otherwise eat the rest of the
    # `while read <<< "$ITEMS"` loop input and silently process only the first item.
    ( cd "$BACKEND_DIR" && ./gradlew pitest \
        -Ppit.targetClasses="$fqcn" \
        -Ppit.targetTests="$target_tests" \
        -Ppit.mutators="$kill_mutator" \
        -Ppit.noIncremental \
        --no-daemon --console=plain -q </dev/null ) >/tmp/vacuity_pit_$$.log 2>&1
    pit_rc=$?
    report="$BACKEND_DIR/build/reports/pitest/mutations.xml"
    if [ "$pit_rc" -ne 0 ] || [ ! -f "$report" ]; then
        echo "vacuity_class_proof_guard: FAIL — $label: PIT run failed (rc=$pit_rc) or produced no report. Tail:" >&2
        tail -15 /tmp/vacuity_pit_$$.log | sed 's/^/    /' >&2
        rm -f /tmp/vacuity_pit_$$.log
        FAIL=1; continue
    fi
    rm -f /tmp/vacuity_pit_$$.log

    verdict="$(assert_gate_killed "$report" "$fqcn" "$method")"; vrc=$?
    if [ "$vrc" -ne 0 ]; then
        echo "vacuity_class_proof_guard: FAIL — $label is HOLLOW: $verdict" >&2
        echo "    the test passes even when $gate_method is mutated with $kill_mutator — it asserts nothing about the gate." >&2
        FAIL=1; continue
    fi
    echo "vacuity_class_proof_guard: [$label] $verdict"
    PROVEN=$((PROVEN + 1))
done <<< "$ITEMS"

if [ "$FAIL" -ne 0 ]; then
    echo "" >&2
    echo "vacuity_class_proof_guard: FAIL — one or more declared gates are hollow (mutant not KILLED)." >&2
    exit 1
fi

echo "vacuity_class_proof_guard: PASS — $PROVEN declared gate(s) kill-proofed (every mutant KILLED)"
exit 0
