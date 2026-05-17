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

echo "[install-hooks] git core.hooksPath = .githooks"
echo "[install-hooks] pre-commit is executable"
[[ -f .githooks/pre-push ]] && echo "[install-hooks] pre-push is executable"
echo ""
echo "Stage gates wired:"
echo "  pre-commit:"
echo "    1. 4 binary guards (spec_ref / substance / time_decay / evidence)"
echo "    2. when archive/backend-reference/.../practices/ changes — ./gradlew testPractices"
echo "  pre-push:"
echo "    3. when archive/backend-reference/ or practices/ changes — full regression"
echo "       (./gradlew testPractices testAsvs testCrud)"
echo ""
echo "Uninstall: git config --unset core.hooksPath"
