#!/usr/bin/env bash
# practices/evals/test_tag_task_coverage_guard.sh
# 2026-06-01 adversarial audit (verification-teeth / spec-impl-test lens) — closes the
# @Tag→per-domain-task hard-gate escape that verification_checklist_task_coverage_guard
# [58] and rule_tag_binding_guard [64] both MISS.
#
# [58] binds  build.gradle.kts test{Domain} task  ↔  verification-checklist.yaml command.
# [64] binds  rule.verification.tag               →  a real @Tag somewhere in src/test.
# Neither binds the OTHER direction at the TEST-CLASS level: a test class whose every
# @Tag is consumed by NO includeTags(...) runs only under the aggregate `./gradlew test`
# step — which verification-checklist.yaml marks advisory:true (WARN, not FAIL). So a
# regression in that class CANNOT fail the Iron Law completion gate.
#
# The 2026-06-01 audit found 9 such escaping classes (7 common/* primitives +
# MdcCorrelationIdIT + OAuthFullFlowTest), each carrying only an orphan @Tag
# (COMMON_*, OBSERVABILITY, OAUTH-FLOW) absent from every includeTags(...).
#
# THE INVARIANT (class-centric, not tag-centric — a per-item @Tag like PAYMENT-AUTHZ-001
# is consumed INDIRECTLY when its class also carries the family umbrella @Tag("PAYMENT")
# that some includeTags names):
#
#   every test class under backend/src/test that declares @Test AND at least one @Tag
#   MUST carry at least one @Tag value that some includeTags(...) in build.gradle.kts
#   consumes — otherwise it escapes every per-domain (hard-gate) task.
#
# Scope/boundary: classes with @Test but ZERO @Tag are intentionally NOT flagged
# (abstract bases / shared IT support pulled in transitively); this guard targets the
# precise "tagged-but-unconsumed" escape the audit proved real.
#
# Exit: 0 PASS · 1 at least one test class escapes every per-domain task · 2 usage/setup.
#
# Usage:
#   bash practices/evals/test_tag_task_coverage_guard.sh
#   bash practices/evals/test_tag_task_coverage_guard.sh --test-root DIR --gradle FILE

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

TEST_ROOT="$REPO_ROOT/backend/src/test"
GRADLE="$REPO_ROOT/backend/build.gradle.kts"
while [ $# -gt 0 ]; do
    case "$1" in
        --test-root) TEST_ROOT="$2"; shift 2 ;;
        --test-root=*) TEST_ROOT="${1#--test-root=}"; shift ;;
        --gradle) GRADLE="$2"; shift 2 ;;
        --gradle=*) GRADLE="${1#--gradle=}"; shift ;;
        *) echo "test_tag_task_coverage_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

[ -d "$TEST_ROOT" ] || { echo "test_tag_task_coverage_guard: test root '$TEST_ROOT' not found — nothing to check"; exit 0; }
[ -f "$GRADLE" ]    || { echo "test_tag_task_coverage_guard: gradle file '$GRADLE' not found" >&2; exit 2; }

# Tags consumed by some includeTags(...). Flatten newlines first so a multi-line
# includeTags("A", "B", ... ) call is captured whole (grep is line-oriented).
INCLUDE_TAGS="$(tr '\n' ' ' < "$GRADLE" \
    | grep -oE 'includeTags\([^)]*\)' \
    | grep -oE '"[^"]+"' | tr -d '"' | sort -u)"

if [ -z "$INCLUDE_TAGS" ]; then
    echo "test_tag_task_coverage_guard: no includeTags(...) found in $GRADLE — cannot evaluate coverage" >&2
    exit 2
fi

escapes=0
checked=0
while IFS= read -r f; do
    grep -q '@Test' "$f" || continue
    tags="$(grep -oE '@Tag\("[^"]+"\)' "$f" | sed -E 's/@Tag\("([^"]+)"\)/\1/' | sort -u)"
    [ -z "$tags" ] && continue          # @Test but no @Tag — out of scope (see header)
    checked=$((checked + 1))
    consumed=0
    while IFS= read -r t; do
        [ -z "$t" ] && continue
        if printf '%s\n' "$INCLUDE_TAGS" | grep -Fxq "$t"; then consumed=1; break; fi
    done <<< "$tags"
    if [ "$consumed" -eq 0 ]; then
        echo "VIOLATION: $(basename "$f") declares @Tag(s) [$(echo $tags | tr '\n' ',' | sed 's/,$//')] but none is consumed by any includeTags(...) in build.gradle.kts" >&2
        echo "  → this class runs ONLY under the advisory aggregate './gradlew test', escaping every per-domain hard gate." >&2
        echo "  Fix: add a consumed tag to the class (e.g. its domain umbrella @Tag), or wire its tag into a per-domain task's includeTags(...)." >&2
        escapes=$((escapes + 1))
    fi
done < <(find "$TEST_ROOT" -name '*.java')

if [ "$escapes" -gt 0 ]; then
    echo "" >&2
    echo "test_tag_task_coverage_guard: $escapes/$checked tagged test class(es) escape every per-domain task — BLOCKED" >&2
    exit 1
fi

echo "test_tag_task_coverage_guard: PASS — all $checked tagged test classes carry a tag consumed by some includeTags(...)"
exit 0
