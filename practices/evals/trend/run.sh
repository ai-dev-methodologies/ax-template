#!/usr/bin/env bash
# practices/evals/trend/run.sh — P2-C5 trend dashboard.
# Reads the per-date reports in practices/evals/reports/{YYYY-MM-DD}.md and emits a
# single rolling summary in practices/evals/trend/dashboard.md showing per-axis movement
# over time. Advisory only; never exits non-zero.
set -uo pipefail

cd "$(dirname "$0")/../../.."

OUT="practices/evals/trend/dashboard.md"
mkdir -p "$(dirname "$OUT")"

python3 - "$OUT" <<'PY'
import pathlib, sys, re, datetime, json

reports = sorted(pathlib.Path("practices/evals/reports").glob("*.md"))
if not reports:
    pathlib.Path(sys.argv[1]).write_text(
        "# Trend Dashboard (advisory)\n\n_No reports yet — run `practices/evals/run.sh`._\n"
    )
    print("no reports")
    sys.exit(0)

rows = []
axis_re = re.compile(r"^\|\s*(detection|outcome|reference|portability|drift)\s*\|\s*(\S+)\s*\|", re.IGNORECASE)
for r in reports:
    date = r.stem
    text = r.read_text(errors="ignore")
    axes = {}
    for line in text.splitlines():
        m = axis_re.match(line)
        if m:
            axes[m.group(1).lower()] = m.group(2)
    rows.append((date, axes))

lines = ["# Trend Dashboard (advisory)\n",
         f"_Generated {datetime.datetime.now(datetime.timezone.utc).isoformat(timespec='seconds')}Z from {len(rows)} report(s)._\n",
         "",
         "| Date | detection | outcome | reference | portability | drift |",
         "|------|-----------|---------|-----------|-------------|-------|"]
for date, ax in rows:
    lines.append(f"| {date} | {ax.get('detection','-')} | {ax.get('outcome','-')} | {ax.get('reference','-')} | {ax.get('portability','-')} | {ax.get('drift','-')} |")
lines.append("\n_Advisory: weighted-score composites are signal, not gates. Hard gates remain spec_ref + substance + time_decay + testPractices._\n")

pathlib.Path(sys.argv[1]).write_text("\n".join(lines))
print(f"trend dashboard: {sys.argv[1]} ({len(rows)} reports)")
PY
exit 0
