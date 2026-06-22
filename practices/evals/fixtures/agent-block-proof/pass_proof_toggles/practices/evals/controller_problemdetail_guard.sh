#!/usr/bin/env bash
# FIXTURE stand-in for controller_problemdetail_guard.sh.
# Mirrors the real guard's contract just enough to exercise the proof toggle:
#   exit 1 (BLOCK) if any in-scope controller returns a Map body;
#   exit 0 (PASS)  if it returns ProblemDetail.
# Used only by the agent_block_proof_guard fixtures — never the real catalog.
set -uo pipefail
ROOT="."
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT="$2"; shift 2 ;;
        --root=*) ROOT="${1#--root=}"; shift ;;
        *) shift ;;
    esac
done
CTRL="$ROOT/backend/src/main/java/com/ax/template/authblueprint/demo/DemoController.java"
[ -f "$CTRL" ] || { echo "fixture guard: no controller to scan (zero-scan)"; exit 1; }
if grep -q 'Map<String, String>' "$CTRL"; then
    echo "fixture guard: @ExceptionHandler returns Map<String,String> — BLOCKED"
    exit 1
fi
echo "fixture guard: @ExceptionHandler returns ProblemDetail — PASS"
exit 0
