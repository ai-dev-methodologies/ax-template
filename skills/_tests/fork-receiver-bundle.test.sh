#!/usr/bin/env bash
# skills/_tests/fork-receiver-bundle.test.sh
# SP22 TDD anchor: /ax-fork-receiver bundle smoke test.
#
# Asserts:
#   1. bundle.sh exits 0 and produces a tarball
#   2. Tarball exists, >1MB, <100MB
#   3. Extract to temp dir; assert presence of required paths
#   4. Assert absence of excluded paths (.git/, frontend/.next/, .omc/)
#   5. Cleanup temp dirs
#
# Exit 0 = GREEN (all assertions pass)
# Exit 1 = RED  (one or more assertions fail)
#
# Usage:
#   bash skills/_tests/fork-receiver-bundle.test.sh

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
BUNDLE_SH="$REPO_ROOT/skills/ax-fork-receiver/scripts/bundle.sh"

PASS=0
FAIL=0
RESULTS=()

assert_pass() {
    local label="$1"
    PASS=$((PASS + 1))
    RESULTS+=("PASS [$label]")
}

assert_fail() {
    local label="$1"
    local msg="$2"
    FAIL=$((FAIL + 1))
    RESULTS+=("FAIL [$label] $msg")
}

echo "=== fork-receiver-bundle.test.sh (SP22) ==="
echo ""

# ── Setup: temp paths ─────────────────────────────────────────────────────────
TEST_TARBALL="/tmp/test-bundle-sp22-$$.tar.gz"
TEST_EXTRACT="/tmp/test-fork-receiver-sp22-$$"

cleanup() {
    rm -f "$TEST_TARBALL" 2>/dev/null || true
    rm -rf "$TEST_EXTRACT" 2>/dev/null || true
}
trap cleanup EXIT

# ── 1. bundle.sh exists ───────────────────────────────────────────────────────
echo "[1] bundle.sh exists: $BUNDLE_SH"
if [ -f "$BUNDLE_SH" ]; then
    assert_pass "bundle.sh/exists"
else
    assert_fail "bundle.sh/exists" "file not found: $BUNDLE_SH"
    echo ""
    echo "=== Results ==="
    for r in "${RESULTS[@]}"; do echo "  $r"; done
    echo "Total: $PASS passed, $FAIL failed"
    echo "fork-receiver-bundle: FAIL (bundle.sh missing)" >&2
    exit 1
fi

# ── 2. Run bundle.sh ─────────────────────────────────────────────────────────
echo ""
echo "[2] Running bundle.sh → $TEST_TARBALL"
if bash "$BUNDLE_SH" "$TEST_TARBALL"; then
    assert_pass "bundle.sh/exit0"
else
    assert_fail "bundle.sh/exit0" "bundle.sh exited non-zero"
    echo ""
    echo "=== Results ==="
    for r in "${RESULTS[@]}"; do echo "  $r"; done
    echo "Total: $PASS passed, $FAIL failed"
    echo "fork-receiver-bundle: FAIL" >&2
    exit 1
fi

# ── 3. Tarball exists ─────────────────────────────────────────────────────────
echo ""
echo "[3] Tarball exists"
if [ -f "$TEST_TARBALL" ]; then
    assert_pass "tarball/exists"
else
    assert_fail "tarball/exists" "tarball not found at $TEST_TARBALL"
    echo ""
    echo "=== Results ==="
    for r in "${RESULTS[@]}"; do echo "  $r"; done
    echo "Total: $PASS passed, $FAIL failed"
    echo "fork-receiver-bundle: FAIL" >&2
    exit 1
fi

# ── 4. Tarball size: >1MB, <100MB ────────────────────────────────────────────
echo ""
echo "[4] Tarball size sanity"
TARBALL_BYTES=$(wc -c < "$TEST_TARBALL" | tr -d ' ')
TARBALL_MB=$((TARBALL_BYTES / 1024 / 1024))
echo "  size: ${TARBALL_MB}MB (${TARBALL_BYTES} bytes)"

if [ "$TARBALL_BYTES" -gt 1048576 ]; then
    assert_pass "tarball/size_gt_1mb (${TARBALL_MB}MB)"
else
    assert_fail "tarball/size_gt_1mb" "too small: ${TARBALL_BYTES} bytes (expected >1MB)"
fi

if [ "$TARBALL_BYTES" -lt 104857600 ]; then
    assert_pass "tarball/size_lt_100mb (${TARBALL_MB}MB)"
else
    assert_fail "tarball/size_lt_100mb" "too large: ${TARBALL_MB}MB (expected <100MB)"
fi

# ── 5. Extract tarball ────────────────────────────────────────────────────────
echo ""
echo "[5] Extract to $TEST_EXTRACT"
mkdir -p "$TEST_EXTRACT"
if tar -xzf "$TEST_TARBALL" -C "$TEST_EXTRACT"; then
    assert_pass "tarball/extract"
else
    assert_fail "tarball/extract" "tar -xzf failed"
    echo ""
    echo "=== Results ==="
    for r in "${RESULTS[@]}"; do echo "  $r"; done
    echo "Total: $PASS passed, $FAIL failed"
    echo "fork-receiver-bundle: FAIL" >&2
    exit 1
fi

# ── 6. Assert required paths present ─────────────────────────────────────────
echo ""
echo "[6] Required paths present"

REQUIRED_PATHS=(
    "templates/L1"
    "templates/L2"
    "templates/L3"
    "templates/L4"
    "templates/backend"
    "skills/ax-verify-L1"
    "skills/ax-fork-receiver"
    "practices/evals/run-all-guards.sh"
    "practices/rules"
    "practices/upstream"
    "practices-react/rules"
    "specs/auth-asvs-l1.yaml"
    "contracts"
    "blueprints"
    "verify/fork-receiver-smoke.sh"
    "verify/fork-receiver-full-tree-smoke.sh"
    "METHODOLOGY.md"
    "CLAUDE.md"
)

for rp in "${REQUIRED_PATHS[@]}"; do
    full="$TEST_EXTRACT/$rp"
    if [ -e "$full" ]; then
        assert_pass "present/$rp"
    else
        assert_fail "present/$rp" "not found in extracted tarball"
    fi
done

# ── 7. Assert excluded paths absent ──────────────────────────────────────────
echo ""
echo "[7] Excluded paths absent"

EXCLUDED_PATHS=(
    ".git"
    "frontend/.next"
    "frontend/node_modules"
    ".omc"
)

for ep in "${EXCLUDED_PATHS[@]}"; do
    full="$TEST_EXTRACT/$ep"
    if [ ! -e "$full" ]; then
        assert_pass "absent/$ep"
    else
        assert_fail "absent/$ep" "found in tarball but should be excluded"
    fi
fi

# Also assert no large Spring fixture dirs (they're excluded to keep tarball <100MB)
for spring_fixture in "practices/evals/fixtures/spring-realworld" "practices/evals/fixtures/spring-petclinic" "practices/evals/fixtures/spring-modulith-example"; do
    full="$TEST_EXTRACT/$spring_fixture"
    if [ ! -e "$full" ]; then
        assert_pass "absent/$spring_fixture"
    else
        assert_fail "absent/$spring_fixture" "large fixture found in tarball — tarball will exceed 100MB"
    fi
done

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "=== Results ==="
for r in "${RESULTS[@]}"; do
    echo "  $r"
done
echo ""
echo "Total: $PASS passed, $FAIL failed"

if [ "$FAIL" -gt 0 ]; then
    echo "fork-receiver-bundle: FAIL" >&2
    exit 1
fi

echo "fork-receiver-bundle: all assertions PASS"
exit 0
