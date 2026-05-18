#!/usr/bin/env bash
# skills/_tests/tier1-topology.test.sh — TDD anchor for SP4a Tier-1 skill topology.
#
# Asserts:
#   1. Exactly 3 SKILL.md files exist under skills/ax-* directories
#   2. Each has valid frontmatter: `name:` + `description:` fields
#   3. Each `name:` value matches the parent directory name
#
# Exit 0 = GREEN (all assertions pass)
# Exit 1 = RED (one or more assertions fail)
#
# Usage:
#   bash skills/_tests/tier1-topology.test.sh
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SKILLS_DIR="$REPO_ROOT/skills"

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

echo "=== tier1-topology.test.sh ==="
echo ""

# ── 1. Find all ax-* SKILL.md files ─────────────────────────────────────────
# Use process substitution into array without mapfile (bash 3 compatible)
SKILL_FILES=()
while IFS= read -r f; do
    SKILL_FILES+=("$f")
done < <(find "$SKILLS_DIR" -type f -name "SKILL.md" | grep -E "^${SKILLS_DIR}/ax-(transform|verify|scaffold|fork-receiver)/SKILL\.md$" | sort)

EXPECTED_COUNT=4
ACTUAL_COUNT="${#SKILL_FILES[@]}"

echo "[1] Count: expect $EXPECTED_COUNT SKILL.md files under skills/ax-{transform,verify,scaffold,fork-receiver}/"

if [ "$ACTUAL_COUNT" -eq "$EXPECTED_COUNT" ]; then
    assert_pass "count=$ACTUAL_COUNT"
else
    assert_fail "count" "expected $EXPECTED_COUNT, got $ACTUAL_COUNT (found: ${SKILL_FILES[*]:-none})"
fi

# ── 2. Validate frontmatter for each expected skill ──────────────────────────
EXPECTED_DIRS=("ax-transform" "ax-verify" "ax-scaffold" "ax-fork-receiver")

for dir_name in "${EXPECTED_DIRS[@]}"; do
    skill_file="$SKILLS_DIR/$dir_name/SKILL.md"
    label="$dir_name"

    echo "[2] Frontmatter: $skill_file"

    if [ ! -f "$skill_file" ]; then
        assert_fail "$label/exists" "file not found: $skill_file"
        continue
    fi

    # Check `name:` field present in frontmatter (between --- delimiters)
    name_value=$(awk '/^---/{found++; next} found==1 && /^name:/{print; exit}' "$skill_file" | sed 's/^name:[[:space:]]*//' | tr -d '\r')

    if [ -z "$name_value" ]; then
        assert_fail "$label/name_present" "missing 'name:' in frontmatter"
    else
        assert_pass "$label/name_present (value='$name_value')"
    fi

    # Check `description:` field present
    has_desc=$(awk '/^---/{found++; next} found==1 && /^description:/{print; exit}' "$skill_file")

    if [ -z "$has_desc" ]; then
        assert_fail "$label/description_present" "missing 'description:' in frontmatter"
    else
        assert_pass "$label/description_present"
    fi

    # ── 3. name: value must match directory name ─────────────────────────────
    echo "[3] name-matches-dir: $dir_name"

    # Extract just the first token after "name:" (handles multiline description)
    name_token=$(echo "$name_value" | awk '{print $1}')

    if [ "$name_token" = "$dir_name" ]; then
        assert_pass "$label/name_matches_dir"
    else
        assert_fail "$label/name_matches_dir" "name='$name_token' does not match dir='$dir_name'"
    fi
done

# ── Summary ──────────────────────────────────────────────────────────────────
echo ""
echo "=== Results ==="
for r in "${RESULTS[@]}"; do
    echo "  $r"
done
echo ""
echo "Total: $PASS passed, $FAIL failed"

if [ "$FAIL" -gt 0 ]; then
    echo "tier1-topology: FAIL" >&2
    exit 1
fi

echo "tier1-topology: all assertions PASS"
exit 0
