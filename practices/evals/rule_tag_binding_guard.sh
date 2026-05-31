#!/usr/bin/env bash
# practices/evals/rule_tag_binding_guard.sh
# Closes the A1 enforcement hole found by the 2026-05-31 goal-verification:
# the 4 hard gates + every other guard validate a rule's spec_ref, evidence,
# substance, and freshness — but NOTHING verified that a rule's declared
# `verification.tag` actually resolves to a real @Tag in the test sources. A rule
# could declare `tag: PRACTICES-PHANTOM-999` with no backing test and pass every
# gate, silently claiming mechanical verification it does not have.
#
# THE INVARIANT: every practices rule whose frontmatter declares a `verification.tag`
# MUST have that tag present as @Tag("<TAG>") somewhere under the test root — i.e. the
# rule->test binding is real, not a phantom. (On disk today all ~75 distinct rule tags
# resolve against 403 @Tag values; this guard locks that so a future phantom fails.)
#
# Scope: the Java `practices` catalog (verification.tag -> JUnit @Tag). The React
# catalog verifies via ESLint/fixtures, not @Tag, so it is out of scope here.
#
# Exit: 0 PASS · 1 a rule declares a tag with no backing @Tag · 2 usage/setup error.
#
# Usage:
#   bash practices/evals/rule_tag_binding_guard.sh
#   bash practices/evals/rule_tag_binding_guard.sh --rules-dir DIR --test-root DIR   # fixture/test

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

RULES_DIR="$REPO_ROOT/practices/rules"
TEST_ROOT="$REPO_ROOT/backend/src/test"
while [ $# -gt 0 ]; do
    case "$1" in
        --rules-dir) RULES_DIR="$2"; shift 2 ;;
        --rules-dir=*) RULES_DIR="${1#--rules-dir=}"; shift ;;
        --test-root) TEST_ROOT="$2"; shift 2 ;;
        --test-root=*) TEST_ROOT="${1#--test-root=}"; shift ;;
        *) echo "rule_tag_binding_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ ! -d "$RULES_DIR" ]; then
    echo "rule_tag_binding_guard: rules dir '$RULES_DIR' not found — nothing to check"
    exit 0
fi
if [ ! -d "$TEST_ROOT" ]; then
    echo "rule_tag_binding_guard: test root '$TEST_ROOT' not found" >&2
    exit 2
fi

# All @Tag("...") values present under the test root (one per line, deduped).
present_tags="$(grep -rhoE '@Tag\("[^"]+"\)' "$TEST_ROOT" 2>/dev/null | sed -E 's/@Tag\("([^"]+)"\)/\1/' | sort -u)"

# Extract a rule's verification.tag: the singular `tag:` line in the YAML frontmatter
# (distinct from the plural `tags:` list). Returns empty if the rule declares none.
extract_verification_tag() {
    awk '
        /^---[[:space:]]*$/ { fence++; if (fence >= 2) exit; next }
        fence == 1 && /^[[:space:]]+tag:[[:space:]]/ {
            v = $0
            sub(/^[[:space:]]+tag:[[:space:]]*/, "", v)
            gsub(/^["'"'"']|["'"'"']$/, "", v)
            sub(/[[:space:]]*$/, "", v)
            print v
            exit
        }
    ' "$1"
}

violations=0
checked=0
shopt -s nullglob
for rule in "$RULES_DIR"/*.md; do
    [[ "$(basename "$rule")" == ".gitkeep" ]] && continue
    tag="$(extract_verification_tag "$rule")"
    [ -z "$tag" ] && continue          # rule declares no verification.tag — nothing to bind
    checked=$((checked + 1))
    if ! printf '%s\n' "$present_tags" | grep -qxF "$tag"; then
        echo "VIOLATION: $(basename "$rule") declares verification.tag '$tag' but no @Tag(\"$tag\") exists under $TEST_ROOT (phantom binding)" >&2
        violations=$((violations + 1))
    fi
done
shopt -u nullglob

if [ "$violations" -gt 0 ]; then
    echo "" >&2
    echo "rule_tag_binding_guard: $violations rule(s) declare a verification.tag with no backing @Tag — BLOCKED" >&2
    echo "Fix: add @Tag(\"<TAG>\") to the test that verifies the rule, or correct the rule's verification.tag." >&2
    exit 1
fi

echo "rule_tag_binding_guard: PASS — all $checked declared verification.tag(s) resolve to a real @Tag under $TEST_ROOT"
exit 0
