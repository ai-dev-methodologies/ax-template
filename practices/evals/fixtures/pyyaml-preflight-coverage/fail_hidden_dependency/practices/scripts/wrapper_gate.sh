#!/usr/bin/env bash
# FAIL fixture wrapper — a PyYAML-dependent gate that does NOT live under evals/, so the
# preflight's path heuristic misses it. Never executed (verify-completion.sh runs with
# --dry-run); it exists so the transitive reachability scan has a real dependent file
# in the blind spot.
set -uo pipefail

# Fail closed: no parser => nothing verified => exit 2 ("cannot verify"), never 0.
if ! command -v python3 >/dev/null 2>&1 || ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "wrapper_gate: BLOCK - cannot verify: python3 + PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

python3 - "$@" <<'PY'
import sys
import yaml
print("wrapper_gate fixture:", yaml.safe_load("ok: true"))
PY
