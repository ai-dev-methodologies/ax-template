#!/usr/bin/env bash
# practices/evals/fixtures/cross_trio/cross_trio_skip_report_run.sh
#
# BACKLOG P2-45 — non-vacuity harness for cross_trio_guard's tsx-less-L4 SKIP REPORT.
#
# The P2-45 deliverable is not an exit code (a tsx-less L4 dir must NOT fail here — see
# the judgement in cross_trio_guard.sh's header), it is that the skip stops being SILENT.
# An exit-code fixture therefore cannot prove it: pass/ already exits 0 and would exit 0
# with the reporting removed. This harness asserts the OUTPUT instead, so deleting the
# SKIP print or the summary count flips it 0 → 1.
#
# Asserted against fixtures/cross_trio/pass_skip_reported/:
#   1. the guard still exits 0 (no false positive on a legitimately backend-only vertical)
#   2. stdout names the skipped dir on a SKIP line
#   3. the SKIP line states the reason (no .tsx) AND the guard that owns the axis
#   4. the summary line carries the skip count
#
# Exit: 0 all four hold · 1 the skip is silent/underspecified (P2-45 regression) · 2 setup.
#
# WHY NO fixture_kill_manifest.yaml ([87]) ENTRY
#   [87] proves a FAIL fixture non-vacuous: original guard on fixture → 1, neutered → 0.
#   P2-45 has no fail fixture by design (a tsx-less L4 dir must not fail here), so there is
#   no 1 → 0 flip to register. This harness is the equivalent kill-proof in the opposite
#   direction — deleting the SKIP print or the summary count flips it 0 → 1. Verified by
#   mutation on a throwaway copy, 2026-07-29.
#
# Usage:
#   bash practices/evals/fixtures/cross_trio/cross_trio_skip_report_run.sh
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
GUARD="$REPO_ROOT/practices/evals/cross_trio_guard.sh"
FIXTURE="$SCRIPT_DIR/pass_skip_reported"

if [[ ! -f "$GUARD" ]]; then
    echo "cross_trio_skip_report: BLOCK — guard not found at $GUARD" >&2
    exit 2
fi
if [[ ! -d "$FIXTURE/templates/L4/backend-only-vertical" ]]; then
    echo "cross_trio_skip_report: BLOCK — fixture missing its tsx-less L4 dir" >&2
    exit 2
fi

out="$(bash "$GUARD" --root "$FIXTURE" 2>&1)"
rc=$?
echo "$out"

fail=0
if [[ $rc -ne 0 ]]; then
    echo "cross_trio_skip_report: FAIL — guard exited $rc; a tsx-less L4 dir must be skipped, not failed (that axis is owned by full_trio_artifact_completeness_guard)" >&2
    fail=1
fi
if ! grep -q 'SKIP: templates/L4/backend-only-vertical/' <<< "$out"; then
    echo "cross_trio_skip_report: FAIL — the tsx-less L4 dir was skipped SILENTLY (no SKIP line naming it) — P2-45 regression" >&2
    fail=1
fi
if ! grep -q 'SKIP:.*no \.tsx.*full_trio_artifact_completeness_guard' <<< "$out"; then
    echo "cross_trio_skip_report: FAIL — the SKIP line does not state the reason and the guard that owns the completeness axis" >&2
    fail=1
fi
if ! grep -q '1 skipped without \.tsx' <<< "$out"; then
    echo "cross_trio_skip_report: FAIL — the summary line does not carry the skip count" >&2
    fail=1
fi

if [[ $fail -ne 0 ]]; then
    echo "cross_trio_skip_report: FAIL" >&2
    exit 1
fi
echo "cross_trio_skip_report: PASS — tsx-less L4 dir skipped, reported by name, reason + owning guard stated, counted in the summary"
exit 0
