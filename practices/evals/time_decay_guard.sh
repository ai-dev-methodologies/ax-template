#!/usr/bin/env bash
# practices/evals/time_decay_guard.sh — drift axis hard gate.
# Reads {catalog}/upstream/_MANIFEST.yaml; any snapshot whose fetched_at is older than
# the threshold (default 90 days) → BLOCK. This is the "stale upstream" tripwire that
# prevents rules from anchoring to outdated documentation.
#
# Usage:
#   bash practices/evals/time_decay_guard.sh                       # default catalog=practices
#   bash practices/evals/time_decay_guard.sh --catalog practices-react
set -uo pipefail

CATALOG="${CATALOG:-practices}"
while [ $# -gt 0 ]; do
    case "$1" in
        --catalog) CATALOG="$2"; shift 2 ;;
        --catalog=*) CATALOG="${1#--catalog=}"; shift ;;
        *) echo "time_decay_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fail closed: this guard verifies through PyYAML ──────────────────────────
# The freshness check parses upstream/_MANIFEST.yaml with PyYAML. Without the parser the
# python body dies on ImportError and the guard exits 1 — the same code it uses for a
# REAL staleness violation, so a caller cannot tell a tooling failure from a finding.
# Exit 2 = "cannot verify". Pinned by practices/evals/pyyaml_preflight_coverage_guard.sh [95].
if ! command -v python3 >/dev/null 2>&1 || ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "time_decay_guard: BLOCK — cannot verify: python3 + PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CATALOG_DIR="$REPO_ROOT/$CATALOG"

if [ ! -d "$CATALOG_DIR" ]; then
    echo "time_decay_guard: catalog '$CATALOG' not found at $CATALOG_DIR — nothing to check"
    exit 0
fi

cd "$CATALOG_DIR"

MANIFEST="upstream/_MANIFEST.yaml"
THRESHOLD_DAYS="${TIME_DECAY_THRESHOLD_DAYS:-90}"

if [[ ! -f "$MANIFEST" ]]; then
    echo "time_decay_guard: no manifest yet — nothing to check"
    exit 0
fi

# ── templates/ walk extension (§4.10) ────────────────────────────────────────
# Zero-scan guard: if templates/ exists but produces zero matching files → FAIL ZERO_SCAN.
TEMPLATES_DIR="$REPO_ROOT/templates"
if [[ -d "$TEMPLATES_DIR" ]]; then
    templates_count=0
    while IFS= read -r f; do
        [[ -f "$f" ]] && templates_count=$((templates_count + 1))
    done < <(find "$TEMPLATES_DIR" \
        -name "*.md" -o -name "*.tsx" -o -name "*.ts" -o -name "*.yaml" -o -name "*.java" 2>/dev/null)
    if [[ $templates_count -eq 0 ]]; then
        echo "time_decay_guard: ZERO_SCAN — templates/ exists but no scannable files found — merge BLOCKED" >&2
        exit 1
    fi
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
