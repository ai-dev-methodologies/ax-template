#!/usr/bin/env bash
# PASS fixture guard — stands in for the ~15 real catalog guards that parse yaml through
# PyYAML with no yq fallback. Never executed by the coverage guard (verify-completion.sh
# is invoked with --dry-run); it exists so the reachability scan has a real dependent file
# on disk, in the location the preflight heuristic recognises (practices/evals/).
set -uo pipefail

# Fail closed: no parser => nothing verified => exit 2 ("cannot verify"), never 0.
if ! command -v python3 >/dev/null 2>&1 || ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "mini_yaml_guard: BLOCK - cannot verify: python3 + PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

python3 - "$@" <<'PY'
import sys
import yaml
print("mini_yaml_guard fixture:", yaml.safe_load("ok: true"))
PY
