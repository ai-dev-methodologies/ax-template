#!/usr/bin/env bash
# practices/evals/time_decay_guard.sh — drift axis hard gate.
# Reads practices/upstream/_MANIFEST.yaml; any snapshot whose fetched_at is older than
# the threshold (default 90 days) → BLOCK. This is the "stale upstream" tripwire that
# prevents rules from anchoring to outdated documentation.
set -uo pipefail

cd "$(dirname "$0")/.."

MANIFEST="upstream/_MANIFEST.yaml"
THRESHOLD_DAYS="${TIME_DECAY_THRESHOLD_DAYS:-90}"

if [[ ! -f "$MANIFEST" ]]; then
    echo "time_decay_guard: no manifest yet — nothing to check"
    exit 0
fi

python3 - "$MANIFEST" "$THRESHOLD_DAYS" <<'PY'
import datetime, sys, yaml, pathlib

mf_path = pathlib.Path(sys.argv[1])
threshold = int(sys.argv[2])
data = yaml.safe_load(mf_path.read_text()) or {}
now = datetime.datetime.now(datetime.timezone.utc)

violations = []
for s in data.get("snapshots", []):
    fa_raw = s.get("fetched_at", "")
    try:
        fa = datetime.datetime.fromisoformat(fa_raw.replace("Z", "+00:00"))
    except Exception:
        violations.append(f"{s.get('id','?')}: unparseable fetched_at={fa_raw!r}")
        continue
    age = (now - fa).days
    if age > threshold:
        violations.append(f"{s.get('id','?')}: {age}d > {threshold}d")

if violations:
    print(f"VIOLATION [time_decay > {threshold}d]:", file=sys.stderr)
    for v in violations:
        print(f"  - {v}", file=sys.stderr)
    print(f"time_decay_guard: {len(violations)} stale snapshot(s) — merge BLOCKED", file=sys.stderr)
    sys.exit(1)

print(f"time_decay_guard: all snapshots within {threshold}d threshold")
sys.exit(0)
PY
