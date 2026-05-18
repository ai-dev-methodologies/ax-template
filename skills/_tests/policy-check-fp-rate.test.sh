#!/usr/bin/env bash
# skills/_tests/policy-check-fp-rate.test.sh — FP rate eval for policy-check (F13).
#
# Runs all 50 fixtures in practices/evals/fixtures/policy-check/{pass,fail}/
# and asserts that the false-positive rate < 5%.
#
# False-Positive (FP) definition:
#   A fixture's `not_contains` rule ID appears in the policy-check output.
#   This means policy-check claimed a rule is applicable when it isn't.
#
# False-Negative (FN) definition:
#   A fixture's `contains` rule ID is NOT in the policy-check output.
#
# Acceptance thresholds:
#   FP rate  < 5%  (of all not_contains assertions, at most 5% are phantom matches)
#   FN rate  < 5%  (of all contains assertions, at most 5% are misses)
#
# Usage:
#   bash skills/_tests/policy-check-fp-rate.test.sh
#
# Exit:
#   0 — both rates within threshold
#   1 — one or both rates exceed threshold
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
POLICY_CHECK="$REPO_ROOT/skills/ax-verify/scripts/policy-check.sh"
FIXTURES_DIR="$REPO_ROOT/practices/evals/fixtures/policy-check"

echo "=== policy-check-fp-rate.test.sh ==="
echo ""

if [ ! -f "$POLICY_CHECK" ]; then
    echo "FAIL: policy-check.sh not found at $POLICY_CHECK" >&2
    exit 1
fi
if [ ! -d "$FIXTURES_DIR" ]; then
    echo "FAIL: fixtures directory not found at $FIXTURES_DIR" >&2
    exit 1
fi

# Counts
TOTAL_NOT_CONTAINS=0
TOTAL_CONTAINS=0
FP_COUNT=0
FN_COUNT=0
FIXTURE_COUNT=0

# ── Python fixture evaluator ──────────────────────────────────────────────────
evaluate_fixture() {
    local fixture_path="$1"

python3 - "$fixture_path" "$POLICY_CHECK" <<'PY'
import sys, pathlib, subprocess, re

fixture_path = pathlib.Path(sys.argv[1])
policy_check = sys.argv[2]

text = fixture_path.read_text()

# Parse domain from fixture
domain_m = re.search(r"^  domain:\s*(\S+)", text, re.MULTILINE)
if not domain_m:
    print("SKIP:no_domain")
    sys.exit(0)
domain = domain_m.group(1)

# Parse expected contains
contains = re.findall(r"^    - (\S+)", text, re.MULTILINE)

# Parse not_contains (after 'not_contains:' line)
nc_m = re.search(r"  not_contains:\n((?:    - \S+\n)*)", text, re.MULTILINE)
not_contains = []
if nc_m:
    not_contains = [line.strip().lstrip("- ") for line in nc_m.group(1).strip().split("\n") if line.strip()]

# Disambiguate: contains = all items before not_contains line
contains_m = re.search(r"  contains:\n((?:    - \S+\n)*)", text, re.MULTILINE)
contains = []
if contains_m:
    contains = [line.strip().lstrip("- ") for line in contains_m.group(1).strip().split("\n") if line.strip()]

# Run policy-check
try:
    result = subprocess.run(
        ["bash", policy_check, "--domain", domain],
        capture_output=True, text=True, timeout=30
    )
    output = result.stdout + result.stderr
except subprocess.TimeoutExpired:
    print("TIMEOUT")
    sys.exit(1)

# Parse returned rule IDs from output
# Lines like: "  RULE  PRACTICES-PERS-005              [HIGH  ]  ..."
returned_ids = set(re.findall(r"RULE\s+(\S+)\s+\[", output))

fp_count = 0
fn_count = 0
fp_rules = []
fn_rules = []

for rule_id in not_contains:
    if rule_id in returned_ids:
        fp_count += 1
        fp_rules.append(rule_id)

for rule_id in contains:
    if rule_id not in returned_ids:
        fn_count += 1
        fn_rules.append(rule_id)

status = "PASS" if (fp_count == 0 and fn_count == 0) else "WARN"
out = f"{status}:domain={domain}:nc={len(not_contains)}:c={len(contains)}:fp={fp_count}:fn={fn_count}"
if fp_rules:
    out += f":fp_rules={'|'.join(fp_rules)}"
if fn_rules:
    out += f":fn_rules={'|'.join(fn_rules)}"
print(out)
PY
}

