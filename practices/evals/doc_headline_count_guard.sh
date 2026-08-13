#!/usr/bin/env bash
# practices/evals/doc_headline_count_guard.sh
# 2026-05-30 consistency audit (Phase 2 candidates C1/C2/C3 + C7-class) — closes
# the doc-vs-disk-lie surface for the headline composition-kit metrics.
#
# The generated sentinels (practices/AGENTS.md rule_count, practices-react/AGENTS.md
# rule_count) are mechanically correct and now both guarded. But the HAND-WRITTEN
# headline numbers in README.md, CLAUDE.md, and .claude-plugin/plugin.json had
# rotted with no guard: Java rules said 107 (README/CLAUDE) or 64 (plugin.json)
# while disk = 112; React rules said 68 while disk = 86; L4 domains said 20 (or 12
# in the CLAUDE.md vision diagram) while disk = 21; hard guards said 41 (or 29)
# while disk had grown well past that. A README that lies about catalog size
# undermines the "규칙 밖 AI output BLOCKED" promise to fork-receivers.
#
# This guard derives the disk-true counts and asserts the canonical headline
# claims match them, so the numbers cannot silently rot again. Counts are
# computed (never hardcoded), so a legitimate catalog change just requires
# bumping the doc number the guard names.
#
# Checked surfaces:
#   - README.md           — the hero composition line (contains "Java rules")
#   - CLAUDE.md           — the vision-diagram composition line (contains "Java rules")
#   - .claude-plugin/plugin.json — the "<N> rules" Java-catalog size claim
#
# D-7 (2026-08-10) — plugin.json / marketplace.json version agreement:
#   `claude plugin update` compares ONLY .claude-plugin/plugin.json's top-level
#   `version` (measured live, CLI 2.1.220 — a stale plugin.json makes update a
#   silent no-op regardless of what changed). `.claude-plugin/marketplace.json`'s
#   plugin-entry `version` and top-level `metadata.version` are NOT read for that
#   comparison, but a release that bumps one file's number and forgets the other
#   is exactly the kind of cross-file drift this guard exists to catch — so it is
#   checked here too, alongside the existing disk-truth claims. Unlike the counts
#   above, this is a cross-file AGREEMENT check, not a disk-derived count, so it
#   runs unconditionally against $REPO_ROOT (see check_plugin_marketplace_version_sync).
#   It is ALSO independently invocable via --version-fixture-root DIR for fixture
#   testing, because a minimal two-JSON fixture cannot satisfy the README/CLAUDE/
#   SKILL.md headline-line checks above (those need full-doc replicas) — reusing
#   plain --root for this one invariant would force every fixture to fabricate all
#   of the unrelated checks too, so it gets its own narrow entry point instead.
#
# Exit: 0 PASS · 1 a headline count (or version) disagrees with disk/expected · 2 usage/setup error.
#
# Usage:
#   bash practices/evals/doc_headline_count_guard.sh
#   bash practices/evals/doc_headline_count_guard.sh --root DIR
#   bash practices/evals/doc_headline_count_guard.sh --version-fixture-root DIR   # D-7 isolated check

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
VERSION_FIXTURE_ROOT=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --version-fixture-root) VERSION_FIXTURE_ROOT="$2"; shift 2 ;;
        --version-fixture-root=*) VERSION_FIXTURE_ROOT="${1#--version-fixture-root=}"; shift ;;
        *) echo "doc_headline_count_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

violations=0
fail() { echo "VIOLATION: $1" >&2; violations=$((violations + 1)); }

