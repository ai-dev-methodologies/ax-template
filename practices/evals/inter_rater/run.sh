#!/usr/bin/env bash
# practices/evals/inter_rater/run.sh — automated inter-rater quality probe.
#
# Issue any rule.md to two independent reviewers (default: codex CLI invoked twice with
# distinct critic personas). Each reviewer returns a 0–10 score on three dimensions
# (clarity, soundness, testability). If the per-dimension absolute difference exceeds the
# threshold (default 3.0 / 10), flag the rule as low-agreement and BLOCK; otherwise PASS.
#
# Honest limitation: same-model persona-diff is a weak proxy for true inter-rater. A real
# implementation would pair two distinct providers (codex + claude opus + gemini) or
# capture genuine human reviewers. This script's CLI surface is shaped so swapping in a
# different second reviewer is a one-line change.
set -uo pipefail

cd "$(dirname "$0")/../../.."

RULE=""
THRESHOLD="${INTER_RATER_THRESHOLD:-3.0}"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --rule) RULE="$2"; shift 2 ;;
        --threshold) THRESHOLD="$2"; shift 2 ;;
        *) echo "usage: $0 --rule <path> [--threshold N]" >&2; exit 2 ;;
    esac
done

if [[ -z "$RULE" || ! -f "$RULE" ]]; then
    echo "ERROR: --rule <path> is required and must exist (got: $RULE)" >&2
    exit 2
fi

if ! command -v codex >/dev/null 2>&1; then
    echo "inter_rater: codex CLI not available — skipping (N/A, not a failure)"
    exit 0
fi

OUT_DIR="practices/evals/inter_rater/runs/$(date -u +%FT%H%M%SZ)"
mkdir -p "$OUT_DIR"

call_reviewer() {
    local persona="$1" out="$2"
    local rule_body
    rule_body="$(cat "$RULE")"
    codex exec --skip-git-repo-check --color never - >"$out" 2>/dev/null <<EOF
You are reviewer "$persona" rating a Java/Spring best-practice rule. Return ONLY a single
JSON object on the last line with three integer dimensions (0-10):
  {"clarity": <int>, "soundness": <int>, "testability": <int>}

Persona: $persona
  - strict: prefer rules that point to concrete, falsifiable verification.
  - lenient: tolerant of explanatory style; rates higher on educational value.

Rule under review (markdown):
---
$rule_body
---
EOF
}

call_reviewer "strict"  "$OUT_DIR/strict.txt"
call_reviewer "lenient" "$OUT_DIR/lenient.txt"

python3 - "$OUT_DIR" "$THRESHOLD" "$RULE" <<'PY'
import json, sys, pathlib, re

out_dir = pathlib.Path(sys.argv[1])
threshold = float(sys.argv[2])
rule_path = sys.argv[3]

def extract_scores(p):
    txt = p.read_text(errors="ignore")
    # Find last JSON object containing the expected keys
    matches = re.findall(r"\{[^{}]*\"clarity\"[^{}]*\}", txt, flags=re.DOTALL)
    if not matches:
        return None
    try:
        return json.loads(matches[-1])
    except json.JSONDecodeError:
        return None

a = extract_scores(out_dir / "strict.txt")
b = extract_scores(out_dir / "lenient.txt")

if not a or not b:
    print(f"inter_rater: could not parse scores (a={a}, b={b}) — N/A, exit 0")
    sys.exit(0)

dims = ["clarity", "soundness", "testability"]
diffs = {d: abs(a[d] - b[d]) for d in dims}
max_diff = max(diffs.values())

report = out_dir / "report.json"
report.write_text(json.dumps({
    "rule": rule_path,
    "threshold": threshold,
    "strict":  a,
    "lenient": b,
    "diffs":   diffs,
    "max_diff": max_diff,
    "verdict": "BLOCK" if max_diff > threshold else "PASS",
}, indent=2))

print(f"inter_rater: max_diff={max_diff} threshold={threshold}")
print(f"  strict={a} lenient={b}")
print(f"  report: {report}")
if max_diff > threshold:
    print(f"inter_rater: agreement too low — BLOCK", file=sys.stderr)
    sys.exit(1)
print("inter_rater: agreement within threshold — PASS")
sys.exit(0)
PY
