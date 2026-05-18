#!/usr/bin/env bash
# skills/_tests/L4/scheduler-domain.test.sh — R7 SP41 TDD anchor.
#
# Sealed catalog-discoverability test for the scheduled-task L4 row.
# Asserts that a context-0 sub-agent with only templates/L4/scheduled-task/README.md
# + practices/AGENTS.md could discover:
#   1. The L4 README exists at the documented path.
#   2. The README does NOT carry an `applied_recipes:` key (matches file-storage +
#      practices precedent — Architect H2 / Critic M4 closure).
#   3. The README references all 3 Spec Trio paths (spec + contract + manifest).
#   4. The README names ≥3 of the REGISTER / LOCK / EXECUTE / IDEMPOTENCY families.
#   5. The README cites the documented external verbatim (Spring or Quartz).
#   6. templates/DECISIONS.md carries the TD-2026-05-20-020 entry.
#   7. The backend skeleton stub exists with .skeleton suffix (not .java — won't
#      pollute Gradle compile path for forks that copy the L4 tree wholesale).
#
# This replaces `/ax-verify-domain scheduled-task` as the SP41 binary gate
# (Critic L option (c) — scheduler L4 stays catalog-only per CLAUDE.md
# recipe-no-code principle; Gradle `testScheduledTask` task does not exist).
#
# Exit 0 = GREEN (all assertions pass)
# Exit 1 = RED (any assertion fails)
#
# Usage: bash skills/_tests/L4/scheduler-domain.test.sh

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

README="$REPO_ROOT/templates/L4/scheduled-task/README.md"
DECISIONS="$REPO_ROOT/templates/DECISIONS.md"
SKELETON="$REPO_ROOT/templates/L4/scheduled-task/backend/ScheduledTask.java.skeleton"
SPEC="$REPO_ROOT/specs/scheduled-task-l0.yaml"
CONTRACT="$REPO_ROOT/contracts/scheduled-task-openapi.yaml"
MANIFEST="$REPO_ROOT/blueprints/scheduled-task-manifest.yaml"

PASS=0
FAIL=0
RESULTS=()

assert_pass() {
    PASS=$((PASS + 1))
    RESULTS+=("PASS [$1]")
}

assert_fail() {
    FAIL=$((FAIL + 1))
    RESULTS+=("FAIL [$1] $2")
}

# ── 1. L4 README exists ─────────────────────────────────────────────────────
if [ -f "$README" ]; then
    assert_pass "readme-exists"
else
    assert_fail "readme-exists" "$README not found"
fi

# ── 2. README has NO applied_recipes: key (Architect H2 / Critic M4 closure) ─
if [ -f "$README" ]; then
    if grep -qE '^applied_recipes:' "$README"; then
        assert_fail "no-applied-recipes-key" \
            "README must NOT carry applied_recipes: key at introduction (file-storage + practices precedent)"
    else
        assert_pass "no-applied-recipes-key"
    fi
fi

# ── 3. README references all 3 Spec Trio files ──────────────────────────────
if [ -f "$README" ]; then
    for trio_path in \
        "specs/scheduled-task-l0.yaml" \
        "contracts/scheduled-task-openapi.yaml" \
        "blueprints/scheduled-task-manifest.yaml"; do
        if grep -qF "$trio_path" "$README"; then
            assert_pass "trio-ref/$trio_path"
        else
            assert_fail "trio-ref/$trio_path" "README missing reference to $trio_path"
        fi
    done
fi

# ── 4. README names ≥3 of REGISTER / LOCK / EXECUTE / IDEMPOTENCY ─────────────
if [ -f "$README" ]; then
    families_found=0
    for family in REGISTER LOCK EXECUTE IDEMPOTENCY IDEMPOTENT; do
        if grep -qF "$family" "$README"; then
            families_found=$((families_found + 1))
        fi
    done
    if [ "$families_found" -ge 3 ]; then
        assert_pass "spec-families-named/$families_found"
    else
        assert_fail "spec-families-named" \
            "README names only $families_found of REGISTER/LOCK/EXECUTE/IDEMPOTENCY families (need ≥3)"
    fi
fi

# ── 5. README cites documented external verbatim (Spring OR Quartz) ──────────
if [ -f "$README" ]; then
    if grep -qiE 'docs\.spring\.io.*scheduling|quartz-scheduler\.org' "$README"; then
        assert_pass "external-verbatim-cited"
    else
        assert_fail "external-verbatim-cited" \
            "README must cite Spring Scheduling OR Quartz Scheduler tutorial verbatim URL"
    fi
fi

# ── 6. TD-2026-05-20-020 in templates/DECISIONS.md ───────────────────────────
if [ -f "$DECISIONS" ]; then
    if grep -qF 'TD-2026-05-20-020' "$DECISIONS"; then
        assert_pass "td-020-present"
    else
        assert_fail "td-020-present" "templates/DECISIONS.md missing TD-2026-05-20-020 entry"
    fi
else
    assert_fail "td-020-present" "templates/DECISIONS.md not found"
fi

# ── 7. Backend skeleton exists with .skeleton suffix ─────────────────────────
if [ -f "$SKELETON" ]; then
    assert_pass "skeleton-stub-exists"
else
    assert_fail "skeleton-stub-exists" "$SKELETON not found"
fi

# ── 8. Existing Spec Trio files still present (R3 origin — must not regress) ─
for f in "$SPEC" "$CONTRACT" "$MANIFEST"; do
    if [ -f "$f" ]; then
        assert_pass "r3-trio-intact/$(basename "$f")"
    else
        assert_fail "r3-trio-intact/$(basename "$f")" "missing R3 Spec Trio artifact: $f"
    fi
done

# ── Summary ─────────────────────────────────────────────────────────────────
echo "=== scheduler-domain.test.sh — R7 SP41 TDD anchor ==="
echo ""
for r in "${RESULTS[@]}"; do
    echo "  $r"
done
echo ""
echo "Total: $PASS passed, $FAIL failed"

if [ "$FAIL" -gt 0 ]; then
    echo "scheduler-domain: FAIL — $FAIL assertion(s) did not pass" >&2
    exit 1
fi

echo "scheduler-domain: PASS — context-0 sub-agent can discover the scheduler L4 row"
exit 0