# ── D-7: plugin.json / marketplace.json version agreement ────────────────────
# Fail-closed on missing/unparseable manifests (usage/setup error, not a silent
# skip) — consistent with this guard's sibling manifest-provenance guards.
check_plugin_marketplace_version_sync() {
    local root="$1"
    local plugin_json="$root/.claude-plugin/plugin.json"
    local marketplace_json="$root/.claude-plugin/marketplace.json"
    if ! command -v python3 >/dev/null 2>&1; then
        fail "plugin/marketplace version sync: python3 required to parse JSON manifests"
        return
    fi
    if [ ! -f "$plugin_json" ]; then
        fail "$plugin_json: file missing"
        return
    fi
    if [ ! -f "$marketplace_json" ]; then
        fail "$marketplace_json: file missing"
        return
    fi
    local report
    # P2-78: bash 3.2 mis-parses a quoted heredoc nested inside $(...) whenever the body's
    # apostrophe count is odd (a single stray "'version'" edit flips it) — write the python body
    # to a top-level temp file and invoke it by path instead of nesting the heredoc in $(...).
    local version_sync_py
    version_sync_py="$(mktemp "${TMPDIR:-/tmp}/doc_headline_version_sync.XXXXXX")"
    cat <<'PY' > "$version_sync_py"
import json
import sys

plugin_path, marketplace_path = sys.argv[1], sys.argv[2]

try:
    plugin = json.load(open(plugin_path, encoding="utf-8"))
except (OSError, ValueError) as e:
    print(f"UNPARSEABLE {plugin_path}: {e}")
    sys.exit(0)
try:
    marketplace = json.load(open(marketplace_path, encoding="utf-8"))
except (OSError, ValueError) as e:
    print(f"UNPARSEABLE {marketplace_path}: {e}")
    sys.exit(0)

plugin_version = plugin.get("version")
plugin_name = plugin.get("name")
if not plugin_version:
    print(f"MISSING_FIELD {plugin_path}: 'version' is blank or absent")
    sys.exit(0)

entries = marketplace.get("plugins") or []
entry = next((e for e in entries if isinstance(e, dict) and e.get("name") == plugin_name), None)
if entry is None:
    print(f"NO_MATCHING_ENTRY {marketplace_path}: no plugins[] entry with name == {plugin_name!r}")
    sys.exit(0)

entry_version = entry.get("version")
if entry_version != plugin_version:
    print(f"ENTRY_VERSION_MISMATCH {marketplace_path}: plugins[].version={entry_version!r} "
          f"but {plugin_path} version={plugin_version!r}")

metadata_version = (marketplace.get("metadata") or {}).get("version")
if metadata_version != plugin_version:
    print(f"METADATA_VERSION_MISMATCH {marketplace_path}: metadata.version={metadata_version!r} "
          f"but {plugin_path} version={plugin_version!r}")
PY
    report="$(python3 "$version_sync_py" "$plugin_json" "$marketplace_json")"
    rm -f "$version_sync_py"
    if [ -n "$report" ]; then
        while IFS= read -r line; do
            [ -z "$line" ] && continue
            fail "$line"
        done <<< "$report"
    fi
}

# ── isolated fixture mode (D-7 only) — see header comment for why this is a
# separate entry point rather than reusing --root for a minimal fixture.
if [ -n "$VERSION_FIXTURE_ROOT" ]; then
    [ -d "$VERSION_FIXTURE_ROOT" ] || {
        echo "doc_headline_count_guard: --version-fixture-root not found: $VERSION_FIXTURE_ROOT" >&2
        exit 2
    }
    check_plugin_marketplace_version_sync "$VERSION_FIXTURE_ROOT"
    if [ "$violations" -gt 0 ]; then
        echo "doc_headline_count_guard: $violations plugin/marketplace version-sync violation(s) — BLOCKED" >&2
        exit 1
    fi
    echo "doc_headline_count_guard: plugin/marketplace version-sync fixture check PASS"
    exit 0
fi

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"
cd "$REPO_ROOT" || { echo "doc_headline_count_guard: cannot cd $REPO_ROOT" >&2; exit 2; }

# ── disk-true counts ──────────────────────────────────────────────────────────
count_glob() { ls -1 $1 2>/dev/null | wc -l | tr -d ' '; }

JAVA_RULES="$(count_glob 'practices/rules/*.md')"
REACT_RULES="$(ls -1 practices-react/rules/*.md 2>/dev/null | grep -v '/_template.md$' | wc -l | tr -d ' ')"
ESLINT_RULES="$(count_glob 'practices-react/eslint-plugin-ax/rules/*.js')"
L4_DIRS="$(ls -1d templates/L4/*/ 2>/dev/null | wc -l | tr -d ' ')"
HARD_GUARDS="$(ls -1 practices/evals/*_guard.sh practices-react/evals/*_guard.sh 2>/dev/null | wc -l | tr -d ' ')"

# (violations / fail() are already defined above check_plugin_marketplace_version_sync)

# Assert that every "<num> <label>" occurrence on $line equals $expected.
# $1=line text  $2=label (literal, used in regex)  $3=expected  $4=where
assert_metric() {
    local line="$1" label="$2" expected="$3" where="$4"
    local nums
    nums="$(printf '%s' "$line" | grep -oE "[0-9]+ ${label}" | grep -oE '^[0-9]+')"
    if [ -z "$nums" ]; then
        fail "$where: no '<N> ${label}' claim found (expected '${expected} ${label}')"
        return
    fi
    local n
    while IFS= read -r n; do
        [ -z "$n" ] && continue
        if [ "$n" != "$expected" ]; then
            fail "$where: states '${n} ${label}' but disk truth is '${expected} ${label}'"
        fi
    done <<< "$nums"
}

# ── README.md hero composition line ───────────────────────────────────────────
README_LINE="$(grep -F 'Java rules' README.md | head -1 || true)"
if [ -z "$README_LINE" ]; then
    fail "README.md: no hero composition line containing 'Java rules' found"
else
    assert_metric "$README_LINE" "Java rules"   "$JAVA_RULES"  "README.md hero"
    assert_metric "$README_LINE" "React rules"  "$REACT_RULES" "README.md hero"
    assert_metric "$README_LINE" "ESLint rules" "$ESLINT_RULES" "README.md hero"
    assert_metric "$README_LINE" "L4 domains"   "$L4_DIRS"     "README.md hero"
    assert_metric "$README_LINE" "hard guards"  "$HARD_GUARDS" "README.md hero"
