#!/usr/bin/env bash
# skills/ax-verify/scripts/_legacy-call-compat.sh — backward-compat dispatcher.
#
# Preserves the original /ax-verify call surface:
#   /ax-verify all            → run-all.sh (full suite)
#   /ax-verify guards         → practices/evals/run-all-guards.sh
#   /ax-verify backend        → run-backend.sh
#   /ax-verify frontend-unit  → run-frontend-unit.sh
#   /ax-verify e2e            → run-e2e.sh
#
# New subcommands added in SP29:
#   /ax-verify policy-check [args]   → policy-check.sh [args]
#   /ax-verify evidence-fetch [args] → evidence-fetch.sh [args]
#   /ax-verify explain [args]        → explain.sh [args]
#
# Usage:
#   bash _legacy-call-compat.sh <subcommand> [<subcommand-args...>]
#
# This file is intentionally thin — it dispatches and exits. All logic
# lives in the individual scripts.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

if [ $# -eq 0 ]; then
    cat <<'USAGE'
Usage: ax-verify <subcommand> [<args>]

Subcommands:
  all                        Run full verification suite (guards + backend + frontend + e2e)
  guards                     Run all 6 guard scripts only
  backend                    Run Gradle backend tests only
  frontend-unit              Run Vitest unit tests only
  e2e                        Run Playwright E2E tests only
  policy-check [--domain X]  List rules applicable to a domain tag (F13)
  evidence-fetch [--all]     Check evidence freshness of catalog rules (F14)
  explain <rule-id>          Show full explanation for a rule (F15)

Examples:
  ax-verify all
  ax-verify policy-check --domain persistence
  ax-verify evidence-fetch --all
  ax-verify explain PRACTICES-PERS-005
USAGE
    exit 1
fi

SUBCOMMAND="$1"
shift

case "$SUBCOMMAND" in
    # ── Legacy routes (preserved) ──────────────────────────────────────────
    all)
        exec bash "$SCRIPT_DIR/run-all.sh" "$@"
        ;;
    guards)
        exec bash "$REPO_ROOT/practices/evals/run-all-guards.sh" "$@"
        ;;
    backend)
        exec bash "$SCRIPT_DIR/run-backend.sh" "$@"
        ;;
    frontend-unit|frontend_unit)
        exec bash "$SCRIPT_DIR/run-frontend-unit.sh" "$@"
        ;;
    e2e)
        exec bash "$SCRIPT_DIR/run-e2e.sh" "$@"
        ;;
    # ── SP29 subcommands ───────────────────────────────────────────────────
    policy-check|policy_check)
        exec bash "$SCRIPT_DIR/policy-check.sh" "$@"
        ;;
    evidence-fetch|evidence_fetch)
        exec bash "$SCRIPT_DIR/evidence-fetch.sh" "$@"
        ;;
    explain)
        exec bash "$SCRIPT_DIR/explain.sh" "$@"
        ;;
    # ── Help / unknown ─────────────────────────────────────────────────────
    help|-h|--help)
        exec "$0"  # re-invoke with no args to print usage
        ;;
    *)
        echo "ax-verify: unknown subcommand '$SUBCOMMAND'" >&2
        echo "Run 'ax-verify help' for available subcommands." >&2
        exit 1
        ;;
esac
