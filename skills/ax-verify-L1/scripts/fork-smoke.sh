#!/usr/bin/env bash
# skills/ax-verify-L1/scripts/fork-smoke.sh
# SP5.5: Pass-through to canonical fork-receiver smoke in verify/.
# Allows /ax-verify-L1 to invoke the smoke via its scripts/ table.
exec "$(cd "$(dirname "$0")/../../.." && pwd)/verify/fork-receiver-smoke.sh" "$@"
