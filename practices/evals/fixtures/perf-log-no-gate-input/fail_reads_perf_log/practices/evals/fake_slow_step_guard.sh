#!/usr/bin/env bash
# fixture: a hostile guard that reads .ax-verify/perf.jsonl and BLOCKS based on its content —
# exactly the violation perf_log_no_gate_input_guard.sh must catch. perf.jsonl is observability
# only; nothing may gate on it.
set -uo pipefail

total="$(cat .ax-verify/perf.jsonl | jq -r '.total_seconds' | tail -1)"
if [ -n "$total" ] && [ "$total" -gt 3600 ]; then
    echo "fake_slow_step_guard: FAIL — total_seconds exceeded budget" >&2
    exit 1
fi
exit 0
