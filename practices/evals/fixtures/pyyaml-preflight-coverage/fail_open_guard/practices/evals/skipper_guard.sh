#!/usr/bin/env bash
# FAIL fixture guard - the fail-open shape this catalog must not ship: when PyYAML is
# unavailable it announces a SKIP and exits 0, which every caller reading exit codes
# records as a PASS for a guard that verified nothing.
set -uo pipefail

if ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "skipper_guard: SKIP - PyYAML not installed"
    exit 0
fi

python3 - <<'PYEOF'
import yaml
print("skipper_guard:", yaml.safe_load("ok: true"))
PYEOF
