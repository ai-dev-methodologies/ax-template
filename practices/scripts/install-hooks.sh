#!/usr/bin/env bash
# practices/scripts/install-hooks.sh — activate local enforcement.
#
# Run once per clone:
#   bash practices/scripts/install-hooks.sh
#
# Effects:
#   - Sets git core.hooksPath = .githooks
#   - Marks .githooks/pre-commit executable
#   - Prints a smoke-test reminder
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

if [[ ! -d .githooks ]]; then
    echo "ERROR: .githooks/ directory not found. Are you in the repo root?" >&2
    exit 1
fi

git config core.hooksPath .githooks
chmod +x .githooks/pre-commit
[[ -f .githooks/pre-push ]] && chmod +x .githooks/pre-push
[[ -f .githooks/commit-msg ]] && chmod +x .githooks/commit-msg

echo "[install-hooks] git core.hooksPath = .githooks"
echo "[install-hooks] pre-commit is executable"
[[ -f .githooks/pre-push ]] && echo "[install-hooks] pre-push is executable"
[[ -f .githooks/commit-msg ]] && echo "[install-hooks] commit-msg is executable"
echo ""
echo "Stage gates wired:"
echo "  commit-msg:"
echo "    0. private_boundary_guard.sh --commit-msg-file (R26/P2-15) — blocks a"
echo "       fork-receiver identifier (.ax-private-markers) in the commit message"
echo "  pre-commit:"
echo "    1. 4 binary guards (spec_ref / substance / time_decay / evidence)"
echo "    2. when backend/.../practices/ changes — ./gradlew testPractices"
echo "  pre-push:"
echo "    3. when backend/ or practices/ changes — full regression"
echo "       (./gradlew testPractices testAsvs testCrud)"
echo "    4. R25 — completion_checklist_recency_guard.sh (49th hard guard)"
echo "       requires bash practices/scripts/verify-completion.sh to have"
echo "       been run on the current HEAD"
echo ""
echo "R25 — MANDATORY before declaring task done:"
echo "  bash practices/scripts/verify-completion.sh"
echo "  (or, for AI agents:  bash practices/scripts/verify-and-fix-loop.sh)"
echo ""
echo "Optional Claude Code hook integration:"
echo "  See practices/scripts/HOOKS-GUIDE.md"
echo ""
echo "Uninstall: git config --unset core.hooksPath"
