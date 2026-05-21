#!/usr/bin/env bash
# practices/scripts/verify-and-fix-loop.sh — R25 retry orchestrator.
#
# Wraps practices/scripts/verify-completion.sh in a bounded retry loop:
#   - max 3 attempts
#   - between attempts, prints the failing step's fix_playbook and pauses for
#     the AI agent / persona to apply a fix
#   - exits 0 only when verify-completion.sh exits 0
#
# This script does NOT auto-edit files. The fix is the AI agent's
# responsibility — this script just enforces the discipline of:
#   "fail → read playbook → fix → re-verify → repeat (max 3)".
#
# After 3 failed attempts, exits 1 and prompts escalation. This matches the
# R25 brief: "verify-completion.sh가 fail → fix_playbook 따라 자동 시도 (max 3회)".
#
# Usage:
#   bash practices/scripts/verify-and-fix-loop.sh                 # interactive
#   bash practices/scripts/verify-and-fix-loop.sh --non-interactive
#                       # for CI: just attempt verify, no human-pause loop
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

MAX_ATTEMPTS=3
INTERACTIVE=1

while [ $# -gt 0 ]; do
    case "$1" in
        --max-attempts) MAX_ATTEMPTS="$2"; shift 2 ;;
        --max-attempts=*) MAX_ATTEMPTS="${1#--max-attempts=}"; shift ;;
        --non-interactive) INTERACTIVE=0; shift ;;
        --help|-h) sed -n '2,21p' "$0"; exit 0 ;;
        *) echo "verify-and-fix-loop: unknown arg: $1" >&2; exit 2 ;;
    esac
done

attempt=1
while [ "$attempt" -le "$MAX_ATTEMPTS" ]; do
    echo ""
    echo "════════════════════════════════════════════════════════════════════"
    echo "  verify-and-fix-loop — attempt $attempt of $MAX_ATTEMPTS"
    echo "════════════════════════════════════════════════════════════════════"

    set +e
    bash "$REPO_ROOT/practices/scripts/verify-completion.sh"
    rc=$?
    set -e

    if [ "$rc" -eq 0 ]; then
        echo ""
        echo "verify-and-fix-loop: PASS on attempt $attempt"
        exit 0
    fi

    if [ "$attempt" -ge "$MAX_ATTEMPTS" ]; then
        echo ""
        echo "verify-and-fix-loop: FAIL — exhausted $MAX_ATTEMPTS attempts"
        echo "Escalation required. Do NOT loop further without human review."
        echo "Consult: practices/MAINTAINER.md and the failing fix_playbook above."
        exit 1
    fi

    if [ "$INTERACTIVE" -eq 0 ]; then
        echo ""
        echo "verify-and-fix-loop: --non-interactive set, exiting after first FAIL"
        exit 1
    fi

    echo ""
    echo "verify-completion.sh exited non-zero. Read the fix_playbook above."
    echo "After applying the fix, press Enter to retry, or Ctrl-C to abort."
    echo -n "▶ "
    read -r _
    attempt=$((attempt + 1))
done

# Unreachable.
exit 1
