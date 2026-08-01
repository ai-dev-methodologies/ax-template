#!/usr/bin/env bash
# practices/generate_index_selftest.sh — parser unit tests for generate_index.sh,
# run against the fixtures in practices/generate_index_fixtures/*. Not a guard
# (not registered with run-all-guards.sh) — this is a test for the generator
# script itself, kept next to it (PRD d-track T2-1 §8).
#
# Exercises the 4 known frontmatter `verification:` shapes on disk plus the
# hard-fail parse path:
#   block    — block-mapping `verification: { gradle_task: ..., tag: ... }`
#   inline   — inline-flow `verification: { type: review, ... }` + `tags: [a, b]`
#   missing  — no `verification:` key at all -> classified `unclassified` ->
#              trips the non-vacuity census -> exit 1
#   malformed — no closing `---` delimiter -> exit 1 before any write
#
# Also asserts the atomic-write property (PRD T2-1 F-write): a run that exits 1
# (missing/malformed) must leave a pre-existing out file byte-for-byte
# untouched, and must not leave a `.tmp` file behind.
#
# bash 3.2 compatible (no mapfile, no associative arrays) — same posture as
# generate_index.sh / generate_agents.sh.
set -euo pipefail
export LC_ALL=C

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GENERATOR="$SCRIPT_DIR/generate_index.sh"
FIXTURES_DIR="$SCRIPT_DIR/generate_index_fixtures"

TMP_DIR="$(mktemp -d 2>/dev/null || mktemp -d -t 'ax_generate_index_selftest')"
trap 'rm -rf "$TMP_DIR"' EXIT

FAIL_COUNT=0

fail() {
    echo "generate_index_selftest.sh: FAIL — $1" >&2
    FAIL_COUNT=$((FAIL_COUNT + 1))
}

pass() {
    echo "generate_index_selftest.sh: PASS — $1"
}

# run_generator FIXTURE OUT_FILE -> sets RC to the generator's exit code,
# never aborts the selftest itself (generator failure is an expected case
# for missing/malformed fixtures).
run_generator() {
    fixture="$1"
    out_file="$2"
    set +e
    bash "$GENERATOR" \
        --catalog "selftest-$fixture" \
        --catalog-dir "$FIXTURES_DIR/$fixture" \
        --out "$out_file" \
        >"$TMP_DIR/$fixture.stdout" 2>"$TMP_DIR/$fixture.stderr"
    RC=$?
    set -e
}

# ── 1. block fixture: expect rc=0, gradle: classification ───────────────────
BLOCK_OUT="$TMP_DIR/block.md"
run_generator "block" "$BLOCK_OUT"
if [ "$RC" -ne 0 ]; then
    fail "block fixture: expected rc=0, got rc=$RC (stderr: $(cat "$TMP_DIR/block.stderr"))"
elif [ ! -f "$BLOCK_OUT" ]; then
    fail "block fixture: rc=0 but $BLOCK_OUT was not written"
elif ! grep -q '| selftest-block-verification | MEDIUM | gradle:testSelftestBlock |' "$BLOCK_OUT"; then
    fail "block fixture: expected row with 'gradle:testSelftestBlock' classification not found in $BLOCK_OUT"
elif ! grep -q '\*\*block\*\* (1) — selftest-block-verification' "$BLOCK_OUT"; then
    fail "block fixture: expected tag-index line for tag 'block' not found in $BLOCK_OUT"
else
    pass "block fixture: rc=0, gradle:testSelftestBlock classification + tag index correct"
fi

# ── 2. inline fixture: expect rc=0, review classification ───────────────────
INLINE_OUT="$TMP_DIR/inline.md"
run_generator "inline" "$INLINE_OUT"
if [ "$RC" -ne 0 ]; then
    fail "inline fixture: expected rc=0, got rc=$RC (stderr: $(cat "$TMP_DIR/inline.stderr"))"
elif [ ! -f "$INLINE_OUT" ]; then
    fail "inline fixture: rc=0 but $INLINE_OUT was not written"
elif ! grep -q '| selftest-inline-verification | LOW | review |' "$INLINE_OUT"; then
    fail "inline fixture: expected row with 'review' classification not found in $INLINE_OUT"
elif ! grep -q '\*\*inline\*\* (1) — selftest-inline-verification' "$INLINE_OUT"; then
    fail "inline fixture: expected tag-index line for tag 'inline' not found in $INLINE_OUT"
else
    pass "inline fixture: rc=0, review classification + tag index correct"
fi

# ── 3. missing fixture: expect rc=1 (unclassified census BLOCK) + atomicity ──
MISSING_OUT="$TMP_DIR/missing.md"
SENTINEL_CONTENT="PRE-EXISTING SENTINEL CONTENT — must survive a failed run"
printf '%s\n' "$SENTINEL_CONTENT" > "$MISSING_OUT"
run_generator "missing" "$MISSING_OUT"
if [ "$RC" -ne 1 ]; then
    fail "missing fixture: expected rc=1, got rc=$RC"
elif ! grep -q 'unclassified' "$TMP_DIR/missing.stderr"; then
    fail "missing fixture: expected 'unclassified' BLOCK message on stderr, not found"
elif [ "$(cat "$MISSING_OUT")" != "$SENTINEL_CONTENT" ]; then
    fail "missing fixture: atomicity violated — pre-existing $MISSING_OUT was overwritten on a failed run"
elif [ -f "$MISSING_OUT.tmp" ]; then
    fail "missing fixture: atomicity violated — orphaned $MISSING_OUT.tmp left behind"
else
    pass "missing fixture: rc=1 unclassified BLOCK, pre-existing out file untouched (atomic write)"
fi

# ── 4. malformed fixture: expect rc=1 (no closing '---') + atomicity ────────
MALFORMED_OUT="$TMP_DIR/malformed.md"
printf '%s\n' "$SENTINEL_CONTENT" > "$MALFORMED_OUT"
run_generator "malformed" "$MALFORMED_OUT"
if [ "$RC" -ne 1 ]; then
    fail "malformed fixture: expected rc=1, got rc=$RC"
elif ! grep -q "no closing '---' delimiter" "$TMP_DIR/malformed.stderr"; then
    fail "malformed fixture: expected 'no closing ... delimiter' message on stderr, not found"
elif [ "$(cat "$MALFORMED_OUT")" != "$SENTINEL_CONTENT" ]; then
    fail "malformed fixture: atomicity violated — pre-existing $MALFORMED_OUT was overwritten on a failed run"
elif [ -f "$MALFORMED_OUT.tmp" ]; then
    fail "malformed fixture: atomicity violated — orphaned $MALFORMED_OUT.tmp left behind"
else
    pass "malformed fixture: rc=1 parse-error BLOCK, pre-existing out file untouched (atomic write)"
fi

if [ "$FAIL_COUNT" -ne 0 ]; then
    echo "generate_index_selftest.sh: $FAIL_COUNT check(s) FAILED" >&2
    exit 1
fi
echo "generate_index_selftest.sh: all checks PASSED"
