#!/usr/bin/env bash
# practices/evals/verification_checklist_task_coverage_guard.sh
# Closes the IDW/IMW-class enforcement gap surfaced by the 2026-05-30 consistency
# audit (Phase 2 candidate C6): backend/build.gradle.kts registered the test tasks
# `testEmailOutbox` (includeTags EMAIL) and `testCommonAdvice` (includeTags
# COMMON_ADVICE) as green, catalog-load-bearing per-domain suites, but neither was
# listed in practices/verification-checklist.yaml's per-domain-tests step — the
# ONLY hard (non-advisory) gate that verify-completion.sh runs. The sole fallback
# was the aggregate-regression step, which is advisory:true (WARN, not FAIL), so a
# regression in either suite could NOT fail the Iron Law completion contract.
#
# This guard mechanically enforces the invariant the catalog vision requires
# ("single command binary pass/fail" + zero-tolerance enforcement):
#
#   every `tasks.register<Test>("testXxx")` task in backend/build.gradle.kts MUST
#   appear as a `./gradlew testXxx` command in practices/verification-checklist.yaml
#
# so a future domain author cannot register a per-domain test task and silently
# leave it out of the completion gate.
#
# Exit codes:
#   0 — every registered test{Domain} task is covered by the checklist
#   1 — at least one registered task is missing from the checklist
#   2 — usage error / missing required source files
#
# Usage:
#   bash practices/evals/verification_checklist_task_coverage_guard.sh
#   bash practices/evals/verification_checklist_task_coverage_guard.sh --root DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "verification_checklist_task_coverage_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"

GRADLE="$REPO_ROOT/backend/build.gradle.kts"
CHECKLIST="$REPO_ROOT/practices/verification-checklist.yaml"

[ -f "$GRADLE" ]    || { echo "verification_checklist_task_coverage_guard: missing $GRADLE" >&2; exit 2; }
[ -f "$CHECKLIST" ] || { echo "verification_checklist_task_coverage_guard: missing $CHECKLIST" >&2; exit 2; }

# Registered per-domain test tasks: tasks.register<Test>("testXxx") { ... }
registered="$(grep -oE 'tasks\.register<Test>\("test[A-Za-z0-9]+"' "$GRADLE" \
    | sed -E 's/.*\("//; s/"//' | sort -u)"

# Tasks the checklist actually runs: ./gradlew testXxx (any per-domain step).
covered="$(grep -oE '\./gradlew test[A-Za-z0-9]+' "$CHECKLIST" \
    | sed -E 's#\./gradlew ##' | sort -u)"

missing=0
for task in $registered; do
    if ! printf '%s\n' "$covered" | grep -Fxq "$task"; then
        echo "VIOLATION: backend/build.gradle.kts registers '$task' but"
        echo "  practices/verification-checklist.yaml does NOT run it."
        echo "  → a regression in '$task' would escape the verify-completion hard gate."
        echo "  Fix: add a './gradlew $task' command to the per-domain-tests step"
        echo "       (expected_exit: 0, or advisory: true if it is a known-flaky"
        echo "        cross-cutting suite like testIntegration/testPortability)."
        missing=$((missing + 1))
    fi
done

if [ "$missing" -gt 0 ]; then
    echo "verification_checklist_task_coverage_guard: $missing registered test task(s) escape the Iron Law gate — BLOCKED" >&2
    exit 1
fi

echo "verification_checklist_task_coverage_guard: all registered test{Domain} tasks are covered by verification-checklist.yaml"
exit 0
