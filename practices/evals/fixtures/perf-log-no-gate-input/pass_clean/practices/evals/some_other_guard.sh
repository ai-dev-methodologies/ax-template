#!/usr/bin/env bash
# fixture: an unrelated guard whose comments MENTION perf.jsonl (documenting the invariant that
# it is observability-only) without ever reading it. Comment-only mentions must not trigger
# perf_log_no_gate_input_guard.sh.
#
# perf.jsonl is a sidecar; this guard neither writes nor reads it, and no gate should.
set -uo pipefail
echo "some_other_guard: PASS — unrelated to perf.jsonl"
exit 0
