#!/usr/bin/env bash
# practices/evals/agents_md_toc_disk_truth_guard.sh — R13 25th hard guard (TD-033).
# Binary-verifies AGENTS.md TOC matches disk truth: re-runs generate_agents.sh
# and diffs against committed AGENTS.md (whole file + defensive TOC slice).
# Detects: non-idempotent generator, hand-edited TOC, L4/recipe/verdict adds
# not surfaced. --root DIR fixture mode diffs committed.md vs regenerated.md.
# Exit: 0 PASS · 1 drift · 2 usage error.
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "agents_md_toc_disk_truth_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
slice_toc() { awk '/^# Catalog TOC/,EOF' "$1"; }
if [ -n "$ROOT_OVERRIDE" ]; then
    A="$ROOT_OVERRIDE/committed.md"; B="$ROOT_OVERRIDE/regenerated.md"
    [ -f "$A" ] && [ -f "$B" ] || { echo "FAIL: missing committed.md or regenerated.md in $ROOT_OVERRIDE" >&2; exit 2; }
    diff -q <(slice_toc "$A") <(slice_toc "$B") > /dev/null && exit 0 || exit 1
fi
cd "$SCRIPT_DIR/.."   # → practices/
SNAP="$(mktemp -t agents_md_committed.XXXXXX)"
trap 'rm -f "$SNAP" "${SNAP}.a" "${SNAP}.b" "${SNAP}.log"' EXIT
cp AGENTS.md "$SNAP"
bash generate_agents.sh > "${SNAP}.log" || { echo "FAIL: generate_agents.sh non-zero exit" >&2; cp "$SNAP" AGENTS.md; exit 1; }
if ! diff -q "$SNAP" AGENTS.md > /dev/null; then
    echo "FAIL: generate_agents.sh non-idempotent OR committed AGENTS.md drifted from disk truth" >&2
    cp "$SNAP" AGENTS.md
    exit 1
fi
slice_toc "$SNAP" > "${SNAP}.a"; slice_toc AGENTS.md > "${SNAP}.b"
if ! diff -q "${SNAP}.a" "${SNAP}.b" > /dev/null; then
    echo "FAIL: TOC body drift between committed and regenerated AGENTS.md" >&2
    exit 1
fi
echo "PASS: AGENTS.md TOC matches disk truth"
