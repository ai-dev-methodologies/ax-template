#!/usr/bin/env bash
# fixture: mirrors the legitimate WRITE-only shape of verify-completion.sh's perf.jsonl sidecar.
# perf.jsonl is only ever appended to (O_APPEND, no read-back), so this file must NOT trigger
# perf_log_no_gate_input_guard.sh.
set -uo pipefail

AUDIT_DIR="$1"
PERF_LOG="$AUDIT_DIR/perf.jsonl"

python3 -I -S -c '
import json, os, sys
path, ts = sys.argv[1:3]
doc = {"ts": ts, "note": "sidecar only — no gate reads this file"}
fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_APPEND | os.O_NOFOLLOW, 0o644)
with os.fdopen(fd, "a", encoding="utf-8") as fh:
    fh.write(json.dumps(doc) + "\n")
' "$PERF_LOG" "2026-01-01T00:00:00Z"
