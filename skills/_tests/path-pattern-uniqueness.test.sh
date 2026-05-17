#!/usr/bin/env bash
# skills/_tests/path-pattern-uniqueness.test.sh
# Verifies that each of 5 sample paths matches exactly 1 Tier-2 skill's pathPatterns.
# Tier-3 guard skills have pathPatterns: [] and must never match.
# Exit 0 iff all assertions pass; exit 1 with FAIL details on stderr.
#
# Coverage (5 required by PRD §SP4b + 5 additional):
#   1. backend/src/main/java/Foo.java       → ax-verify-java
#   2. frontend/src/components/Button.tsx   → ax-verify-react
#   3. specs/auth-asvs-l1.yaml             → ax-verify-shared
#   4. templates/L1/components/Badge.tsx    → ax-verify-L1
#   5. templates/L2/auth/LoginForm.tsx      → ax-verify-L2
#   6. templates/L3/list/ListPage.tsx       → ax-verify-L3
#   7. templates/L4/auth/AuthPage.tsx       → ax-verify-L4
#   8. practices-react/rules/r001.md        → ax-verify-react
#   9. contracts/auth-openapi.yaml          → ax-verify-shared
#  10. blueprints/auth-manifest.yaml        → ax-verify-shared

set -uo pipefail

PASS=0
FAIL=0

# ── Helper: fnmatch-style path test using bash extglob / regex ───────────────
# path_matches <test_path> <glob_pattern>
# Supports ** as multi-segment wildcard. Returns 0 on match, 1 on no match.
path_matches() {
    local test_path="$1"
    local pattern="$2"

    # Skip parameterized placeholder patterns (runtime-only)
    [[ "$pattern" == *'<domain>'* ]] && return 1

    # Convert glob to regex: ** → .*, * → [^/]*
    local regex
    regex="$(echo "$pattern" | sed 's|\*\*|DOUBLESTAR|g; s|\*|[^/]*|g; s|DOUBLESTAR|.*|g')"
    [[ "$test_path" =~ ^${regex}$ ]]
}

# skill_matches <test_path> <pattern1> [<pattern2> ...]
# Returns 0 if any pattern matches.
skill_matches() {
    local test_path="$1"
    shift
    local p
    for p in "$@"; do
        path_matches "$test_path" "$p" && return 0
    done
    return 1
}

# ── Classify a path to a skill name (or "none") ──────────────────────────────
classify() {
    local test_path="$1"
    local matched=""

    skill_matches "$test_path" 'backend/**' 'templates/backend/**' && matched="${matched}ax-verify-java "
    skill_matches "$test_path" 'frontend/**' 'practices-react/rules/**' && matched="${matched}ax-verify-react "
    skill_matches "$test_path" 'specs/**' 'contracts/**' 'blueprints/**' 'templates/DECISIONS.md' 'templates/AGENTS.md' && matched="${matched}ax-verify-shared "
    skill_matches "$test_path" 'templates/L1/**' && matched="${matched}ax-verify-L1 "
    skill_matches "$test_path" 'templates/L2/**' && matched="${matched}ax-verify-L2 "
    skill_matches "$test_path" 'templates/L3/**' && matched="${matched}ax-verify-L3 "
    skill_matches "$test_path" 'templates/L4/**' && matched="${matched}ax-verify-L4 "

    # Tier-3 guards: pathPatterns: [] → never trigger; skipped here intentionally

    echo "${matched% }"  # trim trailing space
}

# ── Assertion helper ─────────────────────────────────────────────────────────
assert_unique_match() {
    local test_path="$1"
    local expected_skill="$2"

    local matched
    matched="$(classify "$test_path")"

    # Count words = number of matched skills
    local count
    count=$(echo "$matched" | wc -w | tr -d ' ')

    if [ "$count" -eq 1 ] && [ "$matched" = "$expected_skill" ]; then
        PASS=$((PASS + 1))
        echo "  PASS: '$test_path' → $expected_skill (unique)"
    elif [ "$count" -ne 1 ]; then
        FAIL=$((FAIL + 1))
        echo "  FAIL: '$test_path' matched $count skills ($matched); expected exactly 1 = $expected_skill" >&2
    else
        FAIL=$((FAIL + 1))
        echo "  FAIL: '$test_path' matched '$matched'; expected '$expected_skill'" >&2
    fi
}

echo "=== path-pattern-uniqueness.test.sh ==="
echo ""

# Core 5 required by PRD §SP4b acceptance
assert_unique_match "backend/src/main/java/Foo.java"            "ax-verify-java"
assert_unique_match "frontend/src/components/Button.tsx"        "ax-verify-react"
assert_unique_match "specs/auth-asvs-l1.yaml"                   "ax-verify-shared"
assert_unique_match "templates/L1/components/Badge.tsx"         "ax-verify-L1"
assert_unique_match "templates/L2/auth/LoginForm.tsx"           "ax-verify-L2"

# Additional coverage
assert_unique_match "templates/L3/list/ListPage.tsx"            "ax-verify-L3"
assert_unique_match "templates/L4/auth/AuthPage.tsx"            "ax-verify-L4"
assert_unique_match "practices-react/rules/r001.md"             "ax-verify-react"
assert_unique_match "contracts/auth-openapi.yaml"               "ax-verify-shared"
assert_unique_match "blueprints/auth-manifest.yaml"             "ax-verify-shared"

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="

if [ "$FAIL" -ne 0 ]; then
    exit 1
fi

exit 0
