#!/usr/bin/env bash
# practices/evals/quote_match_check.sh — advisory drift probe.
#
# For every rule with `upstream_id`-shape evidence, verify that the `quote` field is a
# substring of the referenced snapshot's stripped text. Drift (quote no longer present)
# means either the rule was rewritten without updating the quote, or the snapshot has
# moved on and the rule's anchoring is now stale.
#
# Advisory only — never exits ≠ 0. Sentinel CI runs it with continue-on-error.
set -uo pipefail

cd "$(dirname "$0")/.."

python3 - <<'PY'
import pathlib, re, sys, yaml

def strip_html(t: str) -> str:
    t = re.sub(r"<script[^>]*>.*?</script>", " ", t, flags=re.S | re.I)
    t = re.sub(r"<style[^>]*>.*?</style>",   " ", t, flags=re.S | re.I)
    t = re.sub(r"<[^>]+>", " ", t)
    return re.sub(r"\s+", " ", t).strip()

snapshot_cache: dict[str, str | None] = {}

def snapshot_text(sid: str) -> str | None:
    if sid not in snapshot_cache:
        p = pathlib.Path("upstream") / f"{sid}.snapshot.md"
        snapshot_cache[sid] = (
            strip_html(p.read_text(errors="ignore")).lower() if p.exists() else None
        )
    return snapshot_cache[sid]

warnings: list[str] = []
ok_count = 0

for rule_path in sorted(pathlib.Path("rules").glob("*.md")):
    if rule_path.name == "_template.md":
        continue
    text = rule_path.read_text()
    if not text.startswith("---"):
        continue
    parts = text.split("---", 2)
    if len(parts) < 3:
        continue
    try:
        fm = yaml.safe_load(parts[1]) or {}
    except yaml.YAMLError as e:
        warnings.append(f"{rule_path.name}: frontmatter parse error: {e}")
        continue

    for i, ev in enumerate(fm.get("evidence", []) or []):
        if not isinstance(ev, dict):
            continue
        sid = ev.get("upstream_id")
        if not sid:
            continue
        quote = str(ev.get("quote", "")).strip()
        if not quote:
            warnings.append(f"{rule_path.name} #{i}: upstream_id={sid!r} has empty quote")
            continue
        snap = snapshot_text(sid)
        if snap is None:
            warnings.append(
                f"{rule_path.name} #{i}: snapshot upstream/{sid}.snapshot.md not present "
                f"(run practices/upstream/fetch.sh)"
            )
            continue
        if quote.lower() not in snap:
            warnings.append(
                f"{rule_path.name} #{i}: quote NOT substring-matched in {sid} — possible drift"
            )
        else:
            ok_count += 1

print("## quote-match-check (advisory)")
print()
if warnings:
    print(f"⚠ {len(warnings)} warning(s), {ok_count} OK:")
    for w in warnings:
        print(f"  - {w}")
else:
    print(f"✓ {ok_count} upstream_id quote(s) substring-matched in their referenced snapshots.")
print()
print("_Advisory only — never blocks merges. Hard gates are spec_ref + substance + time_decay + evidence._")
PY
exit 0
