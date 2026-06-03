#!/usr/bin/env bash
# verify_loop.sh — the verification feedback loop for code generation.
#
#   recommend (lint-clean catalog) ─▶ generate code (codify) ─▶ verify (ax block-lint)
#         ▲                                                              │
#         └──────────── blocklist any lint-failing component ◀──────────┘
#
# Each round: recommend components for every service plan, codify them into real ax blocks, and run the
# ax own-blocks lint. Any component whose generated code fails is added to lint_blocklist.json; the next
# round's recommender excludes it. Loops until every recommended component generates lint-clean code
# (gen_verify exits 0) — the ax "iterate until the verifier is green" discipline applied to code-gen.
set -uo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
MAX="${1:-6}"

for i in $(seq 1 "$MAX"); do
  echo "════ verify-loop iter $i ════"
  python3 "$DIR/recommend.py" >/dev/null 2>&1
  if python3 "$DIR/gen_verify.py"; then
    echo "✓ verify-loop CONVERGED at iter $i — every recommended component generates lint-clean ax code"
    rm -rf "$DIR/__pycache__"
    exit 0
  fi
  echo "  (a component was blocklisted; re-recommending with the lint-clean catalog)"
done

echo "✗ verify-loop did NOT converge within $MAX iterations (see lint_blocklist.json)"
rm -rf "$DIR/__pycache__"
exit 1