# ── Iterate all fixtures ──────────────────────────────────────────────────────
for fixture_file in "$FIXTURES_DIR/pass"/*.yaml "$FIXTURES_DIR/fail"/*.yaml; do
    [ -f "$fixture_file" ] || continue
    FIXTURE_COUNT=$((FIXTURE_COUNT + 1))

    result="$(evaluate_fixture "$fixture_file")"
    fixture_name="$(basename "$fixture_file")"

    # Parse result fields
    status="${result%%:*}"
    domain="$(echo "$result" | sed 's/.*domain=\([^:]*\).*/\1/')"
    nc="$(echo "$result" | sed 's/.*:nc=\([^:]*\).*/\1/')"
    c="$(echo "$result" | sed 's/.*:c=\([^:]*\).*/\1/')"
    fp="$(echo "$result" | sed 's/.*:fp=\([^:]*\).*/\1/')"
    fn="$(echo "$result" | sed 's/.*:fn=\([^:]*\).*/\1/')"

    TOTAL_NOT_CONTAINS=$((TOTAL_NOT_CONTAINS + nc))
    TOTAL_CONTAINS=$((TOTAL_CONTAINS + c))
    FP_COUNT=$((FP_COUNT + fp))
    FN_COUNT=$((FN_COUNT + fn))

    if [ "$status" = "PASS" ]; then
        printf "  PASS [%-50s] domain=%-20s nc=%d c=%d\n" "$fixture_name" "$domain" "$nc" "$c"
    elif [ "$status" = "SKIP" ]; then
        printf "  SKIP [%-50s]\n" "$fixture_name"
    else
        fp_rules="$(echo "$result" | grep -o 'fp_rules=[^:]*' | sed 's/fp_rules=//')"
        fn_rules="$(echo "$result" | grep -o 'fn_rules=[^:]*' | sed 's/fn_rules=//')"
        printf "  WARN [%-50s] domain=%-20s FP=%d(%s) FN=%d(%s)\n" \
            "$fixture_name" "$domain" "$fp" "${fp_rules:-}" "$fn" "${fn_rules:-}"
    fi
done

echo ""
echo "=== Summary ==="
echo "  Fixtures evaluated   : $FIXTURE_COUNT"
echo "  not_contains total   : $TOTAL_NOT_CONTAINS"
echo "  contains total       : $TOTAL_CONTAINS"
echo "  FP (phantom rules)   : $FP_COUNT"
echo "  FN (missed rules)    : $FN_COUNT"

# ── Rate calculation ──────────────────────────────────────────────────────────
FP_OK=true
FN_OK=true

if [ "$TOTAL_NOT_CONTAINS" -gt 0 ]; then
    # FP rate as integer percentage (python for float division)
    FP_PCT="$(python3 -c "print(f'{($FP_COUNT / $TOTAL_NOT_CONTAINS) * 100:.1f}')")"
    echo "  FP rate              : $FP_PCT% (threshold: <5%)"
    FP_OVER="$(python3 -c "print('YES' if $FP_COUNT / $TOTAL_NOT_CONTAINS >= 0.05 else 'NO')")"
    [ "$FP_OVER" = "YES" ] && FP_OK=false
else
    echo "  FP rate              : N/A (no not_contains assertions)"
fi

if [ "$TOTAL_CONTAINS" -gt 0 ]; then
    FN_PCT="$(python3 -c "print(f'{($FN_COUNT / $TOTAL_CONTAINS) * 100:.1f}')")"
    echo "  FN rate              : $FN_PCT% (threshold: <5%)"
    FN_OVER="$(python3 -c "print('YES' if $FN_COUNT / $TOTAL_CONTAINS >= 0.05 else 'NO')")"
    [ "$FN_OVER" = "YES" ] && FN_OK=false
else
    echo "  FN rate              : N/A (no contains assertions)"
fi

echo ""
if [ "$FP_OK" = true ] && [ "$FN_OK" = true ]; then
    echo "policy-check-fp-rate: PASS (both rates within 5% threshold)"
    exit 0
else
    [ "$FP_OK" = false ] && echo "policy-check-fp-rate: FAIL — FP rate >= 5%" >&2
    [ "$FN_OK" = false ] && echo "policy-check-fp-rate: FAIL — FN rate >= 5%" >&2
    exit 1
fi