fi

# ── CLAUDE.md vision-diagram composition line ─────────────────────────────────
CLAUDE_LINE="$(grep -F 'Java rules' CLAUDE.md | head -1 || true)"
if [ -z "$CLAUDE_LINE" ]; then
    fail "CLAUDE.md: no vision-diagram line containing 'Java rules' found"
else
    assert_metric "$CLAUDE_LINE" "Java rules"   "$JAVA_RULES"  "CLAUDE.md vision"
    assert_metric "$CLAUDE_LINE" "React rules"  "$REACT_RULES" "CLAUDE.md vision"
    assert_metric "$CLAUDE_LINE" "ESLint rules" "$ESLINT_RULES" "CLAUDE.md vision"
    assert_metric "$CLAUDE_LINE" "L4 domains"   "$L4_DIRS"     "CLAUDE.md vision"
    assert_metric "$CLAUDE_LINE" "hard guards"  "$HARD_GUARDS" "CLAUDE.md vision"
fi

# ── plugin.json Java-catalog size claim ("<N> rules") ─────────────────────────
PLUGIN_LINE="$(grep -F 'practices catalog' .claude-plugin/plugin.json | head -1 || true)"
if [ -z "$PLUGIN_LINE" ]; then
    fail ".claude-plugin/plugin.json: no 'practices catalog (<N> rules ...)' claim found"
else
    assert_metric "$PLUGIN_LINE" "rules" "$JAVA_RULES" ".claude-plugin/plugin.json"
fi

# ── skills/ax-transform/SKILL.md (the /ax-transform entry point) ──────────────
# The skill entry point is the FIRST document a fork-receiver reads via
# /ax-transform; its counts rotted to 147/86/70 unguarded (found 2026-06-10).
SKILL_MD="skills/ax-transform/SKILL.md"
if [ ! -f "$SKILL_MD" ]; then
    fail "$SKILL_MD: file missing"
else
    SKILL_JAVA_LINE="$(grep -F 'practices/ catalog' "$SKILL_MD" | head -1 || true)"
    if [ -z "$SKILL_JAVA_LINE" ]; then
        fail "$SKILL_MD: no 'practices/ catalog (<N> rules ...)' claim found"
    else
        assert_metric "$SKILL_JAVA_LINE" "rules" "$JAVA_RULES" "$SKILL_MD practices row"
    fi
    SKILL_REACT_LINE="$(grep -F 'practices-react/' "$SKILL_MD" | grep -F 'catalog' | head -1 || true)"
    if [ -z "$SKILL_REACT_LINE" ]; then
        fail "$SKILL_MD: no 'practices-react/ catalog (<N> rules ...)' claim found"
    else
        assert_metric "$SKILL_REACT_LINE" "rules" "$REACT_RULES" "$SKILL_MD practices-react row"
    fi
    # check EVERY 'AI-consumable form' row (Java AND React — head -1 silently skipped the React row)
    SKILL_JAVA_AGENTS="$(grep -F 'AI-consumable form' "$SKILL_MD" | grep -v 'React' | head -1 || true)"
    if [ -n "$SKILL_JAVA_AGENTS" ]; then
        assert_metric "$SKILL_JAVA_AGENTS" "rules" "$JAVA_RULES" "$SKILL_MD AGENTS row (Java)"
    fi
    SKILL_REACT_AGENTS="$(grep -F 'AI-consumable form' "$SKILL_MD" | grep -F 'React rules' | head -1 || true)"
    if [ -n "$SKILL_REACT_AGENTS" ]; then
        assert_metric "$SKILL_REACT_AGENTS" "React rules" "$REACT_RULES" "$SKILL_MD AGENTS row (React)"
    fi
    # every "<N> hard guards" claim anywhere in the skill doc must match disk
    while IFS= read -r gline; do
        [ -z "$gline" ] && continue
        assert_metric "$gline" "hard guards" "$HARD_GUARDS" "$SKILL_MD guard claim"
    done <<< "$(grep -E '[0-9]+ hard guards' "$SKILL_MD" || true)"
fi

# ── D-7: plugin.json / marketplace.json version agreement (live root) ─────────
check_plugin_marketplace_version_sync "$REPO_ROOT"

if [ "$violations" -gt 0 ]; then
    echo "" >&2
    echo "doc_headline_count_guard: $violations headline count / version-sync violation(s) disagree with disk truth — BLOCKED" >&2
    echo "disk truth: ${JAVA_RULES} Java rules · ${REACT_RULES} React rules · ${ESLINT_RULES} ESLint rules · ${L4_DIRS} L4 domains · ${HARD_GUARDS} hard guards" >&2
    exit 1
fi

echo "doc_headline_count_guard: headline counts match disk (${JAVA_RULES} Java · ${REACT_RULES} React · ${ESLINT_RULES} ESLint · ${L4_DIRS} L4 · ${HARD_GUARDS} guards) and plugin/marketplace versions agree"
exit 0
