#!/usr/bin/env bash
# practices/evals/rule_verification_binding_guard.sh
# Generalises rule_tag_binding_guard (which only covers gradle_task+tag rules) to EVERY
# practices rule: a rule's `verification:` block MUST resolve to exactly one RUNNABLE
# mechanism, or explicitly declare the sanctioned advisory tier. Closes the tight-verdict
# A1 "advisory tail" — rules that claimed verification via only pattern/source/notes,
# enforced by nobody, silently passing every gate.
#
# A rule (with a `verification:` block) is BOUND iff it has ONE of:
#   (a) gradle_task + tag           → @Tag existence enforced by rule_tag_binding_guard
#   (b) guard | guard_script | eval → names a real practices/evals/*_guard.sh
#   (c) pattern + (passing_fixture|failing_fixture) → fixture-checked pattern
#   (d) type: review | status: manual               → sanctioned human-judgment tier
# Anything else (only pattern/source/notes, no review declaration) = UNBOUND → BLOCK.
#
# Scope: the Java `practices` catalog (verification → JUnit @Tag / shell guard). The React
# catalog verifies via ESLint/fixtures and already declares review tiers, so it is out of
# scope here.
#
# Exit: 0 all bound · 1 unbound rule(s) · 2 usage.
# Usage: bash practices/evals/rule_verification_binding_guard.sh [--rules-dir DIR] [--evals-dir DIR]

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
RULES_DIR="${REPO_ROOT}/practices/rules"
EVALS_DIR="${REPO_ROOT}/practices/evals"
while [ $# -gt 0 ]; do
    case "$1" in
        --rules-dir) RULES_DIR="$2"; shift 2 ;;
        --evals-dir) EVALS_DIR="$2"; shift 2 ;;
        *) echo "rule_verification_binding_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
[ -d "$RULES_DIR" ] || { echo "rule_verification_binding_guard: rules dir '$RULES_DIR' not found"; exit 0; }

frontmatter() { awk '/^---[[:space:]]*$/{n++; next} n==1{print} n>=2{exit}' "$1"; }

unbound=0; checked=0
shopt -s nullglob
for rule in "$RULES_DIR"/*.md; do
    [[ "$(basename "$rule")" == ".gitkeep" ]] && continue
    fm="$(frontmatter "$rule")"
    echo "$fm" | grep -qE '^verification:' || continue
    checked=$((checked + 1))
    bound=""
    if echo "$fm" | grep -qE '^[[:space:]]*gradle_task:' && echo "$fm" | grep -qE '^[[:space:]]*tag:'; then bound="gradle_task+tag"; fi
    if [ -z "$bound" ]; then
        g="$(echo "$fm" | grep -oE '(guard|guard_script|eval):[[:space:]]*[A-Za-z0-9_./-]+\.sh' | grep -oE '[A-Za-z0-9_-]+\.sh' | head -1)"
        if [ -n "$g" ] && [ -f "$EVALS_DIR/$g" ]; then bound="guard:$g"; fi
    fi
    if [ -z "$bound" ] && echo "$fm" | grep -qE '^[[:space:]]*pattern:' && echo "$fm" | grep -qE '^[[:space:]]*(passing_fixture|failing_fixture):'; then bound="pattern+fixture"; fi
    if [ -z "$bound" ] && echo "$fm" | grep -qE 'type:[[:space:]]*review|status:[[:space:]]*manual'; then bound="review-tier"; fi

    if [ -z "$bound" ]; then
        echo "UNBOUND: $(basename "$rule") — verification block resolves to no runnable mechanism and declares no review tier" >&2
        unbound=$((unbound + 1))
    fi
done
shopt -u nullglob

if [ "$unbound" -gt 0 ]; then
    echo "" >&2
    echo "rule_verification_binding_guard: $unbound/$checked rule(s) UNBOUND — BLOCKED" >&2
    echo "Fix: bind to gradle_task+tag / guard / pattern+fixture, OR add 'type: review' to declare the sanctioned human-judgment tier." >&2
    exit 1
fi
echo "rule_verification_binding_guard: PASS — all $checked verification blocks resolve to a runnable mechanism or a declared review tier"
exit 0
